package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.listeners.FleetEventListener

/**
 * Attached to every siege fleet at spawn. Routes loss/despawn events back to SiegeManager.
 * Identified only by IDs so it serializes cleanly with no direct manager reference.
 *
 * Losses are reported as a **delta in fleet points**, not as a binary "died / survived": [lastFp]
 * tracks the fleet's FP as of the previous battle, so grinding a fleet down over several engagements
 * feeds attrition incrementally instead of only paying out on the killing blow. The spawn-time FP is
 * merely the starting value of that ledger.
 */
class SiegeFleetListener(
    private val siegeId: String,
    private val fleetFp: Float,
    private val isCommandFleet: Boolean
) : FleetEventListener {

    /** FP the fleet had after the previous battle; the baseline the next loss is measured against. */
    private var lastFp: Float = fleetFp

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI
    ) {
        // Migration guard: listeners from saves that predate this field deserialize lastFp as 0
        // (Java deserialization skips constructors), which would read as "already wiped out".
        if (lastFp == 0f) lastFp = fleetFp

        val current = if (fleet.isAlive) fleet.fleetPoints.toFloat() else 0f
        val lost = lastFp - current
        lastFp = current
        if (lost <= 0f) return   // fleet came out intact (or somehow reinforced)

        val manager = findManager() ?: return
        manager.onSiegeFleetLosses(
            siegeId, lost, isCommandFleet,
            destroyed = !fleet.isAlive,
            playerFraction = battle.playerInvolvementFraction
        )
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

    private fun findManager(): SiegeManager? = SiegeManager.get()
}
