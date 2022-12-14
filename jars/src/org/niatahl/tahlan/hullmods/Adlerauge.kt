package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Tags
import org.niatahl.tahlan.utils.Utils
import kotlin.math.roundToInt

class Adlerauge : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.energyWeaponRangeBonus.modifyFlat(id, RANGE_BOOST)
        stats.ballisticWeaponRangeBonus.modifyFlat(id, RANGE_BOOST)
        stats.projectileSpeedMult.modifyPercent(id, SPEED_BOOST)
        stats.autofireAimAccuracy.modifyFlat(id, AUTOAIM_BONUS * 0.01f)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (!ship.variant.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)) {
            ship.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE)
        }
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + RANGE_BOOST.roundToInt() + Utils.txt("su")
        if (index == 1) return "" + SPEED_BOOST.roundToInt() + Utils.txt("%")
        if (index == 2) return Utils.txt("hmd_adler1")
        return if (index == 3) "" + EFFECT_RANGE.roundToInt() + Utils.txt("su") else null
    }

    companion object {
        private const val EFFECT_RANGE = 2000f
        private const val AUTOAIM_BONUS = 50f
        private const val RANGE_BOOST = 100f
        private const val SPEED_BOOST = 20f
    }
}