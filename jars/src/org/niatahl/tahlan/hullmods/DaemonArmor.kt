package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import org.lazywizard.lazylib.combat.DefenseUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.Utils

class DaemonArmor : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
//        stats.getEffectiveArmorBonus().modifyPercent(id,CALC_PERCENT);
        stats.effectiveArmorBonus.modifyFlat(id, CALC_FLAT)
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
        val baseCell = armorGrid.maxArmorInCell * Math.min(ship.hullSpec.armorRating, ARMOR_CAP) / armorGrid.armorRating
        val repairAmount = baseCell * (REGEN_PER_SEC_PERCENT / 100f) * statusMult * amount

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

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + Math.round(REGEN_PER_SEC_PERCENT) + Utils.txt("%")
        if (index == 1) return "" + Math.round(ARMOR_CAP / 100 * REGEN_PER_SEC_PERCENT) + "/s"
        //        if (index == 2) return "" + Math.round(CALC_PERCENT) + txt("%");
        if (index == 2) return "" + Math.round(CALC_FLAT)
        if (index == 3) return Utils.txt("halved")
        if (index == 4) return Utils.txt("disabled")
        return if (index == 5) "" + Math.round(DISUPTION_TIME) + "s" else null
    }

    internal class DaemonArmorListener : DamageTakenModifier {
        override fun modifyDamageTaken(param: Any, target: CombatEntityAPI, damage: DamageAPI, point: Vector2f, shieldHit: Boolean): String? {
            if (shieldHit) return null
            if (target !is ShipAPI) return null
            if (target.variant.hasHullMod("tahlan_daemonarmor") || target.variant.hasHullMod("tahlan_daemonplating")) {
                if (damage.damage > 0) {
                    target.mutableStats.dynamic.getStat("tahlan_daemonarmor").unmodify("tracker")
                }
            }
            return null
        }
    }

    companion object {
        private const val ARMOR_CAP = 2000f
        private const val REGEN_PER_SEC_PERCENT = 10f
        private const val CALC_PERCENT = 50f
        private const val CALC_FLAT = 200f
        private const val DISUPTION_TIME = 2f
    }
}