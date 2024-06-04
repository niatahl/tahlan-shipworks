package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.utils.Utils
import kotlin.math.roundToInt

class OverclockedMounts : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI, id: String) {
        stats.apply {
            energyWeaponDamageMult.modifyMult(id, WEAPON_MULT)
            energyWeaponFluxCostMod.modifyMult(id, WEAPON_MULT)
            ballisticRoFMult.modifyMult(id, WEAPON_MULT)
            weaponDamageTakenMult.modifyMult(id, DAMAGE_MULT)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> "${((WEAPON_MULT-1f)*100).roundToInt()}${Utils.txt("%")}"
            1 -> "${((WEAPON_MULT-1f)*100).roundToInt()}${Utils.txt("%")}"
            2 -> "${((DAMAGE_MULT-1f)*100).roundToInt()}${Utils.txt("%")}"
            else -> null
        }
    }

    companion object {
        const val WEAPON_MULT = 1.3f
        const val DAMAGE_MULT = 1.2f
    }
}