package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.niatahl.tahlan.plugins.CustomRender
import java.awt.Color

class JauntDriveStats : BaseShipSystemScript() {
    private val color = Color(255, 179, 155, 255)
    private val interval = IntervalUtil(0.05f, 0.05f)
    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        var ship: ShipAPI?
        val engine = Global.getCombatEngine()
        if (stats.entity is ShipAPI) {
            ship = stats.entity as ShipAPI
        } else {
            return
        }
        val timeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel
        stats.timeMult.modifyMult(id, timeMult)
        ship.engineController.fadeToOtherColor(this, color, Color(0, 0, 0, 0), effectLevel, 0.67f)
        val driftamount = engine.elapsedInLastFrame
        interval.advance(engine.elapsedInLastFrame)
        if (interval.intervalElapsed()) {
            CustomRender.addAfterimage(
                ship = ship,
                colorIn = AFTERIMAGE_INCOLOR,
                duration = 0.6f,
                jitter = 5f
            )
            for (i in 0..9) {
                engine.addNegativeNebulaParticle(
                    MathUtils.getRandomPointInCircle(ship.location, ship.collisionRadius * 0.75f),
                    MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                    MathUtils.getRandomNumberInRange(60f, 120f),
                    0.3f,
                    0.5f,
                    0.5f,
                    MathUtils.getRandomNumberInRange(1.0f, 1.4f),
                    Color(24, 181, 255)
                )
            }
        }
        if (state == ShipSystemStatsScript.State.IN) {
            ship.mutableStats.acceleration.modifyFlat(id, 5000f)
            ship.mutableStats.deceleration.modifyFlat(id, 5000f)
        } else if (state == ShipSystemStatsScript.State.ACTIVE) {
            ship.isPhased = true
            ship.extraAlphaMult = 0.25f
            ship.setApplyExtraAlphaToEngines(true)
            ship.setJitter(ship, FLICKER_COLOR, 0.5f, 5, 5f, 10f)
            stats.acceleration.unmodify(id)
            stats.deceleration.unmodify(id)
            val speed = ship.velocity.length()
            if (speed <= 0.1f) {
                ship.velocity.set(VectorUtils.getDirectionalVector(ship.location, ship.velocity))
            }
            if (speed < 900f) {
                ship.velocity.normalise()
                ship.velocity.scale(speed + driftamount * 3600f)
            }
        } else {
            val speed = ship.velocity.length()
            if (speed > ship.mutableStats.maxSpeed.modifiedValue) {
                ship.velocity.normalise()
                ship.velocity.scale(speed - driftamount * 3600f)
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        var ship: ShipAPI?
        if (stats.entity is ShipAPI) {
            ship = stats.entity as ShipAPI
        } else {
            return
        }
        stats.maxTurnRate.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        ship.isPhased = false
        ship.extraAlphaMult = 1f
        stats.timeMult.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        stats.empDamageTakenMult.unmodify(id)
        stats.hullDamageTakenMult.unmodify(id)
        stats.armorDamageTakenMult.unmodify(id)
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return null
    }

    override fun getActiveOverride(ship: ShipAPI): Float {
        return when (ship.hullSize) {
            ShipAPI.HullSize.DEFAULT -> 0.6f
            ShipAPI.HullSize.FIGHTER -> 0.6f
            ShipAPI.HullSize.FRIGATE -> 0.6f
            ShipAPI.HullSize.DESTROYER -> 0.5f
            ShipAPI.HullSize.CRUISER -> 0.4f
            ShipAPI.HullSize.CAPITAL_SHIP -> 0.3f
            else -> 0.6f
        }
    }
    override fun getInOverride(ship: ShipAPI): Float {
        return -1f
    }

    override fun getOutOverride(ship: ShipAPI): Float {
        return -1f
    }

    override fun getRegenOverride(ship: ShipAPI): Float {
        return -1f
    }

    override fun getUsesOverride(ship: ShipAPI): Int {
        return -1
    }

    companion object {
        private val FLICKER_COLOR = Color(129, 110, 99, 131)
        private val AFTERIMAGE_INCOLOR = Color(159, 80, 64, 69)
        private val AFTERIMAGE_OUTCOLOR = Color(69, 80, 154, 69)
        const val MAX_TIME_MULT = 5f
    }
}