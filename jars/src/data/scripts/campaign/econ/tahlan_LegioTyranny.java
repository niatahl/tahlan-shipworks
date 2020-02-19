package data.scripts.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class tahlan_LegioTyranny extends BaseMarketConditionPlugin {

    public static final float STAB_BONUS = 5f;

    @Override
    public void apply(String id) {
        super.apply(id);
        if (market.getFaction() != null) {
            if (market.getFaction().getId().contains("tahlan_legioinfernalis")) {
                market.getStability().modifyFlat(id, STAB_BONUS, "Legio Tyranny");
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id,2f, "Legio Tyranny");
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyMult(id,2f, "Legio Tyranny");
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id,2f, "Legio Tyranny");
            } else {
                market.getStability().unmodify(id);
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id);
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).unmodify(id);
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
            }
        }
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        market.getStability().unmodify(id);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).unmodify(id);
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
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
        tooltip.addPara("Increased fleet sizes and ground defenses", 10f);
        tooltip.addPara("Only active while under %s control", 10f, Misc.getHighlightColor(),"Legio Infernalis");
    }
}
