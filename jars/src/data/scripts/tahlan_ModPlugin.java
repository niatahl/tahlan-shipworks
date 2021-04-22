package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import data.scripts.campaign.*;
import data.scripts.weapons.ai.tahlan_FountainAI;
import data.scripts.weapons.ai.tahlan_KriegsmesserAI;
import data.scripts.weapons.ai.tahlan_TwoStageMissileAI;
import data.scripts.world.tahlan_FactionRelationPlugin;
import data.scripts.world.tahlan_Lethia;
import data.scripts.world.tahlan_Rubicon;
import exerelin.campaign.SectorManager;
import org.apache.log4j.Level;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import data.scripts.campaign.siege.LegioSiegeManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.siege.LegioSiegeBaseIntel.log;

public class tahlan_ModPlugin extends BaseModPlugin {
    static private boolean graphicsLibAvailable = false;
    static public boolean isGraphicsLibAvailable () {
        return graphicsLibAvailable;
    }
    //All hullmods related to shields, saved in a convenient list
    public static List<String> SHIELD_HULLMODS = new ArrayList<String>();

    public static final String FOUNTAIN_MISSILE_ID = "tahlan_fountain_msl";
    public static final String KRIEGSMESSER_MISSILE_ID = "tahlan_kriegsmesser_msl";
    public static final String DOLCH_MISSILE_ID = "tahlan_dolch_msl";

    private static final String SETTINGS_FILE = "tahlan_settings.ini";

    public static boolean ENABLE_LETHIA;
    public static boolean ENABLE_LEGIO;
    public static boolean ENABLE_SIEGE;
    public static boolean ENABLE_LIFELESS;
    public static boolean ENABLE_LEGIOBPS;

    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Tahlan Shipworks requires LazyLib by LazyWizard"  + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444");
        }
        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!hasMagicLib) {
            throw new RuntimeException("Tahlan Shipworks requires MagicLib!"  + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718");
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
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()){
            if (ENABLE_LETHIA) new tahlan_Lethia().generate(sector);
            if (ENABLE_LEGIO) new tahlan_Rubicon().generate(sector);
        }

        //Legio things
        if (ENABLE_LEGIO) {

            //Legio Infernalis relations
            tahlan_FactionRelationPlugin.initFactionRelationships(sector);

            //Adding Legio to bounty system
            SharedData.getData().getPersonBountyEventData().addParticipatingFaction("tahlan_legioinfernalis");

            //Legio siege event
            if (ENABLE_SIEGE) {
                Global.getSector().addScript(new LegioSiegeManager());
                log.info("added LegioSiegeManager");
            }

            //Legio stealing pirates homework
            if (ENABLE_LEGIOBPS) {
                Global.getSector().addScript(new tahlan_LegioStealingHomework());
            }

        } else {
            sector.getFaction("tahlan_legioinfernalis").setShowInIntelTab(false);

            if (haveNexerelin){
                sector.getFaction("tahlan_legioinfernalis").getMemoryWithoutUpdate().set("$nex_respawn_cooldown",true);
            }
        }

        //Rosenritter Blueprint Script
        Global.getSector().addScript(new tahlan_regaliablueprintscript());
        log.info("added Rosenritter Blueprint script");

        if (ENABLE_LIFELESS) {
            sector.getFaction("remnant").removeKnownShip("tahlan_Timeless");
            sector.getFaction("remnant").removeKnownShip("tahlan_Nameless");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_disparax");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_relparax");
            sector.getFaction("remnant").removeKnownWeapon("tahlan_nenparax");
        }

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
        } else if (!sector.getFaction("remnant").knowsShip("tahlan_Timeless")){
            sector.getFaction("remnant").addKnownShip("tahlan_Timeless",false);
            sector.getFaction("remnant").addKnownShip("tahlan_Nameless",false);
            sector.getFaction("remnant").addKnownWeapon("tahlan_disparax",false);
            sector.getFaction("remnant").addKnownWeapon("tahlan_relparax", false);
            sector.getFaction("remnant").addKnownWeapon("tahlan_nenparax", false);
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
        ENABLE_SIEGE = setting.getBoolean("enableLegioSiege");
        ENABLE_LIFELESS = setting.getBoolean("enableLifelessShips");
        ENABLE_LEGIOBPS = setting.getBoolean("enableLegioBlueprintLearning");
    }
}