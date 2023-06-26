package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.combat.entities.Ship
import org.lazywizard.lazylib.combat.DefenseUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.Utils.txt
import kotlin.math.roundToInt

class DaemonArmor : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.effectiveArmorBonus.modifyFlat(id, CALC_FLAT)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.addListener(DaemonArmorListener())
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if (!DefenseUtils.hasArmorDamage(ship)) {
            return
        }
        if (ship.isHulk) return
        if (ship.fluxTracker.isVenting || ship.isPhased) return
        ship.mutableStats.dynamic.getStat("tahlan_daemonarmor").modifyFlat("nuller", -1f)
        val timer = ship.mutableStats.dynamic.getStat("tahlan_daemonarmor").modifiedValue + amount
        ship.mutableStats.dynamic.getStat("tahlan_daemonarmor").modifyFlat("tracker", timer)
        if (timer < DISRUPTION_TIME) return
        val armorGrid = ship.armorGrid
        val grid = armorGrid.grid
        val max = armorGrid.maxArmorInCell
        val statusMult = if (ship.fluxTracker.isOverloaded) 0.5f else 1f
        val baseCell = armorGrid.maxArmorInCell * ship.hullSpec.armorRating.coerceAtMost(ARMOR_CAP) / armorGrid.armorRating
        val repairAmount = baseCell * (REGEN_PER_SEC_PERCENT / 100f) * statusMult * amount

        // Iterate through all armor cells and find any that aren't at max
        for (x in grid.indices) {
            for (y in grid[0].indices) {
                if (grid[x][y] < max) {
                    val regen = (grid[x][y] + repairAmount).coerceAtMost(max)
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
        return when (index) {
            0 -> "${REGEN_PER_SEC_PERCENT.roundToInt()}${txt("%")}"
            1 -> "${(ARMOR_CAP / 100 * REGEN_PER_SEC_PERCENT).roundToInt()}/s"
            2 -> "${CALC_FLAT.roundToInt()}"
            3 -> "${DAMAGE_CAP.roundToInt()}"
            4 -> "${((1f - DAMAGE_CAP_REDUCTION) * 100f).roundToInt()}${txt("%")}"
            5 -> txt("halved")
            6 -> txt("disabled")
            7 -> "${DISRUPTION_TIME.roundToInt()} ${txt("seconds")}"
            else -> null
        }
    }

    internal class DaemonArmorListener : DamageTakenModifier {
        override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI, damage: DamageAPI, point: Vector2f, shieldHit: Boolean): String? {
            if (shieldHit) return null
            if (target !is ShipAPI) return null

            if (damage.damage > DAMAGE_CAP) {
                damage.damage = DAMAGE_CAP + (damage.damage - DAMAGE_CAP) * DAMAGE_CAP_REDUCTION
            }

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
        private const val CALC_FLAT = 200f
        private const val DISRUPTION_TIME = 2f
        const val DAMAGE_CAP = 2000f
        const val DAMAGE_CAP_REDUCTION = 0.5f
    }
}