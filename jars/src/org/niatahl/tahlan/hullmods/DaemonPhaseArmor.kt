package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import org.niatahl.tahlan.hullmods.DaemonArmor.Companion.DAMAGE_CAP
import org.niatahl.tahlan.hullmods.DaemonArmor.Companion.DAMAGE_CAP_REDUCTION
import org.niatahl.tahlan.utils.Utils.txt
import kotlin.math.roundToInt

class DaemonPhaseArmor : BaseHullMod() {

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.addListener(DaemonArmor.DaemonArmorListener())
        ship.setNextHitHullDamageThresholdMult(DAMAGE_CAP, DAMAGE_CAP_REDUCTION)
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        return when (index) {
            0 -> "${DAMAGE_CAP.roundToInt()}"
            1 -> "${((1f - DAMAGE_CAP_REDUCTION) * 100f).roundToInt()}${txt("%")}"
            else -> null
        }
    }

}