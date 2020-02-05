package data.scripts.campaign.siege;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import java.awt.Color;

import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.ReturnStage;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.List;

public class LegioSiegeMissionStage5Defend extends ReturnStage {

    protected StarSystemAPI target;
    
    public LegioSiegeMissionStage5Defend(RaidIntel raid, StarSystemAPI target) {
        super(raid);
        this.target = target;
    }

    @Override
    protected void updateRoutes() {
        giveDefendOrder(getRoutes());
        maxDays = 7f;
    }

    private void giveDefendOrder(List<RouteData> routes) {
        for (RouteManager.RouteData route : routes) {

            float orbitDays = 666666f;

            if (route.getActiveFleet() != null) {
                CampaignFleetAPI fleet = route.getActiveFleet();
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, target.getHyperspaceAnchor(), orbitDays, "blockading " + target.getNameWithLowercaseType());
            }
        }
    }

    @Override
    public void showStageInfo(TooltipMakerAPI info) {
        int curr = intel.getCurrentStage();
        int index = intel.getStageIndex(this);

        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;

        if (curr >= index) {
            info.addPara("The siege fleet has succeeded in establishing a base from which to besiege the system.", opad);
        }
    }
}
