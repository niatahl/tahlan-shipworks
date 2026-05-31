package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.Utils.txt

class SiegeCondition : BaseMarketConditionPlugin(), MarketImmigrationModifier {

    companion object {
        const val ACCESSIBILITY_MOD = -0.30f  // -30%
        const val STABILITY_MOD     = -2f
        const val HAZARD_MOD        =  0.25f  // +25%
    }

    override fun apply(id: String) {
        super.apply(id)
        market.accessibilityMod.modifyFlat(id, ACCESSIBILITY_MOD, txt("siege_condition_name"))
        market.stability.modifyFlat(id, STABILITY_MOD, txt("siege_condition_name"))
        market.hazard.modifyFlat(id, HAZARD_MOD, txt("siege_condition_name"))
        market.addTransientImmigrationModifier(this)
    }

    override fun unapply(id: String) {
        super.unapply(id)
        market.removeTransientImmigrationModifier(this)
    }

    override fun modifyIncoming(market: MarketAPI, incoming: PopulationComposition) {
        incoming.weight.modifyFlat(
            modId,
            -market.size.toFloat(),
            Misc.ucFirst(condition.getName().lowercase())
        )
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        val neg = Misc.getNegativeHighlightColor()
        tooltip.addPara(txt("siege_condition_accessibility"), 10f, neg, "${(ACCESSIBILITY_MOD * 100).toInt()}%")
        tooltip.addPara(txt("siege_condition_stability"), 3f, neg, "${STABILITY_MOD.toInt()}")
        tooltip.addPara(txt("siege_condition_hazard"), 3f, neg, "+${(HAZARD_MOD * 100).toInt()}%")
    }
}
