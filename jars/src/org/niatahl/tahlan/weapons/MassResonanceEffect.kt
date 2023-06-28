package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import org.lwjgl.util.vector.Vector2f
import kotlin.math.absoluteValue

class MassResonanceEffect : OnHitEffectPlugin {

    val DAMAGE = 4000f
    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI?, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI?, engine: CombatEngineAPI) {
        if (target !is ShipAPI) return

        var toTest = target
        while (toTest is ShipAPI && toTest.isStationModule) {
            toTest = toTest.parentStation
        }
        val area = if (toTest is ShipAPI) calculateArea(toTest) else return

        val damageFactor = area / 100000

        engine.applyDamage(target,point,DAMAGE*damageFactor,DamageType.HIGH_EXPLOSIVE,0f,false,false,projectile.source)
    }

    private fun calculateArea(ship: ShipAPI): Float {
        var area = 0f
        ship.exactBounds.segments.forEach{
            area += it.p1.x * it.p2.y - it.p2.x * it.p1.y
        }
        area = area.absoluteValue / 2

        ship.childModulesCopy.forEach { child ->
            area += calculateArea(child)
        }

        return area
    }
}