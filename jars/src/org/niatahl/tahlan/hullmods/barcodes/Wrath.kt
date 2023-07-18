package org.niatahl.tahlan.hullmods.barcodes

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize

class Wrath : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI, id: String) {
        stats.apply {
            damageToCapital.modifyPercent(id, MAG[HullSize.CAPITAL_SHIP]!!)
            damageToCruisers.modifyPercent(id, MAG[HullSize.CRUISER]!!)
            damageToDestroyers.modifyPercent(id, MAG[HullSize.DESTROYER]!!)
            damageToFrigates.modifyPercent(id, MAG[HullSize.FRIGATE]!!)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String? {
        return when (index) {
            0 -> MAG[HullSize.FRIGATE].toString()
            1 -> MAG[HullSize.DESTROYER].toString()
            2 -> MAG[HullSize.CRUISER].toString()
            3 -> MAG[HullSize.CAPITAL_SHIP].toString()
            else -> null
        }
    }

    companion object {
        private val MAG = mapOf(
            HullSize.FRIGATE to 5f,
            HullSize.DESTROYER to 10f,
            HullSize.CRUISER to 15f,
            HullSize.CAPITAL_SHIP to 20f
        ).withDefault { 0f }
    }
}