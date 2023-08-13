package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import org.niatahl.tahlan.utils.SystemVectorThrusters

class AuxThrustersStats : BaseShipSystemScript() {

    private var runOnce = false
    private val thrusters = SystemVectorThrusters()
    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.maxSpeed.unmodify(id) // to slow down ship to its regular top speed while powering drive down
            stats.maxTurnRate.unmodify(id)
        } else {
            stats.maxSpeed.modifyFlat(id, 50f * effectLevel)
            stats.acceleration.modifyFlat(id, 100f * effectLevel)
            stats.deceleration.modifyFlat(id, 100f * effectLevel)
            stats.turnAcceleration.modifyFlat(id, 60f * effectLevel)
            stats.maxTurnRate.modifyFlat(id, 30f * effectLevel)
        }

        val ship = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }

        if (!runOnce) {
            thrusters.initEngines(ship)
            runOnce = true
        }
        thrusters.handleThrusters(ship, state, effectLevel)
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




}
