package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color
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
        clue1System = pickQuestSystem() ?: return false
        clue2System = pickQuestSystem(clue1System) ?: return false
        clue3System = pickQuestSystem(clue2System, clue1System) ?: return false
        wreckSystem = pickQuestSystem(clue3System, clue2System, clue1System) ?: return false

        // --- Stage graph ---
        setStartingStage(Stage.GO_TO_CLUE1)
        setSuccessStage(Stage.COMPLETED)
        setFailureStage(Stage.FAILED)

        // Defeating each guard seizes that dead-drop and advances the trail.
        connectWithGlobalFlag(Stage.GO_TO_CLUE1, Stage.GO_TO_CLUE2, ILLUSTRIOUS_CLUE1)
        connectWithGlobalFlag(Stage.GO_TO_CLUE2, Stage.GO_TO_CLUE3, ILLUSTRIOUS_CLUE2)
        connectWithGlobalFlag(Stage.GO_TO_CLUE3, Stage.RECOVER_SHIP, ILLUSTRIOUS_CLUE3)
        // Salvaging the drifting hull sets the recovered latch and completes the mission.
        connectWithGlobalFlag(Stage.RECOVER_SHIP, Stage.COMPLETED, ILLUSTRIOUS_RECOVERED)

        // --- Per-hop spawns: escalating faceless Legio guards over the dead-drop. ---
        spawnClueGuard(Stage.GO_TO_CLUE1, clue1System!!, FleetSize.SMALL, FleetTypes.PATROL_SMALL, 1)
        spawnClueGuard(Stage.GO_TO_CLUE2, clue2System!!, FleetSize.MEDIUM, FleetTypes.PATROL_MEDIUM, 2)
        spawnClueGuard(Stage.GO_TO_CLUE3, clue3System!!, FleetSize.LARGE, FleetTypes.PATROL_LARGE, 3)

        // --- Final wreck: the drifting, undefended Illustrious. ---
        beginStageTrigger(Stage.RECOVER_SHIP)
        triggerSpawnDerelict(makeIllustriousDerelict(), LocData(pickEntityIn(wreckSystem!!)))
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
        endTrigger()
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
            else "Seize the Legio-held Nightwatch dead-drop in the "
        info.addPara(what + sys.nameWithLowercaseTypeShort, tc, pad)
        return true
    }
}
