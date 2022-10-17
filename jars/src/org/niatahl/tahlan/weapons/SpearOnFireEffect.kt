package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.plugins.CustomRender
import java.awt.Color

class SpearOnFireEffect : OnFireEffectPlugin {

    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI) {
        CustomRender.addProjectile(projectile)
        val scale = if (weapon.size == WeaponAPI.WeaponSize.LARGE) 1f else 0.6f
        engine.spawnExplosion(projectile.location, weapon.ship.velocity, PARTICLE_COLOR, 90f * scale, 0.2f)
        engine.addSwirlyNebulaParticle(projectile.location, weapon.ship.velocity, 70f * scale, 1.5f, 0.1f, 0.2f, 0.5f, PARTICLE_COLOR, true)
        engine.addSmoothParticle(projectile.location, Misc.ZERO, 60f * scale, 0.7f, 0.1f, PARTICLE_COLOR)
        engine.addSmoothParticle(projectile.location, Misc.ZERO, 100f * scale, 0.7f, 1f, GLOW_COLOR)
        engine.addHitParticle(projectile.location, Misc.ZERO, 120f * scale, 1f, 0.05f, FLASH_COLOR)
    }

    companion object {
        val PARTICLE_COLOR = Color(255, 90, 20)
        val GLOW_COLOR = Color(255, 100, 80, 40)
        val FLASH_COLOR = Color(255, 245, 100)
    }
}
