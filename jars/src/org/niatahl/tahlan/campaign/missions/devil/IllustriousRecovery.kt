package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.apache.log4j.Logger
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_CLUE1
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_CLUE2
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_CLUE3
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_HULL
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_RECOVERED
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_REF
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO

/**
 * One-time quest given by Louisa Ferre (tahlan_devil, Blackwatch) to recover the unique
 * Nightwatch command-ship, the Illustrious.
 *
 * Shape (mirrors vanilla RecoverAPlanetkiller's stage/waypoint structure, offered through the
 * contact mechanic like DaemonCoreSale):
 *   GO_TO_CLUE1 -> GO_TO_CLUE2 -> GO_TO_CLUE3 -> RECOVER_SHIP -> COMPLETED
 * Each clue site is a Nightwatch dead-drop guarded by an escalating, faceless Legio fleet.
 * Defeating the guards seizes the dead-drop and reveals the next coordinate. The final dead-drop
 * reveals the drifting Illustrious, which is salvaged peacefully (no guard) with three D-mods.
 *
 * Stage advancement: each guard fleet carries a defeat-trigger (Misc.addDefeatTrigger) that fires a
 * rules row on defeat; the row sets the matching clue flag, which advances the trail via
 * connectWithGlobalFlag. See data/campaign/rules.csv (tahlan_illustriousClueNDefeated rows).
 *
 * The post-recovery restoration is handled separately by the RestoreIllustrious rule command, since
 * it is offered by Louisa after this mission has already completed.
 *
 * NOTE: written against the API but not compiled in this environment; expect to iterate against the
 * IntelliJ compiler (see the change's tasks 6.x).
 */
class IllustriousRecovery : HubMissionWithBarEvent() {

    enum class Stage {
        GO_TO_CLUE1, GO_TO_CLUE2, GO_TO_CLUE3, RECOVER_SHIP, COMPLETED, FAILED
    }

    // Picked at create(); persisted with the mission so waypoints survive save/load.
    var clue1System: StarSystemAPI? = null
    var clue2System: StarSystemAPI? = null
    var clue3System: StarSystemAPI? = null
    var wreckSystem: StarSystemAPI? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        val person = person ?: return false

        // One-time gate: never offer again once the ship has been recovered.
        if (Global.getSector().memoryWithoutUpdate.getBoolean(ILLUSTRIOUS_RECOVERED)) return false

        // Reputation gate: COOPERATIVE (the top relationship level) with Louisa herself.
        if (person.relToPlayer.level.ordinal < RepLevel.COOPERATIVE.ordinal) return false

        // Single active instance bound to the person (also dedupes re-creation).
        if (!setPersonMissionRef(person, ILLUSTRIOUS_REF)) return false

        if (barEvent) setGiverIsPotentialContactOnSuccess()

        setNoAbandon()
        setRepPersonChangesNone()
        setRepFactionChangesNone()

        // --- Pick four distinct, out-of-the-way systems for the trail and the wreck. ---
        clue1System = pickQuestSystem()
        clue2System = pickQuestSystem(clue1System)
        clue3System = pickQuestSystem(clue2System, clue1System)
        wreckSystem = pickQuestSystem(clue3System, clue2System, clue1System)
        if (clue1System == null || clue2System == null || clue3System == null || wreckSystem == null) {
            LOG.warn(
                "Illustrious: aborting mission creation — could not pick four distinct quest systems " +
                    "(clue1=${clue1System?.name}, clue2=${clue2System?.name}, " +
                    "clue3=${clue3System?.name}, wreck=${wreckSystem?.name})"
            )
            return false
        }
        LOG.info(
            "Illustrious: trail systems chosen — clue1=${clue1System?.name}, clue2=${clue2System?.name}, " +
                "clue3=${clue3System?.name}, wreck=${wreckSystem?.name}"
        )

        // --- Stage graph ---
        setStartingStage(Stage.GO_TO_CLUE1)
        setSuccessStage(Stage.COMPLETED)
        setFailureStage(Stage.FAILED)

