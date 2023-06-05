package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class DreadSabotOnHitEffect : OnHitEffectPlugin {
    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI?, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI, engine: CombatEngineAPI) {
        if (target is ShipAPI && !shieldHit) {
            val emp = projectile.empAmount / 2f
            val dam = 0f
            val arcs = MathUtils.getRandomNumberInRange(0,3)
            for (i in 0..arcs) {
                engine.spawnEmpArc(
                    projectile.source, point, target, target,
                    DamageType.ENERGY,
                    dam,
                    emp,  // emp
                    100000f,  // max range
                    "tachyon_lance_emp_impact",
                    20f,  // thickness
                    Color(155, 25, 25, 255),
                    Color(255, 255, 255, 255)
                )
            }
        }
    }
}