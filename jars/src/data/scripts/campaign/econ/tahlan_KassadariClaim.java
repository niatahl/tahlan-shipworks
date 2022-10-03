package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static data.scripts.TahlanUtils.Utils.txt;

public class tahlan_KassadariClaim extends BaseMarketConditionPlugin implements MarketImmigrationModifier {

    public static float ACCESSIBILITY_BONUS = -50f;

    @Override
    public void apply(String id) {
        super.apply(id);
        market.getAccessibilityMod().modifyFlat(id, getAccessibilityBonus()/100, txt("econ_claim1"));
        market.addTransientImmigrationModifier(this);
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        market.removeTransientImmigrationModifier(this);
    }

    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        incoming.add(Factions.POOR, 10f);
        incoming.getWeight().modifyFlat(getModId(), getThisImmigrationBonus(), Misc.ucFirst(condition.getName().toLowerCase()));
    }

    private float getThisImmigrationBonus() {
        return -2*market.getSize();
    }

    private float getAccessibilityBonus(){
        return Math.max(0, ACCESSIBILITY_BONUS-(Math.max(0, market.getSize()-3)));
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        tooltip.addPara(txt("access"),
                10f, Misc.getHighlightColor(),
                ""+(int)ACCESSIBILITY_BONUS+"%"
        );
        tooltip.addPara(
                txt("econ_claim2"),
                10f,
                Misc.getHighlightColor(),
                "" + (int) getThisImmigrationBonus()
        );
    }
}