        // Defeating each guard seizes that dead-drop and advances the trail.
        connectWithGlobalFlag(Stage.GO_TO_CLUE1, Stage.GO_TO_CLUE2, ILLUSTRIOUS_CLUE1)
        connectWithGlobalFlag(Stage.GO_TO_CLUE2, Stage.GO_TO_CLUE3, ILLUSTRIOUS_CLUE2)
        connectWithGlobalFlag(Stage.GO_TO_CLUE3, Stage.RECOVER_SHIP, ILLUSTRIOUS_CLUE3)
        // Complete when the salvaged Illustrious actually enters the player's fleet. The peaceful
        // wreck has no defeat trigger to set a flag (unlike the clue guards), so detect the hull
        // directly — mirroring RestoreIllustrious.findIllustrious(). The COMPLETED trigger below
        // then sets ILLUSTRIOUS_RECOVERED as a *consequence* of completion (cf. vanilla
        // RecoverAPlanetkiller's separate $pk_completed condition vs. $pk_recovered latch). Gating
        // this transition on ILLUSTRIOUS_RECOVERED itself would deadlock: nothing sets that flag
        // until the COMPLETED stage is reached.
        connectWithCustomCondition(Stage.RECOVER_SHIP, Stage.COMPLETED) {
            Global.getSector().playerFleet.fleetData.membersListCopy.any {
                it.hullSpec.hullId == ILLUSTRIOUS_HULL
            }
        }

        // --- Per-hop spawns: escalating faceless Legio guards over the dead-drop. ---
        spawnClueGuard(Stage.GO_TO_CLUE1, clue1System!!, FleetSize.SMALL, FleetTypes.PATROL_SMALL, 1)
        spawnClueGuard(Stage.GO_TO_CLUE2, clue2System!!, FleetSize.MEDIUM, FleetTypes.PATROL_MEDIUM, 2)
        spawnClueGuard(Stage.GO_TO_CLUE3, clue3System!!, FleetSize.LARGE, FleetTypes.PATROL_LARGE, 3)

        // --- Final wreck: the drifting, undefended Illustrious. ---
        beginStageTrigger(Stage.RECOVER_SHIP)
        triggerSpawnDerelict(makeIllustriousDerelict(), LocData(pickEntityIn(wreckSystem!!)))
        val wreckRefKey = "\$tahlan_illustrious_wreckRef"
        triggerSaveGlobalEntityRef(wreckRefKey)
        triggerRunScriptAfterDelay(0f) {
            val wreck = getEntityFromGlobal(wreckRefKey)
            if (wreck == null) {
                LOG.warn("Illustrious: wreck FAILED to spawn in ${wreckSystem?.name}; player will find an empty system")
            } else {
                LOG.info("Illustrious: wreck spawned in ${wreckSystem?.name} at ${wreck.containingLocation?.name}")
            }
        }
        endTrigger()

        // Set the recovered latch when the mission completes (i.e. when the wreck is salvaged).
        beginStageTrigger(Stage.COMPLETED)
        triggerSetGlobalMemoryValue(ILLUSTRIOUS_RECOVERED, true)
        endTrigger()

