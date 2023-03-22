package org.niatahl.tahlan.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static org.niatahl.tahlan.utils.Utils.txt;

public class KassadariClaim extends BaseMarketConditionPlugin implements MarketImmigrationModifier {

    public static final float ACCESSIBILITY_BONUS = -100f;
    public static final float HAZARD_BONUS = 50f;

    @Override
    public void apply(String id) {
        super.apply(id);
        market.getAccessibilityMod().modifyFlat(id, ACCESSIBILITY_BONUS/100, txt("econ_claim1"));
        market.getHazard().modifyFlat(id,HAZARD_BONUS/100, txt("econ_claim1"));
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

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        tooltip.addPara(txt("access"),
                10f, Misc.getHighlightColor(),
                ""+(int)ACCESSIBILITY_BONUS+"%"
        );
        tooltip.addPara(txt("+") + txt("hazard"),
                10f, Misc.getHighlightColor(),
                ""+(int)HAZARD_BONUS+"%"
        );
        tooltip.addPara(
                txt("econ_claim2"),
                10f,
                Misc.getHighlightColor(),
                "" + (int) getThisImmigrationBonus()
        );
    }
}
