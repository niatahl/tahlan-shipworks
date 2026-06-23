package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.Utils.txt

/**
 * The lasting scar a successful no-Nex siege leaves on its primary target market. Penalties are
 * "half a siege" — [SiegeCondition]'s stat mods scaled by [SiegeConfig.AFTERMATH_PENALTY_FRACTION],
 * derived live so the scar always tracks the active siege penalty (and any future LunaLib slider on
 * it). The condition self-expires from [advance] after [SiegeConfig.AFTERMATH_DURATION_DAYS], using
 * the established vanilla idiom (PirateActivity / CommRelayCondition) of removing itself by token.
 *
 * [isTransient] = false so the elapsed-days counter persists across save/load.
 */
class SiegeAftermathCondition : BaseMarketConditionPlugin() {

    private var elapsedDays = 0f

    // Derived live: always half (or whatever fraction) of the current siege penalty.
    private val accessibilityMod: Float get() = SiegeCondition.ACCESSIBILITY_MOD * SiegeConfig.AFTERMATH_PENALTY_FRACTION
    private val stabilityMod:     Float get() = SiegeCondition.STABILITY_MOD     * SiegeConfig.AFTERMATH_PENALTY_FRACTION
    private val hazardMod:        Float get() = SiegeCondition.HAZARD_MOD        * SiegeConfig.AFTERMATH_PENALTY_FRACTION

    override fun apply(id: String) {
        super.apply(id)
        val label = txt("siege_aftermath_name")
        market.accessibilityMod.modifyFlat(id, accessibilityMod, label)
        market.stability.modifyFlat(id, stabilityMod, label)
        market.hazard.modifyFlat(id, hazardMod, label)
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        elapsedDays += Global.getSector().clock.convertToDays(amount)
        if (elapsedDays >= SiegeConfig.AFTERMATH_DURATION_DAYS) {
            // Self-remove by this instance's token; the next econ recompute drops the stat mods.
            market.removeSpecificCondition(condition.idForPluginModifications)
        }
    }

    override fun isTransient(): Boolean = false

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        val neg = Misc.getNegativeHighlightColor()
        tooltip.addPara(txt("siege_aftermath_accessibility"), 10f, neg, "${(accessibilityMod * 100).toInt()}%")
        tooltip.addPara(txt("siege_aftermath_stability"), 3f, neg, "${stabilityMod.toInt()}")
        tooltip.addPara(txt("siege_condition_hazard"), 3f, neg, "+${(hazardMod * 100).toInt()}%")
    }
}
