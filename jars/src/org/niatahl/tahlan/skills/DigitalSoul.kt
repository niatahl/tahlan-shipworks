package org.niatahl.tahlan.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.FleetStatsSkillEffect
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI
import org.niatahl.tahlan.campaign.DigitalSoulScript
import org.niatahl.tahlan.utils.Utils.txt

object DigitalSoul {

    val MAX_CR_MOD = 15f

    class Level1a : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("digitalSoulLevel1a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            return
        }

        override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
            return
        }
    }

    class Level1b : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("digitalSoulLevel1b")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            val ship = if (stats.entity is ShipAPI) stats.entity as ShipAPI else return
            if (ship.variant.hasHullMod("automated")) {
                stats.maxCombatReadiness.modifyFlat(id, -MAX_CR_MOD * 0.01f)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            stats.maxCombatReadiness.unmodify(id)
        }
    }

    class Level2 : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("digitalSoulLevel2")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            val ship = if (stats.entity is ShipAPI) stats.entity as ShipAPI else return
            if (ship.variant.hasHullMod("automated")) {
                stats.maxCombatReadiness.modifyFlat(id, MAX_CR_MOD * 0.01f)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            stats.maxCombatReadiness.unmodify(id)
        }
    }
}