package org.niatahl.tahlan.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.listeners.NeuralLinkReplacer
import org.niatahl.tahlan.utils.TahlanIDs.NEURALLINK_COMM
import org.niatahl.tahlan.utils.Utils.txt

object DigitalSoul {

    val MAX_CR_MOD = 15f

    class Level1a : CharacterStatsSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("digitalSoulLevel1a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableCharacterStatsAPI, id: String?, level: Float) {
            val player = if (stats.isPlayerStats) Global.getSector().playerPerson else return
            player.aiCoreId = NEURALLINK_COMM
        }

        override fun unapply(stats: MutableCharacterStatsAPI, id: String?) {
            val player = if (stats.isPlayerStats) Global.getSector().playerPerson else return
            player.aiCoreId = null
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