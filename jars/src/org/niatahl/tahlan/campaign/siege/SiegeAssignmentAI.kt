package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.Utils.txt

/**
 * Drives the command fleet through its lifecycle:
 * TRAVELING → BESIEGING → (PLANETFALL →) (GARRISONING →) RETURNING.
 * Reads withdrawal, planetfall and garrison signals from SiegeManager via fleet/sector memory rather
 * than a direct reference, so serialization is safe.
 */
class SiegeAssignmentAI(
    private val fleet: CampaignFleetAPI,
    private val sourceMarket: MarketAPI,
    private val siegeId: String
) : EveryFrameScript {

    private enum class Phase { TRAVELING, BESIEGING, PLANETFALL, GARRISONING, RETURNING, DONE }

    private var phase = Phase.TRAVELING
    private val tick = IntervalUtil(0.2f, 0.25f)
    private var returnOrdered = false
    private var garrisonMarket: MarketAPI? = null
    private var planetfallTarget: SectorEntityToken? = null

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
                // The manager writes the planetfall key when the subjugation meter fills — same
                // read-it-yourself signalling as the garrison key below.
                if (takeUpPlanetfall()) return
                if (fleet.currentAssignment == null) anchorAtFringe()
                checkGarrisonHandoff()
            }

            Phase.PLANETFALL -> {
                // No withdrawal branch: the manager resolves a planetfall siege outright instead of
                // ordering a withdrawal, so isSiegeWithdrawing can never come back true from here.
                // Re-assert the orbit if something knocked it loose.
                if (fleet.currentAssignment == null) holdOverPlanet()
                // The capture writes the garrison key as the window closes — this is the normal
                // handoff now, which is why the check runs in both siege phases.
                checkGarrisonHandoff()
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
            txt("siege_assign_return").format(sourceMarket.name))
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f)
    }

    private fun anchorAtFringe() {
        val loc = fleet.containingLocation ?: return
        val anchor = loc.jumpPoints.firstOrNull() ?: fleet.starSystem?.center ?: return
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, anchor, 9999f,
            txt("siege_assign_besiege").format(loc.nameWithLowercaseType))
    }

    /**
     * Leave the fringe anchor and go sit on the planet. Returns true once the order has been taken up
     * (and the phase switched), so the caller stops running besieging logic on the same tick.
     */
    private fun takeUpPlanetfall(): Boolean {
        val entityId = fleet.memoryWithoutUpdate.getString(SiegeManager.FLEET_PLANETFALL_KEY) ?: return false
        val planet = Global.getSector().getEntityById(entityId) ?: return false
        planetfallTarget = planet
        phase = Phase.PLANETFALL
        holdOverPlanet()
        return true
    }

    private fun holdOverPlanet() {
        val planet = planetfallTarget ?: return
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, planet, 9999f,
            txt("siege_assign_planetfall").format(planet.name))
    }

    /**
     * Pick up the garrison order the manager writes to fleet memory on a successful Nex capture. The
     * siege is removed from manager tracking at that point, so this has to be read off our own memory
     * rather than polled from the manager.
     */
    private fun checkGarrisonHandoff() {
        if (garrisonMarket != null) return
        val gmId = fleet.memoryWithoutUpdate.getString(SiegeManager.FLEET_GARRISON_MARKET_KEY) ?: return
        val gm = Global.getSector().economy.getMarket(gmId) ?: return
        garrisonMarket = gm
        phase = Phase.GARRISONING
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE,
            gm.primaryEntity, SiegeConfig.GARRISON_DURATION_DAYS,
            txt("siege_assign_garrison").format(gm.name))
    }

    private fun findManager(): SiegeManager? = SiegeManager.get()

    override fun isDone(): Boolean = phase == Phase.DONE
    override fun runWhilePaused(): Boolean = false
}
