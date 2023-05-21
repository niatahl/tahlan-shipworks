package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import org.niatahl.tahlan.utils.random
import java.awt.Color

class EradicatorBeamEffect : BeamEffectPlugin {
    private val fireInterval = IntervalUtil(0.2f, 0.3f)
    private val flashInterval = IntervalUtil(0.1f, 0.1f)
    private var wasZero = true
    override fun advance(amount: Float, engine: CombatEngineAPI, beam: BeamAPI) {
        flashInterval.advance(engine.elapsedInLastFrame)
        if (flashInterval.intervalElapsed()) {
            val size = beam.width * MathUtils.getRandomNumberInRange(2f, 2.2f)
            val dur = MathUtils.getRandomNumberInRange(0.2f, 0.25f)
            engine.addHitParticle(beam.from, beam.source.velocity, beam.width, 0.8f, dur, beam.coreColor)
            engine.addHitParticle(beam.from, beam.source.velocity, size, 0.8f, dur, beam.fringeColor.brighter())
            if (beam.didDamageThisFrame()) engine.addHitParticle(beam.to, beam.source.velocity, size * 3f, 0.8f, dur, beam.fringeColor)
        }

        val flare1 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val flare2 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        MagicRender.singleframe(
            /* sprite = */ flare1,
            /* loc = */ MathUtils.getRandomPointInCircle(beam.from, (0f..2f).random()),
            /* size = */ Vector2f(200f, 10f),
            /* angle = */ 0f,
            /* color = */ Color(255, 30, 0, 80),
            /* additive = */ true
        )
        MagicRender.singleframe(
            /* sprite = */ flare2,
            /* loc = */ MathUtils.getRandomPointInCircle(beam.from, (0f..2f).random()),
            /* size = */ Vector2f(150f, 2f),
            /* angle = */ 0f,
            /* color = */ Color(255, 200, 200, 100),
            /* additive = */ true
        )

        val target = beam.damageTarget
        if (target is ShipAPI && beam.brightness >= 1f) {
            var dur = beam.damage.dpsDuration
            // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
            if (!wasZero) dur = 0f
            wasZero = beam.damage.dpsDuration <= 0
            fireInterval.advance(dur)
            if (fireInterval.intervalElapsed()) {
                val hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.to)
                //piercedShield = true;
                if (!hitShield) {
                    val dir = Vector2f.sub(beam.to, beam.from, Vector2f())
                    if (dir.lengthSquared() > 0) dir.normalise()
                    dir.scale(50f)
                    val point = Vector2f.sub(beam.to, dir, Vector2f())
                    val dam = beam.weapon.damage.damage * 0.1f
                    val arc = engine.spawnEmpArc(
                        beam.source, point, beam.damageTarget, beam.damageTarget,
                        DamageType.ENERGY,
                        dam,
                        0f,
                        10000f,
                        "tachyon_lance_emp_impact",
                        beam.width,
                        beam.fringeColor,
                        beam.coreColor
                    )
                }
            }
        }
    }
}