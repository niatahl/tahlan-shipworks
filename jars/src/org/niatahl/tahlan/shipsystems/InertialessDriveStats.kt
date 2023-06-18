package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import java.awt.Color

class InertialessDriveStats : BaseShipSystemScript() {
    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.maxSpeed.unmodify(id)
            stats.maxTurnRate.unmodify(id)
        } else {
            stats.maxSpeed.modifyFlat(id, 100f * effectLevel)
            stats.acceleration.modifyFlat(id, 1000f * effectLevel)
            stats.deceleration.modifyFlat(id, 1000f * effectLevel)
            stats.turnAcceleration.modifyFlat(id, 300f * effectLevel)
            stats.maxTurnRate.modifyMult(id, 1f + 2f * effectLevel)
        }
        ship.setJitterUnder(ship, SHIMMER_COLOR, 1f, 6, 2f, 4f)
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        stats.maxSpeed.unmodify(id)
        stats.maxTurnRate.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return if (index == 0) {
            StatusData("improved maneuverability", false)
        } else null
    }

    companion object {
        private val SHIMMER_COLOR = Color(96, 251, 171, 117)
    }
}