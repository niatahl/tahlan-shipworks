package data.scripts.world;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class tahlan_Lethia {

    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Lethia");

        switch (MathUtils.getRandomNumberInRange(1,4)) {
            case 1:
                system.getLocation().set(45000,30000);
                break;
            case 2:
                system.getLocation().set(-46000,28000);
                break;
            case 3:
                system.getLocation().set(-42000,-31000);
                break;
            case 4:
                system.getLocation().set(41000,-27000);
        }

        system.setBackgroundTextureFilename("graphics/tahlan/backgrounds/tahlan_lethia.jpg");

        PlanetAPI lethia_star = system.initStar("tahlan_lethia",
                "star_yellow",
                350f,
                600f);

        system.setLightColor(new Color(255,255,255));

        /*
        PlanetAPI lethia_1 = system.addPlanet("tahlan_lethia_p01",
                lethia_star,
                "Marandil",
                "lava_minor",
                360f*(float)Math.random(),
                100,
                2000,
                130);

        PlanetConditionGenerator.generateConditionsForPlanet(lethia_1, StarAge.AVERAGE);
        */

        PlanetAPI lethia_2 = system.addPlanet("tahlan_lethia_p02",
                lethia_star,
                "Atanor",
                "lava",
                360f*(float)Math.random(),
                180,
                2800,
                170);

        PlanetConditionGenerator.generateConditionsForPlanet(lethia_2, StarAge.AVERAGE);
        lethia_2.setCustomDescriptionId("tahlan_planet_atanor");
        lethia_2.getMarket().addCondition("tahlan_kassadariclaim");

        //debris field near Atanor
        DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                130f,
                1.2f,
                10000000f,
                10000000f);
        params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE;
        params1.baseSalvageXP = 550;
        params1.glowColor = Color.white;
        SectorEntityToken debrisLethia2 = Misc.addDebrisField(system,params1,StarSystemGenerator.random);
        debrisLethia2.setSensorProfile(1500f);
        debrisLethia2.setDiscoverable(true);
        debrisLethia2.setCircularOrbit(lethia_2,360*(float)Math.random(),300,250f);
        debrisLethia2.setId("tahlan_lethia_debrisLethia2");

        //asteroid ring
        system.addAsteroidBelt(lethia_star, 1000, 3800, 1000, 120, 500, Terrain.ASTEROID_BELT, "The Brink");
        system.addRingBand(lethia_star, "misc", "rings_asteroids0", 256f, 3, Color.gray, 256f, 3650, 220f);
        system.addRingBand(lethia_star, "misc", "rings_asteroids0", 256f, 0, Color.gray, 256f, 3800, 370f);
        system.addRingBand(lethia_star, "misc", "rings_asteroids0", 256f, 2, Color.gray, 256f, 4050, 235f);
        addDerelict(system,lethia_star,"tahlan_onslaught_gh_knight", ShipRecoverySpecial.ShipCondition.BATTERED, 3900, Math.random()<0.05);

        PlanetAPI lethia_3 = system.addPlanet("tahlan_lethia_p03",
                lethia_star,
                "Akrom",
                "barren",
                360f*(float)Math.random(),
                140,
                4500,
                240);

        PlanetConditionGenerator.generateConditionsForPlanet(lethia_3, StarAge.AVERAGE);

        SectorEntityToken stableLoc1 = system.addCustomEntity("tahlan_lethia_stableloc_1", "Stable Location", "stable_location", Factions.NEUTRAL);
        stableLoc1.setCircularOrbit(lethia_star, MathUtils.getRandomNumberInRange(0f,360f),5100, 460);

        PlanetAPI lethia_4 = system.addPlanet("tahlan_lethia_p04",
                lethia_star,
                "Heridal",
                "jungle",
                360f*(float)Math.random(),
                170,
                5400,
                190);

        PlanetConditionGenerator.generateConditionsForPlanet(lethia_4, StarAge.AVERAGE);
        lethia_4.setCustomDescriptionId("tahlan_planet_heridal");
        lethia_4.getMarket().addCondition("tahlan_kassadariclaim");

        //debris field near Heridal
        DebrisFieldTerrainPlugin.DebrisFieldParams params2 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                150f,
                1.3f,
                10000000f,
                10000000f);
        params2.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE;
        params2.baseSalvageXP = 650;
        params2.glowColor = Color.white;
        SectorEntityToken debrisLethia4 = Misc.addDebrisField(system,params2,StarSystemGenerator.random);
        debrisLethia4.setSensorProfile(1500f);
        debrisLethia4.setDiscoverable(true);
        debrisLethia4.setCircularOrbit(lethia_4,360*(float)Math.random(),350,280f);
        debrisLethia4.setId("tahlan_lethia_debrisLethia4");

        SectorEntityToken relay = system.addCustomEntity("tahlan_lethia_relay", // unique id
                "Lethia Relay", // name - if null, defaultName from custom_entities.json will be used
                "comm_relay", // type of object, defined in custom_entities.json
                "independent"); // faction
        relay.setCircularOrbitPointingDown( lethia_star, 360f*(float)Math.random(), 5900, MathUtils.getRandomNumberInRange(250, 410));

        PlanetAPI lethia_5 = system.addPlanet("tahlan_lethia_p05",
                lethia_star,
                "Kassadar",
                "tundra",
                360f*(float)Math.random(),
                190,
                6600,
                260);

        lethia_5.setCustomDescriptionId("tahlan_planet_kassadar");
        lethia_5.setInteractionImage("illustrations","tahlan_kassadar_illus");
        system.addRingBand(lethia_5, "misc", "rings_dust0", 256f, 1, Color.gray, 256f, 360, 400f);
        //SectorEntityToken lethia_5_station = system.addCustomEntity("tahlan_lethia_station", "Skyward Station", "tahlan_station_skyward", "independent");
        //lethia_5_station.setCircularOrbitPointingDown(lethia_5, 360*(float)Math.random(), 200, 50);

        MarketAPI lethia_5_market = addMarketplace("independent", lethia_5, null,
                "Kassadar",
                7,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_7,
                                Conditions.HABITABLE,
                                Conditions.FARMLAND_POOR,
                                Conditions.ORE_MODERATE,
                                Conditions.RARE_ORE_SPARSE,
                                Conditions.ORGANICS_TRACE,
                                Conditions.COLD,
                                Conditions.LARGE_REFUGEE_POPULATION
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE,
                                "tahlan_kassadarmarket"
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.LIGHTINDUSTRY,
                                Industries.FARMING,
                                Industries.WAYSTATION,
                                Industries.STARFORTRESS,
                                Industries.HEAVYBATTERIES,
                                Industries.MILITARYBASE,
                                Industries.ORBITALWORKS
                        )
                ),
                0.3f,
                false,
                true);

        //lethia_5_market.addIndustry(Industries.ORBITALWORKS,new ArrayList<String>(Arrays.asList(Items.CORRUPTED_NANOFORGE)));


        SectorEntityToken stableLoc2 = system.addCustomEntity("tahlan_lethia_stableloc_2", "Stable Location", "stable_location", Factions.NEUTRAL);
        stableLoc2.setCircularOrbit(lethia_star, MathUtils.getRandomNumberInRange(0f,360f),8200, 520);

        //Jump point for Kassadar
        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("tahlan_lethia_kassadar_jump", "Kassadar Jump Point");
        jumpPoint1.setCircularOrbit( system.getEntityById("tahlan_lethia_p05"), 290, 1400, 120);
        jumpPoint1.setRelatedPlanet(lethia_5);
        system.addEntity(jumpPoint1);

        // Some procgen can go out here.
        float radiusAfter = StarSystemGenerator.addOrbitingEntities(system, lethia_star, StarAge.AVERAGE,
                1, 2, // min/max entities to add
                8000, // radius to start adding at
                5, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
                true); // whether to use custom or system-name based names


        // Inactive gate at the edge of the system
        SectorEntityToken lethiaGate = system.addCustomEntity("tahlan_lethia_gate",
                "Lethia Gate",
                "inactive_gate",
                null);
        lethiaGate.setCircularOrbit(lethia_star, MathUtils.getRandomNumberInRange(0f,360f),radiusAfter+1000, 520);
        lethiaGate.setCustomDescriptionId("tahlan_gate_lethia");

        //debris fields around gate
        DebrisFieldTerrainPlugin.DebrisFieldParams params3 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                600f,
                0.8f,
                10000000f,
                10000000f);
        params3.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE;
        params3.baseSalvageXP = 850;
        params3.glowColor = Color.white;

        SectorEntityToken debrisLethiaGate1 = Misc.addDebrisField(system,params3,StarSystemGenerator.random);
        debrisLethiaGate1.setSensorProfile(1500f);
        debrisLethiaGate1.setDiscoverable(true);
        debrisLethiaGate1.setCircularOrbit(lethiaGate,360*(float)Math.random(),100,250f);
        debrisLethiaGate1.setId("tahlan_lethia_debrisGate1");

        //derelicts near gate
        addDerelict(system, lethiaGate, "tahlan_Ristreza_knight", ShipRecoverySpecial.ShipCondition.BATTERED, 500f, Math.random()<0.1);
        addDerelict(system, lethiaGate, "tahlan_Vale_crusader", ShipRecoverySpecial.ShipCondition.AVERAGE, 240f, Math.random()<0.2);

        SectorEntityToken debrisLethiaRim = Misc.addDebrisField(system,params3,StarSystemGenerator.random);
        debrisLethiaRim.setSensorProfile(1200f);
        debrisLethiaRim.setDiscoverable(true);
        debrisLethiaRim.setCircularOrbit(lethia_star,360*(float)Math.random(),radiusAfter+2200, 640);
        debrisLethiaRim.setId("tahlan_lethia_debrisRim");

        //remainders of the Sins of the Past mission
        addDerelict(system,debrisLethiaRim,"tahlan_legion_gh_knight",ShipRecoverySpecial.ShipCondition.WRECKED, 200f, Math.random()<0.1);
        addDerelict(system,debrisLethiaRim, "tahlan_Timeless_standard",ShipRecoverySpecial.ShipCondition.WRECKED, 240f, false);
        addDerelict(system,debrisLethiaRim, "tahlan_Nameless_standard",ShipRecoverySpecial.ShipCondition.WRECKED, 300f, false);
        addDerelict(system,debrisLethiaRim, "tahlan_Nameless_standard",ShipRecoverySpecial.ShipCondition.WRECKED, 320f, false);

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true);

        //Finally cleans up hyperspace
        cleanup(system);
    }


    //Shorthand function for cleaning up hyperspace
    private void cleanup(StarSystemAPI system){
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0f, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0f, 360f, 0.25f);
    }


    //Shorthand function for adding a market
    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name,
                                           int size, ArrayList<String> marketConditions, ArrayList<String> submarkets, ArrayList<String> industries, float tarrif,
                                           boolean freePort, boolean withJunkAndChatter) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String planetID = primaryEntity.getId();
        String marketID = planetID + "_market";

        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        newMarket.getTariff().modifyFlat("generator", tarrif);

        //Adds submarkets
        if (null != submarkets) {
            for (String market : submarkets) {
                newMarket.addSubmarket(market);
            }
        }

        //Adds market conditions
        for (String condition : marketConditions) {
            newMarket.addCondition(condition);
        }

        //Add market industries
        for (String industry : industries) {
            newMarket.addIndustry(industry);
        }

        //Sets us to a free port, if we should
        newMarket.setFreePort(freePort);

        //Adds our connected entities, if any
        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                newMarket.getConnectedEntities().add(entity);
            }
        }

        globalEconomy.addMarket(newMarket, withJunkAndChatter);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                entity.setMarket(newMarket);
                entity.setFaction(factionID);
            }
        }

        //Finally, return the newly-generated market
        return newMarket;
    }

    //Shorthand for adding derelicts, thanks Tart
    protected void addDerelict(StarSystemAPI system, SectorEntityToken focus, String variantId,
                               ShipRecoverySpecial.ShipCondition condition, float orbitRadius, boolean recoverable) {
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        float orbitDays = orbitRadius / (10f + (float) Math.random() * 5f);
        ship.setCircularOrbit(focus, (float) Math.random() * 360f, orbitRadius, orbitDays);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }
    }
}
