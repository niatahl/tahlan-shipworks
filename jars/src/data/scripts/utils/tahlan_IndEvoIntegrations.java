package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.plugins.derelicts.IndEvo_ArtilleryStationPlacer;

public class tahlan_IndEvoIntegrations {
    public static void addDefenses(StarSystemAPI system) {
        SectorAPI sector = Global.getSector();
        MarketAPI lucifron = sector.getEconomy().getMarket("tahlan_rubicon_p03_market");
        if (lucifron != null) {
            MarketAPI melchi = sector.getEconomy().getMarket("tahlan_rubicon_p01_market");
            MarketAPI adra = sector.getEconomy().getMarket("tahlan_rubicon_outpost_market");
            if (!lucifron.hasCondition("IndEvo_mineFieldCondition")) {
                lucifron.addCondition("IndEvo_mineFieldCondition");
                melchi.addCondition("IndEvo_mineFieldCondition");
                adra.addCondition("IndEvo_mineFieldCondition");
            }
            if (!lucifron.hasCondition("IndEvo_ArtilleryStationCondition")) {
                lucifron.addCondition("IndEvo_ArtilleryStationCondition");
                lucifron.addIndustry("IndEvo_Artillery_mortar");
                melchi.addCondition("IndEvo_ArtilleryStationCondition");
                melchi.addIndustry("IndEvo_Artillery_railgun");
                adra.addCondition("IndEvo_ArtilleryStationCondition");
                adra.addIndustry("IndEvo_Artillery_missile");
                IndEvo_ArtilleryStationPlacer.placeWatchtowers(system,"tahlan_legioinfernalis");
            }
        }
    }
}
