package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_LegioTyranny extends BaseMarketConditionPlugin {

    public static final float STAB_BONUS = 5f;

    @Override
    public void apply(String id) {
        super.apply(id);
        if (market.getFaction() != null) {
            if (market.getFaction().getId().contains("tahlan_legioinfernalis")) {
                market.getStability().modifyFlat(id, STAB_BONUS, txt("tyranny"));
                if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$tahlan_triggered")) {
                    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, 1.25f, txt("tyranny"));
                    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyMult(id, 1.25f, txt("tyranny"));
                } else {
                    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, 1.1f, txt("tyranny"));
                    market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyMult(id, 1.1f, txt("tyranny"));
                }
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, 2f, txt("tyranny"));
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

        tooltip.addPara(txt("stab"),
                10f, Misc.getHighlightColor(),
                "+" + (int) STAB_BONUS);
        tooltip.addPara(txt("tyranny2"), 10f);
        tooltip.addPara(txt("tyranny3"), 10f, Misc.getHighlightColor(), txt("tyranny4"));
    }
}
