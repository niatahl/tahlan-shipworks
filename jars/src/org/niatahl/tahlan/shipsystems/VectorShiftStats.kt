package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lazywizard.lazylib.combat.entities.SimpleEntity
import org.niatahl.tahlan.plugins.CustomRender
import java.awt.Color

class VectorShiftStats : BaseShipSystemScript() {
    private val interval = IntervalUtil(0.05f, 0.1f)
    private val imageval = IntervalUtil(0.05f, 0.05f)
    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.maxSpeed.unmodify(id)
            stats.maxTurnRate.unmodify(id)
        }
        stats.maxSpeed.modifyFlat(id, 100f * effectLevel)
        stats.acceleration.modifyFlat(id, 1000f * effectLevel)
        stats.deceleration.modifyFlat(id, 1000f * effectLevel)
        stats.turnAcceleration.modifyFlat(id, 300f * effectLevel)
        stats.maxTurnRate.modifyMult(id, 1f + 2f * effectLevel)
        stats.timeMult.modifyMult(id, 1f + 0.2f * effectLevel)

        ship.setJitterUnder(ship, SHIMMER_COLOR, 1f, 10, 2f, 3f)

        interval.advance(Global.getCombatEngine().elapsedInLastFrame)
        imageval.advance(Global.getCombatEngine().elapsedInLastFrame)
        if (interval.intervalElapsed()) {
            val target = CombatUtils.getEntitiesWithinRange(ship.location, 500f)
                .filter { potential ->
                    (potential is ShipAPI || potential is MissileAPI) && potential.owner != ship.owner
                }
                .randomOrNull()

            //Choose a random vent port to send lightning from
            val bounds = ship.exactBounds
            bounds.update(ship.location, ship.facing)
            val origin = bounds.segments.random().p1
            val angle = VectorUtils.getAngle(ship.location, origin)

            if (target != null) {
                Global.getCombatEngine().spawnEmpArc(
                    ship,
                    origin,
                    ship,
                    target,
                    DamageType.ENERGY,  //Damage type
                    0f,  //Damage
                    100f,  //Emp
                    100000f,  //Max range
                    "tachyon_lance_emp_impact",  //Impact sound
                    8f,  // thickness of the lightning bolt
                    LIGHTNING_CORE_COLOR,  //Central color
                    LIGHTNING_FRINGE_COLOR //Fringe Color
                )
            } else {
                Global.getCombatEngine().spawnEmpArc(
                    ship,
                    origin,
                    ship,
                    SimpleEntity(MathUtils.getRandomPointInCone(origin, 150f, angle - 45f, angle + 45f)),
                    DamageType.ENERGY,  //Damage type
                    0f,  //Damage
                    0f,  //Emp
                    100000f,  //Max range
                    null,  //Impact sound
                    8f,  // thickness of the lightning bolt
                    LIGHTNING_CORE_COLOR,  //Central color
                    LIGHTNING_FRINGE_COLOR //Fringe Color
                )
            }
        }

        if (imageval.intervalElapsed()) {
            CustomRender.addAfterimage(
                ship,
                AFTERIMAGE_IN_COLOR,
                AFTERIMAGE_OUT_COLOR,
                0.5f,
                2f
            )
        }
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
        private val SHIMMER_COLOR = Color(0, 255, 201, 80)
        private val AFTERIMAGE_IN_COLOR = Color(0, 255, 201, 80)
        private val AFTERIMAGE_OUT_COLOR = Color(255, 20, 0, 100)
        private val LIGHTNING_CORE_COLOR = Color(195, 255, 230, 150)
        private val LIGHTNING_FRINGE_COLOR = Color(24, 156, 124, 200)
    }
}