package org.niatahl.tahlan.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static org.niatahl.tahlan.plugins.TahlanModPlugin.ENABLE_HARDMODE;
import static org.niatahl.tahlan.utils.TahlanIDs.LEGIO;
import static org.niatahl.tahlan.utils.Utils.txt;

public class LegioTyranny extends BaseMarketConditionPlugin {

    public static final float STAB_BONUS = 1f;
    public static final float DEFENSE_BONUS = 0.2f;

    private float FLEET_PERCENT = 0f;

    @Override
    public void apply(String id) {
        if (market.getFaction() == null) {
            return;
        }
        if (market.getFactionId().contains(LEGIO)) {
            int marketMult = Misc.getFactionMarkets(market.getFactionId()).size();
            market.getStability().modifyFlat(id, STAB_BONUS * marketMult, txt("tyranny"));

            if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$tahlan_triggered")) {
                if (ENABLE_HARDMODE) {
                    FLEET_PERCENT = 30f;
                } else {
                    FLEET_PERCENT = 20f;
                }
            } else {
                FLEET_PERCENT = 10f;
            }

            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyPercent(id, FLEET_PERCENT, txt("tyranny"));
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyPercent(id, FLEET_PERCENT, txt("tyranny"));

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

        int marketMult = Misc.getFactionMarkets(LEGIO).size();
        tooltip.addPara(txt("fleetsize"),
                10f, Misc.getHighlightColor(),
                "+" + (int) FLEET_PERCENT);
        tooltip.addPara(txt("stab"),
                10f, Misc.getHighlightColor(),
                "+" + (int) STAB_BONUS * marketMult);
        tooltip.addPara(txt("tyranny2"), 10f);
        tooltip.addPara(txt("tyranny5"), 10f);
        tooltip.addPara(txt("tyranny3"), 10f, Misc.getHighlightColor(), txt("tyranny4"));
    }

    @Override
    public boolean showIcon() {
        return market.getFactionId().equals(LEGIO);
    }
}
