package org.niatahl.tahlan.campaign.siege;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.intel.raid.ActionStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidStageStatus;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI.FleetActionDelegate;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.niatahl.tahlan.campaign.siege.LegioSiegeMissionIntel.LegioRaidSetupOutcome;
import static java.lang.Math.random;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class LegioSiegeMissionStage4Construct extends ActionStage implements FleetActionDelegate {

    public static Logger log = Global.getLogger(LegioSiegeMissionStage4Construct.class);

    protected StarSystemAPI target;
    protected boolean playerTargeted = true;
    protected boolean gaveOrders = true; // will be set to false in updateRoutes()
    protected LegioSiegeMissionIntel raidIntel = ((LegioSiegeMissionIntel) this.intel);
    protected float untilConstruct = 0f;
    protected boolean checkedFP = false;

    public LegioSiegeMissionStage4Construct(LegioSiegeMissionIntel raid, StarSystemAPI target) {
        super(raid);
        this.target = target;
        untilConstruct = (float) (7f + (7f * random()));
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        float days = Misc.getDays(amount);
        untilConstruct -= days;

        if (!gaveOrders) {
            gaveOrders = true;
            log.info(String.format("Giving the construct base order at %s", target.getNameWithLowercaseType()));
            giveOrbitOrder(getRoutes());
        }
    }

    public void giveOrbitOrder(List<RouteData> routes) {
        for (RouteData route : routes) {

            float orbitDays = untilConstruct;

            if (route.getActiveFleet() != null) {
                CampaignFleetAPI fleet = route.getActiveFleet();
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target.getHyperspaceAnchor(), orbitDays, "constructing siege base");
            } else {
                route.addSegment(new RouteSegment(orbitDays, target.getHyperspaceAnchor()));
            }
        }
    }

    @Override
    protected void abortIfNeededBasedOnFP(boolean giveReturnOrders) {
        List<RouteData> routes = getRoutes();
        List<RouteData> stragglers = new ArrayList<>();

        if (!enoughMadeIt(routes, stragglers)) {
            log.info(String.format("Decided not enough space fascists made it to %s", target.getNameWithLowercaseType()));
            status = RaidStageStatus.FAILURE;
            if (giveReturnOrders) {
                giveReturnOrdersToStragglers(routes);
            }
        }
    }

    @Override
    protected boolean enoughMadeIt(List<RouteData> routes, List<RouteData> stragglers) {
        float madeItFP = 0;
        for (RouteData route : RouteManager.getInstance().getRoutesForSource(intel.getRouteSourceId())) {
            CampaignFleetAPI fleet = route.getActiveFleet();
            if (fleet != null) {
                float mult = Misc.getAdjustedFP(1f, route.getMarket());
                if (mult < 1) {
                    mult = 1f;
                }
                madeItFP += fleet.getFleetPoints() / mult;
                log.info(String.format("counting %s FP from %s route activefleet", fleet.getFleetPoints(), fleet.getNameWithFaction()));
            } else {
                madeItFP += route.getExtra().fp;
                log.info(String.format("counting %s FP from route itself", route.getExtra().fp));
            }
        }
        log.info(String.format("%s FP of space fascists made it to %s, our threshold is %s", madeItFP, target.getNameWithLowercaseType(), abortFP));
        return madeItFP >= abortFP;
    }

    @Override
    protected void updateStatus() {

        if (!checkedFP) {
            abortIfNeededBasedOnFP(true);
            checkedFP = true;
        }

        if (status != RaidStageStatus.ONGOING) {

            String outcomeString = "null";

            if (raidIntel.getOutcome() != null) {
                outcomeString = raidIntel.getOutcome().name();
            }

            log.info(String.format("Outcome == %s, Raid status == %s, so cutting this updateStatus off short (at %s)", outcomeString, status.name(), target.getNameWithLowercaseType()));
            return;
        }

        if (untilConstruct <= 0) {
            abortIfNeededBasedOnFP(true);
            if (status == RaidStageStatus.FAILURE) {
                log.info(String.format("Giving up the ghost at %s -- not enough FP made it", target.getNameWithLowercaseType()));
                return;
            }

            status = RaidStageStatus.SUCCESS;
            raidIntel.sendOutcomeUpdate();
            constructBase(target);

            if (raidIntel.getOutcome() != null) {
                if (status == RaidStageStatus.SUCCESS) {
                    raidIntel.sendOutcomeUpdate();
                } else {
                    giveReturnOrdersToStragglers(getRoutes());
                }
            }
        }
    }

    @Override
    public String getRaidActionText(CampaignFleetAPI fleet, MarketAPI market) {
        return "besieging " + market.getName();
    }

    @Override
    public String getRaidApproachText(CampaignFleetAPI fleet, MarketAPI market) {
        return "moving to besiege " + market.getName();
    }

    @Override
    protected void updateRoutes() {
        resetRoutes();

        ((LegioSiegeMissionIntel) intel).sendInSystemUpdate();

        gaveOrders = false;
        ((LegioSiegeMissionIntel) intel).setReachedTarget(true);

        List<RouteData> routes = RouteManager.getInstance().getRoutesForSource(intel.getRouteSourceId());

        for (RouteData route : routes) {
            if (target != null) { // so that fleet may spawn NOT at the target
                route.addSegment(new RouteSegment(5f, target.getHyperspaceAnchor()));
            }
            route.addSegment(new RouteSegment(1000f, target.getHyperspaceAnchor()));
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

        if (status == RaidStageStatus.FAILURE) {
            info.addPara("The siege fleet was destroyed before they could colonize "
                    + target.getName() + ". The siege effort is now over.", opad);
        } else if (status == RaidStageStatus.SUCCESS) {

            if (raidIntel.getOutcome() == LegioRaidSetupOutcome.BASE_ESTABLISHED) {
                info.addPara("The siege fleet arrived successfully and established a siege base.", opad);
            }
        } else if (curr == index) {
            info.addPara("The construction of a siege base at " + target.getName() + " is currently under way.", opad);

        }
    }

    @Override
    public boolean canRaid(CampaignFleetAPI fleet, MarketAPI market) {
        if (raidIntel.getOutcome() != null) {
            return false;
        }
        return market == target;
    }

    @Override
    public String getRaidPrepText(CampaignFleetAPI fleet, SectorEntityToken from) {
        return "orbiting " + from.getName();
    }

    @Override
    public String getRaidInSystemText(CampaignFleetAPI fleet) {
        return "beseiging " + fleet.getContainingLocation().getNameWithTypeIfNebula();
    }

    @Override
    public String getRaidDefaultText(CampaignFleetAPI fleet) {
        return "establishing siege base";
    }

    @Override
    public boolean isPlayerTargeted() {
        return playerTargeted;
    }

    private void constructBase(StarSystemAPI system) {
        LegioSiegeBaseIntel base = new LegioSiegeBaseIntel(system);

        List<RouteData> routes = getRoutes();
        for (RouteData route : routes) {

            float orbitDays = 666666f;

            if (route.getActiveFleet() != null) {
                CampaignFleetAPI fleet = route.getActiveFleet();
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, target.getHyperspaceAnchor(), orbitDays, "blockading " + target.getNameWithLowercaseType());
            }
        }
    }

    @Override
    public void performRaid(CampaignFleetAPI fleet, MarketAPI market) {
        constructBase(market.getStarSystem());
    }
}
