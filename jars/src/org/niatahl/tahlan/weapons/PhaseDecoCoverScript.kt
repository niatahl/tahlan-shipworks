package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipSystemAPI.*
import com.fs.starfarer.api.combat.WeaponAPI

class PhaseDecoCoverScript : EveryFrameWeaponEffectPlugin {
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val ship = weapon.ship
        val system = when {
            ship.phaseCloak != null -> ship.phaseCloak
            ship.system != null -> ship.system
            else -> return
        }
        val level = system.effectLevel
        if (system.state == SystemState.IDLE || system.state == SystemState.COOLDOWN) {
            weapon.sprite?.let{it.alphaMult = 1f}
            weapon.barrelSpriteAPI?.let{it.alphaMult = 1f}
            weapon.underSpriteAPI?.let{it.alphaMult = 1f}
        } else {
            weapon.sprite?.let{it.alphaMult = 1f - level}
            weapon.barrelSpriteAPI?.let{it.alphaMult = 1f - level}
            weapon.underSpriteAPI?.let{it.alphaMult = 1f - level}
        }
    }
}