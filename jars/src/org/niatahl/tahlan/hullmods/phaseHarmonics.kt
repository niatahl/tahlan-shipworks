package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import org.niatahl.tahlan.utils.Utils
import kotlin.math.roundToInt

class phaseHarmonics : BaseHullMod() {
    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        val system = ship.system ?: return
        val stats = ship.mutableStats
        if (system.ammo == 0) {
            stats.ventRateMult.modifyMult(ID, VENT_MOD)
            stats.shieldDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
            stats.armorDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
            stats.hullDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
            stats.empDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
        } else {
            stats.ventRateMult.unmodify(ID)
            stats.shieldDamageTakenMult.unmodify(ID)
            stats.armorDamageTakenMult.unmodify(ID)
            stats.hullDamageTakenMult.unmodify(ID)
            stats.empDamageTakenMult.unmodify(ID)
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        val shield = ship.shield ?: return
        shield.setRadius(ship.shieldRadiusEvenIfNoShield,"graphics/tahlan/fx/tahlan_nxashield.png","graphics/tahlan/fx/tahlan_tempshield_ring.png")
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize, ship: ShipAPI): String? {
        return when (index) {
            0 -> Utils.txt("phaseBreaker")
            1 -> "${((1f - DAMAGE_MOD) * 100f).roundToInt()}${Utils.txt("%")}"
            2 -> "${((VENT_MOD - 1f) * 100f).roundToInt()}${Utils.txt("%")}"
            else -> null
        }
    }

    companion object {
        private const val ID = "tahlan_phaseHarmonics"
        private const val VENT_MOD = 1.2f
        private const val DAMAGE_MOD = 0.9f
    }
}