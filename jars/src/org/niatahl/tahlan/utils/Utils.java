package org.niatahl.tahlan.utils;

import com.fs.starfarer.api.Global;
import indevo.exploration.crucible.plugin.CrucibleSpawner;
import indevo.utils.animation.particles.DeceleratingDustCloudEjectionRenderer;
import lunalib.lunaUtil.campaign.LunaCampaignRenderer;

public class Utils {
    private static final String tahlan = "tahlan";

    // For translation friendliness
    public static String txt(String id) {
        return Global.getSettings().getString(tahlan, id);
    }

    // Interpolation
    public static float lerp(float x, float y, float alpha) {
        return (1f - alpha) * x + alpha * y;
    }

    /**
     * When SirHartley sends you a new version of one of his mods with the words "This one's newer", take it as a warning
     */
    public static void unfuckHartleysShit() {
        CrucibleSpawner.removeFromtLoc(Global.getSector().getPlayerFleet().getContainingLocation().getEntitiesWithTag("IndEvo_crucible").get(0).getStarSystem());
        while (LunaCampaignRenderer.hasRendererOfClass(DeceleratingDustCloudEjectionRenderer.class)) {
            DeceleratingDustCloudEjectionRenderer bullshit = (DeceleratingDustCloudEjectionRenderer) LunaCampaignRenderer.getRendererOfClass(DeceleratingDustCloudEjectionRenderer.class);
            LunaCampaignRenderer.removeRenderer(bullshit);
        }
    }
}