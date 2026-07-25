package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.Utils.txt

/**
 * Assignment AI for a coalition intervention fleet: travel to the besieged system, find the siege's
 * command fleet, and go at it. If the command fleet is gone before the relief force arrives, it
 * settles for whatever siege force remains; if the siege is over entirely, it turns around and goes
 * home. No rendezvous or coordination with the other coalition fleets — piecemeal arrival is
 * accepted (and is arguably how relief forces actually behave).
 *
 * Identified by siege id only, resolving [SiegeManager] via [SiegeManager.get], so it serializes
 * with no direct manager reference — same shape as [SiegeAssignmentAI] and [SiegeBlockadeAI].
 */
class SiegeInterventionAI(
    private val fleet: CampaignFleetAPI,
    private val homeMarket: MarketAPI,
    private val siegeId: String,
    private val targetSystem: StarSystemAPI
) : EveryFrameScript {

    private enum class Phase { TRAVELING, ENGAGING, RETURNING, DONE }

    private var phase = Phase.TRAVELING
    private val tick = IntervalUtil(0.2f, 0.25f)
    private var returnOrdered = false
    private var currentTarget: CampaignFleetAPI? = null

    override fun advance(amount: Float) {
        if (!fleet.isAlive) { phase = Phase.DONE; return }

        val days = Misc.getDays(amount)
        tick.advance(days)
        if (!tick.intervalElapsed()) return
        if (fleet.battle != null) return   // never yank mid-combat

        // The manager flags this on siege resolution / teardown; each intervention fleet routes
        // itself back to its OWN market rather than being dispersed to a Legio one.
        if (fleet.memoryWithoutUpdate.getBoolean(SiegeManager.FLEET_RETURN_FLAG)) { orderReturn(); return }

        val manager = SiegeManager.get()
        if (manager == null || !manager.isSiegeActive(siegeId)) { orderReturn(); return }

        when (phase) {
            Phase.TRAVELING -> {
                if (fleet.containingLocation === targetSystem) { phase = Phase.ENGAGING; return }
                if (fleet.currentAssignment == null) {
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, targetSystem.center, 1000f,
                        txt("siege_assign_intervene").format(targetSystem.nameWithLowercaseType))
                }
            }

            Phase.ENGAGING -> {
                // Command fleet while it holds; otherwise whatever siege force is left worth hitting.
                val target = manager.getInterventionTarget(siegeId)
                if (target == null || !target.isAlive) { orderReturn(); return }
                engage(target)
            }

            Phase.RETURNING -> {
                if (fleet.currentAssignment == null) phase = Phase.DONE
            }

            Phase.DONE -> { /* no-op */ }
        }
    }

    private fun engage(target: CampaignFleetAPI) {
        // Already brawling within reach — pile in rather than shadowing the fight.
        val battle: BattleAPI? = target.battle
        if (battle != null && battle.canJoin(fleet) &&
            Misc.getDistance(fleet.location, target.location) < JOIN_RANGE) {
            battle.join(fleet)
            return
        }
        // Let an in-flight chase ride instead of re-issuing orders every tick (which would reset
        // the pursuit and leave the fleet yo-yoing).
        if (currentTarget === target && fleet.currentAssignment?.target === target) return

        currentTarget = target
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.INTERCEPT, target, CHASE_DURATION_DAYS,
            txt("siege_assign_engage").format(target.name))
    }

    private fun orderReturn() {
        if (returnOrdered) return
        returnOrdered = true
        phase = Phase.RETURNING
        val home = homeMarket.primaryEntity
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, home, 1000f,
            txt("siege_assign_return").format(homeMarket.name))
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f)
    }

    override fun isDone(): Boolean = phase == Phase.DONE
    override fun runWhilePaused(): Boolean = false

    companion object {
        /** Close enough to join a fight already in progress rather than continuing to close. */
        private const val JOIN_RANGE = 200f
        /** A pursuit order is re-evaluated after this long if it hasn't produced a battle. */
        private const val CHASE_DURATION_DAYS = 30f
    }
}

/**
 * Death watch for a coalition intervention fleet. Interventions are NOT siege fleets, so this
 * deliberately does none of what [SiegeFleetListener] does: no siege-health damage, no meter
 * knockback, no player bounty. The single effect is command-CR strain when the fleet dies *fighting
 * the siege* — the mechanism by which even a failed rescue leaves the siege softer for whoever
 * comes next. A relief force that bounces off cleanly, or dies to something unrelated, changes
 * nothing.
 */
class SiegeInterventionListener(
    private val siegeId: String,
    private val fleetFp: Float
) : FleetEventListener {

    /** FP as of the previous battle — the strain is booked against what actually died, not spawn size. */
    private var lastFp: Float = fleetFp
    private var reported = false

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI
    ) {
        if (reported) return
        // Migration guard, same as SiegeFleetListener: a listener from a save predating this field
        // deserializes lastFp as 0, which would read as "already wiped out".
        if (lastFp == 0f) lastFp = fleetFp

        if (fleet.isAlive) {
            lastFp = fleet.fleetPoints.toFloat()
            return
        }

        // Only a death at the siege's hands counts. The snapshot variant is the one that matters
        // here: the live other-side list can already have been emptied by the time we are called.
        val enemies = try {
            battle.getOtherSideSnapshotFor(fleet)?.takeIf { it.isNotEmpty() }
                ?: battle.getOtherSideFor(fleet)
        } catch (_: Exception) { null } ?: return

        val foughtTheSiege = enemies.any {
            it.memoryWithoutUpdate?.getString(SiegeManager.FLEET_SIEGE_ID_KEY) == siegeId
        }
        if (!foughtTheSiege) return

        reported = true
        SiegeManager.get()?.onInterventionFleetLost(siegeId, lastFp)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI,
        reason: FleetDespawnReason,
        param: Any?
    ) { /* a relief force that goes home, or vanishes off-battle, strains nothing */ }
}
