package org.niatahl.tahlan.skills

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.niatahl.tahlan.utils.Utils.txt

object HyperCoordination  {

    val LOGISTICS_BONUS = 5f
    val DP_REDUCTION = 0.1f
    val DP_REDUCTION_MAX = 10f

    class Level1 : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("hypercoordination_L1_desc")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            val logiMult = (100f - LOGISTICS_BONUS) / 100f
            stats.fuelUseMod.modifyMult(id,logiMult)
            stats.suppliesPerMonth.modifyMult(id,logiMult)
            stats.suppliesToRecover.modifyMult(id,logiMult)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.fuelUseMod.unmodify(id)
            stats.suppliesToRecover.unmodify(id)
            stats.suppliesPerMonth.unmodify(id)
        }
    }

    class Level2 : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("hypercoordination_L2_desc")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            val baseCost = stats.suppliesToRecover.baseValue
            val reduction = Math.min(DP_REDUCTION_MAX, baseCost * DP_REDUCTION)
            stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, -reduction)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(id)
        }
    }

}