package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.random.Random

class tahlan_PhaseEngines : EveryFrameWeaponEffectPlugin {
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
            if (alphaMult < 0.5f) (alphaMult + amount * 2f).coerceAtMost(0.5f) else (alphaMult - amount * 2f).coerceAtLeast(
                0.5f
            )
        } else {
            (alphaMult - amount * 2f).coerceAtLeast(0f)
        }

        // jump out if interval hasn't elapsed yet
        val vel = Vector2f(100f, 0f)
        VectorUtils.rotate(vel, ship.facing + 180f)
        for (e in ship.engineController.shipEngines) {
            if (interval1.intervalElapsed())
                Global.getCombatEngine().addNegativeNebulaParticle(
                    e.location,
                    vel,
                    MathUtils.getRandomNumberInRange(40f, 60f),
                    1.2f,
                    0.1f,
                    0.5f,
                    MathUtils.getRandomNumberInRange(1.2f, 1.5f),
                    Color(24, 254, 109, (10 * alphaMult).roundToInt())
                )
            if (interval2.intervalElapsed())
                Global.getCombatEngine().addNebulaParticle(
                    e.location,
                    vel,
                    MathUtils.getRandomNumberInRange(30f, 50f),
                    1.2f,
                    0.1f,
                    0.5f,
                    MathUtils.getRandomNumberInRange(1f, 1.3f),
                    Color(204, 30, 109, (70 * alphaMult).roundToInt())
                )
            if (interval3.intervalElapsed())
                Global.getCombatEngine().addNebulaParticle(
                    e.location,
                    vel,
                    MathUtils.getRandomNumberInRange(30f, 40f),
                    0.6f,
                    0.1f,
                    0.5f,
                    MathUtils.getRandomNumberInRange(0.3f, 0.7f),
                    Color(255, 100, 189, (120 * alphaMult).roundToInt())
                )
            if (interval4.intervalElapsed())
                Global.getCombatEngine().addSwirlyNebulaParticle(
                    MathUtils.getRandomPointInCircle(e.location, 2f),
                    vel,
                    MathUtils.getRandomNumberInRange(20f, 40f),
                    1.1f,
                    0.1f,
                    0.5f,
                    MathUtils.getRandomNumberInRange(0.8f, 1.1f),
                    Color(255, 112, 251, (50 * alphaMult).roundToInt()),
                    false
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