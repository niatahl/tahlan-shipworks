package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs

/**
 * Blockade-fleet behavior: holds station on its assigned jump point, but breaks off to INTERCEPT any
 * trade fleet inbound to the siege's primary target market, then drifts back to the jump point once
 * the prey is gone. Combined with the $cfai "hostile to trade fleets" flag (set at spawn when
 * SiegeConfig.BLOCKADE_HOSTILE_TO_TRADERS is on), this lets the blockade actually interdict traders
 * regardless of base faction relations.
 *
 * Stops cleanly when the siege ends: the manager either sets FLEET_RETURN_FLAG or replaces the
 * fleet's orders with a GO_TO_LOCATION_AND_DESPAWN during dispersal — either signals the AI to retire.
 */
class SiegeBlockadeAI(
    private val fleet: CampaignFleetAPI,
    private val jumpPoint: SectorEntityToken,
    private val targetMarket: MarketAPI?,
    private val siegeId: String
) : EveryFrameScript {

    private val tick = IntervalUtil(0.25f, 0.4f)
    private var chasing: CampaignFleetAPI? = null
    private var done = false

    override fun advance(amount: Float) {
        if (!fleet.isAlive) { done = true; return }

        val days = Misc.getDays(amount)
        tick.advance(days)
        if (!tick.intervalElapsed()) return
        if (fleet.battle != null) return  // never yank mid-combat

        // Siege ended — manager flagged a return, or dispersal has us heading home to despawn.
        if (fleet.memoryWithoutUpdate.getBoolean(SiegeManager.FLEET_RETURN_FLAG)) { done = true; return }
        if (fleet.currentAssignment?.assignment == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN) {
            done = true; return
        }

        val target = targetMarket ?: run { holdStation(); return }

        // Already chasing a valid trader — let the INTERCEPT ride.
        val current = chasing
        if (current != null) {
            if (isValidPrey(current, target)) return
            chasing = null  // prey gone/docked/left — re-evaluate below
        }

        val prey = pickPrey(target)
        if (prey != null) {
            chasing = prey
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.INTERCEPT, prey, SiegeConfig.BLOCKADE_INTERCEPT_DURATION_DAYS,
                "intercepting ${prey.name}")
            // After the chase, fall back to blockading the jump point.
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, jumpPoint, 9999f,
                "blockading ${jumpPoint.name}")
        } else {
            holdStation()
        }
    }

    private fun pickPrey(target: MarketAPI): CampaignFleetAPI? {
        val loc = fleet.containingLocation ?: return null
        return loc.fleets
            .filter { isValidPrey(it, target) }
            .minByOrNull { Misc.getDistance(fleet.location, it.location) }
    }

    private fun isValidPrey(other: CampaignFleetAPI, target: MarketAPI): Boolean {
        if (other === fleet) return false
        if (!other.isAlive || other.containingLocation != fleet.containingLocation) return false
        // Never chase our own side.
        val fid = other.faction.id
        if (fid == TahlanIDs.LEGIO || fid == TahlanIDs.BLACKWATCH) return false
        // Trade fleets only.
        if (!other.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET)) return false
        return isInboundTo(other, target)
    }

    /** Inbound = its current assignment points at the besieged market, or it has closed to within range. */
    private fun isInboundTo(other: CampaignFleetAPI, target: MarketAPI): Boolean {
        val marketEntity = target.primaryEntity ?: return false
        if (other.currentAssignment?.target === marketEntity) return true
        return Misc.getDistance(other.location, marketEntity.location) < SiegeConfig.BLOCKADE_INTERCEPT_RANGE
    }

    private fun holdStation() {
        if (fleet.currentAssignment != null) return
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, jumpPoint, 9999f, "blockading ${jumpPoint.name}")
    }

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false
}
