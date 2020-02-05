package data.scripts.campaign.siege;

import com.fs.starfarer.api.Global;
import java.awt.Color;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.intel.raid.AssembleStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidStageStatus;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

public class LegioSiegeMissionStage2Assemble extends AssembleStage {

    public static Logger log = Global.getLogger(LegioSiegeMissionStage2Assemble.class);

    public LegioSiegeMissionStage2Assemble(RaidIntel raid, SectorEntityToken gatheringPoint) {
        super(raid, gatheringPoint);
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
            info.addPara("The siege fleet has failed to successfully assemble at the rendezvous point. The siege has been cancelled.", opad);
        } else if (curr == index) {
            if (isSourceKnown()) {
                info.addPara("The siege fleet is currently assembling in the " + gatheringPoint.getContainingLocation().getNameWithLowercaseType() + ".", opad);
            } else {
                info.addPara("The siege fleet is currently assembling at an unknown location.", opad);
            }
        }
    }

    @Override
    protected String pickNextType() {
        return FleetTypes.MERC_ARMADA;
    }

    @Override
    protected float getFP(String type) {
        float base = 100f;
        if (spawnFP < base * 1.5f) {
            base = spawnFP;
        }
        if (base > spawnFP) {
            base = spawnFP;
        }

        spawnFP -= base;
        return base;
    }
}
