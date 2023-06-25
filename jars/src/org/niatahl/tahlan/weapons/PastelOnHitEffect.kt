package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnHitEffectPlugin
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc.ZERO
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class PastelOnHitEffect : OnHitEffectPlugin {
    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI?, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI?, engine: CombatEngineAPI) {
        val effectCol = Color(
            projectile.projectileSpec.fringeColor.red,
            projectile.projectileSpec.fringeColor.green,
            projectile.projectileSpec.fringeColor.blue,
            100
        )

        engine.addSmoothParticle(
            projectile.location,
            ZERO,
            200f,
            0.8f,
            0.05f,
            effectCol
        )
        engine.addSwirlyNebulaParticle(projectile.location, ZERO, 25f, 1.5f, 0.1f, 0.2f, 0.6f, effectCol, true)
        engine.addNebulaParticle(
            projectile.location,
            MathUtils.getRandomPointInCircle(ZERO, 15f),
            MathUtils.getRandomNumberInRange(40f, 70f),
            2f,
            0f,
            0.3f,
            MathUtils.getRandomNumberInRange(1.5f, 3f),
            Color(240, 200, 246, 40),
            true
        )
    }
}