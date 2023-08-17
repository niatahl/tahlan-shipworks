package org.niatahl.tahlan.weapons.deco

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import org.niatahl.tahlan.plugins.TahlanModPlugin.Companion.HAS_GRAPHICSLIB
import org.niatahl.tahlan.utils.GraphicLibEffects.customLight

class blinker : EveryFrameWeaponEffectPlugin {

    var runOnce = false
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val ship = weapon.ship ?: return
        weapon.sprite.setAdditiveBlend()
        if (ship.isAlive && !ship.fluxTracker.isOverloadedOrVenting) {
            weapon.animation.alphaMult = 1f
        } else {
            weapon.animation.alphaMult = 0f
        }

//        if (runOnce) return
//        runOnce = true
//
//        if (HAS_GRAPHICSLIB) {
//            customLight(weapon.location,ship,10f,1f,weapon.spec.glowColor,0f,Float.MAX_VALUE,0f)
//        }
    }
}