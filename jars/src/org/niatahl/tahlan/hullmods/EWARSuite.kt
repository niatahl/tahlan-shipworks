package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import org.niatahl.tahlan.utils.Utils
import org.niatahl.tahlan.weapons.deco.EWARSuiteEffectScript
import kotlin.math.roundToInt

class EWARSuite : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {

    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {

    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        return when (index) {
            0 -> "" + EWARSuiteEffectScript.EFFECT_RANGE.roundToInt() + Utils.txt("su")
            1 -> "" + ((EWARSuiteEffectScript.DAMAGE_MULT - 1f) * 100f).roundToInt() + Utils.txt("%")
            2 -> "" + ((1f - EWARSuiteEffectScript.PDDMG_MULT) * 100f).roundToInt() + Utils.txt("%")
            else -> null
        }
    }

}