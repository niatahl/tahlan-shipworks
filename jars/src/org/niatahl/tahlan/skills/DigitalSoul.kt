package org.niatahl.tahlan.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.AutomatedShips
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs.NEURALLINK_COMM
import org.niatahl.tahlan.utils.Utils.txt

object DigitalSoul {

    val ELITE_CR = 15f
    val BASE_CR = 85f
    val SKILL_ID = "tahlan_digitalSoul"

    class Level1a : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String? {
            return txt("digitalSoulLevel1a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.CUSTOM
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            //Global.getSector().playerPerson.aiCoreId = NEURALLINK_COMM
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
        }
    }

    class Level1b : ShipSkillEffect, BaseSkillEffectDescription() {

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.CUSTOM
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            if (Misc.isAutomated(stats)) {
                val skill = Global.getSettings().getSkillSpec(SKILL_ID)
                val automatedShipsBonus: Float = computeAndCacheThresholdBonus(stats, "auto_cr", AutomatedShips.MAX_CR_BONUS, ThresholdBonusType.AUTOMATED_POINTS) // We subtract the automated ship points bonus to not double dip on that
                val eliteBonus = if (Global.getSector().playerPerson.stats.getSkillLevel(SKILL_ID) > 1f) ELITE_CR else 0f
                stats.maxCombatReadiness.modifyFlat(id, (BASE_CR + eliteBonus - automatedShipsBonus).coerceAtLeast(0f) * 0.01f, skill.name + txt("skill_suffix"))
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            stats.maxCombatReadiness.unmodify(id)
        }

        override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI, width: Float) {
            info.addPara(txt("digitalSoulLevel1b"), 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), txt("digitalSoulLevel1b_hl"))
        }
    }

    class Level2a : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("digitalSoulLevel2a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.CUSTOM
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            return
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            return
        }
    }

    class Level2b : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("digitalSoulLevel2b")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            Global.getSector().playerPerson.memoryWithoutUpdate.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT,0f)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            Global.getSector().playerPerson.memoryWithoutUpdate.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT,1f)
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.CUSTOM
        }
    }
}