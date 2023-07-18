package org.niatahl.tahlan.hullmods.barcodes

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI

class Sloth : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI, id: String) {
        stats.apply {
            hullDamageTakenMult.modifyMult(id, AMOUNT)
            armorDamageTakenMult.modifyMult(id, AMOUNT)
            shieldDamageTakenMult.modifyMult(id, AMOUNT)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> "${(1f - AMOUNT) * 100}"
            else -> null
        }
    }

    companion object {
        const val AMOUNT = 0.9f
    }
}