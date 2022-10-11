package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.util.MagicRender
import org.niatahl.tahlan.utils.random
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.plugins.CustomRender
import java.awt.Color

class ManannanEveryFrameEffect : EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val flare1 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val flare2 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val flare3 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val point = Vector2f(10f, 0f);
        VectorUtils.rotate(point, weapon.ship.facing)
        Vector2f.add(point, weapon.location, point)
        MagicRender.singleframe(
            /* sprite = */ flare1,
            /* loc = */ MathUtils.getRandomPointInCircle(point, (0f..2f).random()),
            /* size = */ Vector2f(400f, 10f),
            /* angle = */ 0f,
            /* color = */ Color(255, 50, 50, 80),
            /* additive = */ true
        )
        MagicRender.singleframe(
            /* sprite = */ flare2,
            /* loc = */ MathUtils.getRandomPointInCircle(point, (0f..2f).random()),
            /* size = */ Vector2f(200f, 5f),
            /* angle = */ 0f,
            /* color = */ Color(255, 150, 150, 120),
            /* additive = */ true
        )

        interval1.advance(amount)
        interval2.advance(amount)
        val toRemove: MutableList<DamagingProjectileAPI> = ArrayList()
        projectiles.forEach { proj ->
            if (proj.isFading || proj.didDamage() || !engine.isEntityInPlay(proj)) {
                toRemove.add(proj)
            } else {
                val point2 = Vector2f(-14f, 0f);
                VectorUtils.rotate(point2, proj.facing)
                Vector2f.add(point2, proj.location, point2)
                MagicRender.singleframe(
                    /* sprite = */ flare3,
                    /* loc = */ MathUtils.getRandomPointInCircle(point2, (0f..2f).random()),
                    /* size = */ Vector2f(600f, 14f),
                    /* angle = */ 0f,
                    /* color = */ Color(255, 50, 50, 80),
                    /* additive = */ true
                )

                if (interval1.intervalElapsed())
                    CustomRender.addNebula(
                        proj.location,
                        Misc.ZERO,
                        (20f..40f).random(),
                        2f,
                        (1f..1.3f).random(),
                        0.1f,
                        0.4f,
                        Color((150..255).random(), 0, 0, 40)
                    )
                if (interval2.intervalElapsed())
                    CustomRender.addNebula(
                        proj.location,
                        Misc.ZERO,
                        (10f..20f).random(),
                        1.3f,
                        (0.3f..0.6f).random(),
                        0.1f,
                        0.5f,
                        Color((150..255).random(), 80, 30, 70)
                    )
            }
        }
        toRemove.forEach { proj ->
            projectiles.remove(proj)
        }
    }

    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        projectiles.add(projectile)
    }

    companion object {
        val projectiles: MutableList<DamagingProjectileAPI> = ArrayList()
        val interval1: IntervalUtil = IntervalUtil(0.02f, 0.07f)
        val interval2: IntervalUtil = IntervalUtil(0.02f, 0.07f)
    }
}
