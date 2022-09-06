package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.*;
import data.scripts.weapons.ai.tahlan_FountainAI;
import data.scripts.weapons.ai.tahlan_KriegsmesserAI;
import data.scripts.weapons.ai.tahlan_TwoStageMissileAI;
import data.scripts.world.tahlan_FactionRelationPlugin;
import data.scripts.world.tahlan_Lethia;
import data.scripts.world.tahlan_Rubicon;
import exerelin.campaign.SectorManager;
import exerelin.utilities.NexConfig;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import data.scripts.campaign.siege.LegioSiegeManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin.addMarketToList;
import static data.scripts.campaign.siege.LegioSiegeBaseIntel.log;

public class tahlan_ModPlugin extends BaseModPlugin {
    static private boolean graphicsLibAvailable = false;

    static public boolean isGraphicsLibAvailable() {
        return graphicsLibAvailable;
    }



    //All hullmods related to shields, saved in a convenient list
    public static List<String> SHIELD_HULLMODS = new ArrayList<>();

    public static final String FOUNTAIN_MISSILE_ID = "tahlan_fountain_msl";
    public static final String KRIEGSMESSER_MISSILE_ID = "tahlan_kriegsmesser_msl";
    public static final String DOLCH_MISSILE_ID = "tahlan_dolch_msl";

    private static final String SETTINGS_FILE = "tahlan_settings.ini";

    public static boolean ENABLE_LETHIA;
    public static boolean ENABLE_LEGIO;
//    public static boolean ENABLE_SIEGE;
    public static boolean ENABLE_LIFELESS;
    public static boolean ENABLE_LEGIOBPS;
    public static boolean ENABLE_DAEMONS;
    public static boolean ENABLE_HARDMODE;
    public static boolean HAS_NEX = false;
    public static boolean HAS_INDEVO = false;

