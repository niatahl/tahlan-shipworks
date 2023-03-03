// by Nia, written in an effort to stop Alfonzo from breaking the game again
package org.niatahl.tahlan.weapons.deco

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier

class LostechRangeEffect : EveryFrameWeaponEffectPlugin {
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        if (weapon.ship == null) {
            return
        }
        if (!weapon.ship.hasListenerOfClass(LostechMod::class.java)) {
            weapon.ship.addListener(LostechMod())
        }
    }

    private class LostechMod : WeaponBaseRangeModifier {
        override fun getWeaponBaseRangePercentMod(ship: ShipAPI?, weapon: WeaponAPI?): Float {
            return 0f
        }

        override fun getWeaponBaseRangeMultMod(ship: ShipAPI?, weapon: WeaponAPI?): Float {
            return 1f
        }

        override fun getWeaponBaseRangeFlatMod(ship: ShipAPI, weapon: WeaponAPI): Float {
            return if (weapon.slot.weaponType == WeaponAPI.WeaponType.ENERGY) {
                RANGE_MODIFIERS[weapon.spec.weaponId] ?: 0f
            } else {
                0f
            }
        }
    }

    companion object {

        private val RANGE_MODIFIERS = mapOf(
            "tahlan_cashmere" to -100f,
            "tahlan_silk" to -100f,
            "tahlan_taffeta" to -100f,
            "tahlan_velvet" to -100f
        )

    }
}