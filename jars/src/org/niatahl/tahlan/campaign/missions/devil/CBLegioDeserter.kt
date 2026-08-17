package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality
import org.niatahl.tahlan.utils.TahlanIDs.BLACKWATCH
import org.niatahl.tahlan.utils.TahlanIDs.CB_LEGIO_DESERTER
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.Utils.txt

/**
 * Louisa Ferre's low-tier bounty: a genuine Legio Infernalis deserter.
 *
 * Modelled on vanilla [com.fs.starfarer.api.impl.campaign.missions.cb.CBDeserter], with one
 * deliberate departure. Vanilla composes the target fleet from the mission giver's faction; Louisa's
 * faction is Blackwatch, whose roster weights daemons 12 to 6 over standard Legio hulls, so every
 * low-tier deserter would arrive daemon-heavy. The composition faction is a parameter of
 * [HubMissionWithBarEvent.triggerCreateFleet], so it ladders off difficulty here instead: a
 * rank-and-file crew that bolted at the bottom, someone who left with something they shouldn't have
 * at the top.
 *
 * The Legio has disowned these people, so the contract is consequence-free: the fleet is
 * pirate-flagged and carries `MEMORY_KEY_LOW_REP_IMPACT` via
 * [HubMissionWithBarEvent.triggerSetStandardAggroPirateFlags], exactly as vanilla ships it. Compare
 * [CBLegioPurge], which deliberately omits that flag.
 */
class CBLegioDeserter : BaseCustomBountyCreator() {

    override fun getId(): String = CB_LEGIO_DESERTER

    override fun getFrequency(mission: HubMissionWithBarEvent, difficulty: Int): Float =
        super.getFrequency(mission, difficulty) * CBStats.DESERTER_FREQ

    override fun getBountyNamePostfix(mission: HubMissionWithBarEvent, data: CustomBountyData): String =
        txt("devilcb_deserter_postfix")

    override fun getMinDifficulty(): Int = 4

    override fun createBounty(
        createdAt: MarketAPI?,
        mission: HubMissionWithBarEvent,
        difficulty: Int,
        bountyStage: Any?
    ): CustomBountyData? {
        val data = CustomBountyData()
        data.difficulty = difficulty

        mission.requireSystemInterestingAndNotUnsafeOrCore()
        mission.requireSystemNotHasPulsar()
        val system = mission.pickSystem() ?: return null
        data.system = system

        // Size / officer / fleet-type ladder: vanilla CBDeserter's, unchanged.
        val size: FleetSize
        val type: String
        val oQuality: OfficerQuality
        val oNum: OfficerNum
        when {
            difficulty <= 4 -> {
                size = FleetSize.SMALL; type = FleetTypes.PATROL_SMALL
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT
            }
            difficulty <= 5 -> {
                size = FleetSize.MEDIUM; type = FleetTypes.PATROL_MEDIUM
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT
            }
            difficulty == 6 -> {
                size = FleetSize.LARGE; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT
            }
            difficulty == 7 -> {
                size = FleetSize.LARGE; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.MORE
            }
            difficulty == 8 -> {
                size = FleetSize.VERY_LARGE; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.MORE
            }
            difficulty == 9 -> {
                size = FleetSize.HUGE; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.HIGHER; oNum = OfficerNum.MORE
            }
            else -> {
                size = FleetSize.MAXIMUM; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.HIGHER; oNum = OfficerNum.MORE
            }
        }

        // Composition ladder (replaces vanilla's "compose from the giver's faction"). Higher-value
        // deserters are worth more precisely because of what they left with.
        val compositionFaction: String
        val quality: FleetQuality
        when {
            difficulty <= 5 -> { compositionFaction = LEGIO; quality = FleetQuality.DEFAULT }
            difficulty <= 7 -> { compositionFaction = LEGIO; quality = FleetQuality.HIGHER }
            else -> { compositionFaction = BLACKWATCH; quality = FleetQuality.HIGHER }
        }

        beginFleet(mission, data)
        mission.triggerCreateFleet(size, quality, compositionFaction, type, data.system)
        mission.triggerSetFleetOfficers(oNum, oQuality)
        mission.triggerAutoAdjustFleetSize(size, size.next())
        mission.triggerSetFleetFaction(Factions.PIRATES)
        mission.triggerFleetSetShipPickMode(ShipPickMode.PRIORITY_THEN_ALL)

        // The fleet reads as a Legio deserter at every tier — including the top of the ladder, where
        // the hulls came out of the Blackwatch roster. Without this the pirate flag above would show
        // through as a pirate fleet name.
        mission.triggerFleetSetNoFactionInName()
        mission.triggerFleetSetName(txt("devilcb_deserter_fleetname"))

        mission.triggerSetStandardAggroPirateFlags()
        mission.triggerPickLocationAtInSystemJumpPoint(data.system)
        mission.triggerSpawnFleetAtPickedLocation(null, null)
        mission.triggerOrderFleetPatrol(data.system, true, Tags.JUMP_POINT, Tags.SALVAGEABLE, Tags.PLANET)
        data.fleet = createFleet(mission, data) ?: return null

        // Vanilla's difficulty-scaled reputation ladder is the intended value here: deserter work is
        // infinite and consequence-free, so it must stay the slow lane. CBLegioPurge overrides these.
        setRepChangesBasedOnDifficulty(data, difficulty)
        data.baseReward = CBStats.getBaseBounty(difficulty, CBStats.DESERTER_MULT, mission)

        return data
    }
}
