package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepRewards
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
import org.niatahl.tahlan.utils.TahlanIDs.CB_LEGIO_PURGE
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.Utils.txt

/**
 * Louisa Ferre's high-tier bounty: Blackwatch's internal enemies inside the Legio Infernalis.
 *
 * Three things make this contract different from every other bounty on her table, and all three are
 * deliberate:
 *
 * 1. The target is genuinely [LEGIO]-flagged and [triggerMakeHostile][HubMissionWithBarEvent.triggerMakeHostile]
 *    is applied **without** `triggerMakeLowRepImpact()`. There is no vanilla helper for "hostile, full
 *    consequences" — every `triggerSetStandard*Flags()` variant bundles `MEMORY_KEY_LOW_REP_IMPACT`,
 *    which is exactly why ordinary bounties cost nothing. Omitting it is the mechanism, not an
 *    oversight. [IllustriousRecovery.spawnClueGuard] already does the same.
 * 2. Omitting that flag also re-arms the vanilla witness system:
 *    [com.fs.starfarer.api.impl.campaign.TOffAlarm.notifyNearby] returns early on
 *    `MEMORY_KEY_LOW_REP_IMPACT`, so leaving it off is what lets nearby Legio fleets see the strike
 *    and turn hostile. That same criterion is what the clean-kill bonus is judged on — see
 *    [DevilCustomBounty.isCleanKill].
 * 3. It is gated at FRIENDLY through [getFrequency] returning `0f`, so the creator is excluded from
 *    `BaseCustomBounty.pickCreator`'s picker rather than offered and then failing.
 *
 * Composition comes off the Blackwatch roster — these are people with real ships and real backing —
 * while the flag says Legio. That roster is daemon-heavy, so the size/quality ladder below sits a
 * notch under [CBLegioDeserter]'s at equal difficulty.
 */
class CBLegioPurge : BaseCustomBountyCreator() {

    override fun getId(): String = CB_LEGIO_PURGE

    /**
     * FRIENDLY gate. `0f` is a hard exclusion: `pickCreator` adds each eligible creator to a
     * [com.fs.starfarer.api.util.WeightedRandomPicker] weighted by this value, and a zero weight is
     * never picked.
     */
    override fun getFrequency(mission: HubMissionWithBarEvent, difficulty: Int): Float {
        val person = mission.person ?: return 0f
        if (person.relToPlayer.level.ordinal < RepLevel.FRIENDLY.ordinal) return 0f
        return super.getFrequency(mission, difficulty) * PURGE_FREQ
    }

    override fun getBountyNamePostfix(mission: HubMissionWithBarEvent, data: CustomBountyData): String =
        txt("devilcb_purge_postfix")

    // Purge work sits at the top of the dossier range: it is the reward for trust, not an entry-level
    // contract, and the Blackwatch-roster fleets would be unfair below this.
    override fun getMinDifficulty(): Int = 6

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

        // One notch below CBLegioDeserter at equal difficulty — the Blackwatch roster weights daemons
        // 12 to 6 over standard Legio hulls, and daemon fleets scale further still under
        // LegioFleetInflationListener in adaptive/hard mode.
        val size: FleetSize
        val quality: FleetQuality
        val type: String
        val oQuality: OfficerQuality
        val oNum: OfficerNum
        when (difficulty) {
            6 -> {
                size = FleetSize.MEDIUM; quality = FleetQuality.DEFAULT; type = FleetTypes.PATROL_MEDIUM
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT
            }
            7 -> {
                size = FleetSize.LARGE; quality = FleetQuality.DEFAULT; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.MORE
            }
            8 -> {
                size = FleetSize.LARGE; quality = FleetQuality.HIGHER; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.MORE
            }
            9 -> {
                size = FleetSize.VERY_LARGE; quality = FleetQuality.HIGHER; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.HIGHER; oNum = OfficerNum.MORE
            }
            else -> {
                size = FleetSize.HUGE; quality = FleetQuality.HIGHER; type = FleetTypes.PATROL_LARGE
                oQuality = OfficerQuality.HIGHER; oNum = OfficerNum.MORE
            }
        }

        beginFleet(mission, data)
        mission.triggerCreateFleet(size, quality, BLACKWATCH, type, data.system)
        mission.triggerSetFleetOfficers(oNum, oQuality)
        mission.triggerAutoAdjustFleetSize(size, size.next())
        // Composed from Blackwatch's roster, flying under Legio colours — which is what makes killing
        // it cost Legio standing.
        mission.triggerSetFleetFaction(LEGIO)
        mission.triggerFleetSetShipPickMode(ShipPickMode.PRIORITY_THEN_ALL)

        // DO NOT replace this with triggerSetStandardHostileNonPirateFlags() or any other
        // triggerSetStandard*Flags() helper: every one of them also calls triggerMakeLowRepImpact(),
        // which would silently undo both the reputation stake and the witness alarm (see the class
        // doc). Hostile only, on purpose.
        mission.triggerMakeHostile()
        mission.triggerPickLocationAtInSystemJumpPoint(data.system)
        mission.triggerSpawnFleetAtPickedLocation(null, null)
        mission.triggerOrderFleetPatrol(data.system, true, Tags.JUMP_POINT, Tags.SALVAGEABLE, Tags.PLANET)
        data.fleet = createFleet(mission, data) ?: return null

        // Set explicitly rather than via setRepChangesBasedOnDifficulty: that helper keys off
        // difficulty alone and caps repFaction at SMALL, so a difficulty-9 purge would pay Louisa and
        // Blackwatch exactly what an infinite, consequence-free difficulty-9 deserter hunt pays. The
        // whole point of purge work is that it is the fast lane from FRIENDLY to COOPERATIVE.
        data.repPerson = RepRewards.VERY_HIGH
        data.repFaction = RepRewards.MEDIUM
        data.baseReward = CBStats.getBaseBounty(difficulty, PURGE_MULT, mission)

        return data
    }

    companion object {
        /** Deliberately below [CBStats.DESERTER_FREQ] — purge work is the rare high-trust contract. */
        const val PURGE_FREQ = 0.5f

        /**
         * Base payout multiplier, on the vanilla scale that runs `TRADER_MULT` 0.5 to
         * `REMNANT_PLUS_MULT` 3.0. A clean kill lifts this by
         * [DevilCustomBounty.CLEAN_KILL_BONUS] to land near a Remnant hunt.
         */
        const val PURGE_MULT = 1.2f
    }
}
