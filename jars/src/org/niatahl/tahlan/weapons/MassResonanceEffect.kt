package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import java.awt.Color
import kotlin.math.absoluteValue

class MassResonanceEffect : OnHitEffectPlugin {


    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI?, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI?, engine: CombatEngineAPI) {

        engine.spawnExplosion(point, Misc.ZERO, Color(30,0,30,20), 1000f, 1.2f)
//        engine.spawnExplosion(point, Misc.ZERO, CORE_COLOR, 300f, 0.8f)
        engine.addSmoothParticle(point, Misc.ZERO, 1000f, 1f, 0.1f, FLASH_COLOR)
//        engine.addSmoothParticle(point, Misc.ZERO, 1300f, 1f, 0.2f, FLASH_COLOR)
        engine.addSmoothParticle(point, Misc.ZERO, 400f, 0.5f, 0.1f, PARTICLE_COLOR)
//        engine.addHitParticle(point, Misc.ZERO, 200f, 0.5f, 0.25f, FLASH_COLOR)
//        engine.addNegativeSwirlyNebulaParticle(point, Misc.ZERO, 100f, 2f, 0.2f, 0.2f, 1f, NEG_COLOR)
//        engine.addNegativeSwirlyNebulaParticle(point, Misc.ZERO, 150f, 2f, 0.2f, 0.2f, 1f, NEG_COLOR)
//        engine.addNegativeSwirlyNebulaParticle(point, Misc.ZERO, 200f, 2f, 0.2f, 0.2f, 1f, NEG_COLOR)

        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tempshieldIN"),
            point,
            Misc.ZERO,
            Vector2f(400f, 400f),   // initial size
            Vector2f(600f, 600f),  // expansion
            360 * Math.random().toFloat(),
            0f,
            Color(30,0,30,50),
            true,
            0f,
            0.1f,
            0.4f
        )

        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tempshieldIN"),
            point,
            Misc.ZERO,
            Vector2f(350f, 350f),   // initial size
            Vector2f(400f, 400f),  // expansion
            360 * Math.random().toFloat(),
            0f,
            Color(30,0,30,50),
            true,
            0f,
            0.1f,
            0.4f
        )

        MagicRender.battlespace(
            Global.getSettings().getSprite("fx", "tahlan_tempshieldIN"),
            point,
            Misc.ZERO,
            Vector2f(300f, 300f),   // initial size
            Vector2f(200f, 200f),  // expansion
            360 * Math.random().toFloat(),
            0f,
            Color(30,0,30,50),
            true,
            0f,
            0.1f,
            0.4f
        )

        for (i in 0..2) {
            MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "tahlan_lens_flare2"),
                point,
                Misc.ZERO,
                Vector2f(700f + i * 100f, 10f + i * 5f),   // initial size
                Vector2f(100f, 0f),  // expansion
                360 * Math.random().toFloat(),
                0f,
                Color(255, 50, 30, 120),
                true,
                0f,
                0.1f,
                0.6f + 0.1f * i
            )
        }




        if (target !is ShipAPI) return

        var toTest = target
        while (toTest is ShipAPI && toTest.isStationModule) {
            toTest = toTest.parentStation
        }
        val area = if (toTest is ShipAPI) calculateArea(toTest) else return

        val damageFactor = ( area / 70000 ) * ( projectile.damageAmount / 2000 )

        engine.applyDamage(target,point,DAMAGE * damageFactor,DamageType.HIGH_EXPLOSIVE,0f,false,false,projectile.source)
    }

    private fun calculateArea(ship: ShipAPI): Float {

        val bounds = ship.exactBounds ?: ship.visualBounds ?: return 0f

        var area = 0f
        bounds.segments.forEach{
            area += it.p1.x * it.p2.y - it.p2.x * it.p1.y
        }
        area = area.absoluteValue / 2

        ship.childModulesCopy.forEach { child ->
            area += calculateArea(child)
        }

        return area
    }

    companion object {
        private val PARTICLE_COLOR = Color(255, 41, 21, 150)
        private val CORE_COLOR = Color(255, 54, 34)
        private val FLASH_COLOR = Color(255, 175, 175)
        private val NEG_COLOR = Color(34,254,255, 50)
        private const val DAMAGE = 2000f
    }
}