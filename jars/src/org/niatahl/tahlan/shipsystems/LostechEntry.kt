package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.entities.SimpleEntity
import org.niatahl.tahlan.plugins.CustomRender
import org.niatahl.tahlan.utils.modify
import java.awt.Color

class LostechEntry : BaseShipSystemScript() {

    private val zapterval = IntervalUtil(0.05f, 0.05f)
    private val imageval = IntervalUtil(0.05f, 0.05f)
    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.maxSpeed.unmodify(id) // to slow down ship to its regular top speed while powering drive down
        } else {
            stats.maxSpeed.modifyFlat(id, 600f * effectLevel)
            stats.acceleration.modifyFlat(id, 600f * effectLevel)
        }
        ship.setJitterUnder(ship, SHIMMER_COLOR, 1f, 12, 2f, 6f)
        ship.setJitter(ship, SHIMMER_COLOR, 1f, 3, 2f, 4f)

        //Choose a random vent port to send lightning from
        zapterval.advance(Global.getCombatEngine().elapsedInLastFrame)
        imageval.advance(Global.getCombatEngine().elapsedInLastFrame)
        if (zapterval.intervalElapsed()) {
            val bounds = ship.exactBounds
            bounds.update(ship.location, ship.facing)
            val origin = bounds.segments.random().p1
            val angle = VectorUtils.getAngle(ship.location, origin)
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

        if (imageval.intervalElapsed()) {
            CustomRender.addAfterimage(
                ship,
                SHIMMER_COLOR,
                AFTERIMAGE_COLOR,
                0.8f,
                4f
            )
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        stats.maxSpeed.unmodify(id)
        stats.maxTurnRate.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)

        val ship = stats.entity as ShipAPI
        val bounds = ship.exactBounds
        bounds.update(ship.location, ship.facing)

        for (i in 0..50) {
            val origin = bounds.segments.random().p1
            val angle = VectorUtils.getAngle(ship.location, origin)
            val target = SimpleEntity(MathUtils.getRandomPointInCone(origin, 200f, angle - 45f, angle + 45f))
            Global.getCombatEngine().spawnEmpArc(
                ship,
                origin,
                ship,
                target,
                DamageType.ENERGY,  //Damage type
                0f,  //Damage
                0f,  //Emp
                100000f,  //Max range
                null,  //Impact sound
                8f,  // thickness of the lightning bolt
                LIGHTNING_CORE_COLOR,  //Central color
                LIGHTNING_FRINGE_COLOR //Fringe Color
            )
            CustomRender.addNebula(
                location = origin,
                velocity = Misc.ZERO,
                size = 30f,
                endSizeMult = 2f,
                duration = 1f,
                inFraction = 0.3f,
                outFraction = 0.5f,
                color = LIGHTNING_CORE_COLOR.modify(alpha = 80),
                layer = CombatEngineLayers.ABOVE_SHIPS_LAYER,
                expandAsSqrt = true
            )
        }
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return if (index == 0) {
            StatusData("increased engine power", false)
        } else null
    }

    companion object {
        private val SHIMMER_COLOR = Color(96, 251, 171, 70)
        private val AFTERIMAGE_COLOR = Color(255, 105, 0, 70)
        private val LIGHTNING_CORE_COLOR = Color(195, 255, 230, 150)
        private val LIGHTNING_FRINGE_COLOR = Color(24, 156, 124, 200)
    }
}