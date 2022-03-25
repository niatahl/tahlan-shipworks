package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_LegioTyranny extends BaseMarketConditionPlugin {

    public static final float STAB_BONUS = 1f;
    public static final float DEFENSE_BONUS = 0.2f;

    @Override
    public void apply(String id) {
        if (market.getFaction() == null) {
            return;
        }
        if (market.getFaction().getId().contains("tahlan_legioinfernalis")) {
            int marketMult = Misc.getFactionMarkets(market.getFactionId()).size();
            market.getStability().modifyFlat(id, STAB_BONUS * marketMult, txt("tyranny"));

            if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$tahlan_triggered")) {
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, 1.25f, txt("tyranny"));
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyMult(id, 1.25f, txt("tyranny"));
            } else {
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, 1.1f, txt("tyranny"));
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyMult(id, 1.1f, txt("tyranny"));
            }

            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, Math.min(1f + DEFENSE_BONUS * marketMult, 3f), txt("tyranny"));
        } else {
            market.getStability().unmodify(id);
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id);
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).unmodify(id);
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
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

        int marketMult = Misc.getFactionMarkets(market.getFactionId()).size();
        tooltip.addPara(txt("stab"),
                10f, Misc.getHighlightColor(),
                "+" + (int) STAB_BONUS * marketMult);
        tooltip.addPara(txt("tyranny2"), 10f);
        tooltip.addPara(txt("tyranny3"), 10f, Misc.getHighlightColor(), txt("tyranny4"));
    }
}
