package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.combat.DefenseUtils
import org.niatahl.tahlan.hullmods.DaemonArmor.DaemonArmorListener
import org.niatahl.tahlan.utils.Utils

class DaemonPlating : BaseHullMod() {
    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return !ship.variant.hasHullMod("tahlan_daemonarmor") && !ship.variant.hasHullMod("tahlan_heavyconduits")
    }

    override fun getCanNotBeInstalledNowReason(ship: ShipAPI, marketOrNull: MarketAPI, mode: CoreUITradeMode): String? {
        if (ship.variant.hasHullMod("tahlan_daemonarmor")) return "Already equipped with Hel Carapace"
        return if (ship.variant.hasHullMod("tahlan_heavyconduits")) "Incompatible with LosTech" else null
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if (!ship.hasListenerOfClass(DaemonArmorListener::class.java)) {
            ship.addListener(DaemonArmorListener())
        }
        if (!DefenseUtils.hasArmorDamage(ship)) {
//            ship.clearDamageDecals();
            return
        }
        if (ship.isHulk) return
        if (ship.fluxTracker.isVenting) return
        ship.mutableStats.dynamic.getStat("tahlan_daemonarmor").modifyFlat("nuller", -1f)
        val timer = ship.mutableStats.dynamic.getStat("tahlan_daemonarmor").modifiedValue + amount
        ship.mutableStats.dynamic.getStat("tahlan_daemonarmor").modifyFlat("tracker", timer)
        if (timer < DISUPTION_TIME) return
        val armorGrid = ship.armorGrid
        val grid = armorGrid.grid
        val max = armorGrid.maxArmorInCell
        val statusMult = if (ship.fluxTracker.isOverloaded) 0.5f else 1f
        var regenPercent = REGEN_PER_SEC_PERCENT
        if (ship.variant.sMods.contains("tahlan_daemonplating") || ship.variant.hullSpec.isBuiltInMod("tahlan_daemonplating")) {
            regenPercent = REGEN_PER_SEC_PERCENT_SMOD
        }
        val baseCell = armorGrid.maxArmorInCell * Math.min(ship.hullSpec.armorRating, ARMOR_CAP) / armorGrid.armorRating
        val repairAmount = baseCell * (regenPercent / 100f) * statusMult * amount

        // Iterate through all armor cells and find any that aren't at max
        for (x in grid.indices) {
            for (y in grid[0].indices) {
                if (grid[x][y] < max) {
                    val regen = grid[x][y] + repairAmount
                    armorGrid.setArmorValue(x, y, regen)
                }
            }
        }
        ship.syncWithArmorGridState()
    }

    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI, width: Float, isForModSpec: Boolean) {
        if (isForModSpec) {
            tooltip.addPara(Utils.txt("daemonPlatingSmod"), 10f, Misc.getGrayColor(), Misc.getHighlightColor(), "" + Math.round((1f - ARMOR_MULT_SMOD) * 100f) + Utils.txt("%"), "" + Math.round(REGEN_PER_SEC_PERCENT_SMOD) + Utils.txt("%"))
            return
        } else if (ship.variant.sMods.contains("tahlan_daemonplating") || ship.hullSpec.isBuiltInMod("tahlan_daemonplating")) {
            tooltip.addPara(
                Utils.txt("daemonPlatingSmod"),
                10f,
                Misc.getPositiveHighlightColor(),
                Misc.getHighlightColor(),
                "" + Math.round((1f - ARMOR_MULT_SMOD) * 100f) + Utils.txt("%"),
                "" + Math.round(REGEN_PER_SEC_PERCENT_SMOD) + Utils.txt("%")
            )
        } else {
            tooltip.addPara(Utils.txt("daemonPlatingSmod"), 10f, Misc.getGrayColor(), Misc.getHighlightColor(), "" + Math.round((1f - ARMOR_MULT_SMOD) * 100f) + Utils.txt("%"), "" + Math.round(REGEN_PER_SEC_PERCENT_SMOD) + Utils.txt("%"))
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize, ship: ShipAPI): String? {
        if (index == 0) return "" + Math.round(REGEN_PER_SEC_PERCENT) + Utils.txt("%")
        if (index == 1) return "" + Math.round(ARMOR_CAP / 100 * REGEN_PER_SEC_PERCENT) + "/s"
        if (index == 2) return "" + Math.round(CALC_FLAT)
        if (index == 3) return Utils.txt("halved")
        if (index == 4) return Utils.txt("disabled")
        if (index == 5) return "" + Math.round(DISUPTION_TIME) + "s"
        if (index == 6) return "" + Math.round((1f - ARMOR_MULT) * 100f) + Utils.txt("%")
        return if (index == 7) Utils.txt("heavyarmor") else null
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        if (stats.variant.sMods.contains("tahlan_daemonplating") || stats.variant.hullSpec.isBuiltInMod("tahlan_daemonplating")) {
            stats.armorBonus.modifyMult(id, ARMOR_MULT_SMOD)
        } else {
            stats.armorBonus.modifyMult(id, ARMOR_MULT)
        }
        stats.effectiveArmorBonus.modifyFlat(id, CALC_FLAT)
    }

    companion object {
        private const val ARMOR_MULT = 0.3014275134f
        private const val ARMOR_MULT_SMOD = 0.49815742465f
        private const val CALC_FLAT = 200f
        private const val ARMOR_CAP = 2000f
        private const val REGEN_PER_SEC_PERCENT = 5f
        private const val REGEN_PER_SEC_PERCENT_SMOD = 3f
        private const val DISUPTION_TIME = 2f
    }
}