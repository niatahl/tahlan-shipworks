package data.scripts.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import data.scripts.utils.tahlan_Utils
import kotlin.math.roundToInt

class tahlan_phaseHarmonics : BaseHullMod() {
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

    override fun getDescriptionParam(index: Int, hullSize: HullSize, ship: ShipAPI): String? {
        return when (index) {
            0 -> tahlan_Utils.txt("phaseBreaker")
            1 -> "${((1f - DAMAGE_MOD) * 100f).roundToInt()}${tahlan_Utils.txt("%")}"
            2 -> "${((VENT_MOD - 1f) * 100f).roundToInt()}${tahlan_Utils.txt("%")}"
            else -> null
        }
    }

    companion object {
        private const val ID = "tahlan_phaseHarmonics"
        private const val VENT_MOD = 1.2f
        private const val DAMAGE_MOD = 0.9f
    }
}