        return true
    }

    /**
     * Spawns the escalating Legio guard fleet over a hop's dead-drop. The guard patrols a fixed point
     * in the system and carries a defeat-trigger that, on defeat, fires the rules row which sets the
     * hop's clue flag (advancing the trail).
     */
    private fun spawnClueGuard(stage: Stage, system: StarSystemAPI, size: FleetSize, type: String, hop: Int) {
        val anchor = pickEntityIn(system)
        beginStageTrigger(stage)
        triggerCreateFleet(size, FleetQuality.HIGHER, LEGIO, type, system)
        triggerSetFleetFaction(LEGIO)
        triggerFleetSetNoFactionInName()              // faceless: no Legio leadership named
        triggerMakeHostile()
        triggerMakeFleetIgnoredByOtherFleets()
        triggerFleetNoAutoDespawn()
        triggerOrderFleetPatrol(anchor)
        triggerFleetMakeImportant("\$tahlan_illustrious_guard", stage)
        triggerFleetAddDefeatTrigger("tahlan_illustriousClue${hop}Defeated")
        // Actually place the fleet in the world. triggerCreateFleet only builds the fleet object;
        // without an explicit spawn it is never added to a StarSystem, so the player is pointed at an
        // empty system. This mirrors the vanilla pattern (create -> configure -> orders -> spawn).
        triggerSpawnFleetNear(anchor, "\$tahlan_illustrious_guard${hop}Flag", null)
        val refKey = "\$tahlan_illustrious_guard${hop}Ref"
        triggerSaveGlobalFleetRef(refKey)
        // Runtime spawn confirmation: fires just after the guard is placed, when this stage becomes
        // active. A missing/empty fleet here is the "player pointed at an empty system" failure — the
        // trail can only advance by defeating a guard that never spawned.
        triggerRunScriptAfterDelay(0f) {
            val fleet = getEntityFromGlobal(refKey) as? CampaignFleetAPI
            if (fleet == null || fleet.fleetData.membersListCopy.isEmpty()) {
                LOG.warn(
                    "Illustrious: clue$hop guard FAILED to spawn in ${system.name} (fleet=$fleet); " +
                        "player will find an empty system and the trail cannot advance"
                )
            } else {
                LOG.info(
                    "Illustrious: clue$hop guard spawned in ${system.name} — " +
                        "${fleet.fleetData.membersListCopy.size} ships, ${fleet.fleetPoints.toInt()} FP, " +
                        "at ${fleet.containingLocation?.name}"
                )
            }
        }
        endTrigger()
    }

    /**
     * Save-repair for pre-fix saves: earlier builds created each clue guard but never spawned it into
     * its system (see [spawnClueGuard]), leaving the player at an empty system with no way forward.
     * Because a mission's triggers are serialized with the save, that defect persists for the current
     * *and* not-yet-reached clue stages even on the fixed jar — so this rebuilds the current clue
     * stage's guard directly, bypassing the stale triggers.
     *
     * Spawns only if the current stage is a clue hop, its clue flag is not yet set, and no guard is
     * already present (detected by the hop's defeat trigger, which both the normal and repair paths
     * attach). Idempotent: a no-op on healthy saves and new games. Driven on an interval by
     * [IllustriousGuardRepair].
     */
    fun ensureCurrentGuardSpawned() {
        val hop: Int
        val system: StarSystemAPI?
        val fleetType: String
        val combatPts: Float
        val clueFlag: String
        when (currentStage) {
            Stage.GO_TO_CLUE1 -> { hop = 1; system = clue1System; fleetType = FleetTypes.PATROL_SMALL;  combatPts = 40f;  clueFlag = ILLUSTRIOUS_CLUE1 }
            Stage.GO_TO_CLUE2 -> { hop = 2; system = clue2System; fleetType = FleetTypes.PATROL_MEDIUM; combatPts = 90f;  clueFlag = ILLUSTRIOUS_CLUE2 }
            Stage.GO_TO_CLUE3 -> { hop = 3; system = clue3System; fleetType = FleetTypes.PATROL_LARGE;  combatPts = 160f; clueFlag = ILLUSTRIOUS_CLUE3 }
            else -> return
        }
        val sys = system ?: return

        // Guard was defeated (flag set) but the stage hasn't ticked over yet — don't respawn it.
        if (Global.getSector().memoryWithoutUpdate.getBoolean(clueFlag)) return

        // A guard is already in place (healthy save / new game / already repaired).
        val defeatTrigger = "tahlan_illustriousClue${hop}Defeated"
        if (sys.fleets.any { Misc.getDefeatTriggers(it, false)?.contains(defeatTrigger) == true }) return

        // Build and place the missing guard, mirroring spawnClueGuard's intended end state.
        val anchor = pickEntityIn(sys)
        val params = FleetParamsV3(sys.location, LEGIO, null, fleetType, combatPts, 0f, 0f, 0f, 0f, 0f, 0.25f)
        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.fleetData.membersListCopy.isEmpty()) {
            LOG.warn("Illustrious: save-repair could not build a clue$hop guard for ${sys.name} (fleet=$fleet)")
            return
        }
        fleet.setNoFactionInName(true)
        sys.addEntity(fleet)
        fleet.setLocation(anchor.location.x, anchor.location.y)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true)
        fleet.memoryWithoutUpdate.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
        Misc.makeImportant(fleet, "\$tahlan_illustrious_guard")
        Misc.addDefeatTrigger(fleet, defeatTrigger)
        fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, anchor, 1_000_000f)
        LOG.info(
            "Illustrious: save-repair spawned missing clue$hop guard in ${sys.name} — " +
                "${fleet.fleetData.membersListCopy.size} ships at ${anchor.name}"
        )
    }

    /** Builds the recoverable Illustrious derelict in a battered state with exactly three D-mods. */
    private fun makeIllustriousDerelict(): DerelictShipData {
        val variant = Global.getSettings().getVariant("${ILLUSTRIOUS_HULL}_standard").clone()
        // Exactly three D-mods (no destroyed-only mods); addDmods=false below so no extras are layered on.
        DModManager.addDMods(variant, false, 3, genRandom)
        val ship = PerShipData(variant, ShipCondition.BATTERED, "Illustrious", null, 0f)
        ship.addDmods = false
        return DerelictShipData(ship, false)
    }

    /** Picks a planet, then jump point, then the system center to anchor spawns/waypoints on. */
    private fun pickEntityIn(system: StarSystemAPI): SectorEntityToken {
        system.planets.firstOrNull { !it.isStar }?.let { return it }
        system.jumpPoints.firstOrNull()?.let { return it }
        return system.center
    }

    /** Picks an interesting, non-core system distinct from any already chosen. */
    private fun pickQuestSystem(vararg exclude: StarSystemAPI?): StarSystemAPI? {
        resetSearch()
        requireSystemInterestingAndNotCore()
        requireSystemNotEnteredByPlayerFor(30f)
        for (s in exclude) if (s != null) requireSystemNot(s)
        preferSystemUnexplored()
        return pickSystem()
    }

    // Waypoint marker per stage (mirrors RecoverAPlanetkiller.getMapLocation).
    override fun getMapLocation(map: SectorMapAPI?, currentStage: Any?): SectorEntityToken? {
        return when (currentStage) {
            Stage.GO_TO_CLUE1 -> clue1System?.center
            Stage.GO_TO_CLUE2 -> clue2System?.center
            Stage.GO_TO_CLUE3 -> clue3System?.center
            Stage.RECOVER_SHIP -> wreckSystem?.center
            else -> super.getMapLocation(map, currentStage)
        }
    }

    override fun updateInteractionDataImpl() {
        set("\$tahlan_illustrious_ref2", this)
        (currentStage as? Stage)?.let { set("\$tahlan_illustrious_stage", it.name) }
        clue1System?.let { set("\$tahlan_illustrious_clue1System", it.nameWithLowercaseTypeShort) }
        clue2System?.let { set("\$tahlan_illustrious_clue2System", it.nameWithLowercaseTypeShort) }
        clue3System?.let { set("\$tahlan_illustrious_clue3System", it.nameWithLowercaseTypeShort) }
        wreckSystem?.let { set("\$tahlan_illustrious_wreckSystem", it.nameWithLowercaseTypeShort) }
        set("\$tahlan_illustrious_manOrWoman", person.manOrWoman)
    }

    override fun getBaseName(): String = "The Last Illustrious"

    // Waypoint guidance shown in the intel entry.
    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        val sys = when (currentStage) {
            Stage.GO_TO_CLUE1 -> clue1System
            Stage.GO_TO_CLUE2 -> clue2System
            Stage.GO_TO_CLUE3 -> clue3System
            Stage.RECOVER_SHIP -> wreckSystem
            else -> null
        } ?: return false
        val what =
            if (currentStage == Stage.RECOVER_SHIP) "Salvage the drifting Illustrious in the "
            else "Seize the Nightwatch dead-drop in the "
        info.addPara(what + sys.nameWithLowercaseTypeShort, tc, pad)
        return true
    }

    companion object {
        private val LOG: Logger = Global.getLogger(IllustriousRecovery::class.java)
    }
}
