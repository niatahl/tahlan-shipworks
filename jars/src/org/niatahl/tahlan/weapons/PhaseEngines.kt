package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.plugins.CustomRender
import org.niatahl.tahlan.utils.modify
import org.niatahl.tahlan.utils.random
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.random.Random

class PhaseEngines : EveryFrameWeaponEffectPlugin {
    private var alphaMult = 0f
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        interval1.advance(amount)
        interval2.advance(amount)
        interval3.advance(amount)
        interval4.advance(amount)

        // we calculate our alpha every frame since we smoothly shift it
        val ship = weapon.ship ?: return
        val ec = ship.engineController
        alphaMult = if (ec.isAccelerating) {
            (alphaMult + amount * 2f).coerceAtMost(1f)
        } else if (ec.isDecelerating || ec.isAcceleratingBackwards || ec.isStrafingLeft || ec.isStrafingRight) {
            if (alphaMult < 0.5f) (alphaMult + amount * 2f).coerceAtMost(0.5f) else (alphaMult - amount * 2f).coerceAtLeast(0.5f)
        } else {
            (alphaMult - amount * 2f).coerceAtLeast(0f)
        }

        // jump out if interval hasn't elapsed yet
        val vel = Vector2f(100f, 0f)
        VectorUtils.rotate(vel, ship.facing + 180f)
        for (e in ship.engineController.shipEngines) {
            if (interval1.intervalElapsed())
                CustomRender.addNebula(
                    e.location,
                    vel,
                    (30f..50f).random(),
                    1.2f,
                    (1f..1.3f).random(),
                    0.1f,
                    0.4f,
                    Color(24, 254, 109).modify(alpha = (10 * alphaMult).roundToInt()),
                    if (Random.nextFloat() > 0.5f) CombatEngineLayers.BELOW_SHIPS_LAYER else CombatEngineLayers.ABOVE_SHIPS_LAYER,
                    negative = true
                )
            if (interval2.intervalElapsed())
                CustomRender.addNebula(
                    e.location,
                    vel,
                    (30f..50f).random(),
                    1.2f,
                    (1f..1.3f).random(),
                    0.1f,
                    0.4f,
                    Color(204, 30, 109).modify(alpha = (60 * alphaMult).roundToInt()),
                    if (Random.nextFloat() > 0.5f) CombatEngineLayers.BELOW_SHIPS_LAYER else CombatEngineLayers.ABOVE_SHIPS_LAYER
                )
            if (interval3.intervalElapsed())
                CustomRender.addNebula(
                    e.location,
                    vel,
                    (30f..40f).random(),
                    0.6f,
                    (0.3f..0.7f).random(),
                    0.1f,
                    0.4f,
                    Color(255, 160, 230).modify(alpha = (90 * alphaMult).roundToInt()),
                    if (Random.nextFloat() > 0.5f) CombatEngineLayers.BELOW_SHIPS_LAYER else CombatEngineLayers.ABOVE_SHIPS_LAYER,
                    CustomRender.NebulaType.NORMAL
                )
            if (interval4.intervalElapsed())
                CustomRender.addNebula(
                    e.location,
                    vel,
                    (20f..40f).random(),
                    0.6f,
                    (0.8f..1.1f).random(),
                    0.1f,
                    0.4f,
                    Color(255, 112, 251).modify(alpha = (40 * alphaMult).roundToInt()),
                    if (Random.nextFloat() > 0.5f) CombatEngineLayers.BELOW_SHIPS_LAYER else CombatEngineLayers.ABOVE_SHIPS_LAYER,
                    CustomRender.NebulaType.SWIRLY
                )
        }
    }

    companion object {
        val interval1 = IntervalUtil(0.03f, 0.09f)
        val interval2 = IntervalUtil(0.03f, 0.09f)
        val interval3 = IntervalUtil(0.03f, 0.09f)
        val interval4 = IntervalUtil(0.03f, 0.09f)
    }
}