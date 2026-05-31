package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import org.niatahl.tahlan.utils.TahlanIDs

/**
 * Attached to every siege fleet at spawn. Routes kill/despawn events back to SiegeManager.
 * Identified only by IDs so it serializes cleanly with no direct manager reference.
 */
class SiegeFleetListener(
    private val siegeId: String,
    private val fleetFp: Float,
    private val isCommandFleet: Boolean
) : FleetEventListener {

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI
    ) {
        if (fleet.isAlive) return   // fleet survived this battle
        val manager = findManager() ?: return
        manager.onSiegeFleetKilled(siegeId, fleetFp, isCommandFleet, battle.isPlayerInvolved)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI,
        reason: FleetDespawnReason,
        param: Any?
    ) {
        // Battle deaths are already handled by reportBattleOccurred; handle other clean-despawns here
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) return
        val manager = findManager() ?: return
        manager.onSiegeFleetDespawned(siegeId, fleetFp, isCommandFleet)
    }

    private fun findManager(): SiegeManager? =
        Global.getSector().memoryWithoutUpdate.get(TahlanIDs.SIEGE_MANAGER_KEY) as? SiegeManager
}
