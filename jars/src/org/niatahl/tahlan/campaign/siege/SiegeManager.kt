package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import java.util.Random
import exerelin.campaign.SectorManager
import exerelin.utilities.NexConfig
import org.niatahl.tahlan.utils.ModCompat
import org.niatahl.tahlan.utils.TahlanSettings
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.Utils.txt
import org.apache.log4j.Logger
import kotlin.math.max
import kotlin.math.min

/**
 * Pattern A manager (fleet_behavior.md): permanent BaseCampaignEventListener + EveryFrameScript.
 * Registered once in TahlanModPlugin.onGameLoad; stored in sector memory under SIEGE_MANAGER_KEY
 * so fleet listeners and assignment AIs can find it without a direct reference.
 *
 * Implements tasks 3.1–3.3, 4.1–4.3, 5.1–5.7, 6.2, 7a.1–7a.6, 8.1–8.2.
 */
class SiegeManager : BaseCampaignEventListener(true), EveryFrameScript {

    // --- Per-siege state (nested class; serialized as part of the manager) ---
    class SiegeData(
        val id: String,
        val targetSystem: StarSystemAPI,
        val sourceMarket: MarketAPI,
        val primaryTargetMarket: MarketAPI?
    ) {
        enum class Stage { INBOUND, BESIEGING, BROKEN, LIFTED, SUCCEEDED }

        var stage = Stage.INBOUND
        var intensity = 1f      // captured at launch; scales command/escort/raid budgets
        var commandFleet: CampaignFleetAPI? = null
        val escortFleets  = mutableListOf<CampaignFleetAPI>()
        val raidFleets    = mutableListOf<CampaignFleetAPI>()
        val conditionedMarkets = mutableListOf<MarketAPI>()

        var siegeHealth         = SiegeConfig.SIEGE_HEALTH_MAX
        var commandCR           = 1.0f
        var commandFleetPresent = true
        var withdrawalOrdered   = false
        var garrisonMarket: MarketAPI? = null

        var daysSinceLastLoss = 0f
        var daysElapsed       = 0f
        var captureProgress   = 0f                  // unified subjugation meter, 0..CAPTURE_PROGRESS_MAX
        var lastPressureMult  = 1f                  // last strangle multiplier (for the intel display)
        var raidCooldown      = SiegeConfig.RAID_INTERVAL_DAYS

        var intel: SiegeIntel? = null
        var playerBountyAccrued = 0f
    }

    // --- Fields ---
    private val activeSieges = mutableListOf<SiegeData>()
    private val spawnTimer   = IntervalUtil(SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN, SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX)
    // Pending broken-checks deferred from battle callbacks (safe to do inline would risk CME
    // if resolveSiege touches campaign listeners while CampaignEngine iterates them). Holds siege
    // ids only — every deferred step reads its state off the SiegeData, not off the loss report.
    // @Transient + lazy getter: list is session-only (no cross-save meaning), and guards against
    // the field being null when loaded from a save that predates it (Java deserialization skips
    // constructors, leaving new val fields as null).
    @Transient
    private var _pendingKills: MutableList<String>? = null
    private val pendingKills: MutableList<String>
        get() = _pendingKills ?: mutableListOf<String>().also { _pendingKills = it }
    // Slow cadence for maintainPressureConditions. Same @Transient + lazy-getter shape as
    // _pendingKills, and for the same reason: a plain non-null IntervalUtil field added to the
    // serialized manager would deserialize as null on any save that predates it, then NPE on tick.
    @Transient
    private var _conditionSweepTimer: IntervalUtil? = null
    private val conditionSweepTimer: IntervalUtil
        get() = _conditionSweepTimer ?: IntervalUtil(2f, 3f).also { _conditionSweepTimer = it }
    // Captured when the manager is first created (≈ campaign start, or feature-enable on an existing
    // save); persists with the manager. Used to scale siege intensity off elapsed campaign time.
    private val gameStartTimestamp: Long = Global.getSector().clock.timestamp

