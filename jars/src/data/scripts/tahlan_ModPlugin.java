package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

public class tahlan_ModPlugin extends BaseModPlugin {
    static private boolean graphicsLibAvailable = false;
    static public boolean isGraphicsLibAvailable () {
        return graphicsLibAvailable;
    }
    //All hullmods related to shields, saved in a convenient list
    public static List<String> SHIELD_HULLMODS = new ArrayList<String>();

    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Tahlan Shipworks requires LazyLib by LazyWizard");
        }
        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!hasMagicLib) {
            throw new RuntimeException("Tahlan Shipworks requires MagicLib!");
        }
        boolean hasSSFX = Global.getSettings().getModManager().isModEnabled("xxx_ss_FX_mod");
        if (hasSSFX) {
            throw new RuntimeException("Tahlan Shipworks is not compatible with Starsector FX");
        }

        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            graphicsLibAvailable = true;
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/tahlan_lights.csv");
            TextureData.readTextureDataCSV("data/lights/tahlan_texture.csv");
        } else {
            graphicsLibAvailable = false;
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
        //Prevents Vendetta (GH) from appearing in fleets unless DaRa is installed
        if (!Global.getSettings().getModManager().isModEnabled("DisassembleReassemble")) {
            Global.getSector().getFaction(Factions.INDEPENDENT).removeKnownShip("tahlan_vendetta_gh");
            if (Global.getSector().getFaction("tahlan_greathouses") != null) {
                Global.getSector().getFaction("tahlan_greathouses").removeKnownShip("tahlan_vendetta_gh");
            }
        }
    }
}
