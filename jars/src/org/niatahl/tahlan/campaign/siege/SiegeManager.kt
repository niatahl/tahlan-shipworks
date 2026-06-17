package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import exerelin.campaign.SectorManager
import exerelin.utilities.NexConfig
import org.niatahl.tahlan.utils.ModCompat
import org.niatahl.tahlan.utils.TahlanSettings
import org.niatahl.tahlan.utils.TahlanIDs
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
        var commandFleetFP      = SiegeConfig.COMMAND_FP_BASE
        var commandFleetPresent = true
        var withdrawalOrdered   = false
        var garrisonMarket: MarketAPI? = null

        var daysSinceLastLoss = 0f
        var daysElapsed       = 0f
        var captureProgress   = 0f
        var raidCooldown      = SiegeConfig.RAID_INTERVAL_DAYS

        var intel: SiegeIntel? = null
        var playerBountyAccrued = 0f
    }

    // --- Fields ---
    private val activeSieges = mutableListOf<SiegeData>()
    private val spawnTimer   = IntervalUtil(SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN, SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX)
    // Pending broken-checks deferred from battle callbacks (safe to do inline would risk CME
    // if resolveSiege touches campaign listeners while CampaignEngine iterates them).
    // @Transient + lazy getter: list is session-only (no cross-save meaning), and guards against
    // the field being null when loaded from a save that predates it (Java deserialization skips
    // constructors, leaving new val fields as null).
    @Transient
    private var _pendingKills: MutableList<Triple<String, Float, Boolean>>? = null
    private val pendingKills: MutableList<Triple<String, Float, Boolean>>
        get() = _pendingKills ?: mutableListOf<Triple<String, Float, Boolean>>().also { _pendingKills = it }
    // Captured when the manager is first created (≈ campaign start, or feature-enable on an existing
    // save); persists with the manager. Used to scale siege intensity off elapsed campaign time.
    private val gameStartTimestamp: Long = Global.getSector().clock.timestamp

    // --- EveryFrameScript ---

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)

        if (!TahlanSettings.ENABLE_SIEGE) {
            if (activeSieges.isNotEmpty()) tearDown()
            return
        }

        // Flush kills that were enqueued during battle callbacks (safe to resolve here,
        // outside any CampaignEngine listener iteration)
        if (pendingKills.isNotEmpty()) {
            for ((siegeId, fleetFp, isCommand) in pendingKills.toList()) {
                flushKill(siegeId, fleetFp, isCommand)
            }
            pendingKills.clear()
        }

        pruneDeadSieges()
        advanceHealthModel(days)

        // Re-derive the spawn cadence from config so a mid-save LunaLib frequency-slider change takes
        // effect — the manager (and its spawnTimer) persists across saves, so the construction-time
        // interval would otherwise be frozen forever. Guarded on a real change: setInterval() rolls a
        // new currInterval and resets elapsed, so calling it every tick would stall the timer.
        if (spawnTimer.minInterval != SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN ||
            spawnTimer.maxInterval != SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX) {
            spawnTimer.setInterval(SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN, SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX)
        }

        spawnTimer.advance(days)
        if (spawnTimer.intervalElapsed() && activeSieges.size < SiegeConfig.ACTIVE_SIEGE_CAP) {
            tryLaunchSiege()
        }
    }

    // --- Callbacks from SiegeFleetListener ---

    /** Called from SiegeFleetListener (inside a battle callback) — enqueues the kill for safe
     *  processing in the next advance() tick, avoiding CME in CampaignEngine's listener iteration. */
    fun onSiegeFleetKilled(siegeId: String, fleetFp: Float, isCommand: Boolean, playerInvolved: Boolean) {
        val siege = findSiege(siegeId) ?: return

        // Stat updates are safe inline (just field writes); complex resolution is deferred.
        siege.daysSinceLastLoss = 0f

        if (isCommand) {
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
        } else {
            // Escort/blockade/raid: uncapped per-FP health damage (floored at 0) + FP-weighted CR strain.
            val healthDmg = fleetFp / SiegeConfig.HEALTH_PER_FP
            siege.siegeHealth = max(0f, siege.siegeHealth - healthDmg)
            siege.commandCR = max(0f, siege.commandCR - fleetFp * SiegeConfig.STRAIN_K)
        }

        if (playerInvolved) {
            val bounty = if (isCommand) SiegeConfig.COMMAND_FLEET_BOUNTY
                         else fleetFp * SiegeConfig.ESCORT_BOUNTY_PER_FP
            siege.playerBountyAccrued += bounty
            siege.intel?.addPlayerBounty(bounty)
        }

        // Defer CR application and broken-check to advance() — safe side of the battle callback boundary
        pendingKills.add(Triple(siegeId, fleetFp, isCommand))
    }

    private fun flushKill(siegeId: String, fleetFp: Float, isCommand: Boolean) {
        val siege = findSiege(siegeId) ?: return
        applyCommandCR(siege)
        checkBroken(siege)
    }

    fun onSiegeFleetDespawned(siegeId: String, fleetFp: Float, isCommand: Boolean) {
        val siege = findSiege(siegeId) ?: return
        if (isCommand && siege.commandFleetPresent && !siege.withdrawalOrdered) {
            // Unexpected non-battle despawn of command fleet — treat as killed for health accounting
            onSiegeFleetKilled(siegeId, fleetFp, true, false)
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
            escort.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, fleet, 9999f, "screening ${fleet.name}")
        }
        spawnBlockadeFleets(siege)
        siege.intel?.updateStage(siege.commandCR)
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
        for (siege in activeSieges.toList()) {
            if (siege.stage != SiegeData.Stage.BESIEGING) continue
            siege.daysElapsed += days

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

            siege.intel?.updateStage(siege.commandCR)

            // Withdrawal at CR floor
            if (!siege.withdrawalOrdered && siege.commandCR <= SiegeConfig.COMMAND_CR_WITHDRAWAL_FLOOR) {
                triggerWithdrawal(siege)
            }

            // Raid sorties
            siege.raidCooldown -= days
            if (siege.raidCooldown <= 0f && siege.raidFleets.size < SiegeConfig.MAX_ACTIVE_RAID_FLEETS) {
                siege.raidCooldown = SiegeConfig.RAID_INTERVAL_DAYS
                spawnRaidFleet(siege)
            }

            // ── Resolution checks ──

            // BROKEN: siege health 0 (universal counter in both pathways)
            if (siege.siegeHealth <= 0f) { resolveSiege(siege, SiegeIntel.SiegeOutcome.BROKEN); continue }

            if (ModCompat.HAS_NEX) {
                // Nex pathway: capture only advances while the command fleet still coordinates the
                // strangle — a withdrawn or destroyed command cannot complete the takeover (task 7a.3)
                val target = siege.primaryTargetMarket
                if (target == null || isNexCaptureBlocked(target)) {
                    // Target can never be captured — e.g. a core/"starting" market while Nex's
                    // allowInvadeStartingMarkets is off. Don't strangle it forever: behave like the
                    // no-Nex case and let the siege run its finite lifetime, then LIFTED.
                    if (siege.daysElapsed >= SiegeConfig.SIEGE_LIFETIME_NO_NEX_DAYS) {
                        resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); continue
                    }
                } else if (siege.commandFleetPresent) {
                    val accessibility = target.accessibilityMod.computeEffective(0f).coerceIn(0f, 1f)
                    val pressureMult = 1f + max(0f, 0.7f - accessibility)  // more strangled = faster
                    siege.captureProgress += SiegeConfig.CAPTURE_PROGRESS_PER_DAY_BASE * pressureMult * days
                    if (siege.captureProgress >= SiegeConfig.CAPTURE_PROGRESS_MAX) {
                        attemptNexCapture(siege); continue
                    }
                }
            } else {
                // No-Nex pathway: finite lifetime → LIFTED with full market recovery (task 7a.1)
                if (siege.daysElapsed >= SiegeConfig.SIEGE_LIFETIME_NO_NEX_DAYS) {
                    resolveSiege(siege, SiegeIntel.SiegeOutcome.LIFTED); continue
                }
            }
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
        // The SiegeAssignmentAI polls isSiegeWithdrawing() and calls orderReturn() on itself
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

    // --- Fleet spawning ---

    private fun tryLaunchSiege() {
        val source = pickSourceMarket() ?: return
        val (targetSystem, primaryMarket) = pickTargetSystem(source) ?: return

        val intensity = computeIntensity()
        val factor = SiegeConfig.intensityFactor(intensity)
        val id = "siege_${targetSystem.id}_${System.nanoTime()}"
        val siege = SiegeData(id, targetSystem, source, primaryMarket)
        siege.intensity = intensity

        // Intel entry
        val intel = SiegeIntel(targetSystem, primaryMarket)
        Global.getSector().intelManager.addIntel(intel)
        siege.intel = intel

        // Command fleet — Blackwatch (task 4.1)
        val commandFP = SiegeConfig.COMMAND_FP_BASE + SiegeConfig.COMMAND_FP_SCALE * factor
        val cmdFleet = spawnCommandFleet(source, commandFP, intensity, id) ?: return
        siege.commandFleet = cmdFleet
        siege.commandFleetFP = cmdFleet.fleetPoints.toFloat()

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
            "en route to ${targetSystem.nameWithLowercaseType}")
        cmdFleet.addScript(SiegeAssignmentAI(cmdFleet, source, id))

        // Escort travel alongside
        for (escort in siege.escortFleets) {
            escort.clearAssignments()
            escort.addAssignment(FleetAssignment.GO_TO_LOCATION, travelDest, 1000f,
                "en route to ${targetSystem.nameWithLowercaseType}")
        }

        activeSieges.add(siege)
        LOG.info("Tahlan siege: launched on ${targetSystem.baseName} from ${source.name} (intensity=${"%.2f".format(intensity)})")
    }

    private fun spawnCommandFleet(source: MarketAPI, fp: Float, intensity: Float, siegeId: String): CampaignFleetAPI? {
        val sMods = (SiegeConfig.COMMAND_SMODS_BASE +
                SiegeConfig.intensityFactor(intensity) * (SiegeConfig.COMMAND_SMODS_MAX - SiegeConfig.COMMAND_SMODS_BASE))
            .toInt().coerceIn(SiegeConfig.COMMAND_SMODS_BASE, SiegeConfig.COMMAND_SMODS_MAX)
        // Inflate with Blackwatch doctrine (elite spearhead ship composition), then reassign the
        // fleet to Legio so it flies under Legio colors and uses Legio relationships. The ships are
        // already rolled by createFleet, so setFaction keeps the Blackwatch loadout intact.
        val params = FleetParamsV3(source, FleetTypes.MERC_ARMADA, fp, 0f, 0f, 0f, 0f, 0f, 0.25f)
        params.factionId = TahlanIDs.BLACKWATCH
        params.averageSMods = sMods
        params.officerNumberMult = 2f

        val fleet = FleetFactoryV3.createFleet(params) ?: return null
        fleet.setFaction(TahlanIDs.LEGIO, true)
        fleet.name = "Vanguard"
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
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, jp, 9999f, "blockading ${jp.name}")
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

        val spawnAnchor = siege.commandFleet?.takeIf { it.isAlive } ?: raidTarget
        siege.targetSystem.addEntity(fleet)
        fleet.setLocation(spawnAnchor.location.x, spawnAnchor.location.y)
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.RAID_SYSTEM, raidTarget, 30f, "raiding ${raidTarget.name}")
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
            val noEscorts = siege.escortFleets.all { f ->
                f.containingLocation == null || !f.isAlive
            }
            if (cmdGone && !siege.commandFleetPresent && noEscorts && siege.raidFleets.isEmpty()) {
                // All fleets gone and command already accounted for — auto-broken (mopped up)
                if (siege.stage == SiegeData.Stage.BESIEGING) {
                    removePressureCondition(siege)
                    siege.intel?.resolve(SiegeIntel.SiegeOutcome.BROKEN)
                }
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

    private fun pickTargetSystem(source: MarketAPI): Pair<StarSystemAPI, MarketAPI?>? {
        val sector = Global.getSector()
        val legioFaction = sector.getFaction(TahlanIDs.LEGIO)
        val activeSystems = activeSieges.map { it.targetSystem }.toSet()

        val picker = WeightedRandomPicker<Pair<StarSystemAPI, MarketAPI?>>()

        for (system in sector.starSystems) {
            if (system in activeSystems) continue

            val allMarkets = sector.economy.marketsCopy.filter { it.starSystem == system && !it.isHidden }

            // Must have at least one hostile market
            val hostileMarkets = allMarkets.filter {
                legioFaction.isHostileTo(sector.getFaction(it.factionId))
            }
            if (hostileMarkets.isEmpty()) continue

            // Exclude systems with existing Legio presence
            if (allMarkets.any { it.factionId == TahlanIDs.LEGIO || it.factionId == TahlanIDs.BLACKWATCH }) continue

            // Nex: filter out story-protected markets; skip system if none remain eligible
            val eligibleMarkets = if (ModCompat.HAS_NEX) {
                hostileMarkets.filter { !isNexProtected(it) }
            } else {
                hostileMarkets
            }
            if (eligibleMarkets.isEmpty()) continue

            // Declare primary target: worst-relation hostile market (task 3.2 + design decision)
            val primaryMarket = eligibleMarkets.minByOrNull { legioFaction.getRelationship(it.factionId) }

            // Weight: combined market size + hostility + prioritize at-war factions
            var weight = eligibleMarkets.sumOf { it.size.toInt() }.toFloat() * 10f
            val worstRel = legioFaction.getRelationship(primaryMarket!!.factionId)
            if (worstRel < -0.5f) weight *= 1.5f
            if (legioFaction.isHostileTo(sector.getFaction(primaryMarket.factionId))) weight *= 2f

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

    private fun isNexProtected(market: MarketAPI): Boolean {
        // Story-critical markets are flagged no-deciv by quests; never capture those (holds even
        // without Nex). Also honor Nex's faction-level "invasion only to retake" territory rule.
        if (market.memoryWithoutUpdate.getBoolean(DecivTracker.NO_DECIV_KEY)) return true
        if (!ModCompat.HAS_NEX) return false
        try {
            if (NexConfig.getFactionConfig(market.factionId)?.invasionOnlyRetake == true) return true
        } catch (_: Exception) {}
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

        val LOG: Logger = Global.getLogger(SiegeManager::class.java)!!
    }
}
