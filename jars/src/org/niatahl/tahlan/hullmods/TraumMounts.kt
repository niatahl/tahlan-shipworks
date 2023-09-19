package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags

class TraumMounts : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.dynamic.getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_LARGE)
        stats.dynamic.getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_LARGE)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (!ship.variant.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)) {
            ship.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        return if (index == 0) "" + COST_REDUCTION_LARGE.toInt() + "" else null
    }

    //Built-in only
    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun affectsOPCosts(): Boolean {
        return true
    }

    companion object {
        const val COST_REDUCTION_LARGE = 10f
    }
}