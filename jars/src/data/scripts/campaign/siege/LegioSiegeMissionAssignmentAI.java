package data.scripts.campaign.siege;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetActionTextProvider;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.intel.raid.AssembleStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.BaseRaidStage;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI;
import com.fs.starfarer.api.util.Misc;

public class LegioSiegeMissionAssignmentAI extends RouteFleetAssignmentAI implements FleetActionTextProvider {

    public LegioSiegeMissionAssignmentAI(CampaignFleetAPI fleet, RouteData route, BaseAssignmentAI.FleetActionDelegate delegate) {
        super(fleet, route, delegate);
        fleet.getAI().setActionTextProvider(this);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount, false);

        RouteSegment curr = route.getCurrent();
        if (curr != null
                && (BaseRaidStage.STRAGGLER.equals(route.getCustom())
                || AssembleStage.WAIT_STAGE.equals(curr.custom)
                || curr.isTravel())) {
            Misc.setFlagWithReason(fleet.getMemoryWithoutUpdate(), MemFlags.FLEET_BUSY, "raid_wait", true, 1);
        }

        checkCapture(amount);

        if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_RAIDER)) {
            checkRaid(amount);
        }
    }

    @Override
    protected String getInSystemActionText(RouteSegment segment) {
        if (AssembleStage.WAIT_STAGE.equals(segment.custom)) {
            return "waiting at rendezvous point";
        }
        String s = "on a siege mission";
        return s;
    }

    @Override
    protected String getEndingActionText(RouteSegment segment) {
        return super.getEndingActionText(segment);
    }

    @Override
    protected String getStartingActionText(RouteSegment segment) {
        if (AssembleStage.PREP_STAGE.equals(segment.custom)) {
            String s = "preparing for siege mission";
            return s;
        }
        if (segment.from == route.getMarket().getPrimaryEntity()) {
            return "assembling siege mission at " + route.getMarket().getName();
        }

        String s = "on a siege mission";
        return s;
    }

    @Override
    protected String getTravelActionText(RouteSegment segment) {
        String s = "on a siege mission";
        return s;
    }

    @Override
    public String getActionText(CampaignFleetAPI fleet) {
        FleetAssignmentDataAPI curr = fleet.getCurrentAssignment();
        if (curr != null && curr.getAssignment() == FleetAssignment.PATROL_SYSTEM
                && curr.getActionText() == null) {

            String s = null;
            if (delegate != null) {
                s = delegate.getRaidDefaultText(fleet);
            }
            if (s == null) {
                s = "on a siege mission";
            }
            return s;

        }
        return null;
    }

}
