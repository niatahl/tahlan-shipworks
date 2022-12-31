package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.modify
import java.awt.Color

class PenetrationDriveStats : BaseShipSystemScript() {
    private val FLICKER_COLOR = Color(129, 110, 99, 131)
    private val AFTERIMAGE_COLOR = Color(129, 80, 64, 100)
    private val ENGINE_COLOR = Color(255, 179, 155, 255)
    private val BLAST_COLOR = Color(146, 226, 50, 57)
    val MAX_TIME_MULT = 3f

    private val collided = ArrayList<ShipAPI>()

    private val interval = IntervalUtil(0.05f, 0.05f)

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        val engine = Global.getCombatEngine()

        val TimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel
        stats.timeMult.modifyMult(id, TimeMult)
        ship.engineController.fadeToOtherColor(this, ENGINE_COLOR, Color(0, 0, 0, 0), effectLevel, 0.67f)
        val driftamount = engine.elapsedInLastFrame
        interval.advance(engine.elapsedInLastFrame)
        if (interval.intervalElapsed()) {
            ship.addAfterimage(
                AFTERIMAGE_COLOR,
                0f,
                0f,
                ship.velocity.getX() * -1f,
                ship.velocity.getY() * -1f,
                5f, 0f, 0.1f, 0.5f, true, true, false
            )
        }

        if (state == ShipSystemStatsScript.State.IN) {
            ship.mutableStats.acceleration.modifyFlat(id, SPEED)
            ship.mutableStats.deceleration.modifyFlat(id, SPEED)
        } else if (state == ShipSystemStatsScript.State.ACTIVE) {
            ship.isPhased = true
            ship.extraAlphaMult = 0.25f
            ship.setApplyExtraAlphaToEngines(true)
            ship.setJitter(ship, FLICKER_COLOR, 0.5f, 5, 5f, 5f)
            stats.acceleration.unmodify(id)
            stats.deceleration.unmodify(id)
            val speed = ship.velocity.length()
            val direction = VectorUtils.rotate(Vector2f(1f,0f),ship.facing)
            ship.velocity.set(direction)
            ship.velocity.scale(speed)
            if (speed <= 300f) {
                ship.velocity.set(VectorUtils.getDirectionalVector(ship.location, ship.velocity))
            }
            if (speed < 900f) {
                ship.velocity.normalise()
                ship.velocity.scale(speed + driftamount * SPEED)
            }
        } else {
            val speed = ship.velocity.length()
            if (speed > ship.mutableStats.maxSpeed.modifiedValue) {
                ship.velocity.normalise()
                ship.velocity.scale(speed - driftamount * SPEED)
            }
        }

        if (state == ShipSystemStatsScript.State.ACTIVE) {
            val nose = ship.hullSpec.allWeaponSlotsCopy.find { it.isSystemSlot } ?: return
            val point = nose.computePosition(ship)
            CombatUtils.getShipsWithinRange(point, 1000f)
                .filter { CollisionUtils.isPointWithinCollisionCircle(point, it) && !collided.contains(it) && it != ship }
                .forEach { target ->
                    var hit = CollisionUtils.isPointWithinBounds(point,target)
                    if (!hit && target.shield != null && target.shield.isOn) {
                        val delta = Vector2f.sub(point,target.location,Vector2f())
                        val angle = VectorUtils.getFacing(delta)
                        val distance = MathUtils.getDistance(target.location,point)
                        hit = ( ( Math.abs(angle - target.shield.facing) < target.shield.activeArc/2f ) && distance < target.shield.radius )
                    }
                    if (hit) {
                        collided.add(target)
                        explode(point, ship)
                        engine.applyDamage(target, point, 2000f, DamageType.KINETIC, 0f, false, false, ship)
                    }
                }
        }

    }

    fun explode(point: Vector2f, source: ShipAPI) {
        val boom = DamagingExplosionSpec(
            1f,
            300f,
            150f,
            1000f,
            500f,
            CollisionClass.PROJECTILE_FF,
            CollisionClass.PROJECTILE_FIGHTER,
            0f,
            0f,
            0f,
            0,
            Color.RED,
            Color.RED
        )
        boom.isShowGraphic = false
        boom.damageType = DamageType.HIGH_EXPLOSIVE
        Global.getCombatEngine().spawnDamagingExplosion(boom,source,point)
        Global.getCombatEngine().addSwirlyNebulaParticle(
            point,
            Misc.ZERO,
            200f,
            3f,
            0.1f,
            0.2f,
            2f,
            BLAST_COLOR,
            true)
        Global.getCombatEngine().spawnExplosion(point,Misc.ZERO,ENGINE_COLOR.modify(alpha = 150),300f,3f)
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        val ship: ShipAPI = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
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
        collided.clear()
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State?, effectLevel: Float): StatusData? {
        return null
    }

    companion object {
        const val SPEED = 5000f
    }

}