    // --- EveryFrameScript ---

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)

        // Runs before the feature gate: the leak it repairs is from conditions applied in the past,
        // and self-disables after the first pass.
        sweepLeakedConditionMods()

        if (!TahlanSettings.ENABLE_SIEGE) {
            if (activeSieges.isNotEmpty()) tearDown()
            return
        }

        // The campaign is run out of Lucifron. If Legio has lost their capital, call off every
        // in-flight siege (fleets disperse home) — new launches are gated in tryLaunchSiege.
        if (activeSieges.isNotEmpty() && !legioHoldsCapital()) {
            for (siege in activeSieges.toList()) resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED)
            LOG.info("Tahlan siege: capital Lucifron lost — all active sieges lifted")
            return
        }

        // Flush losses that were enqueued during battle callbacks (safe to resolve here,
        // outside any CampaignEngine listener iteration)
        if (pendingKills.isNotEmpty()) {
            for (siegeId in pendingKills.toList()) {
                flushKill(siegeId)
            }
            pendingKills.clear()
        }

        pruneDeadSieges()
        advanceHealthModel(days)

        // Keep the pressure condition pointed at whoever is actually hostile right now. Slow interval:
        // this tracks diplomacy and market ownership, which move on the order of months.
        conditionSweepTimer.advance(days)
        if (conditionSweepTimer.intervalElapsed()) maintainPressureConditions()

        // Re-derive the spawn cadence from config so a mid-save LunaLib frequency-slider change takes
        // effect — the manager (and its spawnTimer) persists across saves, so the construction-time
        // interval would otherwise be frozen forever. Guarded on a real change: setInterval() rolls a
        // new currInterval and resets elapsed, so calling it every tick would stall the timer.
        if (spawnTimer.minInterval != SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN ||
            spawnTimer.maxInterval != SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX) {
            spawnTimer.setInterval(SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN, SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX)
        }

        // The launch cadence measures the *gap between sieges*, not launch-to-launch: only tick the
        // timer while no siege is ongoing, so a long-running siege can't burn down the interval and
        // trigger the next launch the moment it resolves. Sieges therefore run strictly one at a time.
        if (activeSieges.isEmpty()) {
            spawnTimer.advance(days)
            if (spawnTimer.intervalElapsed()) {
                tryLaunchSiege()
            }
        }
    }

    // --- Callbacks from SiegeFleetListener ---

    /**
     * Called from SiegeFleetListener (inside a battle callback) — books the losses a siege fleet took
     * and enqueues the siege for safe resolution in the next advance() tick, avoiding CME in
     * CampaignEngine's listener iteration.
     *
     * [lostFp] is the fleet points the fleet *shed in this engagement*, not its spawn-time size, so a
     * fleet ground down over several battles contributes attrition as it happens instead of only on
     * the killing blow; [destroyed] flags that killing blow. [playerFraction] is the battle's
     * player-involvement fraction and scales the bounty share.
     */
    fun onSiegeFleetLosses(siegeId: String, lostFp: Float, isCommand: Boolean, destroyed: Boolean, playerFraction: Float) {
        val siege = findSiege(siegeId) ?: return

        // Stat updates are safe inline (just field writes); complex resolution is deferred.
        // Partial losses reset the loss clock as well as kills do: a siege actively being ground down
        // is not stalled, so it should neither recover CR nor trip the mop-up stall timeout.
        siege.daysSinceLastLoss = 0f

        if (isCommand && destroyed) {
            // The command fleet's health contribution is solely its chunk — NOT the per-FP path —
            // so a kill matches a withdrawal (triggerWithdrawal) in health effect (per design: killing
            // vs. driving off differs only in reward), and always leaves the escort residual to mop up.
            // CR strain is skipped: it's moot once the command fleet is dead (governs regen / its own
            // strength / withdrawal floor, all irrelevant now).
            if (siege.commandFleetPresent) {
                siege.commandFleetPresent = false
                val cmdChunk = SiegeConfig.SIEGE_HEALTH_MAX * SiegeConfig.COMMAND_HEALTH_SHARE
                siege.siegeHealth = max(0f, siege.siegeHealth - cmdChunk)
            }
        } else if (isCommand) {
            // Command fleet bloodied but alive: bleeding the flagship force strains coordination, so it
            // takes FP-weighted CR strain like any other loss. No health damage — the command's
            // contribution to siege health is the one-shot chunk above, never the per-FP path — and no
            // direct meter knockback either: the CR drop already brakes the subjugation meter through
            // the commandCR multiplier in advanceHealthModel.
            siege.commandCR = max(0f, siege.commandCR - lostFp * SiegeConfig.STRAIN_K)
        } else {
            // Escort/blockade/raid: uncapped per-FP health damage (floored at 0) + FP-weighted CR strain.
            val healthDmg = lostFp / SiegeConfig.HEALTH_PER_FP
            siege.siegeHealth = max(0f, siege.siegeHealth - healthDmg)
            siege.commandCR = max(0f, siege.commandCR - lostFp * SiegeConfig.STRAIN_K)
            // Knock the subjugation meter back (a command kill already freezes it via commandFleetPresent).
            siege.captureProgress = max(0f, siege.captureProgress - lostFp * SiegeConfig.CAPTURE_KNOCKBACK_PER_FP)
        }

        // Bounty, scaled by how much of the battle was the player's own work. Escort losses pay out
        // incrementally per FP shed; the command bounty stays a single kill-only lump sum (driving the
        // flagship force off is not the same as putting it down).
        val involvement = playerFraction.coerceIn(0f, 1f)
        if (involvement > 0f) {
            val bounty = when {
                isCommand && destroyed -> SiegeConfig.COMMAND_FLEET_BOUNTY * involvement
                isCommand              -> 0f
                else                   -> lostFp * SiegeConfig.ESCORT_BOUNTY_PER_FP * involvement
            }
            if (bounty > 0f) {
                siege.playerBountyAccrued += bounty
                siege.intel?.addPlayerBounty(bounty)
            }
        }

        // Surface the kill as a one-time factor (display-only; the meter was already knocked back).
        // Kills only: a row per bruising engagement would spam the factor table, and partial losses
        // are already legible in the bar receding.
        if (destroyed) {
            val knockback = if (isCommand) 0f else lostFp * SiegeConfig.CAPTURE_KNOCKBACK_PER_FP
            siege.intel?.addFleetKill(knockback, isCommand)
        }

        // Defer CR application and broken-check to advance() — safe side of the battle callback boundary
        pendingKills.add(siegeId)
    }

    private fun flushKill(siegeId: String) {
        val siege = findSiege(siegeId) ?: return
        applyCommandCR(siege)
        checkBroken(siege)
        // Decapitation before arrival: the stage only leaves INBOUND via onCommandFleetArrived, which
        // is driven by the command fleet's own SiegeAssignmentAI — so a command fleet lost en route
        // would leave the siege stuck INBOUND forever (which also blocks every future launch, since
        // spawnTimer only ticks while activeSieges is empty). Abort the whole expedition instead:
        // escorts disperse home and the accrued bounty pays out. Covers both the battle-kill path and
        // the non-battle despawn path (both funnel through onSiegeFleetLosses -> pendingKills).
        if (siege.stage == SiegeData.Stage.INBOUND && !siege.commandFleetPresent) {
            resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN)
        }
    }

    fun onSiegeFleetDespawned(siegeId: String, fleetFp: Float, isCommand: Boolean) {
        val siege = findSiege(siegeId) ?: return
        if (isCommand && siege.commandFleetPresent && !siege.withdrawalOrdered) {
            // Unexpected non-battle despawn of command fleet — treat as killed for health accounting
            onSiegeFleetLosses(siegeId, fleetFp, true, destroyed = true, playerFraction = 0f)
        }
        if (isCommand) siege.commandFleetPresent = false
    }

    /** Called by SiegeAssignmentAI on arrival at target system. */
    fun onCommandFleetArrived(siegeId: String, fleet: CampaignFleetAPI) {
        val siege = findSiege(siegeId) ?: return
        siege.stage = SiegeData.Stage.BESIEGING
        applyPressureCondition(siege)
        // Give the travel escorts a job: screen the command fleet. They arrived on a 1000-day
        // GO_TO_LOCATION and would otherwise idle at the system center. Do this BEFORE spawnBlockadeFleets,
        // which appends the (separately-tasked) blockade fleets to the same escortFleets list.
        for (escort in siege.escortFleets.filter { it.isAlive }) {
            escort.clearAssignments()
            escort.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, fleet, 9999f,
                txt("siege_assign_screen").format(fleet.name))
        }
        spawnBlockadeFleets(siege)
        siege.intel?.syncProgress(siege)
    }

    /** Polled by SiegeAssignmentAI to know if withdrawal has been ordered. */
    fun isSiegeWithdrawing(siegeId: String): Boolean =
        findSiege(siegeId)?.withdrawalOrdered == true

    // --- BaseCampaignEventListener --- fleet despawn pruning ---

    override fun reportFleetDespawned(fleet: CampaignFleetAPI, reason: FleetDespawnReason, param: Any?) {
        super.reportFleetDespawned(fleet, reason, param)
        if (!fleet.memoryWithoutUpdate.contains(FLEET_SIEGE_ID_KEY)) return
        val siegeId = fleet.memoryWithoutUpdate.getString(FLEET_SIEGE_ID_KEY) ?: return
        val siege = findSiege(siegeId) ?: return
        // Remove from tracking lists (backup pruning — triple-check in pruneDeadSieges handles the rest)
        siege.escortFleets.remove(fleet)
        siege.raidFleets.remove(fleet)
        if (siege.commandFleet == fleet) siege.commandFleet = null
    }

    // --- Health model (called each advance tick) ---

    private fun advanceHealthModel(days: Float) {
        val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO)
        for (siege in activeSieges.toList()) {
            // Both live stages age; anything else is already resolved and awaiting pruning.
            if (siege.stage != SiegeData.Stage.INBOUND && siege.stage != SiegeData.Stage.BESIEGING) continue
            siege.daysElapsed += days

            if (siege.stage == SiegeData.Stage.INBOUND) {
                // Travel backstop: an expedition that never arrives would otherwise sit INBOUND
                // forever, which also blocks every future launch (spawnTimer only ticks while no
                // siege is active). Everything below is besieging-only.
                if (siege.daysElapsed >= SiegeConfig.INBOUND_TIMEOUT_DAYS) {
                    LOG.info("Tahlan siege: ${siege.id} never reached ${siege.targetSystem.baseName} — lifted on travel timeout")
                    resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED)
                }
                continue
            }

            // CR recovery: no losses within window → recover toward 1.0
            siege.daysSinceLastLoss += days
            if (siege.daysSinceLastLoss >= SiegeConfig.CR_RECOVERY_DELAY_DAYS) {
                val oldCR = siege.commandCR
                siege.commandCR = min(1.0f, siege.commandCR + SiegeConfig.CR_RECOVERY_RATE_PER_DAY * days)
                if (siege.commandCR != oldCR) applyCommandCR(siege)
            }

            // Health regen: only while command fleet alive, rate scales with commandCR
            val cmdAlive = siege.commandFleetPresent && siege.commandFleet?.isAlive == true
            if (cmdAlive) {
                val regen = SiegeConfig.HEALTH_REGEN_PER_DAY_BASE * siege.commandCR * days
                siege.siegeHealth = min(SiegeConfig.SIEGE_HEALTH_MAX, siege.siegeHealth + regen)
            }

            // Withdrawal at CR floor
            if (!siege.withdrawalOrdered && siege.commandCR <= SiegeConfig.COMMAND_CR_WITHDRAWAL_FLOOR) {
                triggerWithdrawal(siege)
            }

            // Raid sorties — command-coordinated, like the subjugation meter below: once the command
            // fleet is gone (killed/withdrawn) the siege is in mop-up and launches no new sorties.
            if (siege.commandFleetPresent) {
                siege.raidCooldown -= days
                if (siege.raidCooldown <= 0f && siege.raidFleets.size < SiegeConfig.MAX_ACTIVE_RAID_FLEETS) {
                    siege.raidCooldown = SiegeConfig.RAID_INTERVAL_DAYS
                    spawnRaidFleet(siege)
                }
            }

            // ── Resolution checks ──

            // LIFTED: the target stopped being a valid one. Eligibility is otherwise only checked at
            // launch, so a mid-siege change of circumstance — a Nexerelin peace treaty, the market
            // decivilizing or being depopulated, or someone else taking it (including another Legio
            // force) — would otherwise still end in "subjugating" a market Legio has no quarrel with,
            // or in laying siege to a rock. All of those resolve LIFTED: the expedition packs up with
            // no capture and no scar, since none of it was earned by the siege — there is no
            // undeserved SUCCEEDED climax to be had here.
            val target = siege.primaryTargetMarket
            if (target != null) {
                val gone = !target.isInEconomy || target.hasCondition(Conditions.DECIVILIZED)
                if (gone || target.factionId == TahlanIDs.LEGIO || !legioFaction.isHostileTo(target.faction)) {
                    LOG.info("Tahlan siege: ${siege.id} target ${target.name} is no longer a valid objective — lifted")
                    resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); continue
                }
            }

            // BROKEN: siege health 0 (universal counter in both pathways)
            if (siege.siegeHealth <= 0f) { resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN); continue }

            // BROKEN: stalled mop-up. The command fleet is gone but nobody is finishing off the
            // residual fleets, so the health bar would never reach 0 and the siege (plus its pressure
            // condition) would persist indefinitely. daysSinceLastLoss resets on every siege-fleet
            // loss, partials included, so an actively-ground siege never trips this — only a genuinely
            // abandoned one.
            if (!siege.commandFleetPresent && siege.daysSinceLastLoss >= SiegeConfig.MOPUP_STALL_TIMEOUT_DAYS) {
                LOG.info("Tahlan siege: ${siege.id} mop-up stalled for ${"%.0f".format(siege.daysSinceLastLoss)} days — broken")
                resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN); continue
            }

            // Unified subjugation meter — advances in BOTH modes while the command fleet coordinates
            // the strangle (a withdrawn/destroyed command freezes commandFleetPresent). There is no
            // fixed siege lifetime anymore: both pathways are pure races between the meter filling and
            // the siege being broken. Rate is scaled by how strangled the target is and braked by CR.
            if (siege.commandFleetPresent) {
                // `target` was resolved (and re-validated) with the resolution checks above.
                val pressureMult = if (target != null) {
                    val accessibility = target.accessibilityMod.computeEffective(0f).coerceIn(0f, 1f)
                    1f + max(0f, 0.7f - accessibility)  // more strangled = faster
                } else 1f
                siege.lastPressureMult = pressureMult
                siege.captureProgress += SiegeConfig.CAPTURE_PROGRESS_PER_DAY_BASE * pressureMult * siege.commandCR * days

                if (siege.captureProgress >= SiegeConfig.CAPTURE_PROGRESS_MAX) {
                    siege.captureProgress = SiegeConfig.CAPTURE_PROGRESS_MAX
                    // Branch on whether the target is actually capturable under Nex; everything else
                    // (no Nex, no target, story-protected, or a Nex-locked core market) resolves via
                    // the lasting no-Nex scar against the target.
                    if (ModCompat.HAS_NEX && target != null
                        && !isNexCaptureBlocked(target) && !isNexProtected(target)) {
                        attemptNexCapture(siege); continue
                    } else {
                        applyNoNexAftermath(siege)
                        resolveSiege(siege, SiegeIntel.SiegeOutcome.SUCCEEDED); continue
                    }
                }
            }

            siege.intel?.syncProgress(siege)
        }
    }

    // --- Nex capture (task 7a.4–7a.6) ---

    private fun attemptNexCapture(siege: SiegeData) {
        val target = siege.primaryTargetMarket ?: run {
            resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); return
        }
        // Re-check story protection at transfer time (task 7a.5)
        if (isNexProtected(target)) {
            resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); return
        }
        try {
            val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO)
            val oldOwner = Global.getSector().getFaction(target.factionId)
            // transferMarket(market, newOwner, oldOwner, playerInvolved, isCapture, factionsToNotify, repChangeStrength)
            SectorManager.transferMarket(target, legioFaction, oldOwner, false, true, emptyList<String>(), 0f)
        } catch (e: Exception) {
            LOG.warn("Tahlan siege: Nex market transfer failed — ${e.message}")
            resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); return
        }
        // Hand the garrison target to the command fleet via its OWN memory: resolveSiege is about to
        // drop this siege from tracking, so the assignment AI must read it independently of the manager.
        siege.garrisonMarket = target
        siege.commandFleet?.memoryWithoutUpdate?.set(FLEET_GARRISON_MARKET_KEY, target.id)
        resolveSiege(siege, SiegeIntel.SiegeOutcome.SUCCEEDED, keepCommandFleet = true)
    }

    // --- No-Nex aftermath scar (task 4.1–4.3) ---

    /**
     * Apply the lasting "you let the siege win" consequence to the primary target market. Must be
     * called BEFORE resolveSiege (which clears conditionedMarkets). Acts on the primary target only —
     * other in-system markets already endured the blockade. Adds the self-expiring half-siege scar
     * condition and disrupts the market's core industries for the scar's duration.
     */
    private fun applyNoNexAftermath(siege: SiegeData) {
        val target = siege.primaryTargetMarket ?: return
        if (!target.hasCondition(TahlanIDs.SIEGE_AFTERMATH_CONDITION_ID)) {
            target.addCondition(TahlanIDs.SIEGE_AFTERMATH_CONDITION_ID)
        }
        // Disrupt core industries (skip population/spaceport infrastructure) for the scar duration.
        val random = Random()
        for (industry in target.industries) {
            if (!industry.canBeDisrupted() || !industry.isIndustry()) continue
            val dur = SiegeConfig.AFTERMATH_DURATION_DAYS * StarSystemGenerator.getNormalRandom(random, 1f, 1.25f)
            industry.setDisrupted(dur, true)  // useMax: only ever extend an existing disruption
        }
        LOG.info("Tahlan siege: no-Nex aftermath applied to ${target.name}")
    }

    // --- Withdrawal ---

    private fun triggerWithdrawal(siege: SiegeData) {
        siege.withdrawalOrdered = true
        if (siege.commandFleetPresent) {
            siege.commandFleetPresent = false
            // The command's coordination leaves with it: drop its health share (as on a kill, minus
            // the reward) so residual mop-up can drive the siege to 0 via the health bar.
            val cmdChunk = SiegeConfig.SIEGE_HEALTH_MAX * SiegeConfig.COMMAND_HEALTH_SHARE
            siege.siegeHealth = max(0f, siege.siegeHealth - cmdChunk)
        }
        // Cut the screening escorts loose. onCommandFleetArrived parked them on ORBIT_AGGRESSIVE around
        // the command FLEET; left alone they trail it home into Legio space and idle there forever once
        // it despawns — still alive in escortFleets, so the residual could never be mopped up and the
        // pressure condition would stick. Re-anchor them in-system instead. Matched by IDENTITY of the
        // assignment target, not action text; the blockade fleets target jump points and are untouched.
        siege.commandFleet?.let { cmd ->
            val anchor = siege.targetSystem.jumpPoints.firstOrNull() ?: siege.targetSystem.center
            for (escort in siege.escortFleets.filter { it.isAlive }) {
                if (escort.currentAssignment?.target !== cmd) continue
                escort.clearAssignments()
                escort.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, anchor, 9999f,
                    txt("siege_assign_blockade").format(anchor.name))
            }
        }
        // The SiegeAssignmentAI polls isSiegeWithdrawing() and calls orderReturn() on itself
        siege.intel?.notifyWithdrawal()
    }

    // --- Siege resolution ---

    private fun resolveSiege(
        siege: SiegeData,
        outcome: SiegeIntel.SiegeOutcome,
        keepCommandFleet: Boolean = false
    ) {
        siege.stage = when (outcome) {
            SiegeIntel.SiegeOutcome.BROKEN    -> SiegeData.Stage.BROKEN
            SiegeIntel.SiegeOutcome.LIFTED    -> SiegeData.Stage.LIFTED
            SiegeIntel.SiegeOutcome.SUCCEEDED -> SiegeData.Stage.SUCCEEDED
        }
        removePressureCondition(siege)
        siege.intel?.resolve(outcome)
        disperseFleets(siege, keepCommandFleet)
        activeSieges.remove(siege)
    }

    // --- Pressure condition (tasks 6.2) ---

    private fun applyPressureCondition(siege: SiegeData) {
        val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO)
        for (market in Global.getSector().economy.marketsCopy) {
            if (market.isHidden) continue
            if (market.starSystem != siege.targetSystem) continue
            if (!legioFaction.isHostileTo(Global.getSector().getFaction(market.factionId))) continue
            if (!market.hasCondition(TahlanIDs.SIEGE_CONDITION_ID)) {
                market.addCondition(TahlanIDs.SIEGE_CONDITION_ID)
            }
            if (!siege.conditionedMarkets.contains(market)) siege.conditionedMarkets.add(market)
        }
    }

    private fun removePressureCondition(siege: SiegeData) {
        for (market in siege.conditionedMarkets) {
            try { market.removeCondition(TahlanIDs.SIEGE_CONDITION_ID) } catch (_: Exception) {}
        }
        siege.conditionedMarkets.clear()
    }

    /**
     * Re-validate the pressure condition against the current state of the besieged system.
     * [applyPressureCondition] only ever runs once, on arrival, so on its own the condition sticks to
     * markets that made peace with Legio or changed hands mid-siege, and never reaches markets that
     * turned hostile (or were founded) after the expedition arrived. Called from [advance] on the slow
     * [conditionSweepTimer] cadence.
     */
    private fun maintainPressureConditions() {
        val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO)
        for (siege in activeSieges) {
            if (siege.stage != SiegeData.Stage.BESIEGING) continue
            for (market in siege.conditionedMarkets.toList()) {
                val stillValid = market.isInEconomy &&
                        market.factionId != TahlanIDs.LEGIO &&
                        legioFaction.isHostileTo(market.faction)
                if (stillValid) continue
                try { market.removeCondition(TahlanIDs.SIEGE_CONDITION_ID) } catch (_: Exception) {}
                siege.conditionedMarkets.remove(market)
            }
            // Idempotent (hasCondition-guarded), so this is purely additive: it picks up markets in the
            // besieged system that have newly become hostile to Legio or newly changed hands into a
            // hostile faction's column.
            applyPressureCondition(siege)
        }
    }

    /**
     * One-time repair for saves made before [SiegeCondition] / [SiegeAftermathCondition] learned to
     * unapply their stat mods. `BaseMarketConditionPlugin.unapply()` is empty, so removing either
     * condition used to leave its flat accessibility/stability/hazard mods on the market forever.
     * Sweeps every market that does NOT currently carry the condition and drops any orphaned flat mod
     * keyed to it — mod keys are `MarketConditionAPI.idForPluginModifications`, i.e.
     * `"<conditionId>_<uid>"`, so a prefix match is exact. The leak never shipped in a release, so
     * this can be dropped once dev saves have rolled over.
     */
    private fun sweepLeakedConditionMods() {
        val sector = Global.getSector()
        if (sector.memoryWithoutUpdate.getBoolean(SIEGE_LEAK_SWEPT_KEY)) return
        sector.memoryWithoutUpdate.set(SIEGE_LEAK_SWEPT_KEY, true, 0f)

        var cleaned = 0
        for (market in sector.economy.marketsCopy) {
            val stale = listOf(TahlanIDs.SIEGE_CONDITION_ID, TahlanIDs.SIEGE_AFTERMATH_CONDITION_ID)
                .filterNot { market.hasCondition(it) }
            if (stale.isEmpty()) continue
            // Snapshot the key sets first — unmodifyFlat mutates the backing maps.
            for (key in market.accessibilityMod.flatBonuses.keys.toList()) {
                if (stale.any { key.startsWith(it) }) { market.accessibilityMod.unmodifyFlat(key); cleaned++ }
            }
            for (key in market.stability.flatMods.keys.toList()) {
                if (stale.any { key.startsWith(it) }) { market.stability.unmodifyFlat(key); cleaned++ }
            }
            for (key in market.hazard.flatMods.keys.toList()) {
                if (stale.any { key.startsWith(it) }) { market.hazard.unmodifyFlat(key); cleaned++ }
            }
        }
        if (cleaned > 0) LOG.info("Tahlan siege: swept $cleaned leaked siege-condition stat mod(s)")
    }

    // --- Fleet spawning ---

    /**
     * The Legio siege campaign is directed from their capital, Lucifron. It counts as "held" only
     * while the market still exists, is Legio-owned, and is a going concern (not decivilized). Losing
     * Lucifron — to the player, to a Nex invasion, or to decivilization — shuts the campaign down:
     * no new launches (tryLaunchSiege) and any in-flight sieges are lifted (advance).
     */
    private fun legioHoldsCapital(): Boolean {
        val lucifron = Global.getSector().economy.getMarket(TahlanIDs.LUCIFRON_MARKET) ?: return false
        if (lucifron.factionId != TahlanIDs.LEGIO) return false
        if (lucifron.hasCondition(Conditions.DECIVILIZED)) return false
        return true
    }

    private fun tryLaunchSiege() {
        if (!legioHoldsCapital()) return
        val source = pickSourceMarket() ?: return
        val (targetSystem, primaryMarket) = pickTargetSystem(source) ?: return
        launchSiege(source, targetSystem, primaryMarket)
    }

    /**
     * Build and register a siege on [targetSystem] from [source]. Shared by the auto-launcher
     * (tryLaunchSiege) and the TahlanSiegeStart console command. Returns the new siege, or null if the
     * command fleet could not be spawned (the only hard failure point — nothing is committed on null).
     */
    private fun launchSiege(source: MarketAPI, targetSystem: StarSystemAPI, primaryMarket: MarketAPI?): SiegeData? {
        val intensity = computeIntensity()
        val factor = SiegeConfig.intensityFactor(intensity)
        val id = "siege_${targetSystem.id}_${System.nanoTime()}"
        val siege = SiegeData(id, targetSystem, source, primaryMarket)
        siege.intensity = intensity

        // Command fleet — Blackwatch (task 4.1). Spawn BEFORE the intel entry: it is the only hard
        // failure point in the launch, so if it can't be built we must bail with nothing committed.
        // Creating the intel first would orphan it (a siege announced in the feed with no fleets in
        // the world).
        val commandFP = SiegeConfig.COMMAND_FP_BASE + SiegeConfig.COMMAND_FP_SCALE * factor
        val cmdFleet = spawnCommandFleet(source, commandFP, intensity, id)
        if (cmdFleet == null) {
            LOG.warn("Tahlan siege: aborted launch on ${targetSystem.baseName} — command fleet failed to spawn")
            return null
        }
        siege.commandFleet = cmdFleet

        // Intel entry — only now that the siege force actually exists.
        val intel = SiegeIntel(targetSystem, primaryMarket, ModCompat.HAS_NEX)
        Global.getSector().intelManager.addIntel(intel)
        siege.intel = intel

        // Initial escort fleets — standard Legio (task 4.1)
        val escortCount = (SiegeConfig.ESCORT_COUNT_BASE +
                factor * (SiegeConfig.ESCORT_COUNT_MAX - SiegeConfig.ESCORT_COUNT_BASE))
            .toInt().coerceIn(SiegeConfig.ESCORT_COUNT_BASE, SiegeConfig.ESCORT_COUNT_MAX)
        repeat(escortCount) {
            val eFP = SiegeConfig.ESCORT_FP_BASE + SiegeConfig.ESCORT_FP_SCALE * factor
            spawnEscortFleet(source, eFP, id)?.let { siege.escortFleets.add(it) }
        }

        // Give command fleet travel assignment + assignment AI (task 4.2)
        val travelDest = targetSystem.center
        cmdFleet.clearAssignments()
        cmdFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, travelDest, 1000f,
            txt("siege_assign_travel").format(targetSystem.nameWithLowercaseType))
        cmdFleet.addScript(SiegeAssignmentAI(cmdFleet, source, id))

        // Escort travel alongside
        for (escort in siege.escortFleets) {
            escort.clearAssignments()
            escort.addAssignment(FleetAssignment.GO_TO_LOCATION, travelDest, 1000f,
                txt("siege_assign_travel").format(targetSystem.nameWithLowercaseType))
        }

        activeSieges.add(siege)
        LOG.info("Tahlan siege: launched on ${targetSystem.baseName} from ${source.name} (intensity=${"%.2f".format(intensity)})")
        return siege
    }

    private fun spawnCommandFleet(source: MarketAPI, fp: Float, intensity: Float, siegeId: String): CampaignFleetAPI? {
        val sMods = (SiegeConfig.COMMAND_SMODS_BASE +
                SiegeConfig.intensityFactor(intensity) * (SiegeConfig.COMMAND_SMODS_MAX - SiegeConfig.COMMAND_SMODS_BASE))
            .toInt().coerceIn(SiegeConfig.COMMAND_SMODS_BASE, SiegeConfig.COMMAND_SMODS_MAX)
        // Inflate with Blackwatch doctrine (elite spearhead ship composition), then reassign the
        // fleet to Legio so it flies under Legio colors and uses Legio relationships. The ships are
        // already rolled by createFleet, so setFaction keeps the Blackwatch loadout intact.
        //
        // modeOverride is REQUIRED: the source market is Legio, so factionId=Blackwatch differs from
        // the market faction, which makes FleetFactoryV3 default to IMPORTED ship-pick mode — that
        // rolls only Blackwatch's meagre `shipsWhenImporting` list, not its elite roster, producing a
        // wrong/near-empty command fleet. PRIORITY_THEN_ALL forces the full Blackwatch roster. This
        // mirrors LegioHQ's off-faction patrol spawn.
        val params = FleetParamsV3(source, FleetTypes.MERC_ARMADA, fp, 0f, 0f, 0f, 0f, 0f, 0.25f)
        params.factionId = TahlanIDs.BLACKWATCH
        params.averageSMods = sMods
        params.officerNumberMult = 2f
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL

        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return null
        fleet.setFaction(TahlanIDs.LEGIO, true)
        fleet.name = txt("siege_fleet_command_name")
        tagSiegeFleet(fleet, siegeId, fp, isCommand = true)
        val loc = source.primaryEntity.location
        source.primaryEntity.containingLocation.addEntity(fleet)
        fleet.setLocation(loc.x, loc.y)
        fleet.addEventListener(SiegeFleetListener(siegeId, fp, isCommandFleet = true))
        return fleet
    }

    private fun spawnEscortFleet(source: MarketAPI, fp: Float, siegeId: String): CampaignFleetAPI? {
        val params = FleetParamsV3(source, FleetTypes.PATROL_LARGE, fp, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return null
        tagSiegeFleet(fleet, siegeId, fp, isCommand = false)
        source.primaryEntity.containingLocation.addEntity(fleet)
        fleet.setLocation(source.primaryEntity.location.x, source.primaryEntity.location.y)
        fleet.addEventListener(SiegeFleetListener(siegeId, fp, isCommandFleet = false))
        return fleet
    }

    /** Called by onCommandFleetArrived to spawn blockade fleets at jump points (task 4.3). */
    private fun spawnBlockadeFleets(siege: SiegeData) {
        for (jp in siege.targetSystem.jumpPoints) {
            val params = FleetParamsV3(siege.sourceMarket, FleetTypes.PATROL_MEDIUM,
                SiegeConfig.ESCORT_FP_BASE, 0f, 0f, 0f, 0f, 0f, 0f)
            val fleet = FleetFactoryV3.createFleet(params) ?: continue
            tagSiegeFleet(fleet, siege.id, SiegeConfig.ESCORT_FP_BASE, isCommand = false)
            if (SiegeConfig.BLOCKADE_HOSTILE_TO_TRADERS) {
                fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE_TO_ALL_TRADE_FLEETS, true)
            }
            siege.targetSystem.addEntity(fleet)
            fleet.setLocation(jp.location.x, jp.location.y)
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, jp, 9999f,
                txt("siege_assign_blockade").format(jp.name))
            fleet.addScript(SiegeBlockadeAI(fleet, jp, siege.primaryTargetMarket, siege.id))
            fleet.addEventListener(SiegeFleetListener(siege.id, SiegeConfig.ESCORT_FP_BASE, isCommandFleet = false))
            siege.escortFleets.add(fleet)
        }
    }

    /** Launch a raid sortie toward the primary target market (task 4.3). */
    private fun spawnRaidFleet(siege: SiegeData) {
        val raidTarget = (siege.primaryTargetMarket?.primaryEntity ?: siege.targetSystem.center) ?: return
        val fp = SiegeConfig.RAID_FP_BASE + SiegeConfig.RAID_FP_SCALE * SiegeConfig.intensityFactor(siege.intensity)
        val params = FleetParamsV3(siege.sourceMarket, FleetTypes.PATROL_LARGE, fp, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        tagSiegeFleet(fleet, siege.id, fp, isCommand = false)

        // Anchor on the command fleet (it holds station at a system fringe). Fall back to a jump
        // point / system center if it's momentarily unavailable — NEVER to raidTarget, which is the
        // besieged enemy planet: spawning there makes raids appear to erupt from the planet itself.
        val spawnAnchor = siege.commandFleet?.takeIf { it.isAlive }
            ?: siege.targetSystem.jumpPoints.firstOrNull()
            ?: siege.targetSystem.center
        siege.targetSystem.addEntity(fleet)
        fleet.setLocation(spawnAnchor.location.x, spawnAnchor.location.y)
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.RAID_SYSTEM, raidTarget, 30f,
            txt("siege_assign_raid").format(raidTarget.name))
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, siege.sourceMarket.primaryEntity, 60f)
        fleet.addEventListener(SiegeFleetListener(siege.id, fp, isCommandFleet = false))
        siege.raidFleets.add(fleet)
    }

    // --- Dispersal (task 5.7 + 8.1) ---

    private fun disperseFleets(siege: SiegeData, keepCommandFleet: Boolean) {
        val home = siege.sourceMarket.primaryEntity
        for (fleet in (siege.escortFleets + siege.raidFleets).filter { it.isAlive }) {
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f)
        }
        if (!keepCommandFleet) {
            siege.commandFleet?.takeIf { it.isAlive }?.let { cmd ->
                // Signal the SiegeAssignmentAI via fleet memory — it polls this flag and calls orderReturn()
                cmd.memoryWithoutUpdate.set(FLEET_RETURN_FLAG, true)
            }
        }
    }

    // --- CR application (task 5.4) ---

    private fun applyCommandCR(siege: SiegeData) {
        val fleet = siege.commandFleet?.takeIf { it.isAlive } ?: return
        val cr = siege.commandCR.coerceIn(0.1f, 1.0f)
        for (member in fleet.fleetData.membersListCopy) {
            if (!member.isFighterWing) {
                member.repairTracker.cr = cr * member.repairTracker.maxCR
            }
        }
    }

    // --- Broken check (task 5.7) ---

    private fun checkBroken(siege: SiegeData) {
        if (siege.stage == SiegeData.Stage.BESIEGING && siege.siegeHealth <= 0f) {
            resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN)
        }
    }

    // --- Pruning (task 3.3) ---

    private fun pruneDeadSieges() {
        val toRemove = mutableListOf<SiegeData>()
        for (siege in activeSieges) {
            if (siege.stage != SiegeData.Stage.INBOUND && siege.stage != SiegeData.Stage.BESIEGING) {
                toRemove.add(siege); continue
            }
            // Triple liveness check per fleet_behavior.md
            val cmdGone = siege.commandFleet?.let { f ->
                f.containingLocation == null ||
                !f.containingLocation.fleets.contains(f) ||
                !f.isAlive
            } ?: true
            // Same triple check — the middle leg catches a fleet stripped from its location's fleet
            // list mid-transition, which the two-leg version used to read as still alive.
            val noEscorts = siege.escortFleets.all { f ->
                f.containingLocation == null ||
                !f.containingLocation.fleets.contains(f) ||
                !f.isAlive
            }
            if (cmdGone && !siege.commandFleetPresent && noEscorts && siege.raidFleets.isEmpty()) {
                // All fleets gone and command already accounted for — auto-broken (mopped up).
                // Stage here is always INBOUND or BESIEGING (later stages were filtered above), and an
                // INBOUND wipe must resolve its intel too, or the entry hangs in the feed forever.
                // removePressureCondition is a safe no-op on an empty conditionedMarkets list.
                removePressureCondition(siege)
                siege.intel?.resolve(SiegeIntel.SiegeOutcome.BROKEN)
                toRemove.add(siege)
            }
        }
        activeSieges.removeAll(toRemove)
    }

    // --- Source / target picking (task 3.2) ---

    private fun pickSourceMarket(): MarketAPI? =
        Global.getSector().economy.marketsCopy
            .filter { it.factionId == TahlanIDs.LEGIO && !it.isHidden }
            .maxByOrNull { it.size }

    /**
     * Eligibility + primary-target determination for a single system, shared by the auto-picker
     * (pickTargetSystem) and the TahlanSiegeStart console command. Returns
     * (primaryTargetMarket, eligibleMarkets) if [system] is a valid siege target — has a hostile
     * non-Legio market, no existing Legio/Blackwatch presence, is not already under siege, and (under
     * Nex) has at least one non-story-protected market — else null. The primary target is the
     * worst-relation eligible market (task 3.2 + design decision).
     */
    private fun evaluateTarget(system: StarSystemAPI): Pair<MarketAPI, List<MarketAPI>>? {
        val sector = Global.getSector()
        val legioFaction = sector.getFaction(TahlanIDs.LEGIO)

        if (activeSieges.any { it.targetSystem == system }) return null

        val allMarkets = sector.economy.marketsCopy.filter { it.starSystem == system && !it.isHidden }

        // Must have at least one hostile market
        val hostileMarkets = allMarkets.filter { legioFaction.isHostileTo(sector.getFaction(it.factionId)) }
        if (hostileMarkets.isEmpty()) return null

        // Exclude systems with existing Legio presence
        if (allMarkets.any { it.factionId == TahlanIDs.LEGIO || it.factionId == TahlanIDs.BLACKWATCH }) return null

        // Nex: filter out story-protected markets; skip system if none remain eligible
        val eligibleMarkets = if (ModCompat.HAS_NEX) hostileMarkets.filter { !isNexProtected(it) } else hostileMarkets
        if (eligibleMarkets.isEmpty()) return null

        val primaryMarket = eligibleMarkets.minByOrNull { legioFaction.getRelationship(it.factionId) } ?: return null
        return primaryMarket to eligibleMarkets
    }

    private fun pickTargetSystem(source: MarketAPI): Pair<StarSystemAPI, MarketAPI?>? {
        val sector = Global.getSector()
        val legioFaction = sector.getFaction(TahlanIDs.LEGIO)

        val picker = WeightedRandomPicker<Pair<StarSystemAPI, MarketAPI?>>()

        for (system in sector.starSystems) {
            val (primaryMarket, eligibleMarkets) = evaluateTarget(system) ?: continue

            // Weight: combined market size, graded by how much Legio hates the primary target's owner.
            var weight = eligibleMarkets.sumOf { it.size.toInt() }.toFloat() * 10f
            val worstRel = legioFaction.getRelationship(primaryMarket.factionId)
            // Graded hostility: deeper hatred = likelier target (-0.5 relation → ×1.5, -1.0 → ×2.0).
            // Replaces a flat ×2 that applied to every candidate, since all candidates are hostile by
            // construction, plus an equally coarse ×1.5 step at relation < -0.5.
            weight *= 1f + max(0f, -worstRel)

            // Distance weight: prefer closer targets (inverse-distance with floor)
            val dist = Misc.getDistance(source.locationInHyperspace, system.location)
            weight *= 10000f / (dist.coerceAtLeast(1000f) + 10000f)

            picker.add(system to primaryMarket, weight)
        }

        return picker.pick()
    }

    // --- Intensity scaling (task 2.4: replaces currentCycle-206) ---

    private fun computeIntensity(): Float {
        val sector = Global.getSector()
        // Elapsed campaign time, not a hardcoded start cycle (works for any start).
        val elapsedYears = (sector.clock.getElapsedDaysSince(gameStartTimestamp) / 365f).coerceAtLeast(0f)
        val legioMarkets = sector.economy.marketsCopy.count { it.factionId == TahlanIDs.LEGIO }
        return (SiegeConfig.INTENSITY_BASE
                + elapsedYears * SiegeConfig.INTENSITY_PER_YEAR
                + legioMarkets * SiegeConfig.INTENSITY_PER_LEGIO_MARKET
               ).coerceIn(SiegeConfig.INTENSITY_BASE, SiegeConfig.INTENSITY_MAX)
    }

    // --- Nexerelin story-market protection (task 7a.5) ---

    /**
     * True when Nex's invasion rules forbid capturing this market because it is a core/"starting"
     * market and the player has turned off allowInvadeStartingMarkets. Mirrors the exact check in
     * Nexerelin's NexUtilsMarket (allowInvadeStartingMarkets + the $nex_existed_at_start flag) so the
     * siege never sits forever trying to take something Nex will never let it have. Reading the
     * NexConfig field directly picks up the live LunaLib override.
     */
    private fun isNexCaptureBlocked(market: MarketAPI): Boolean {
        if (!ModCompat.HAS_NEX) return false
        return try {
            !NexConfig.allowInvadeStartingMarkets &&
                market.memoryWithoutUpdate.getBoolean(NEX_MARKET_EXISTED_AT_START)
        } catch (_: Exception) { false }
    }

    /**
     * True when this market must never be captured. attemptNexCapture calls Nex's
     * SectorManager.transferMarket directly, bypassing Nex's own NexUtilsMarket.canBeInvaded checks,
     * so this guard is the ONLY gate — it is deliberately unconditional. In particular we do not
     * consult NexConfig.allowInvadeStoryCritical: the spec says story-protected markets are never
     * captured, full stop.
     */
    private fun isNexProtected(market: MarketAPI): Boolean {
        // Vanilla story protection — these are two DISTINCT flags and both must be checked:
        // $storyCritical (Misc.isStoryCritical) and $core_noDeciv (quests flag no-deciv markets).
        // Both hold even without Nex.
        if (Misc.isStoryCritical(market)) return true
        if (market.memoryWithoutUpdate.getBoolean(DecivTracker.NO_DECIV_KEY)) return true
        if (!ModCompat.HAS_NEX) return false
        try {
            if (NexConfig.getFactionConfig(market.factionId)?.invasionOnlyRetake == true) return true
        } catch (_: Exception) {}
        // Nex's own per-market opt-out, honored even though we bypass canBeInvaded.
        if (market.memoryWithoutUpdate.getBoolean(NEX_NPC_NO_INVADE)) return true
        return false
    }

    // --- Clean teardown for toggle-off mid-save (task 8.1) ---

    fun tearDown() {
        for (siege in activeSieges.toList()) {
            removePressureCondition(siege)
            siege.intel?.resolve(SiegeIntel.SiegeOutcome.LIFTED)
            disperseFleets(siege, keepCommandFleet = false)
        }
        activeSieges.clear()
        LOG.info("Tahlan siege: torn down (feature disabled mid-save)")
    }

    // --- Console-command debug API (SiegeInfo / SiegeKill / SiegeStart) ---

    /** Human-readable dump of manager state and every active siege, for the TahlanSiegeInfo command. */
    fun debugDump(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Tahlan Siege Manager ===")
        sb.appendLine("ENABLE_SIEGE=${TahlanSettings.ENABLE_SIEGE}  HAS_NEX=${ModCompat.HAS_NEX}  legioHoldsCapital=${legioHoldsCapital()}")
        sb.appendLine("spawnTimer=${"%.1f".format(spawnTimer.elapsed)}/${"%.1f".format(spawnTimer.intervalDuration)} days (bounds ${SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN}-${SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX}; ticks only while no siege active)")
        sb.appendLine("intensity if launched now=${"%.2f".format(computeIntensity())}")
        sb.appendLine("active sieges: ${activeSieges.size}")
        if (activeSieges.isEmpty()) {
            sb.appendLine("  (none)")
            return sb.toString()
        }
        for ((i, s) in activeSieges.withIndex()) {
            sb.appendLine("--- #${i + 1}  ${s.id} ---")
            sb.appendLine("  stage=${s.stage}  intensity=${"%.2f".format(s.intensity)}")
            sb.appendLine("  target=${s.targetSystem.baseName}  primaryMarket=${s.primaryTargetMarket?.name ?: "none"}  source=${s.sourceMarket.name}")
            sb.appendLine("  siegeHealth=${"%.1f".format(s.siegeHealth)}/${SiegeConfig.SIEGE_HEALTH_MAX}  captureProgress=${"%.1f".format(s.captureProgress)}/${SiegeConfig.CAPTURE_PROGRESS_MAX}")
            sb.appendLine("  commandCR=${"%.2f".format(s.commandCR)}  commandFleetPresent=${s.commandFleetPresent}  withdrawalOrdered=${s.withdrawalOrdered}")
            sb.appendLine("  daysElapsed=${"%.1f".format(s.daysElapsed)}  daysSinceLastLoss=${"%.1f".format(s.daysSinceLastLoss)}  raidCooldown=${"%.1f".format(s.raidCooldown)}  lastPressureMult=${"%.2f".format(s.lastPressureMult)}")
            val cmd = s.commandFleet
            sb.appendLine("  commandFleet=" + if (cmd == null) "null"
                else "${cmd.name} [alive=${cmd.isAlive}, fp=${cmd.fleetPoints}, at=${cmd.starSystem?.baseName ?: cmd.containingLocation?.name ?: "?"}]")
            sb.appendLine("  escortFleets=${s.escortFleets.count { it.isAlive }} alive / ${s.escortFleets.size} tracked")
            sb.appendLine("  raidFleets=${s.raidFleets.count { it.isAlive }} alive / ${s.raidFleets.size} tracked (max ${SiegeConfig.MAX_ACTIVE_RAID_FLEETS})")
            sb.appendLine("  conditionedMarkets=${s.conditionedMarkets.joinToString { it.name }.ifEmpty { "none" }}")
            sb.appendLine("  garrisonMarket=${s.garrisonMarket?.name ?: "none"}  playerBountyAccrued=${"%.0f".format(s.playerBountyAccrued)}")
        }
        return sb.toString()
    }

    /** Force-end (lift) every active siege, dispersing their fleets home. Returns the count ended. */
    fun debugEndAllSieges(): Int {
        val n = activeSieges.size
        for (siege in activeSieges.toList()) resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED)
        return n
    }

    /**
     * Force-launch a siege now, bypassing the spawn timer (for the TahlanSiegeStart command).
     * [forcedSystem] optionally pins the target; when null the normal weighted picker chooses one.
     * Returns a status string describing success or the reason for failure.
     */
    fun debugForceLaunch(forcedSystem: StarSystemAPI?): String {
        if (!TahlanSettings.ENABLE_SIEGE)
            return "Siege feature is disabled (ENABLE_SIEGE=false); a forced siege would be torn down next tick."
        if (!legioHoldsCapital())
            return "Legio does not hold Lucifron; forced sieges would be lifted next tick."
        val source = pickSourceMarket()
            ?: return "No eligible Legio source market found."
        val target: Pair<StarSystemAPI, MarketAPI?> = if (forcedSystem != null) {
            val primary = evaluateTarget(forcedSystem)?.first
                ?: return "${forcedSystem.baseName} is not a valid siege target (needs a hostile, non-Legio, non-story-protected market and must not already be under siege)."
            forcedSystem to primary
        } else {
            pickTargetSystem(source)
                ?: return "No eligible target system found."
        }
        val siege = launchSiege(source, target.first, target.second)
            ?: return "Launch failed — command fleet could not be spawned."
        return "Launched ${siege.id} on ${target.first.baseName} from ${source.name} (intensity ${"%.2f".format(siege.intensity)})."
    }

    // --- Helpers ---

    private fun tagSiegeFleet(fleet: CampaignFleetAPI, siegeId: String, fp: Float, isCommand: Boolean) {
        fleet.memoryWithoutUpdate.set(FLEET_SIEGE_ID_KEY,  siegeId)
        fleet.memoryWithoutUpdate.set(FLEET_IS_CMD_KEY,    isCommand)
        fleet.memoryWithoutUpdate.set(FLEET_FP_KEY,        fp)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true)
    }

    private fun findSiege(id: String): SiegeData? = activeSieges.find { it.id == id }

    companion object {
        const val FLEET_SIEGE_ID_KEY = "\$tahlan_siege_id"
        const val FLEET_IS_CMD_KEY   = "\$tahlan_siege_is_cmd"
        const val FLEET_FP_KEY       = "\$tahlan_siege_fp"
        const val FLEET_RETURN_FLAG  = "\$tahlan_siege_return"
        const val FLEET_GARRISON_MARKET_KEY = "\$tahlan_siege_garrison"

        // Nexerelin's ExerelinConstants.MEMKEY_MARKET_EXISTED_AT_START — markets present at sector
        // start (core/"starting" markets). Hardcoded as a literal so this class never hard-links the
        // Nex constant outside a HAS_NEX guard; it's a serialized memory key and is stable.
        const val NEX_MARKET_EXISTED_AT_START = "\$nex_existed_at_start"

        // Nexerelin's ExerelinConstants.MEMORY_KEY_NPC_NO_INVADE — per-market opt-out from NPC
        // invasion. Hardcoded as a literal for the same reason as the key above: no hard link to the
        // Nex constant, and it's a serialized memory key so it's stable.
        const val NEX_NPC_NO_INVADE = "\$nex_npc_no_invade"

        // One-shot marker for the legacy stat-mod leak repair (see sweepLeakedConditionMods).
        const val SIEGE_LEAK_SWEPT_KEY = "\$tahlan_siegeleak_swept"

        val LOG: Logger = Global.getLogger(SiegeManager::class.java)!!

        /**
         * Locate the live siege manager, robustly across save/load. The **scripts list is the source
         * of truth** — [SectorAPI.addScript] reliably round-trips (cf. PlanetkillerStrikeWatcher), so
         * a scan of it always finds the instance that persisted with the save. `persistentData` and
         * sector memory are only fast-lookup caches (and cover older saves that stored the pointer
         * there); whatever is found is re-cached so later lookups stay cheap. Returns null only when
         * no manager has ever been registered.
         *
         * This exists because the AIs and fleet listeners used to read the manager *solely* from
         * sector memory. If that single pointer ever failed to resolve, the onGameLoad guard created a
         * second, empty manager whose callbacks — routed to it, never flipping sieges to BESIEGING —
         * stalled all progress. A scripts-list scan makes the lookup independent of any one store.
         */
        fun get(): SiegeManager? {
            val sector = Global.getSector() ?: return null
            (sector.persistentData[TahlanIDs.SIEGE_MANAGER_KEY] as? SiegeManager)?.let { return it }
            // Cache miss: scan the (reliably-persisted) scripts list. If more than one manager exists —
            // a legacy save corrupted by the old duplicate-on-load bug — prefer the one actually
            // holding sieges so the real state wins over a stray empty instance.
            val found = sector.scripts.filterIsInstance(SiegeManager::class.java)
                .maxByOrNull { it.activeSieges.size }
                ?: sector.memoryWithoutUpdate.get(TahlanIDs.SIEGE_MANAGER_KEY) as? SiegeManager
            if (found != null) {
                sector.persistentData[TahlanIDs.SIEGE_MANAGER_KEY] = found
                sector.memoryWithoutUpdate.set(TahlanIDs.SIEGE_MANAGER_KEY, found, 0f)
            }
            return found
        }

        /**
         * Return the single live manager, creating and registering it only if none exists yet.
         * Idempotent — safe to call on every onGameLoad. Reuses the instance that persisted with the
         * save (found via the scripts list) and prunes any duplicates left by the old bug, so exactly
         * one manager runs and every lookup resolves to it.
         *
         * Event-listener registration is handled entirely through [SectorAPI.addListener]: a freshly
         * constructed manager registers itself there via `BaseCampaignEventListener(true)`, and that
         * list persists in the save, so a deserialized manager is already registered. The defensive
         * re-add below only covers a manager that somehow round-tripped in `scripts` but not in the
         * listener list. Note `sector.listenerManager` is a *different* registry
         * (`ListenerManagerAPI`) that never delivers `CampaignEventListener` callbacks — registering
         * there would silently receive nothing.
         */
        fun getOrCreate(): SiegeManager {
            val sector = Global.getSector()
            val managers = sector.scripts.filterIsInstance(SiegeManager::class.java)
            val mgr = when {
                managers.isEmpty() -> SiegeManager().also { sector.addScript(it) }  // ctor registers listener
                else -> managers.maxByOrNull { it.activeSieges.size }!!             // richest = the real one
            }
            // Prune stray duplicate managers (legacy corruption from the pre-fix duplicate-on-load bug).
            for (dup in managers) {
                if (dup !== mgr) {
                    sector.removeScript(dup)
                    sector.removeListener(dup)
                }
            }
            // Guard against double registration (which would double every callback).
            if (sector.allListeners.none { it === mgr }) sector.addListener(mgr)
            sector.persistentData[TahlanIDs.SIEGE_MANAGER_KEY] = mgr
            sector.memoryWithoutUpdate.set(TahlanIDs.SIEGE_MANAGER_KEY, mgr, 0f)
            return mgr
        }
    }
}
