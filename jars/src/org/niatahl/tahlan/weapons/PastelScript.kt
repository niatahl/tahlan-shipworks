package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicInterference
import java.awt.Color

class PastelScript : EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        if (engine.isPaused || weapon == null) {
            return
        }
        if (weapon.ship.originalOwner < 0 && !weapon.slot.isBuiltIn) {
            MagicInterference.ApplyInterference(weapon.ship.variant)
        }
    }

    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI) {
        val point = projectile.location
        Global.getCombatEngine().spawnExplosion(point, Vector2f(0f, 0f), PARTICLE_COLOR, 80f, 0.1f)
        engine.addSmoothParticle(point, Misc.ZERO, 100f, 0.5f, 0.02f, PARTICLE_COLOR)
        engine.addHitParticle(point, Misc.ZERO, 200f, 0.8f, 0.01f, FLASH_COLOR)
    }

    companion object {
        val PARTICLE_COLOR = Color(41, 255, 150, 200)
        val GLOW_COLOR = Color(50, 238, 255, 50)
        val FLASH_COLOR = Color(223, 255, 248, 160)
    }


}