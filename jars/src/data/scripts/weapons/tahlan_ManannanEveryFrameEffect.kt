package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.util.MagicRender
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class tahlan_ManannanEveryFrameEffect : EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {

        val flare1 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val flare2 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val flare3 = Global.getSettings().getSprite("fx", "tahlan_lens_flare2")
        val point = Vector2f(10f,0f);
        VectorUtils.rotate(point,weapon.ship.facing)
        Vector2f.add(point,weapon.location,point)
        MagicRender.singleframe(
            flare1,
            MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 2f)),
            Vector2f(400f,10f),
            0f,
            Color(255, 50, 50, 80),
            true
        )
        MagicRender.singleframe(
            flare2,
            MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(0f, 2f)),
            Vector2f(200f,5f),
            0f,
            Color(255, 150, 150, 120),
            true
        )

        inteval.advance(amount)
        val toRemove: MutableList<DamagingProjectileAPI> = ArrayList()
        projectiles.forEach { proj ->
            if (proj.isFading || proj.didDamage() || !engine.isEntityInPlay(proj)) {

                toRemove.add(proj)
            } else {
                val point2 = Vector2f(-14f,0f);
                VectorUtils.rotate(point2,proj.facing)
                Vector2f.add(point2,proj.location,point2)
                MagicRender.singleframe(
                    flare3,
                    MathUtils.getRandomPointInCircle(point2, MathUtils.getRandomNumberInRange(0f, 2f)),
                    Vector2f(600f,14f),
                    0f,
                    Color(255, 50, 50, 80),
                    true
                )

                if (inteval.intervalElapsed()) {
                    engine.addNebulaParticle(
                        proj.location,
                        Misc.ZERO,
                        MathUtils.getRandomNumberInRange(20f, 50f),
                        3f,
                        0.1f,
                        0.5f,
                        MathUtils.getRandomNumberInRange(1f, 1.3f),
                        Color(MathUtils.getRandomNumberInRange(150, 255), 0, 0, 100)
                    )
                }
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
        val inteval: IntervalUtil = IntervalUtil(0.1f, 0.1f)
    }
}
