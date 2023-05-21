package org.niatahl.tahlan.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;


public class FactionRelationPlugin implements SectorGeneratorPlugin {

    //Just call initFactionRelationships: this is only intended as a means to set faction relations at start
    @Override
    public void generate(SectorAPI sector) {
        initFactionRelationships(sector);
    }

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI legio = sector.getFaction("tahlan_legioinfernalis");
        FactionAPI player = sector.getFaction(Factions.PLAYER);

        //Sets player relations
        player.setRelationship(legio.getId(), RepLevel.HOSTILE);

        //Set inhospitable to everyone else
        for (FactionAPI other : Global.getSector().getAllFactions()) {
            if (!other.getId().contains("tahlan_legioinfernalis")) {
                legio.setRelationship(other.getId(), RepLevel.INHOSPITABLE);
            }
        }

        //but not pirates and dabble
        legio.setRelationship("pirates",0.50f);
        legio.setRelationship("diableavionics", 0.10f);
        legio.setRelationship("hegemony", -0.6f);
        legio.setRelationship("persean", -0.5f);
        legio.setRelationship("luddic_path", -0.5f);
        legio.setRelationship("luddic_church", -0.5f);
        legio.setRelationship("player", -0.8f);
        legio.setRelationship("tritachyon", 0f);
        legio.setRelationship("vic", 0.1f);
    }
}
