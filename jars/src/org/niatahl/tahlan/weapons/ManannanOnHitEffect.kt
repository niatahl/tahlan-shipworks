package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AsteroidAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.util.MagicRender
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lazywizard.lazylib.combat.entities.SimpleEntity
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.random
import java.awt.Color
import kotlin.random.Random

class ManannanOnHitEffect : OnHitEffectPlugin {
    override fun onHit(
        projectile: DamagingProjectileAPI,
        target: CombatEntityAPI,
        point: Vector2f,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI,
        engine: CombatEngineAPI
    ) {
        engine.spawnExplosion(point, Misc.ZERO, PARTICLE_COLOR, 800f, 4f)
        engine.spawnExplosion(point, Misc.ZERO, CORE_COLOR, 400f, 2f)
        engine.addSmoothParticle(point, Misc.ZERO, 1000f, 1f, 1f, FLASH_COLOR)
        engine.addSmoothParticle(point, Misc.ZERO, 1500f, 1f, 0.5f, FLASH_COLOR)
        engine.addSmoothParticle(point, Misc.ZERO, 800f, 0.5f, 0.1f, PARTICLE_COLOR)
        engine.addHitParticle(point, Misc.ZERO, 500f, 0.5f, 0.25f, FLASH_COLOR)
        for (x in 0 until NUM_PARTICLES) {
            engine.addHitParticle(
                point,
                MathUtils.getPointOnCircumference(
                    null,
                    (100f..500f).random(),
                    Math.random().toFloat() * 360f
                ),
                10f, 1f, (0.3f..0.6f).random(), PARTICLE_COLOR
            )
            engine.addNebulaParticle(
                MathUtils.getRandomPointInCircle(point, 120f),
                Misc.ZERO,
                MathUtils.getRandomNumberInRange(100f, 120f),
                1.3f,
                0.1f,
                0.25f,
                MathUtils.getRandomNumberInRange(3f, 6f),
                Color(50, 50, 50, 100)
            )
        }

        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tineola_blast1"),
            point,
            Misc.ZERO,
            Vector2f(100f, 100f),   // initial size
            Vector2f(800f, 800f),  // expansion
            360 * Random.nextFloat(),
            0f,
            Color(255, 50, 50, 60),
            true,
            0f,
            0.1f,
            0.8f
        )

        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tineola_blast1"),
            point,
            Misc.ZERO,
            Vector2f(100f, 100f),   // initial size
            Vector2f(1100f, 1100f),  // expansion
            360 * Random.nextFloat(),
            0f,
            Color(255, 50, 50, 90),
            true,
            0f,
            0.1f,
            0.8f
        )

        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tineola_blast1"),
            point,
            Misc.ZERO,
            Vector2f(100f, 100f),   // initial size
            Vector2f(1400f, 1400f),  // expansion
            360 * Random.nextFloat(),
            0f,
            Color(255, 50, 50, 120),
            true,
            0f,
            0.1f,
            0.8f
        )

        // Arcing stuff
        val validTargets: MutableList<CombatEntityAPI> = ArrayList()
        for (entityToTest in CombatUtils.getEntitiesWithinRange(point, 1500f)) {
            if (entityToTest is ShipAPI || entityToTest is AsteroidAPI || entityToTest is MissileAPI) {
                //Phased targets, and targets with no collision, are ignored
                if (entityToTest is ShipAPI) {
                    if (entityToTest.isPhased) {
                        continue
                    }
                }
                if (entityToTest.collisionClass == CollisionClass.NONE) continue

                if (entityToTest.owner == projectile.owner) continue
                validTargets.add(entityToTest)
            }
        }

        for (x in 0 until NUM_ARCS) {
            //If we have no valid targets, zap a random point near us

            //If we have no valid targets, zap a random point near us
            if (validTargets.isEmpty()) {
                validTargets.add(SimpleEntity(MathUtils.getRandomPointInCircle(point, 800f)))
            }

            val bonusDamage = projectile.damageAmount * 0.05f

            //And finally, fire at a random valid target

            //And finally, fire at a random valid target
            val arcTarget = validTargets[MathUtils.getRandomNumberInRange(0, validTargets.size - 1)]


            Global.getCombatEngine().spawnEmpArc(
                projectile.source, point, projectile.source, arcTarget,
                DamageType.ENERGY,  //Damage type
                (0.8f..1.2f).random() * bonusDamage,  //Damage
                (0.8f..1.2f).random() * bonusDamage,  //Emp
                100000f,  //Max range
                "",  //Impact sound
                10f,  // thickness of the lightning bolt
                ARC_CORE_COLOR,  //Central color
                ARC_FRINGE_COLOR //Fringe Color
            )
        }

        Global.getSoundPlayer().playSound("tahlan_helrazer_impact", 0.7f, 1.5f, point, Misc.ZERO)
    }

    companion object {
        private val PARTICLE_COLOR = Color(255, 21, 21, 150)
        private val CORE_COLOR = Color(255, 34, 34)
        private val FLASH_COLOR = Color(255, 175, 175)
        private const val NUM_PARTICLES = 50
        private const val NUM_ARCS = 20
        private val ARC_FRINGE_COLOR = Color(250, 0, 0)
        private val ARC_CORE_COLOR = Color(255, 215, 215)
    }
}