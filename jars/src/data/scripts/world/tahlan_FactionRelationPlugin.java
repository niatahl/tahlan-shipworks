package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;


public class tahlan_FactionRelationPlugin implements SectorGeneratorPlugin {

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

        //Set hostile to everyone else
        for (FactionAPI other : Global.getSector().getAllFactions()) {
            legio.setRelationship(other.getId(), -0.6f);
        }

        //but not pirates and dabble
        legio.setRelationship("pirates",0.50f);
        legio.setRelationship("diableavionics", 0.00f);
        legio.setRelationship("hegemony", -1f);
    }
}
