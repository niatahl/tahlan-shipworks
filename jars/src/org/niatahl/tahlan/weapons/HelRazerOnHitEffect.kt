package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.util.Misc
import org.magiclib.util.MagicRender
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.absoluteValue

class HelRazerOnHitEffect : OnHitEffectPlugin {
    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI, engine: CombatEngineAPI) {

        //MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 8, 400, 0, new Color(186, 240, 255), new Color(255, 255, 255));
        engine.spawnExplosion(point, Misc.ZERO, PARTICLE_COLOR, 300f, 1f)
        engine.spawnExplosion(point, Misc.ZERO, CORE_COLOR, 150f, 1f)
        engine.addHitParticle(point, Misc.ZERO, 600f, 1f, 0.05f, FLASH_COLOR)
        engine.addSmoothParticle(point, Misc.ZERO, 400f, 0.5f, 0.1f, PARTICLE_COLOR)
        engine.addHitParticle(point, Misc.ZERO, 200f, 0.5f, 0.25f, FLASH_COLOR)
        for (x in 0 until NUM_PARTICLES) {
            engine.addHitParticle(
                point,
                MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(100f, 500f), Math.random().toFloat() * 360f),
                10f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR
            )
        }
        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tineola_blast1"),
            point,
            Vector2f(),
            Vector2f(100f, 100f),
            Vector2f(500f, 500f),  //angle,
            360 * Math.random().toFloat(),
            0f,
            Color(236, 31, 31, 100),
            true,
            0f,
            0.2f,
            0.6f
        )
        val bonusDamage = projectile.damageAmount / 4f
        val blast = DamagingExplosionSpec(
            0.1f,
            300f,
            150f,
            bonusDamage,
            bonusDamage / 2,
            CollisionClass.PROJECTILE_FF,
            CollisionClass.PROJECTILE_FIGHTER,
            10f,
            10f,
            0f,
            0,
            BLAST_COLOR,
            null
        )
        blast.damageType = DamageType.HIGH_EXPLOSIVE
        blast.isShowGraphic = false
        engine.spawnDamagingExplosion(blast, projectile.source, point, false)
        Global.getSoundPlayer().playSound("tahlan_helrazer_impact", 1f, 1.5f, point, Misc.ZERO)



    }

    companion object {
        private val PARTICLE_COLOR = Color(255, 47, 21, 150)
        private val BLAST_COLOR = Color(255, 16, 16, 0)
        private val CORE_COLOR = Color(255, 67, 34)
        private val FLASH_COLOR = Color(255, 234, 212)
        private const val NUM_PARTICLES = 50
    }
}