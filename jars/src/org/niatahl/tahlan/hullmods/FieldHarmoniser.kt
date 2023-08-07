package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier
import kotlin.math.roundToInt

class FieldHarmoniser : BaseHullMod() {



    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.addListener(HarmoniserListener())
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> "${MEDIUM_BONUS.roundToInt()}"
            1 -> "${SMALL_BONUS.roundToInt()}"
            else -> null
        }
    }

    class HarmoniserListener : WeaponBaseRangeModifier {
        override fun getWeaponBaseRangePercentMod(ship: ShipAPI?, weapon: WeaponAPI): Float {
            return 0f
        }

        override fun getWeaponBaseRangeMultMod(ship: ShipAPI?, weapon: WeaponAPI): Float {
            return 1f
        }

        override fun getWeaponBaseRangeFlatMod(ship: ShipAPI?, weapon: WeaponAPI): Float {
            return if (weapon.isBeam) 0f else when (weapon.size) {
                WeaponAPI.WeaponSize.SMALL -> MEDIUM_BONUS
                WeaponAPI.WeaponSize.MEDIUM -> SMALL_BONUS
                WeaponAPI.WeaponSize.LARGE -> 0f
                else -> 0f
            }
        }
    }

    companion object {
        const val SMALL_BONUS = 100f
        const val MEDIUM_BONUS = 200f
    }
}