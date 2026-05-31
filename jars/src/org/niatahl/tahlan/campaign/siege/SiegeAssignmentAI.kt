package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs

/**
 * Drives the command fleet through its lifecycle: TRAVELING → BESIEGING → (GARRISONING →) RETURNING.
 * Reads withdrawal and garrison signals from SiegeManager via sector memory rather than a direct
 * reference, so serialization is safe.
 */
class SiegeAssignmentAI(
    private val fleet: CampaignFleetAPI,
    private val sourceMarket: MarketAPI,
    private val siegeId: String
) : EveryFrameScript {

    private enum class Phase { TRAVELING, BESIEGING, GARRISONING, RETURNING, DONE }

    private var phase = Phase.TRAVELING
    private val tick = IntervalUtil(0.2f, 0.25f)
    private var returnOrdered = false
    private var garrisonMarket: MarketAPI? = null

    override fun advance(amount: Float) {
        if (!fleet.isAlive) { phase = Phase.DONE; return }

        val days = Misc.getDays(amount)
        tick.advance(days)
        if (!tick.intervalElapsed()) return
        if (fleet.battle != null) return  // never yank mid-combat

        // Check fleet memory return flag (set by manager on siege resolution/teardown)
        if (fleet.memoryWithoutUpdate.getBoolean(SiegeManager.FLEET_RETURN_FLAG)) {
            orderReturn(); return
        }

        val manager = findManager()

        when (phase) {
            Phase.TRAVELING -> {
                if (manager?.isSiegeWithdrawing(siegeId) == true) { orderReturn(); return }
                if (fleet.currentAssignment == null) {
                    // Travel complete — flip siege to BESIEGING and anchor
                    phase = Phase.BESIEGING
                    manager?.onCommandFleetArrived(siegeId, fleet)
                    anchorAtFringe()
                }
            }

            Phase.BESIEGING -> {
                if (manager?.isSiegeWithdrawing(siegeId) == true) { orderReturn(); return }
                if (fleet.currentAssignment == null) anchorAtFringe()

                // Check for garrison order — written to fleet memory by the manager on Nex capture.
                // (The siege is removed from manager tracking at that point, so this must be read
                // independently of the manager rather than polled from it.)
                if (garrisonMarket == null) {
                    val gmId = fleet.memoryWithoutUpdate.getString(SiegeManager.FLEET_GARRISON_MARKET_KEY)
                    val gm = gmId?.let { Global.getSector().economy.getMarket(it) }
                    if (gm != null) {
                        garrisonMarket = gm
                        phase = Phase.GARRISONING
                        fleet.clearAssignments()
                        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE,
                            gm.primaryEntity, SiegeConfig.GARRISON_DURATION_DAYS,
                            "garrisoning ${gm.name}")
                    }
                }
            }

            Phase.GARRISONING -> {
                if (fleet.currentAssignment == null) orderReturn()
            }

            Phase.RETURNING -> {
                if (fleet.currentAssignment == null) phase = Phase.DONE
            }

            Phase.DONE -> { /* no-op */ }
        }
    }

    fun orderReturn() {
        if (returnOrdered) return
        returnOrdered = true
        phase = Phase.RETURNING
        fleet.clearAssignments()
        val home = sourceMarket.primaryEntity
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, home, 1000f,
            "returning to ${sourceMarket.name}")
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f)
    }

    private fun anchorAtFringe() {
        val loc = fleet.containingLocation ?: return
        val anchor = loc.jumpPoints.firstOrNull() ?: fleet.starSystem?.center ?: return
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, anchor, 9999f,
            "besieging ${loc.nameWithLowercaseType}")
    }

    private fun findManager(): SiegeManager? =
        Global.getSector().memoryWithoutUpdate.get(TahlanIDs.SIEGE_MANAGER_KEY) as? SiegeManager

    override fun isDone(): Boolean = phase == Phase.DONE
    override fun runWhilePaused(): Boolean = false
}
