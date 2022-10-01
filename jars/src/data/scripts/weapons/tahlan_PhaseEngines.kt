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

class tahlan_PhaseEngines : EveryFrameWeaponEffectPlugin {
    private var alphaMult = 0f
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        interval.advance(amount)

        // we calculate our alpha every frame since we smoothly shift it
        val ship = weapon.ship
        val ec = ship.engineController
        alphaMult = if (ec.isAccelerating || ec.isStrafingLeft || ec.isStrafingRight) {
            (alphaMult + amount * 2f).coerceAtMost(1f)
        } else if (ec.isDecelerating || ec.isAcceleratingBackwards) {
            if (alphaMult < 0.5f) (alphaMult + amount * 2f).coerceAtMost(0.5f) else (alphaMult - amount * 2f).coerceAtLeast(0.5f)
        } else {
            (alphaMult - amount * 2f).coerceAtLeast(0f)
        }

        // jump out if interval hasn't elapsed yet
        if (!interval.intervalElapsed()) return
        val vel = Vector2f(100f, 0f)
        VectorUtils.rotate(vel, ship.facing + 180f)
        for (e in ship.engineController.shipEngines) {
            Global.getCombatEngine().addNegativeNebulaParticle(
                e.location,
                vel,
                MathUtils.getRandomNumberInRange(40f, 60f),
                1.5f,
                0.1f,
                0.5f,
                MathUtils.getRandomNumberInRange(1.2f, 1.5f),
                Color(24, 254, 109, (10 * alphaMult).roundToInt())
            )
            Global.getCombatEngine().addNebulaParticle(
                e.location,
                vel,
                MathUtils.getRandomNumberInRange(30f, 50f),
                1.5f,
                0.1f,
                0.5f,
                MathUtils.getRandomNumberInRange(1f, 1.3f),
                Color(204, 30, 109, (70 * alphaMult).roundToInt())
            )
            Global.getCombatEngine().addSwirlyNebulaParticle(
                MathUtils.getRandomPointInCircle(e.location, 2f),
                vel,
                MathUtils.getRandomNumberInRange(20f, 40f),
                1.3f,
                0.1f,
                0.5f,
                MathUtils.getRandomNumberInRange(0.8f, 1.1f),
                Color(255, 112, 251, (50 * alphaMult).roundToInt()),
                false
            )
        }
    }

    companion object {
        val interval = IntervalUtil(0.06f, 0.07f)
    }
}