package data.scripts.campaign.siege;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.raid.OrganizeStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.util.Misc;

public class LegioSiegeMissionStage1Organize extends OrganizeStage {

    public LegioSiegeMissionStage1Organize(RaidIntel raid, MarketAPI market, float durDays) {
        super(raid, market, durDays);
    }

    @Override
    public void advance(float amount) {
        float days = Misc.getDays(amount);

        elapsed += days;

        statusInterval.advance(days);
        if (statusInterval.intervalElapsed()) {
            updateStatus();
        }
    }

    @Override
    protected String getForcesString() {
        return "The siege fleet";
    }

    @Override
    protected String getRaidString() {
        return "siege fleet";
    }
}
