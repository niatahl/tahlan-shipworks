package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import data.scripts.util.MagicIncompatibleHullmods
import org.niatahl.tahlan.TahlanModPlugin
import org.niatahl.tahlan.utils.Utils

class HeavyConduits : BaseHullMod() {
    private val INNERLARGE = "graphics/tahlan/fx/tahlan_pinshield.png"
    private val OUTERLARGE = "graphics/tahlan/fx/tahlan_tempshield_ring.png"
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        for (tmp in BLOCKED_HULLMODS) {
            if (ship.variant.hullMods.contains(tmp)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, tmp, "tahlan_heavyconduits")
            }
        }
        for (tmp in TahlanModPlugin.SHIELD_HULLMODS) {
            if (ship.variant.hullMods.contains(tmp)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, tmp, "tahlan_heavyconduits")
            }
        }
        if (ship.shield != null) {
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.empDamageTakenMult.modifyMult(id, 1f - FLUX_RESISTANCE * 0.01f)
        stats.ventRateMult.modifyPercent(id, VENT_RATE_BONUS)
        stats.suppliesPerMonth.modifyPercent(id, SUPPLIES_INCREASE)
        stats.crLossPerSecondPercent.modifyPercent(id, SUPPLIES_INCREASE)
        stats.combatEngineRepairTimeMult.modifyMult(id, 1f - REPAIR_BONUS * 0.01f)
        stats.combatWeaponRepairTimeMult.modifyMult(id, 1f - REPAIR_BONUS * 0.01f)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + FLUX_RESISTANCE.toInt() + Utils.txt("%")
        if (index == 1) return "" + VENT_RATE_BONUS.toInt() + Utils.txt("%")
        if (index == 2) return "" + REPAIR_BONUS.toInt() + Utils.txt("%")
        if (index == 3) return "" + SUPPLIES_INCREASE.toInt() + Utils.txt("%")
        if (index == 4) return Utils.txt("hmd_HeavyCond1")
        return if (index == 5) Utils.txt("hmd_HeavyCond2") else null
    }

    companion object {
        const val FLUX_RESISTANCE = 50f
        const val VENT_RATE_BONUS = 50f
        const val SUPPLIES_INCREASE = 100f
        const val REPAIR_BONUS = 50f
        private val BLOCKED_HULLMODS: MutableSet<String> = HashSet(1)

        init {
            BLOCKED_HULLMODS.add("fluxbreakers")
        }
    }
}