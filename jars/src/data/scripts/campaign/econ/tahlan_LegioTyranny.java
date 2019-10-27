package data.scripts.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class tahlan_LegioTyranny extends BaseMarketConditionPlugin {

    public static final float STAB_BONUS = 5f;

    @Override
    public void apply(String id) {
        super.apply(id);
        market.getStability().modifyFlat (id, STAB_BONUS, "Legio Tyranny");
    }
    @Override
    public void unapply(String id) {
        super.unapply(id);
        market.getStability().unmodify(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        if (market == null) {
            return;
        }

        tooltip.addPara("%s stability",
                10f, Misc.getHighlightColor(),
                "+" + (int) STAB_BONUS);
    }
}
