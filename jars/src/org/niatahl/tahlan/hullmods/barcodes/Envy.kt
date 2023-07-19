package org.niatahl.tahlan.hullmods.barcodes

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.utils.Utils.txt
import kotlin.math.roundToInt

class Envy : BaseHullMod() {

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI, id: String) {
        stats.apply {
            maxCombatReadiness.modifyFlat(id, AMOUNT)
        }
    }


    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> AMOUNT.roundToInt().toString() + txt("%")
            else -> null
        }
    }

    companion object {
        const val AMOUNT = 15f
    }
}