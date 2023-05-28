package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class OmnaOnHitEffect : OnHitEffectPlugin {

    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI?, engine: CombatEngineAPI) {

        if (target !is ShipAPI) return

        val bonusDamage = projectile.empAmount
        for (i in 0..2) {

            var pierceChance = target.hardFluxLevel - 0.1f
            pierceChance *= target.mutableStats.dynamic.getValue(Stats.SHIELD_PIERCED_MULT)

            val piercedShield = shieldHit && Math.random().toFloat() < pierceChance

            if (!shieldHit || piercedShield) {
                Global.getCombatEngine().spawnEmpArcPierceShields(
                    projectile.source, point, projectile, target,
                    DamageType.ENERGY,  //Damage type
                    0f,  //Damage
                    bonusDamage,  //Emp
                    100000f,  //Max range
                    "tachyon_lance_emp_impact",  //Impact sound
                    5f,  // thickness of the lightning bolt
                    ARC_CORE_COLOR,  //Central color
                    ARC_FRINGE_COLOR //Fringe Color
                )
            }
        }
    }


    companion object {
        private val ARC_FRINGE_COLOR = Color(255, 52, 55)
        private val ARC_CORE_COLOR = Color(255, 212, 215, 100)
    }
}