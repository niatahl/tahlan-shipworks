package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.Utils.txt
import kotlin.math.sqrt

/**
 * Hunting behavior for the siege's huntsman task force. Travels out with the expedition, then works
 * a strict priority list inside the besieged system, re-evaluated on a short interval:
 *
 *  1. **The player** — but only while *marked*, i.e. once their accumulated heat from killing siege
 *     fleets crosses the threshold. An uninvolved or lightly-involved player is invisible to it.
 *  2. **Threats to the siege** — coalition intervention fleets first (weighted well above everything
 *     else), then hostile military patrols, picked by a weighted roll on strength and proximity.
 *  3. **Nothing to hunt** — screen the command fleet.
 *
 * It never pursues beyond the target system: this is a siege-attached hunter, not a vengeance fleet
 * that follows the player across the sector.
 *
 * Identified by siege id only, resolving [SiegeManager] via [SiegeManager.get], so it serializes
 * with no direct manager reference.
 */
class SiegeTaskForceAI(
    private val fleet: CampaignFleetAPI,
    private val sourceMarket: MarketAPI,
    private val siegeId: String,
    private val targetSystem: StarSystemAPI
) : EveryFrameScript {

    private enum class Phase { TRAVELING, HUNTING, RETURNING, DONE }

    private var phase = Phase.TRAVELING
    private val tick = IntervalUtil(0.3f, 0.5f)
    private var returnOrdered = false
    private var currentPrey: CampaignFleetAPI? = null

    override fun advance(amount: Float) {
        if (!fleet.isAlive) { phase = Phase.DONE; return }

        val days = Misc.getDays(amount)
        tick.advance(days)
        if (!tick.intervalElapsed()) return
        if (fleet.battle != null) return   // never yank mid-combat

        if (fleet.memoryWithoutUpdate.getBoolean(SiegeManager.FLEET_RETURN_FLAG)) { orderReturn(); return }

        val manager = SiegeManager.get()
        if (manager == null || !manager.isSiegeActive(siegeId)) { orderReturn(); return }

        when (phase) {
            Phase.TRAVELING -> {
                if (fleet.containingLocation === targetSystem) { phase = Phase.HUNTING; return }
                if (fleet.currentAssignment == null) {
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, targetSystem.center, 1000f,
                        txt("siege_assign_travel").format(targetSystem.nameWithLowercaseType))
                }
            }

            Phase.HUNTING -> hunt(manager)

            Phase.RETURNING -> {
                if (fleet.currentAssignment == null) phase = Phase.DONE
            }

            Phase.DONE -> { /* no-op */ }
        }
    }

    private fun hunt(manager: SiegeManager) {
        // Chased something out of the system, or got dragged out otherwise: come straight back.
        if (fleet.containingLocation !== targetSystem) {
            if (fleet.currentAssignment == null) {
                currentPrey = null
                fleet.clearAssignments()
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, targetSystem.center, 1000f,
                    txt("siege_assign_travel").format(targetSystem.nameWithLowercaseType))
            }
            return
        }

        // 1. The marked player.
        if (manager.isPlayerMarked(siegeId)) {
            val player = Global.getSector().playerFleet
            if (player != null && player.isAlive && player.containingLocation === targetSystem) {
                pursue(player, txt("siege_assign_hunt").format(player.name))
                return
            }
        }

        // 2/3. Interventions and hostile patrols.
        val threat = pickThreat()
        if (threat != null) {
            pursue(threat, txt("siege_assign_hunt").format(threat.name))
            return
        }

        // 4. Nothing worth killing — hold close to the command fleet.
        screen(manager.getCommandFleet(siegeId))
    }

    /**
     * Weighted pick over in-system enemies: strength x proximity, with coalition relief forces
     * weighted an order of magnitude above ordinary patrols (they are the ones actually threatening
     * the siege). Weighted rather than nearest-first so the huntsmen do not become perfectly
     * predictable. Distance is floored before inverting, per the fleet-behavior playbook, or
     * anything adjacent would swamp the roll.
     */
    private fun pickThreat(): CampaignFleetAPI? {
        val loc = fleet.containingLocation ?: return null
        val legio = Global.getSector().getFaction(TahlanIDs.LEGIO) ?: return null
        val picker = WeightedRandomPicker<CampaignFleetAPI>()

        for (other in loc.fleets) {
            if (other === fleet || !other.isAlive) continue
            // The player is priority 1 and only while marked — never a fallback target here.
            if (other.isPlayerFleet) continue
            val fid = other.faction?.id ?: continue
            if (fid == TahlanIDs.LEGIO || fid == TahlanIDs.BLACKWATCH || fid == TahlanIDs.DAEMONS) continue

            val isIntervention = other.memoryWithoutUpdate.getBoolean(SiegeManager.FLEET_INTERVENTION_KEY)
            if (!isIntervention) {
                if (!legio.isHostileTo(other.faction)) continue
                if (other.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET)) continue
                if (other.fleetPoints <= 0) continue
            }

            var weight = other.fleetPoints.toFloat().coerceAtLeast(1f)
            if (isIntervention) weight *= INTERVENTION_PRIORITY_MULT
            val dist = Misc.getDistance(fleet.location, other.location).coerceAtLeast(DIST_FLOOR)
            weight *= sqrt(DIST_FLOOR * 2f / dist)
            picker.add(other, weight)
        }
        return picker.pick()
    }

    private fun pursue(prey: CampaignFleetAPI, action: String) {
        val battle: BattleAPI? = prey.battle
        if (battle != null && battle.canJoin(fleet) &&
            Misc.getDistance(fleet.location, prey.location) < JOIN_RANGE) {
            battle.join(fleet)
            return
        }
        // Let an in-flight pursuit ride rather than resetting it every tick.
        if (currentPrey === prey && fleet.currentAssignment?.target === prey) return

        currentPrey = prey
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.INTERCEPT, prey, PURSUIT_DURATION_DAYS, action)
    }

    private fun screen(commandFleet: CampaignFleetAPI?) {
        val anchor: SectorEntityToken = commandFleet
            ?: targetSystem.jumpPoints.firstOrNull()
            ?: targetSystem.center
            ?: return
        if (currentPrey == null && fleet.currentAssignment?.target === anchor) return
        currentPrey = null
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, anchor, 9999f,
            txt("siege_assign_screen").format(anchor.name))
    }

    private fun orderReturn() {
        if (returnOrdered) return
        returnOrdered = true
        phase = Phase.RETURNING
        val home = sourceMarket.primaryEntity
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, home, 1000f,
            txt("siege_assign_return").format(sourceMarket.name))
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f)
    }

    override fun isDone(): Boolean = phase == Phase.DONE
    override fun runWhilePaused(): Boolean = false

    companion object {
        private const val JOIN_RANGE = 200f
        private const val PURSUIT_DURATION_DAYS = 20f
        private const val DIST_FLOOR = 3000f
        /** Relief forces are what actually endanger the siege; patrols are incidental. */
        private const val INTERVENTION_PRIORITY_MULT = 10f
    }
}

/**
 * Death watch for the huntsman task force. Reports the loss so [SiegeManager] can arm the
 * replacement cycle — and nothing else. Killing the huntsmen is deliberately *not* progress against
 * the siege: no siege-health damage, no command-CR strain, no meter knockback, no bounty accrual.
 * All it buys is a respite window while the next pack makes the trip out.
 */
class SiegeTaskForceListener(private val siegeId: String) : FleetEventListener {

    private var reported = false

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI
    ) {
        if (reported || fleet.isAlive) return
        reported = true
        SiegeManager.get()?.onTaskForceLost(siegeId)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI,
        reason: FleetDespawnReason,
        param: Any?
    ) {
        if (reported) return
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) return   // handled above
        reported = true
        // Harmless once the siege has resolved: onTaskForceLost no-ops on an untracked siege id,
        // so a task force despawning at the end of its trip home dispatches no replacement.
        SiegeManager.get()?.onTaskForceLost(siegeId)
    }
}
