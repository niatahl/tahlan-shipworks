package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.SystemBountyManager
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import java.util.Random
import exerelin.campaign.AllianceManager
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
        // PLANETFALL sits between BESIEGING and the terminal stages: still live, but past the point
        // where the subjugation meter means anything. It is a distinct stage rather than a flag on
        // BESIEGING because nearly every consumer of `stage` needs different behavior during it —
        // pruning, pressure sweeps, knockback, withdrawal, and the reactive systems all diverge.
        enum class Stage { INBOUND, BESIEGING, PLANETFALL, BROKEN, LIFTED, SUCCEEDED }

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

        // Planetfall window (add-siege-planetfall). Primitives, so a save that predates them
        // deserializes 0f — never read, because such a save can never be in PLANETFALL.
        var planetfallTimer       = 0f              // counts down from PLANETFALL_DURATION_DAYS
        var defenderSweepCooldown = 0f              // cadence for the repeating defender flee sweep

        var intel: SiegeIntel? = null
        var playerBountyAccrued = 0f

        // ── Reactive systems (add-siege-reactivity). Serialized with the manager like everything
        // above — but only the primitives are safe on a save that predates them (they deserialize
        // as harmless zeros); the reference fields deserialize as null despite their Kotlin types,
        // see readResolve below. The AIs and listeners that read them are identified by siege id
        // and resolve the manager via SiegeManager.get(), never by direct reference. ──

        // F3 — coalition interventions. Fleets are tracked so resolution can disperse them; they are
        // NOT siege fleets and never appear in escortFleets/raidFleets.
        var interventionFleets = mutableListOf<CampaignFleetAPI>()
        var interventionCooldown = SiegeConfig.INTERVENTION_INTERVAL_DAYS

        // F2 — desperation system bounty. The intel itself lives in the vanilla SystemBountyManager
        // (looked up by market), so only the cadence and a display flag are kept here.
        var bountyPosted = false
        var bountyTimer  = 0f

        // F1 — huntsman task force. A negative redispatch timer means "no replacement pending".
        var taskForceFleet: CampaignFleetAPI? = null
        var taskForceRedispatchTimer = -1f
        var playerHeat   = 0f
        var playerMarked = false

        // XStream skips constructors, so a SiegeData loaded from a save that predates
        // add-siege-reactivity deserializes [interventionFleets] as null despite the non-null
        // Kotlin type — and NPEs the first prune/dispersal that touches it. readResolve is the
        // deserialization hook XStream honors; the field itself keeps its name and stays
        // non-transient so newer saves keep their tracked lists.
        @Suppress("SENSELESS_COMPARISON")
        private fun readResolve(): Any {
            if (interventionFleets == null) interventionFleets = mutableListOf()
            return this
        }
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
    // Same deferral, for intervention-fleet deaths (siege id -> FP lost). Their CR strain has to
    // wait for the safe side of the battle-callback boundary just like siege-fleet losses do:
    // applyCommandCR walks the command fleet's members and triggerWithdrawal rewrites assignments,
    // neither of which is safe while CampaignEngine is iterating its listeners.
    @Transient
    private var _pendingInterventionLosses: MutableList<Pair<String, Float>>? = null
    private val pendingInterventionLosses: MutableList<Pair<String, Float>>
        get() = _pendingInterventionLosses
            ?: mutableListOf<Pair<String, Float>>().also { _pendingInterventionLosses = it }
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
        if (pendingInterventionLosses.isNotEmpty()) {
            for ((siegeId, fp) in pendingInterventionLosses.toList()) {
                flushInterventionLoss(siegeId, fp)
            }
            pendingInterventionLosses.clear()
        }

        pruneDeadSieges()
        advanceHealthModel(days)

        // Keep the pressure condition pointed at whoever is actually hostile right now. Slow interval:
        // this tracks diplomacy and market ownership, which move on the order of months.
        conditionSweepTimer.advance(days)
        if (conditionSweepTimer.intervalElapsed()) {
            maintainPressureConditions()
            reconcileOrphans()
        }

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
            // Never during planetfall: the meter is spent and no longer gates the capture, so a
            // receding bar would tell the player their kills are buying something they are not.
            // Breaking the siege force or the command fleet is the only counter left, and both of
            // those still work — through siege health and the decapitation rule respectively.
            if (siege.stage != SiegeData.Stage.PLANETFALL) {
                siege.captureProgress = max(0f, siege.captureProgress - lostFp * SiegeConfig.CAPTURE_KNOCKBACK_PER_FP)
            }
        }

        // Bounty, scaled by how much of the battle was the player's own work. Escort losses pay out
        // incrementally per FP shed; the command bounty stays a single kill-only lump sum (driving the
        // flagship force off is not the same as putting it down).
        val involvement = playerFraction.coerceIn(0f, 1f)

        // Heat: the huntsmen's grudge ledger. Kills-only (scaled by how much of the work was the
        // player's) keeps the eventual marking legible — the player felt themselves earn it — and
        // leaves the whole system inert for anyone who never shoots at the siege.
        if (involvement > 0f && SiegeConfig.TASKFORCE_ENABLED) {
            siege.playerHeat += lostFp * SiegeConfig.HEAT_PER_FP * involvement
        }

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
            // Zero knockback (command kills, and every kill during planetfall) renders as a row with
            // no number rather than a misleading "-0".
            val knockback = if (isCommand || siege.stage == SiegeData.Stage.PLANETFALL) 0f
                            else lostFp * SiegeConfig.CAPTURE_KNOCKBACK_PER_FP
            siege.intel?.addFleetKill(knockback, isCommand)
        }

        // Defer CR application and broken-check to advance() — safe side of the battle callback boundary
        pendingKills.add(siegeId)
    }

    private fun flushKill(siegeId: String) {
        val siege = findSiege(siegeId) ?: return
        applyCommandCR(siege)
        checkBroken(siege)
        // Decapitation, in the two stages where losing the command fleet ends the whole expedition
        // rather than dropping it into mop-up. Covers both the battle-kill path and the non-battle
        // despawn path (both funnel through onSiegeFleetLosses -> pendingKills).
        //
        //  * INBOUND: the stage only leaves INBOUND via onCommandFleetArrived, which is driven by the
        //    command fleet's own SiegeAssignmentAI — so a command fleet lost en route would leave the
        //    siege stuck INBOUND forever (which also blocks every future launch, since spawnTimer
        //    only ticks while activeSieges is empty).
        //  * PLANETFALL: the landing is the command fleet; without it there is nothing to mop up and
        //    the whole force scatters. Resolved here rather than left to the next advancePlanetfall
        //    tick so the intel beat lands together with the kill.
        //
        // Either way: escorts disperse home and the accrued bounty pays out.
        if (!siege.commandFleetPresent &&
            (siege.stage == SiegeData.Stage.INBOUND || siege.stage == SiegeData.Stage.PLANETFALL)) {
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
        // Give the defenders a beat to notice before the first relief force mobilizes — half a
        // cooldown, rather than a wave arriving on the heels of the expedition itself.
        siege.interventionCooldown = SiegeConfig.INTERVENTION_INTERVAL_DAYS * 0.5f
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
        siege.interventionFleets.remove(fleet)
        if (siege.commandFleet == fleet) siege.commandFleet = null
        if (siege.taskForceFleet == fleet) siege.taskForceFleet = null
    }

    // --- Health model (called each advance tick) ---

    private fun advanceHealthModel(days: Float) {
        val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO)
        for (siege in activeSieges.toList()) {
            // The three live stages age; anything else is already resolved and awaiting pruning.
            if (siege.stage != SiegeData.Stage.INBOUND &&
                siege.stage != SiegeData.Stage.BESIEGING &&
                siege.stage != SiegeData.Stage.PLANETFALL) continue
            siege.daysElapsed += days

            if (siege.stage == SiegeData.Stage.INBOUND) {
                // Travel backstop: an expedition that never arrives would otherwise sit INBOUND
                // forever, which also blocks every future launch (spawnTimer only ticks while no
                // siege is active). Nothing else in this loop applies before arrival.
                if (siege.daysElapsed >= SiegeConfig.INBOUND_TIMEOUT_DAYS) {
                    LOG.info("Tahlan siege: ${siege.id} never reached ${siege.targetSystem.baseName} — lifted on travel timeout")
                    resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED)
                }
                continue
            }

            // The landing has its own, much shorter rule set — see advancePlanetfall.
            if (siege.stage == SiegeData.Stage.PLANETFALL) { advancePlanetfall(siege, days); continue }

            advanceCommandCondition(siege, days)

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

            val target = siege.primaryTargetMarket
            if (!targetStillValid(siege, legioFaction)) continue

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

            // ── Reactive systems: the world pushing back (add-siege-reactivity) ──
            // Deliberately placed after the resolution checks (a siege about to end mobilizes
            // nobody) and before the meter, so the bounty reads the same progress the player sees.
            advanceHeat(siege, days)
            advanceInterventions(siege, days)
            advanceBounty(siege, days)
            advanceTaskForce(siege, days)

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
                        beginPlanetfall(siege); continue
                    } else {
                        applyNoNexAftermath(siege)
                        resolveSiege(siege, SiegeIntel.SiegeOutcome.SUCCEEDED); continue
                    }
                }
            }

            siege.intel?.syncProgress(siege)
        }
    }

    /**
     * The command fleet's condition, shared verbatim by the besieging and planetfall ticks: CR
     * recovers toward 1.0 during a lull, and siege health regenerates only while the command fleet
     * is alive to coordinate. The planetfall window is short enough that neither moves far, but a
     * landing that has to be fought for should recover the same way a blockade does.
     */
    private fun advanceCommandCondition(siege: SiegeData, days: Float) {
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
    }

    /**
     * The LIFTED gate, shared by the besieging and planetfall ticks. Eligibility is otherwise only
     * checked at launch, so a mid-siege change of circumstance — a Nexerelin peace treaty, the market
     * decivilizing or being depopulated, or someone else taking it (including another Legio force) —
     * would otherwise still end in "subjugating" a market Legio has no quarrel with, or in laying
     * siege to a rock. All of those resolve LIFTED: the expedition packs up with no capture and no
     * scar, since none of it was earned by the siege — there is no undeserved SUCCEEDED climax to be
     * had here. Returns false once it has resolved the siege, so callers must abandon it.
     */
    private fun targetStillValid(siege: SiegeData, legioFaction: FactionAPI): Boolean {
        val target = siege.primaryTargetMarket ?: return true
        val gone = !target.isInEconomy || target.hasCondition(Conditions.DECIVILIZED)
        if (gone || target.factionId == TahlanIDs.LEGIO || !legioFaction.isHostileTo(target.faction)) {
            LOG.info("Tahlan siege: ${siege.id} target ${target.name} is no longer a valid objective — lifted")
            resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED)
            return false
        }
        return true
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    // Planetfall — the Nex capture climax (add-siege-planetfall)
    //
    // A full subjugation meter no longer flips the market in the same tick. Instead the expedition
    // commits: the whole siege force converges on the planet, the defending navy quits the system,
    // the station is starved out, relief stands down, and the transfer fires only if the command
    // fleet is still over the planet when the window closes.
    //
    // The window is a point of no return for the *meter* (no more knockback — see
    // [onSiegeFleetLosses]) but explicitly not for the siege: driving siege health to zero still
    // breaks it, and removing the command fleet breaks it instantly.
    // ═════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Commit to the landing. Everything here is one-shot setup; [advancePlanetfall] owns the window.
     */
    private fun beginPlanetfall(siege: SiegeData) {
        val target = siege.primaryTargetMarket
        val planet = target?.primaryEntity
        if (target == null || planet == null) {
            // Nothing in the world to converge on. Degrade to the pre-planetfall behavior rather than
            // strand the siege in a stage whose every rule reads the planet.
            LOG.info("Tahlan siege: ${siege.id} — no target entity to make planetfall on; capturing directly")
            attemptNexCapture(siege)
            return
        }

        siege.stage = SiegeData.Stage.PLANETFALL
        siege.planetfallTimer = SiegeConfig.PLANETFALL_DURATION_DAYS
        siege.defenderSweepCooldown = SiegeConfig.DEFENDER_SWEEP_INTERVAL_DAYS

        // The command fleet leaves its fringe anchor on its own AI's terms — it reads the key and
        // switches phase, which keeps the anchor logic in one place instead of split between us.
        siege.commandFleet?.takeIf { it.isAlive }
            ?.memoryWithoutUpdate?.set(FLEET_PLANETFALL_KEY, planet.id)

        // Everything else is re-tasked directly: raid fleets carry no AI script at all, and the
        // blockade AI's own station-holding would fight a convergence order it did not issue itself
        // (which is why it gets the key too — see SiegeBlockadeAI).
        for (fleet in (siege.escortFleets + siege.raidFleets).filter { it.isAlive }) {
            fleet.memoryWithoutUpdate.set(FLEET_PLANETFALL_KEY, planet.id)
            // Never yank a fighting fleet. Nothing is stranded by skipping one: blockade fleets
            // re-anchor themselves off the key once the fight ends, plain escorts are orbiting the
            // command fleet and follow it in, and raiders are already pointed at this planet.
            if (fleet.battle != null) continue
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, planet, 9999f,
                txt("siege_assign_landing_support").format(planet.name))
        }

        // Relief stands down: the navy has given up, and its allies with it. Their own AIs route each
        // fleet back to its own market off this flag.
        for (fleet in siege.interventionFleets.filter { it.isAlive }) {
            fleet.memoryWithoutUpdate.set(FLEET_RETURN_FLAG, true)
        }

        disruptStation(target)
        sweepDefenders(siege)
        siege.intel?.notifyPlanetfall()
        siege.intel?.syncProgress(siege)
        LOG.info("Tahlan siege: ${siege.id} — planetfall on ${target.name}, " +
                "${"%.0f".format(SiegeConfig.PLANETFALL_DURATION_DAYS)} days to the landing")
    }

    /**
     * The landing window. Deliberately runs none of the reactive systems that mobilize anybody new:
     * no raid sorties (there is nothing left to soften), no relief waves (the coalition stood down),
     * no bounty keep-alive (it lapses on its own vanilla duration, which conveniently keeps paying
     * through the window). F1 continues in full — the huntsmen are the one thing still hunting.
     */
    private fun advancePlanetfall(siege: SiegeData, days: Float) {
        val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO)
        advanceCommandCondition(siege, days)

        // Decapitation. Killed, despawned and driven to the CR floor all read the same here: the
        // landing collapses and the force scatters. Deliberately NOT triggerWithdrawal — its mop-up
        // machinery (residual health chunk, escort re-anchoring, withdrawal factor) is meaningless
        // when everything is about to disperse home anyway. The battle-kill path resolves a tick
        // earlier, in flushKill; this covers every other way the command fleet can go away.
        val cmdAlive = siege.commandFleetPresent && siege.commandFleet?.isAlive == true
        if (!cmdAlive || siege.commandCR <= SiegeConfig.COMMAND_CR_WITHDRAWAL_FLOOR) {
            LOG.info("Tahlan siege: ${siege.id} — the landing has been decapitated; siege broken")
            resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN); return
        }

        if (!targetStillValid(siege, legioFaction)) return

        // Breaking the siege force during the landing counts exactly as it does during the blockade.
        if (siege.siegeHealth <= 0f) { resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN); return }

        advanceHeat(siege, days)
        advanceTaskForce(siege, days)

        // Repeating rather than one-shot: the market's own military base keeps spawning patrols right
        // through the window, so a single sweep at planetfall start would only clear the first batch.
        siege.defenderSweepCooldown -= days
        if (siege.defenderSweepCooldown <= 0f) {
            siege.defenderSweepCooldown = SiegeConfig.DEFENDER_SWEEP_INTERVAL_DAYS
            sweepDefenders(siege)
        }

        siege.planetfallTimer -= days
        siege.intel?.syncProgress(siege)
        if (siege.planetfallTimer > 0f) return
        // Never flip the market out from under a fight the player might still win. The battle resolves
        // first; if it goes badly for the Legio, the decapitation check above catches it next tick.
        if (siege.commandFleet?.battle != null) return
        attemptNexCapture(siege)
    }

    /**
     * Starve the station out instead of fighting it: months of blockade have emptied its magazines,
     * so it stands down for the landing — which also keeps a Star Fortress from deciding the scripted
     * climax on a die roll. Identified by spec tag rather than an id whitelist, so modded stations are
     * covered. The disruption outlasts the window on purpose (see STATION_DISRUPTION_EXTRA_DAYS), so
     * whoever ends up owning the market inherits a recovering station.
     */
    private fun disruptStation(target: MarketAPI) {
        val duration = SiegeConfig.PLANETFALL_DURATION_DAYS + SiegeConfig.STATION_DISRUPTION_EXTRA_DAYS
        for (industry in target.industries) {
            if (industry.spec?.hasTag(Industries.TAG_STATION) != true) continue
            if (!industry.canBeDisrupted()) continue
            industry.setDisrupted(duration, true)   // useMax: never shorten an existing disruption
            LOG.info("Tahlan siege: starved out ${industry.spec.name} at ${target.name}")
        }
    }

    /**
     * The defending navy gives up. Narrowly scoped on purpose: only *military* fleets of the victim's
     * own bloc are pushed out, so traders keep flying, third parties are untouched, and the station
     * (handled by [disruptStation]) is left where it is. Fleets in a battle are skipped rather than
     * yanked; the repeating sweep catches them once the fight ends.
     *
     * The player's navy is never swept — a player-owned market is a valid siege target, and its
     * defenders have no reason to abandon it just because the Legio showed up in force.
     */
    private fun sweepDefenders(siege: SiegeData) {
        val victimId = siege.primaryTargetMarket?.factionId ?: return
        // Already excludes the Legio side and the player faction; see coalitionFactions.
        val coalition = coalitionFactions(victimId)
        if (coalition.isEmpty()) return
        val legioFaction = Global.getSector().getFaction(TahlanIDs.LEGIO) ?: return
        // Sized to the window so the orders lapse on their own if the siege is broken mid-landing —
        // no teardown code, and a survivor goes straight back to ordinary patrol behavior.
        val orderDays = SiegeConfig.PLANETFALL_DURATION_DAYS

        var pushed = 0
        for (fleet in siege.targetSystem.fleets.toList()) {
            if (!fleet.isAlive) continue
            if (fleet.isPlayerFleet) continue
            val factionId = fleet.faction?.id ?: continue
            if (factionId !in coalition) continue
            if (!legioFaction.isHostileTo(fleet.faction)) continue
            val mem = fleet.memoryWithoutUpdate
            // Siege-attached fleets — interventions and the huntsmen — have their own signaling.
            if (mem.contains(FLEET_SIEGE_ID_KEY)) continue
            if (mem.getBoolean(MemFlags.STATION_FLEET)) continue
            val military = mem.getBoolean(MemFlags.MEMORY_KEY_PATROL_FLEET) ||
                           mem.getBoolean(MemFlags.MEMORY_KEY_WAR_FLEET)
            if (!military) continue
            if (fleet.battle != null) continue

            // Disengage + no-sidetracking is what makes them actually leave rather than turn and
            // fight the first Legio picket on the way out.
            mem.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true, orderDays)
            mem.set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true, orderDays)
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, fleeDestination(fleet, factionId, siege.targetSystem),
                1000f, txt("siege_assign_flee").format(siege.targetSystem.nameWithLowercaseType))
            pushed++
        }
        if (pushed > 0) LOG.info("Tahlan siege: ${siege.id} — $pushed defending fleet(s) ordered out of " +
                siege.targetSystem.baseName)
    }

    /**
     * Where a fleeing defender runs to: the nearest friendly market **outside** the besieged system,
     * so the despawn happens somewhere plausible rather than in the middle of the landing.
     * Despawn-at-destination is what makes the navy *gone* instead of loitering; vanilla respawn
     * economics replace them in their own time. Falls back to the nearest jump point for a faction
     * with nowhere left to run.
     */
    private fun fleeDestination(fleet: CampaignFleetAPI, factionId: String, besieged: StarSystemAPI): SectorEntityToken {
        val refuge = Global.getSector().economy.marketsCopy
            .filter { it.factionId == factionId && it.isInEconomy && !it.isHidden }
            .filter { it.starSystem != null && it.starSystem != besieged }
            .minByOrNull { Misc.getDistance(fleet.locationInHyperspace, it.locationInHyperspace) }
        return refuge?.primaryEntity ?: Misc.findNearestJumpPoint(fleet) ?: besieged.center
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
            // transferMarket(market, newOwner, oldOwner, playerInvolved, isCapture, factionsToNotify,
            //                repChangeStrength, silent)
            // The trailing `silent` suppresses Nex's own generic MarketTransferIntel: the siege's
            // SUCCEEDED resolution is the single beat for this capture, and a second, toneless feed
            // item would undercut it. Nex's diplomacy and war bookkeeping are unaffected by the flag.
            SectorManager.transferMarket(target, legioFaction, oldOwner, false, true, emptyList<String>(), 0f, true)
        } catch (e: Exception) {
            LOG.warn("Tahlan siege: Nex market transfer failed — ${e.message}")
            resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); return
        }
        // Applied after the transfer so it acts on the post-handover industry list.
        applyOccupationAftermath(target)
        // Hand the garrison target to the command fleet via its OWN memory: resolveSiege is about to
        // drop this siege from tracking, so the assignment AI must read it independently of the manager.
        siege.garrisonMarket = target
        siege.commandFleet?.memoryWithoutUpdate?.set(FLEET_GARRISON_MARKET_KEY, target.id)
        resolveSiege(siege, SiegeIntel.SiegeOutcome.SUCCEEDED, keepCommandFleet = true)
    }

    /**
     * A captured world does not change hands pristine. Core industries are disrupted and the
     * population is left restive, so a fresh conquest reads as conquered and stays a natural retake
     * target. The station's own (longer) disruption from [disruptStation] survives this pass: it is a
     * structure, not an industry, and `useMax` would protect it either way.
     *
     * Deliberately does NOT apply the no-Nex scar condition. Its accessibility/stability penalties
     * exist to punish a market that held out against a siege; this one is Legio's now, so the scar
     * would be a nonsense penalty on the conqueror.
     */
    private fun applyOccupationAftermath(target: MarketAPI) {
        val random = Random()
        for (industry in target.industries) {
            if (!industry.canBeDisrupted() || !industry.isIndustry()) continue
            val dur = SiegeConfig.OCCUPATION_DISRUPTION_DAYS * StarSystemGenerator.getNormalRandom(random, 1f, 1.25f)
            industry.setDisrupted(dur, true)  // useMax: only ever extend an existing disruption
        }
        try {
            RecentUnrest.get(target).add(SiegeConfig.OCCUPATION_UNREST_POINTS, txt("siege_unrest_reason"))
        } catch (e: Exception) {
            LOG.warn("Tahlan siege: could not apply occupation unrest at ${target.name} — ${e.message}")
        }
        LOG.info("Tahlan siege: occupation aftermath applied to ${target.name}")
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

    // ═════════════════════════════════════════════════════════════════════════════════════════
    // F3 — Coalition interventions
    //
    // The besieged bloc fights back on its own. Coalition = the victim plus its allies; the member
    // with the highest *response capacity* leads with a fleet sized to contest the command fleet,
    // everyone else contributes capacity-scaled detachments. Distance discounts capacity (and so
    // ranking) but never disqualifies — a far-off umbrella ally simply arrives later.
    //
    // Intervention fleets are NOT siege fleets: no tagSiegeFleet, no SiegeFleetListener. Their
    // deaths never touch siege health, the meter, or the player's bounty ledger — only command CR,
    // through [onInterventionFleetLost], so that even a failed rescue softens the siege.
    // ═════════════════════════════════════════════════════════════════════════════════════════

    /** A coalition faction's single best projection source, and the strength it can project. */
    private class CoalitionMember(val factionId: String, val market: MarketAPI, val capacity: Float)

    private fun advanceInterventions(siege: SiegeData, days: Float) {
        if (!SiegeConfig.INTERVENTION_ENABLED) return
        // Interventions exist to contest the command fleet. Once it is dead or withdrawn the siege
        // is in mop-up and there is nothing left worth mobilizing a relief force against.
        if (!siege.commandFleetPresent) return

        if (siege.interventionCooldown > 0f) { siege.interventionCooldown -= days; return }
        siege.interventionCooldown = SiegeConfig.INTERVENTION_INTERVAL_DAYS

        val victimId = siege.primaryTargetMarket?.factionId ?: return
        val coalition = coalitionFactions(victimId)
            .mapNotNull { responseCapacity(it, siege.targetSystem) }
            .sortedByDescending { it.capacity }
        if (coalition.isEmpty()) return

        // Contest the command fleet as it stands right now — a battered one draws a smaller response.
        val commandFp = siege.commandFleet?.takeIf { it.isAlive }?.fleetPoints?.toFloat()
            ?: (SiegeConfig.COMMAND_FP_BASE +
                SiegeConfig.COMMAND_FP_SCALE * SiegeConfig.intensityFactor(siege.intensity))

        for ((index, member) in coalition.withIndex()) {
            val capacityFp = member.capacity * SiegeConfig.INTERVENTION_AUX_FP_PER_CAPACITY
            val fp = if (index == 0) {
                // Lead: sized to contest, but a weak or distant lead cannot field the full fleet.
                min(commandFp * SiegeConfig.INTERVENTION_PRIMARY_FP_MULT, capacityFp)
            } else {
                if (member.capacity < SiegeConfig.INTERVENTION_AUX_CAPACITY_FLOOR) continue
                capacityFp
            }
            if (fp < MIN_INTERVENTION_FP) continue
            spawnInterventionFleet(siege, member, fp, isPrimary = index == 0)
        }
    }

    /**
     * The victim plus its allies. Under Nexerelin that means the victim's alliance (Nex's own core
     * mechanic and the environment Legio is designed around); without it, any faction at
     * WELCOMING-or-better toward the victim. The Legio side and the player faction are never
     * coalition members — the former for obvious reasons, the latter because the player's own
     * fleets are not the manager's to spawn.
     */
    private fun coalitionFactions(victimId: String): Set<String> {
        val out = linkedSetOf(victimId)
        if (ModCompat.HAS_NEX) {
            try {
                AllianceManager.getFactionAlliance(victimId)?.membersCopy?.let { out.addAll(it) }
            } catch (_: Exception) { /* Nex present but alliance state unavailable — victim alone */ }
        } else {
            val victim = Global.getSector().getFaction(victimId)
            if (victim != null) {
                for (faction in Global.getSector().allFactions) {
                    if (faction.id == victimId) continue
                    if (victim.isAtWorst(faction.id, RepLevel.WELCOMING)) out.add(faction.id)
                }
            }
        }
        out.removeAll(setOf(TahlanIDs.LEGIO, TahlanIDs.BLACKWATCH, TahlanIDs.DAEMONS, Factions.PLAYER))
        return out
    }

    /**
     * A faction's best military projection against [targetSystem]: over its markets, the highest
     * `(size + military-industry tier bonus) x distance falloff`. Local projection, not global
     * economy — a superpower on the far rim should not out-respond the neighbour next door. A
     * market with no *functioning* High Command or Military Base does not qualify at all, so a
     * faction of farming worlds sends nothing rather than a militia.
     */
    private fun responseCapacity(factionId: String, targetSystem: StarSystemAPI): CoalitionMember? {
        var bestCapacity = 0f
        var bestMarket: MarketAPI? = null
        for (market in Global.getSector().economy.marketsCopy) {
            if (market.factionId != factionId) continue
            if (market.isHidden || !market.isInEconomy) continue
            val tier = militaryTierBonus(market)
            if (tier <= 0f) continue
            val dist = Misc.getDistance(market.locationInHyperspace, targetSystem.location)
            val falloff = 10000f / (dist.coerceAtLeast(1000f) + 10000f)
            val capacity = (market.size + tier) * falloff
            if (capacity > bestCapacity) { bestCapacity = capacity; bestMarket = market }
        }
        return bestMarket?.let { CoalitionMember(factionId, it, bestCapacity) }
    }

    /** High Command > Military Base > nothing; a disrupted or non-functional industry projects zero. */
    private fun militaryTierBonus(market: MarketAPI): Float {
        market.getIndustry(Industries.HIGHCOMMAND)
            ?.takeIf { it.isFunctional && !it.isDisrupted }?.let { return 6f }
        market.getIndustry(Industries.MILITARYBASE)
            ?.takeIf { it.isFunctional && !it.isDisrupted }?.let { return 3f }
        return 0f
    }

    private fun spawnInterventionFleet(
        siege: SiegeData,
        member: CoalitionMember,
        fp: Float,
        isPrimary: Boolean
    ): CampaignFleetAPI? {
        // Each fleet spawns with its OWN faction's doctrine and colors — the visual contrast with
        // Legio's Blackwatch is half the point of a coalition response.
        val params = FleetParamsV3(member.market, FleetTypes.TASK_FORCE, fp, 0f, 0f, 0f, 0f, 0f, 0.2f)
        params.officerNumberMult = 1.5f
        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return null

        fleet.name = txt(if (isPrimary) "siege_fleet_intervention_primary" else "siege_fleet_intervention_aux")
        // Siege id so despawn pruning can attribute it; the intervention tag so the huntsmen (and
        // the loss listener) can tell it apart from a passing patrol. Deliberately NOT tagSiegeFleet.
        fleet.memoryWithoutUpdate.set(FLEET_SIEGE_ID_KEY, siege.id)
        fleet.memoryWithoutUpdate.set(FLEET_INTERVENTION_KEY, true)
        fleet.memoryWithoutUpdate.set(FLEET_FP_KEY, fp)

        // The victim is hostile to Legio by construction and Nex allies are almost always dragged in
        // by the shared alliance war — but a non-Nex relationship-ally may be perfectly neutral
        // toward Legio. Fleet-local hostility covers that edge case with no faction-level rep change,
        // so nothing cascades into sector diplomacy or Nex's war bookkeeping.
        val legio = Global.getSector().getFaction(TahlanIDs.LEGIO)
        val owner = Global.getSector().getFaction(member.factionId)
        if (legio != null && owner != null && !owner.isHostileTo(legio)) {
            Misc.makeHostileToFaction(fleet, TahlanIDs.LEGIO, 0f)
        }

        member.market.primaryEntity.containingLocation.addEntity(fleet)
        fleet.setLocation(member.market.primaryEntity.location.x, member.market.primaryEntity.location.y)
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, siege.targetSystem.center, 1000f,
            txt("siege_assign_intervene").format(siege.targetSystem.nameWithLowercaseType))
        fleet.addEventListener(SiegeInterventionListener(siege.id, fp))
        fleet.addScript(SiegeInterventionAI(fleet, member.market, siege.id, siege.targetSystem))
        siege.interventionFleets.add(fleet)

        val factionName = Global.getSector().getFaction(member.factionId)?.displayNameWithArticle
            ?: member.factionId
        siege.intel?.addIntervention(factionName, isPrimary)
        LOG.info("Tahlan siege: ${siege.id} — ${member.factionId} dispatched a " +
                (if (isPrimary) "primary" else "auxiliary") +
                " intervention (${"%.0f".format(fp)} FP) from ${member.market.name}")
        return fleet
    }

    /**
     * Called from [SiegeInterventionListener] inside a battle callback: an intervention fleet died
     * fighting the siege. Books nothing but the loss clock inline; the CR strain itself is deferred
     * to the next [advance] tick.
     */
    fun onInterventionFleetLost(siegeId: String, fp: Float) {
        val siege = findSiege(siegeId) ?: return
        // A relief force dying on the siege's guns is not a lull: the command fleet was fighting,
        // so it should not be recovering CR. (Safe for the mop-up stall backstop, which only
        // applies once the command fleet is gone — and interventions stop being sent at that point.)
        siege.daysSinceLastLoss = 0f
        pendingInterventionLosses.add(siegeId to fp)
    }

    private fun flushInterventionLoss(siegeId: String, fp: Float) {
        val siege = findSiege(siegeId) ?: return
        siege.commandCR = max(0f, siege.commandCR - fp * SiegeConfig.INTERVENTION_STRAIN_K)
        applyCommandCR(siege)
        LOG.info("Tahlan siege: $siegeId — intervention wiped out (${"%.0f".format(fp)} FP), " +
                "command CR now ${"%.2f".format(siege.commandCR)}")
    }

    /**
     * What an in-system intervention fleet should be attacking, polled by [SiegeInterventionAI]:
     * the command fleet while it holds, otherwise whatever siege force is left to fight. Null means
     * "nothing here for you" — the AI then goes home and despawns.
     */
    fun getInterventionTarget(siegeId: String): CampaignFleetAPI? {
        val siege = findSiege(siegeId) ?: return null
        siege.commandFleet?.takeIf { it.isAlive && siege.commandFleetPresent }?.let { return it }
        return (siege.escortFleets + siege.raidFleets)
            .firstOrNull { it.isAlive && it.containingLocation != null }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    // F2 — Desperation system bounty
    //
    // At the Stranglehold threshold the strangled market does what a desperate colony does: it puts
    // money on the table. This is the *vanilla* system bounty — the familiar "Bounty Posted" feed
    // item with vanilla payout, reputation, and lifecycle — with only the "likely cause" attribution
    // corrected to Legio (see SiegeSystemBountyIntel) and an intensity-scaled reward.
    // ═════════════════════════════════════════════════════════════════════════════════════════

    private fun advanceBounty(siege: SiegeData, days: Float) {
        if (!SiegeConfig.BOUNTY_ENABLED) return
        val market = siege.primaryTargetMarket ?: return
        // Keep-alive is conditional on the command fleet still holding the system. Once it is gone
        // we simply STOP refreshing: the bounty then lapses on its own remaining vanilla duration,
        // which conveniently keeps paying through the mop-up. No teardown code at all.
        if (!siege.commandFleetPresent) return
        if (siege.captureProgress < SiegeConfig.BOUNTY_TRIGGER_PROGRESS) return

        if (siege.bountyTimer > 0f) { siege.bountyTimer -= days; return }
        siege.bountyTimer = SiegeConfig.BOUNTY_KEEPALIVE_INTERVAL_DAYS

        if (!market.isInEconomy) return
        // Vanilla guards this in the constructor too (endImmediately), but a player-faction market
        // must never even reach construction: an ended intel would still be registered as active and
        // would NPE on the next reset() (its MilitaryResponseScript is never created).
        if (market.faction?.isPlayerFaction != false) return

        val manager = try { SystemBountyManager.getInstance() } catch (_: Exception) { null } ?: return

        // Registering through the manager is what keeps its own market-dedup honest: if vanilla (or
        // an earlier pass of ours) already has a bounty running here, refresh that one instead of
        // stacking a second.
        val existing = try { manager.getActive(market) } catch (_: Exception) { null }
        if (existing != null) {
            try { existing.reset() } catch (_: Exception) { /* commerce-mode intel has no script */ }
            return
        }

        try {
            // Reward scales from the vanilla amount at base intensity up to ~2x at max, times the
            // config multiplier (0 disables scaling entirely and leaves the vanilla formula alone).
            val mult = if (SiegeConfig.BOUNTY_BASE_REWARD_MULT <= 0f) 0f
                       else SiegeConfig.BOUNTY_BASE_REWARD_MULT *
                            (1f + SiegeConfig.intensityFactor(siege.intensity))
            val intel = SiegeSystemBountyIntel(market, mult)
            manager.addActive(intel)   // the intel self-queues into the feed in its own constructor
            siege.bountyPosted = true
            LOG.info("Tahlan siege: ${siege.id} — desperation bounty posted at ${market.name}")
        } catch (e: Exception) {
            LOG.warn("Tahlan siege: failed to post desperation bounty at ${market.name} — ${e.message}")
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    // F1 — Huntsman task force
    //
    // One standing elite hunter-killer per siege, travelling with the command fleet. It is the
    // siege's punishment arm, so it is emphatically NOT a siege fleet: killing it moves no siege
    // stat and drops no salvage. What it buys instead is a respite window — a replacement is
    // dispatched from the siege source market, and the window is however long the delay plus the
    // trip out takes.
    // ═════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Heat: the player's own kill record against this siege, decaying daily. Crossing the threshold
     * marks them as the task force's priority prey; the mark clears again once heat decays back
     * down (with hysteresis, so a player sitting on the line does not flicker in and out).
     */
    private fun advanceHeat(siege: SiegeData, days: Float) {
        if (siege.playerHeat > 0f) {
            siege.playerHeat = max(0f, siege.playerHeat - SiegeConfig.HEAT_DECAY_PER_DAY * days)
        }
        if (!siege.playerMarked && siege.playerHeat >= SiegeConfig.HEAT_MARK_THRESHOLD) {
            siege.playerMarked = true
            siege.intel?.notifyPlayerMarked()
            LOG.info("Tahlan siege: ${siege.id} — huntsmen have marked the player " +
                    "(heat ${"%.0f".format(siege.playerHeat)})")
        } else if (siege.playerMarked &&
            siege.playerHeat < SiegeConfig.HEAT_MARK_THRESHOLD * SiegeConfig.HEAT_UNMARK_FRACTION) {
            siege.playerMarked = false
            LOG.info("Tahlan siege: ${siege.id} — player heat decayed below the mark")
        }
    }

    private fun advanceTaskForce(siege: SiegeData, days: Float) {
        if (!SiegeConfig.TASKFORCE_ENABLED) return
        if (siege.taskForceRedispatchTimer < 0f) return
        siege.taskForceRedispatchTimer -= days
        if (siege.taskForceRedispatchTimer > 0f) return
        siege.taskForceRedispatchTimer = -1f
        // Replacements only keep coming while there is still a siege to punish for.
        if (!siege.commandFleetPresent) return
        siege.taskForceFleet = spawnTaskForce(siege, sendToTarget = true)
    }

    /**
     * Called from [SiegeTaskForceListener]. Deliberately cheap and side-effect-light: it arms the
     * redispatch timer, which [advanceTaskForce] picks up on the safe side of the tick boundary.
     */
    fun onTaskForceLost(siegeId: String) {
        val siege = findSiege(siegeId) ?: return
        siege.taskForceFleet = null
        if (!SiegeConfig.TASKFORCE_ENABLED) return
        // BESIEGING only, PLANETFALL deliberately excluded: the redispatch delay alone outlasts the
        // landing window, so a replacement armed here could never arrive — killing the huntsmen during
        // the landing simply buys the rest of it.
        if (siege.stage != SiegeData.Stage.BESIEGING || !siege.commandFleetPresent) return
        if (siege.taskForceRedispatchTimer >= 0f) return   // a replacement is already inbound
        siege.taskForceRedispatchTimer = SiegeConfig.TASKFORCE_REDISPATCH_DELAY_DAYS
        siege.intel?.notifyTaskForceReplacement()
        LOG.info("Tahlan siege: $siegeId — huntsmen destroyed, replacement in " +
                "${"%.0f".format(SiegeConfig.TASKFORCE_REDISPATCH_DELAY_DAYS)} days")
    }

    /**
     * Spawn the huntsmen at the siege source market. [sendToTarget] is false at launch (the fleet
     * travels with the command fleet on the shared travel order issued by launchSiege) and true for
     * a replacement, which has to make the trip on its own.
     *
     * Roster is decided HERE, at spawn time, not at launch: kill the Blackwatch pack after the
     * Legio awakening and what comes back for you is daemons.
     */
    private fun spawnTaskForce(siege: SiegeData, sendToTarget: Boolean): CampaignFleetAPI? {
        val source = siege.sourceMarket
        val awakened = Global.getSector().memoryWithoutUpdate.getBoolean(TahlanIDs.TRIGGERED)
        val roster = if (awakened) TahlanIDs.DAEMONS else TahlanIDs.BLACKWATCH
        var fp = SiegeConfig.TASKFORCE_FP_BASE +
                SiegeConfig.TASKFORCE_FP_SCALE * SiegeConfig.intensityFactor(siege.intensity)
        if (awakened) fp *= SiegeConfig.TASKFORCE_DAEMON_FP_MULT

        val fleet = buildHunterFleet(source, roster, fp) ?: run {
            LOG.warn("Tahlan siege: ${siege.id} — could not build a task force from roster $roster")
            return null
        }
        fleet.setFaction(TahlanIDs.LEGIO, true)
        fleet.name = txt("siege_fleet_taskforce_name")
        fleet.memoryWithoutUpdate.set(FLEET_SIEGE_ID_KEY, siege.id)
        fleet.memoryWithoutUpdate.set(FLEET_TASKFORCE_KEY, true)
        fleet.memoryWithoutUpdate.set(FLEET_FP_KEY, fp)
        // Set explicitly: the task force is outside tagSiegeFleet, and a recoverable pure-daemon
        // pack respawning every month would be an outright hull-farming jackpot.
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true)

        source.primaryEntity.containingLocation.addEntity(fleet)
        fleet.setLocation(source.primaryEntity.location.x, source.primaryEntity.location.y)
        if (sendToTarget) {
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, siege.targetSystem.center, 1000f,
                txt("siege_assign_travel").format(siege.targetSystem.nameWithLowercaseType))
        }
        fleet.addEventListener(SiegeTaskForceListener(siege.id))
        fleet.addScript(SiegeTaskForceAI(fleet, source, siege.id, siege.targetSystem))
        LOG.info("Tahlan siege: ${siege.id} — task force spawned from $roster " +
                "(${fleet.fleetPoints} FP, burn ${fleet.fleetData.burnLevel})")
        return fleet
    }

    /**
     * Build a hunter-killer fleet from [rosterFaction], keeping only hulls at or above
     * [SiegeConfig.TASKFORCE_MIN_BURN]. Fleet burn is the *slowest* member, so a single lumbering
     * capital would make the whole force uncatchable-by-nobody — the filter is what makes the
     * huntsmen mechanically what their name says.
     *
     * Strip-and-refill with bounded retries: inflate, drop the slow hulls, re-roll the freed budget,
     * repeat. Accepts an under-budget fleet rather than looping forever if the roster runs dry.
     */
    private fun buildHunterFleet(source: MarketAPI, rosterFaction: String, targetFp: Float): CampaignFleetAPI? {
        val base = rollRosterFleet(source, rosterFaction, targetFp) ?: return null
        stripSlowMembers(base)
        if (base.fleetData.membersListCopy.isEmpty()) return null   // roster has no fast hulls at all

        var attempts = 0
        while (attempts < SiegeConfig.TASKFORCE_BUILD_RETRIES) {
            val deficit = targetFp - base.fleetPoints
            if (deficit <= targetFp * TASKFORCE_BUDGET_TOLERANCE) break
            attempts++
            val extra = rollRosterFleet(source, rosterFaction, deficit) ?: break
            stripSlowMembers(extra)
            val members = extra.fleetData.membersListCopy
            if (members.isEmpty()) continue
            for (member in members) {
                extra.fleetData.removeFleetMember(member)
                base.fleetData.addFleetMember(member)
            }
        }
        base.fleetData.ensureHasFlagship()
        base.forceSync()
        return base
    }

    /**
     * One inflation pass on an off-faction roster. Same trick as the Blackwatch command fleet:
     * PRIORITY_THEN_ALL is REQUIRED because the source market is Legio, so a differing factionId
     * would otherwise put FleetFactoryV3 into IMPORTED mode and roll the roster's meagre
     * `shipsWhenImporting` list instead of its real one.
     */
    private fun rollRosterFleet(source: MarketAPI, rosterFaction: String, fp: Float): CampaignFleetAPI? {
        if (fp <= 0f) return null
        // MERC_ARMADA mirrors the command fleet's proven off-faction spawn exactly; the fleet type
        // only colors naming/quality here, and this is the combination already known to roll a real
        // Blackwatch roster off a Legio market rather than an empty import list.
        val params = FleetParamsV3(source, FleetTypes.MERC_ARMADA, fp, 0f, 0f, 0f, 0f, 0f, 0.25f)
        params.factionId = rosterFaction
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL
        params.officerNumberMult = 2f
        val fleet = FleetFactoryV3.createFleet(params)
        return if (fleet == null || fleet.isEmpty) null else fleet
    }

    private fun stripSlowMembers(fleet: CampaignFleetAPI) {
        for (member in fleet.fleetData.membersListCopy) {
            if (member.isFighterWing) continue
            val burn = member.stats?.maxBurnLevel?.modifiedValue ?: 0f
            if (burn < SiegeConfig.TASKFORCE_MIN_BURN) fleet.fleetData.removeFleetMember(member)
        }
    }

    /** Polled by [SiegeTaskForceAI]: is the player currently the huntsmen's priority prey? */
    fun isPlayerMarked(siegeId: String): Boolean = findSiege(siegeId)?.playerMarked == true

    /** Polled by [SiegeTaskForceAI] for its idle screening station. */
    fun getCommandFleet(siegeId: String): CampaignFleetAPI? =
        findSiege(siegeId)?.commandFleet?.takeIf { it.isAlive }

    /** True while the siege is still being tracked; false once it has resolved or been torn down. */
    fun isSiegeActive(siegeId: String): Boolean = findSiege(siegeId) != null

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
            // Planetfall keeps sweeping: the system is still strangled, and a market that changes
            // hands or makes peace during the landing must still shed the condition.
            if (siege.stage != SiegeData.Stage.BESIEGING && siege.stage != SiegeData.Stage.PLANETFALL) continue
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
     * Reconciliation backstop for a siege lost from tracking WITHOUT passing through [resolveSiege]
     * or [tearDown] — the duplicate-manager corruption is the proven instance of that failure class.
     * Every regular cleanup route reaches conditions and intel through the siege's own SiegeData, so
     * a siege this manager no longer knows about would leave its pressure condition suppressing
     * markets forever and its intel hanging in the feed as "active", with nothing ever able to reach
     * either again. Sweeping from the world side instead — every conditioned market, every unresolved
     * [SiegeIntel] — against the active-siege list converts any such failure into one that self-heals
     * within a sweep interval. Same slow cadence as [maintainPressureConditions]; racing a regular
     * resolution is harmless (removeCondition tolerates repeats, and resolve() is idempotent).
     */
    private fun reconcileOrphans() {
        // A condition may legitimately exist only in a system under a BESIEGING or PLANETFALL siege
        // (it is applied on arrival). Multiple concurrent sieges are possible via the console
        // command, hence the set. Hidden markets are swept too: applyPressureCondition never
        // conditions one, so a condition there is an orphan by definition.
        val besiegedSystems = activeSieges
            .filter { it.stage == SiegeData.Stage.BESIEGING || it.stage == SiegeData.Stage.PLANETFALL }
            .map { it.targetSystem }
            .toSet()
        for (market in Global.getSector().economy.marketsCopy) {
            if (!market.hasCondition(TahlanIDs.SIEGE_CONDITION_ID)) continue
            if (market.starSystem in besiegedSystems) continue
            try { market.removeCondition(TahlanIDs.SIEGE_CONDITION_ID) } catch (_: Exception) {}
            LOG.info("Tahlan siege: reconciled an orphaned siege condition off ${market.name}")
        }

        // Intel matches against ANY live siege, unlike the condition pass: an INBOUND siege
        // legitimately has live intel while its target system carries no conditions yet.
        val activeSystems = activeSieges.map { it.targetSystem }.toSet()
        for (intel in Global.getSector().intelManager.getIntel(SiegeIntel::class.java).filterIsInstance<SiegeIntel>()) {
            if (intel.isResolved) continue
            if (intel.besiegedSystem in activeSystems) continue
            intel.resolve(SiegeIntel.SiegeOutcome.LIFTED)
            LOG.info("Tahlan siege: reconciled an orphaned siege intel entry for ${intel.besiegedSystem.baseName}")
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

        // The huntsmen ride out with the expedition. Spawned last and given the same travel order:
        // its own AI takes over once it reaches the target system. Not a hard failure point — a
        // siege without a task force is simply the pre-reactivity siege.
        if (SiegeConfig.TASKFORCE_ENABLED) {
            siege.taskForceFleet = spawnTaskForce(siege, sendToTarget = false)?.also { tf ->
                tf.clearAssignments()
                tf.addAssignment(FleetAssignment.GO_TO_LOCATION, travelDest, 1000f,
                    txt("siege_assign_travel").format(targetSystem.nameWithLowercaseType))
            }
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
        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return null
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
            val fleet = FleetFactoryV3.createFleet(params)
            if (fleet == null || fleet.isEmpty) continue
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
        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return
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
        // 1000f is the effectively-unlimited travel sentinel every other despawn leg uses. A timed
        // leg can expire mid-trip and strand the raider — it carries no AI script, so nothing would
        // re-order it until dispersal.
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, siege.sourceMarket.primaryEntity, 1000f)
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
        // The task force disperses home like any other siege-attached fleet.
        siege.taskForceFleet?.takeIf { it.isAlive }?.memoryWithoutUpdate?.set(FLEET_RETURN_FLAG, true)
        // Intervention fleets go home to their OWN markets, not to Legio's — so signal their AIs
        // via the shared return flag and let each one route itself back where it came from.
        for (fleet in siege.interventionFleets.filter { it.isAlive }) {
            fleet.memoryWithoutUpdate.set(FLEET_RETURN_FLAG, true)
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
        // PLANETFALL is not a safe stage: driving siege health to zero breaks the landing exactly as
        // it breaks the blockade. INBOUND is excluded because health damage before arrival is handled
        // by flushKill's decapitation rule instead.
        val live = siege.stage == SiegeData.Stage.BESIEGING || siege.stage == SiegeData.Stage.PLANETFALL
        if (live && siege.siegeHealth <= 0f) {
            resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN)
        }
    }

    // --- Pruning (task 3.3) ---

    private fun pruneDeadSieges() {
        val toRemove = mutableListOf<SiegeData>()
        for (siege in activeSieges) {
            // Live stages only survive the reap. PLANETFALL is emphatically live: leaving it out of
            // this set would have a planetfall siege removed on the very first tick after it starts.
            if (siege.stage != SiegeData.Stage.INBOUND &&
                siege.stage != SiegeData.Stage.BESIEGING &&
                siege.stage != SiegeData.Stage.PLANETFALL) {
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
            // Intervention fleets are not part of the siege force, so they never gate the
            // "everything is dead" check — but their references still need reaping or the list
            // grows for the life of the siege.
            siege.interventionFleets.removeAll { f ->
                f.containingLocation == null ||
                !f.containingLocation.fleets.contains(f) ||
                !f.isAlive
            }
            if (cmdGone && !siege.commandFleetPresent && noEscorts && siege.raidFleets.isEmpty()) {
                // All fleets gone and command already accounted for — auto-broken (mopped up).
                // Stage here is always one of the three live ones (later stages were filtered above),
                // and an INBOUND or PLANETFALL wipe must resolve its intel too, or the entry hangs in
                // the feed forever.
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

    /**
     * Needs no per-stage handling, PLANETFALL included: [disperseFleets] covers a converged force the
     * same way it covers a blockading one, and [SiegeAssignmentAI] checks FLEET_RETURN_FLAG ahead of
     * its phase switch, so the return order wins over any planetfall order already in fleet memory.
     *
     * Also the wind-down for a pruned duplicate manager's sieges — see [getOrCreate], which passes
     * its own [reason] so the log tells the two apart.
     */
    fun tearDown(reason: String = "feature disabled mid-save") {
        for (siege in activeSieges.toList()) {
            removePressureCondition(siege)
            siege.intel?.resolve(SiegeIntel.SiegeOutcome.LIFTED)
            disperseFleets(siege, keepCommandFleet = false)
        }
        activeSieges.clear()
        LOG.info("Tahlan siege: torn down ($reason)")
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
            sb.appendLine("  [planetfall] timer=" +
                    (if (s.stage == SiegeData.Stage.PLANETFALL) "${"%.2f".format(s.planetfallTimer)}d of ${SiegeConfig.PLANETFALL_DURATION_DAYS}d" else "n/a (stage ${s.stage})") +
                    "  defenderSweepIn=${"%.2f".format(s.defenderSweepCooldown)}d (every ${SiegeConfig.DEFENDER_SWEEP_INTERVAL_DAYS}d)")
            val cmd = s.commandFleet
            sb.appendLine("  commandFleet=" + if (cmd == null) "null"
                else "${cmd.name} [alive=${cmd.isAlive}, fp=${cmd.fleetPoints}, at=${cmd.starSystem?.baseName ?: cmd.containingLocation?.name ?: "?"}]")
            sb.appendLine("  escortFleets=${s.escortFleets.count { it.isAlive }} alive / ${s.escortFleets.size} tracked")
            sb.appendLine("  raidFleets=${s.raidFleets.count { it.isAlive }} alive / ${s.raidFleets.size} tracked (max ${SiegeConfig.MAX_ACTIVE_RAID_FLEETS})")
            sb.appendLine("  conditionedMarkets=${s.conditionedMarkets.joinToString { it.name }.ifEmpty { "none" }}")
            sb.appendLine("  garrisonMarket=${s.garrisonMarket?.name ?: "none"}  playerBountyAccrued=${"%.0f".format(s.playerBountyAccrued)}")
            sb.appendLine("  [F3] interventions=${s.interventionFleets.count { it.isAlive }} alive / ${s.interventionFleets.size} tracked  cooldown=${"%.1f".format(s.interventionCooldown)}d  enabled=${SiegeConfig.INTERVENTION_ENABLED}")
            sb.appendLine("  [F2] bountyPosted=${s.bountyPosted}  keepAliveIn=${"%.1f".format(s.bountyTimer)}d  triggerAt=${SiegeConfig.BOUNTY_TRIGGER_PROGRESS}  enabled=${SiegeConfig.BOUNTY_ENABLED}")
            val tf = s.taskForceFleet
            sb.appendLine("  [F1] taskForce=" + (if (tf == null) "none" else "${tf.name} [alive=${tf.isAlive}, fp=${tf.fleetPoints}, burn=${tf.fleetData.burnLevel}, at=${tf.starSystem?.baseName ?: "?"}]") +
                    "  redispatchIn=${if (s.taskForceRedispatchTimer < 0f) "n/a" else "%.1fd".format(s.taskForceRedispatchTimer)}")
            sb.appendLine("  [F1] playerHeat=${"%.0f".format(s.playerHeat)}/${SiegeConfig.HEAT_MARK_THRESHOLD}  marked=${s.playerMarked}  enabled=${SiegeConfig.TASKFORCE_ENABLED}")
        }
        return sb.toString()
    }

    /**
     * Jump the subjugation meter on every active siege (for the TahlanSiegeProgress command). Exists
     * for the planetfall pass specifically: reaching the climax honestly takes an in-game season, so
     * there has to be a way to park the meter just short of full and watch the landing from there.
     * Writes the meter only — the next [advanceHealthModel] tick is what actually trips planetfall,
     * so the whole entry path is exercised rather than bypassed.
     */
    fun debugSetProgress(value: Float): String {
        if (activeSieges.isEmpty()) return "No active sieges."
        val clamped = value.coerceIn(0f, SiegeConfig.CAPTURE_PROGRESS_MAX)
        for (siege in activeSieges) {
            siege.captureProgress = clamped
            siege.intel?.syncProgress(siege)
        }
        return "Set captureProgress to ${"%.1f".format(clamped)}/${SiegeConfig.CAPTURE_PROGRESS_MAX} " +
                "on ${activeSieges.size} active siege(s)."
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
        // Set on every converging siege fleet when the landing starts; the value is the target
        // planet's entity id. Same reason as the garrison key for going through fleet memory rather
        // than a manager poll: SiegeAssignmentAI and SiegeBlockadeAI each read it independently, and
        // the command fleet has to keep acting on it after resolveSiege drops the siege.
        const val FLEET_PLANETFALL_KEY = "\$tahlan_siege_planetfall"

        // Reactive-system fleet tags. Both these fleet kinds carry FLEET_SIEGE_ID_KEY (so despawn
        // pruning and battle-side checks can attribute them to a siege) but NOT the rest of
        // tagSiegeFleet — they are deliberately not siege fleets and carry no SiegeFleetListener,
        // so their losses never feed siege health, command CR, the meter, or the bounty ledger.
        const val FLEET_INTERVENTION_KEY = "\$tahlan_siege_intervention"
        const val FLEET_TASKFORCE_KEY    = "\$tahlan_siege_taskforce"

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

        /** Below this, an "intervention" is a rounding error — skip it rather than spawn a token. */
        private const val MIN_INTERVENTION_FP = 20f

        /** How far under its FP budget a burn-filtered task force may land before we stop re-rolling. */
        private const val TASKFORCE_BUDGET_TOLERANCE = 0.15f

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
            // Prune stray duplicate managers (legacy corruption from the pre-fix duplicate-on-load
            // bug). A dup is wound down via tearDown, not just dropped: both managers ran advance()
            // blind to each other, so the dup can hold live sieges of its own — silently discarding
            // those orphans their fleets, conditions and intel forever. If the dup besieged the same
            // system as the survivor, tearDown may strip a condition the survivor legitimately
            // tracks; that self-heals within one sweep, since maintainPressureConditions re-applies
            // idempotently.
            for (dup in managers) {
                if (dup !== mgr) {
                    dup.tearDown("duplicate manager pruned")
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
