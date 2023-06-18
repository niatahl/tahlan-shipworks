package org.niatahl.tahlan.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
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

    class Level1a : ShipSkillEffect, BaseSkillEffectDescription() {
        override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI, width: Float) {
            info.addPara(txt("digitalSoulLevel1a"), 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), txt("digitalSoulLevel1a_hl"))
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.CUSTOM
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            Global.getSector().playerPerson.aiCoreId = NEURALLINK_COMM
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
//            Global.getSector().playerPerson.aiCoreId = null
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
            if (stats.variant != null) stats.variant.addTag(Tags.TAG_AUTOMATED_NO_PENALTY)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            if (stats.variant != null) stats.variant.removeTag(Tags.TAG_AUTOMATED_NO_PENALTY)
        }

        override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI, width: Float) {
            info.addPara(txt("digitalSoulLevel1b"), 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), txt("digitalSoulLevel1b_hl"))
        }
    }
}