    public static final Logger LOGGER = Global.getLogger(tahlan_ModPlugin.class);



    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Tahlan Shipworks requires LazyLib by LazyWizard" + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444");
        }
        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!hasMagicLib) {
            throw new RuntimeException("Tahlan Shipworks requires MagicLib!" + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718");
        }
        if (Global.getSettings().getModManager().isModEnabled("@_ss_rebal_@"))
            throw new RuntimeException("Tahlan Shipworks is incompatible with Starsector Rebal. It breaks everything");

        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            graphicsLibAvailable = true;
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/tahlan_lights.csv");
            TextureData.readTextureDataCSV("data/lights/tahlan_texture.csv");
        } else {
            graphicsLibAvailable = false;
        }

        HAS_INDEVO = Global.getSettings().getModManager().isModEnabled("IndEvo");

        try {
            loadTahlanSettings();
        } catch (IOException | JSONException e) {
            Global.getLogger(tahlan_ModPlugin.class).log(Level.ERROR, "tahlan_settings.ini loading failed! ;....; " + e.getMessage());
        }


        //Adds shield hullmods
        for (HullModSpecAPI hullModSpecAPI : Global.getSettings().getAllHullModSpecs()) {
            if (hullModSpecAPI.hasTag("shields") && !SHIELD_HULLMODS.contains(hullModSpecAPI.getId())) {
                SHIELD_HULLMODS.add(hullModSpecAPI.getId());
            } else if (hullModSpecAPI.getId().contains("swp_shieldbypass") && !SHIELD_HULLMODS.contains(hullModSpecAPI.getId())) {
                SHIELD_HULLMODS.add("swp_shieldbypass"); //Dirty fix for Shield Bypass, since that one is actually not tagged as a Shield mod, apparently
            }
        }

    }


    //New game stuff
    @Override
    public void onNewGame() {
        SectorAPI sector = Global.getSector();

        //If we have Nexerelin and random worlds enabled, don't spawn our manual systems
        HAS_NEX = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!HAS_NEX || SectorManager.getManager().isCorvusMode()) {
            if (ENABLE_LETHIA) new tahlan_Lethia().generate(sector);
            if (ENABLE_LEGIO) new tahlan_Rubicon().generate(sector);
        }

        //Legio things
        if (ENABLE_LEGIO) {

            //Legio Infernalis relations
            tahlan_FactionRelationPlugin.initFactionRelationships(sector);

            //Adding Legio to bounty system
            SharedData.getData().getPersonBountyEventData().addParticipatingFaction("tahlan_legioinfernalis");

            //Legio stealing pirates homework
            if (ENABLE_LEGIOBPS) {
                Global.getSector().addScript(new tahlan_LegioStealingHomework());
            }

        } else {
            sector.getFaction("tahlan_legioinfernalis").setShowInIntelTab(false);

            if (HAS_NEX) {
                sector.getFaction("tahlan_legioinfernalis").getMemoryWithoutUpdate().set("$nex_respawn_cooldown", true);
            }
        }

        // Because apparently I have to do this
        removeDaemons(sector);

        //Rosenritter Blueprint Script
        Global.getSector().addScript(new tahlan_regaliablueprintscript());
        log.info("added Rosenritter Blueprint script");

        if (!ENABLE_LIFELESS) {
            sector.getFaction("remnant").removeKnownShip("tahlan_Timeless");
            sector.getFaction("remnant").removeKnownShip("tahlan_Nameless");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_disparax");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_relparax");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_nenparax");
        }

    }

    private void addDaemons(SectorAPI sector) {
        FactionAPI legio = sector.getFaction("tahlan_legioinfernalis");
        legio.addKnownShip("tahlan_dominator_dmn", false);
        legio.addKnownShip("tahlan_champion_dmn", false);
        legio.addKnownShip("tahlan_manticore_dmn", false);
        legio.addKnownShip("tahlan_hammerhead_dmn", false);
        legio.addKnownShip("tahlan_centurion_dmn", false);
        legio.addKnownShip("tahlan_vanguard_dmn", false);
        legio.addKnownShip("tahlan_DunScaith_dmn", false);
        legio.addKnownShip("tahlan_hound_dmn", false);
        legio.addKnownShip("tahlan_sunder_dmn", false);
        legio.addKnownShip("tahlan_kodai_dmn", false);
        legio.removeKnownFighter("flash_wing");
        legio.removeKnownFighter("spark_wing");
        legio.removeKnownFighter("lux_wing");
        legio.addKnownFighter("tahlan_miasma_drone_wing", false);
        legio.addKnownFighter("tahlan_flash_dmn_wing", false);
        legio.addKnownFighter("tahlan_spark_dmn_wing", false);
        legio.addKnownFighter("tahlan_lux_dmn_wing", false);
        legio.addKnownFighter("tahlan_thunder_dmn_wing", false);
        legio.addKnownFighter("tahlan_gaze_dmn_wing", false);
    }

    private void removeDaemons(SectorAPI sector) {
        FactionAPI legio = sector.getFaction("tahlan_legioinfernalis");
        legio.removeKnownShip("tahlan_dominator_dmn");
        legio.removeKnownShip("tahlan_champion_dmn");
        legio.removeKnownShip("tahlan_manticore_dmn");
        legio.removeKnownShip("tahlan_hammerhead_dmn");
        legio.removeKnownShip("tahlan_centurion_dmn");
        legio.removeKnownShip("tahlan_vanguard_dmn");
        legio.removeKnownShip("tahlan_DunScaith_dmn");
        legio.removeKnownShip("tahlan_hound_dmn");
        legio.removeKnownShip("tahlan_sunder_dmn");
        legio.removeKnownShip("tahlan_kodai_dmn");
    }

    @Override
    public void onNewGameAfterProcGen() {
        //Spawning hidden things
        tahlan_HalbmondSpawnScript.spawnHalbmond(Global.getSector());
        tahlan_DerelictsSpawnScript.spawnDerelicts(Global.getSector());
        tahlan_LostechSpawnScript.spawnLostech(Global.getSector());
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        if (!ENABLE_LIFELESS && sector.getFaction("remnant").knowsShip("tahlan_Timeless")) {
            sector.getFaction("remnant").removeKnownShip("tahlan_Timeless");
            sector.getFaction("remnant").removeKnownShip("tahlan_Nameless");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_disparax");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_relparax");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_nenparax");
        } else if (!sector.getFaction("remnant").knowsShip("tahlan_Timeless")) {
            sector.getFaction("remnant").addKnownShip("tahlan_Timeless", false);
            sector.getFaction("remnant").addKnownShip("tahlan_Nameless", false);
            sector.getFaction("remnant").addKnownWeapon("tahlan_disparax", false);
            sector.getFaction("remnant").addKnownWeapon("tahlan_relparax", false);
            sector.getFaction("remnant").addKnownWeapon("tahlan_nenparax", false);
        }
        if (ENABLE_LEGIO) {
            // Add our listener for stuff
            if (ENABLE_DAEMONS) {
                Global.getSector().addTransientListener(new TahlanTrigger());
            } else {
                removeDaemons(sector);
            }
            // If somehow the Daemons are missing, add them
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$tahlan_triggered")) {
                addDaemons(sector);
            }
            FactionAPI legio = Global.getSector().getFaction("tahlan_legioinfernalis");

            if (ENABLE_HARDMODE) {
                legio.addPriorityShip("tahlan_dominator_dmn");
                legio.addPriorityShip("tahlan_champion_dmn");
                legio.addPriorityShip("tahlan_manticore_dmn");
                legio.addPriorityShip("tahlan_hammerhead_dmn");
                legio.addPriorityShip("tahlan_centurion_dmn");
                legio.addPriorityShip("tahlan_vanguard_dmn");
                legio.addPriorityShip("tahlan_DunScaith_dmn");
                legio.addPriorityShip("tahlan_hound_dmn");
                legio.addPriorityShip("tahlan_sunder_dmn");
                legio.addPriorityShip("tahlan_kodai_dmn");
            } else {
                legio.removePriorityShip("tahlan_dominator_dmn");
                legio.removePriorityShip("tahlan_champion_dmn");
                legio.removePriorityShip("tahlan_manticore_dmn");
                legio.removePriorityShip("tahlan_hammerhead_dmn");
                legio.removePriorityShip("tahlan_centurion_dmn");
                legio.removePriorityShip("tahlan_vanguard_dmn");
                legio.removePriorityShip("tahlan_DunScaith_dmn");
                legio.removePriorityShip("tahlan_hound_dmn");
                legio.removePriorityShip("tahlan_sunder_dmn");
                legio.removePriorityShip("tahlan_kodai_dmn");
            }
            MarketAPI market = sector.getEconomy().getMarket("tahlan_rubicon_p03_market");
            if (market != null) {
                if (!market.hasIndustry("tahlan_legiohq")) {
                    market.addIndustry("tahlan_legiohq");
                }
                if (HAS_INDEVO) {
                    if (!market.hasCondition("IndEvo_mineFieldCondition")) {
                        market.addCondition("IndEvo_mineFieldCondition");
                        sector.getEconomy().getMarket("tahlan_rubicon_p01_market").addCondition("IndEvo_mineFieldCondition");
                        sector.getEconomy().getMarket("tahlan_rubicon_outpost_market").addCondition("IndEvo_mineFieldCondition");
                    }
                }
            }
        }
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$tahlan_triggered")) {
            if (!NexConfig.getFactionConfig("tahlan_legioinfernalis").diplomacyTraits.contains("monstrous")) {
                NexConfig.getFactionConfig("tahlan_legioinfernalis").diplomacyTraits.add("monstrous");
            }
            NexConfig.getFactionConfig("tahlan_legionifernalis").diplomacyPositiveChance.put("default",0.1f);
            NexConfig.getFactionConfig("tahlan_legionifernalis").diplomacyNegativeChance.put("default",2f);
        } else {
            NexConfig.getFactionConfig("tahlan_legioinfernalis").diplomacyTraits.remove("monstrous");
            NexConfig.getFactionConfig("tahlan_legionifernalis").diplomacyPositiveChance.put("default",0.5f);
            NexConfig.getFactionConfig("tahlan_legionifernalis").diplomacyNegativeChance.put("default",1f);
        }
    }

    private static class TahlanTrigger extends BaseCampaignEventListener {
        private TahlanTrigger() {
            super(false);
        }

        @Override
        public void reportEconomyMonthEnd() {
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$tahlan_triggered")) {
                LOGGER.info("Daemons lurk");
                return;
            }
            SectorAPI sector = Global.getSector();

            int iLegioStartingCondition = 0;
            if (Global.getSector().getClock().getCycle() >= 210) {
                iLegioStartingCondition++;
                LOGGER.info("Daemonic Incursion - Cycle");
            } //Choose cycle
            for (MarketAPI market : Misc.getPlayerMarkets(true)) {
                if (market.getSize() >= 5) {
                    iLegioStartingCondition++;
                    LOGGER.info("Daemonic Incursion - Market");
                    break;
                }
            } //Ok size 6
            MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
            if (mem.getBoolean(GateEntityPlugin.CAN_SCAN_GATES) && mem.getBoolean(GateEntityPlugin.GATES_ACTIVE)) {
                iLegioStartingCondition++; //Follow Histidine's "Skip Story format"
                LOGGER.info("Daemonic Incursion - Gates");
            }
            if (sector.getPlayerStats().getLevel() >= 13) {
                iLegioStartingCondition++;
                LOGGER.info("Daemonic Incursion - Level");
            } // Two capitals or Metafalica
            int caps = 0;
            for (FleetMemberAPI bote : sector.getPlayerFleet().getMembersWithFightersCopy()) {
                if (bote.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                    caps++;
                }
                // Metafalica counts double
                if (bote.getHullSpec().getHullId().contains("Metafalica")) {
                    caps++;
                }
                // got Daemons? instant trigger
                if (bote.getHullSpec().getHullId().contains("_dmn")) {
                    iLegioStartingCondition = 99;
                }
            }
            if (caps >= 2) {
                iLegioStartingCondition++;
                LOGGER.info("Daemonic Incursion - Ships");
            }
            if (Global.getSector().getPlayerFleet().getCargo().getCredits().get() > 5000000f) {
                iLegioStartingCondition++;
                LOGGER.info("Daemonic Incursion - Wealth");
            }
            if (Misc.getNumNonMercOfficers(Global.getSector().getPlayerFleet()) > 7) {
                iLegioStartingCondition++;
                LOGGER.info("Daemonic Incursion - Officers");
            }
            if (iLegioStartingCondition >= 4) {
                Global.getSector().getMemoryWithoutUpdate().set("$tahlan_triggered", true);
                LOGGER.info("The Daemonic horde awakens");
                FactionAPI legio = sector.getFaction("tahlan_legioinfernalis");
                if (Misc.getCommissionFaction() != legio) {
                    legio.setRelationship(sector.getPlayerFaction().getId(),RepLevel.HOSTILE);
                    legio.setRelationship(Misc.getCommissionFactionId(),RepLevel.HOSTILE);
                }
                NexConfig.getFactionConfig("tahlan_legioinfernalis").diplomacyTraits.add("monstrous");
                NexConfig.getFactionConfig("tahlan_legionifernalis").diplomacyPositiveChance.put("default",0.1f);
                NexConfig.getFactionConfig("tahlan_legionifernalis").diplomacyNegativeChance.put("default",2f);
                legio.addKnownShip("tahlan_dominator_dmn", false);
                legio.addKnownShip("tahlan_champion_dmn", false);
                legio.addKnownShip("tahlan_manticore_dmn", false);
                legio.addKnownShip("tahlan_hammerhead_dmn", false);
                legio.addKnownShip("tahlan_centurion_dmn", false);
                legio.addKnownShip("tahlan_vanguard_dmn", false);
                legio.addKnownShip("tahlan_DunScaith_dmn", false);
                legio.addKnownShip("tahlan_hound_dmn", false);
                legio.addKnownShip("tahlan_sunder_dmn", false);
                legio.addKnownShip("tahlan_kodai_dmn", false);
                legio.addKnownFighter("tahlan_miasma_drone_wing", false);
                legio.addKnownFighter("tahlan_flash_dmn_wing", false);
                legio.addKnownFighter("tahlan_spark_dmn_wing", false);
                legio.addKnownFighter("tahlan_lux_dmn_wing", false);
                legio.addKnownFighter("tahlan_thunder_dmn_wing", false);
                legio.addKnownFighter("tahlan_gaze_dmn_wing", false);
            }
        }

    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        switch (missile.getProjectileSpecId()) {
            case FOUNTAIN_MISSILE_ID:
                return new PluginPick<MissileAIPlugin>(new tahlan_FountainAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case KRIEGSMESSER_MISSILE_ID:
                return new PluginPick<MissileAIPlugin>(new tahlan_KriegsmesserAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case DOLCH_MISSILE_ID:
                return new PluginPick<MissileAIPlugin>(new tahlan_TwoStageMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
    }

    private static void loadTahlanSettings() throws IOException, JSONException {
        JSONObject setting = Global.getSettings().loadJSON(SETTINGS_FILE);
        ENABLE_LETHIA = setting.getBoolean("enableLethia");
        ENABLE_LEGIO = setting.getBoolean("enableLegio");
        ENABLE_LIFELESS = setting.getBoolean("enableLifelessShips");
        ENABLE_LEGIOBPS = setting.getBoolean("enableLegioBlueprintLearning");
        ENABLE_HARDMODE = setting.getBoolean("enableHardMode");
        ENABLE_DAEMONS = setting.getBoolean("enableDaemons");
    }
}