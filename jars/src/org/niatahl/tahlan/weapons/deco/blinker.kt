package org.niatahl.tahlan.weapons.deco

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI

class blinker : EveryFrameWeaponEffectPlugin {
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val ship = weapon.ship ?: return
        weapon.sprite.setAdditiveBlend()
        if (ship.isAlive && !ship.fluxTracker.isOverloadedOrVenting) {
            weapon.animation.alphaMult = 1f
        } else {
            weapon.animation.alphaMult = 0f
        }
    }
}