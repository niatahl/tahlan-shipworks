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

    const val BASE_CR = 0.15f
    const val CR_DEPLOYMENT_MULT = 0.5f
    const val SKILL_ID = "tahlan_digitalSoul"

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

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            if (Misc.isAutomated(stats)) {
                val skill = Global.getSettings().getSkillSpec(SKILL_ID)
                stats.maxCombatReadiness.modifyFlat(id, BASE_CR, skill.name + txt("skill_suffix"))

            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.maxCombatReadiness.unmodify(id)
        }

        override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI, width: Float) {
            info.addPara(txt("digitalSoulLevel1b"), 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), txt("digitalSoulLevel1b_hl"))
        }
    }

    class Level2a : ShipSkillEffect, BaseSkillEffectDescription() {

        override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI, width: Float) {
            info.addPara(txt("digitalSoulLevel2a"), 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), txt("digitalSoulLevel2a_hl"))
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.CUSTOM
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            if (Misc.isAutomated(stats)) {
                stats.crPerDeploymentPercent.modifyMult(id, CR_DEPLOYMENT_MULT)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            stats.crPerDeploymentPercent.unmodify(id)
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