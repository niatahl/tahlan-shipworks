package data.scripts.campaign.siege;

import java.awt.Color;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.TravelStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidStageStatus;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class LegioSiegeMissionStage3Travel extends TravelStage {

    public LegioSiegeMissionStage3Travel(RaidIntel raid, SectorEntityToken from, SectorEntityToken to, boolean requireNearTarget) {
        super(raid, from, to, requireNearTarget);
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
            info.addPara("The siege fleet has failed to successfully reach the "
                    + intel.getSystem().getNameWithLowercaseType() + ". The siege mission has been aborted.", opad);
        } else if (curr == index) {
            info.addPara("The siege fleet is currently travelling to the "
                    + intel.getSystem().getNameWithLowercaseType() + ".", opad);
        }
    }
}
