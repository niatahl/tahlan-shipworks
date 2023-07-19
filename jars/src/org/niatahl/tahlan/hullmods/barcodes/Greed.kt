package org.niatahl.tahlan.hullmods.barcodes

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.utils.Utils

class Greed : BaseHullMod() {
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        val cnt = ship.variant.sMods.count()
        ship.mutableStats.apply {
            systemCooldownBonus.modifyMult(id, 1f - AMOUNT * cnt)
            systemRegenBonus.modifyMult(id, 1f - AMOUNT * cnt)
            systemRangeBonus.modifyMult(id, 1f + AMOUNT * cnt)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> "${AMOUNT * 100}${Utils.txt("%")}"
            else -> null
        }
    }

    companion object {
        const val AMOUNT = 0.02f
    }
}