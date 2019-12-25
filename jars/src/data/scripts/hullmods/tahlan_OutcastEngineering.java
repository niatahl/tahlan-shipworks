package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.plugins.MagicTrailPlugin;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class tahlan_OutcastEngineering extends BaseHullMod {

    public static final float TIME_MULT = 10f;
    private static final Color AFTERIMAGE_COLOR = new Color(133, 126, 116, 90);
    private static final float AFTERIMAGE_THRESHOLD = 0.4f;

    public static final float MAX_DAMAGE_REDUCTION = 0.3f;
    public static final float MAX_EMP_REDUCTION = 0.6f;

    public static final float SUPPLIES_MULT = 100f;

    private float fadeOut = 1f;
    private static final String ke_id = "tahlan_KnightRefitID";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getSuppliesPerMonth().modifyPercent(ke_id, SUPPLIES_MULT);
        stats.getCRLossPerSecondPercent().modifyMult(ke_id, 2f);
    }

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //The Great Houses are actually timelords
        boolean player = ship == Global.getCombatEngine().getPlayerShip();

        if ( !ship.isAlive() || ship.isPiece() ) {
            return;
        }

        if (player) {
            ship.getMutableStats().getTimeMult().modifyPercent(ke_id, TIME_MULT);
            Global.getCombatEngine().getTimeMult().modifyPercent(ke_id, 1f / TIME_MULT);
            Global.getCombatEngine().maintainStatusForPlayerShip(ke_id, "graphics/icons/hullsys/temporal_shell.png", "Temporal Field", "Timeflow at 110%", false);
        } else {
            ship.getMutableStats().getTimeMult().modifyPercent(ke_id, TIME_MULT);
            Global.getCombatEngine().getTimeMult().unmodify(ke_id);
        }

        fadeOut = 1f;

        ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerNullerID", -1);
        ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerID",
                ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() + amount);
        if (ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHOLD) {

            // Sprite offset fuckery - Don't you love trigonometry?
            SpriteAPI sprite = ship.getSpriteAPI();
            float offsetX = sprite.getWidth() / 2 - sprite.getCenterX();
            float offsetY = sprite.getHeight() / 2 - sprite.getCenterY();

            float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
            float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;

            MagicRender.battlespace(
                    Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                    new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
                    new Vector2f(0, 0),
                    new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                    new Vector2f(0, 0),
                    ship.getFacing() - 90f,
                    0f,
                    AFTERIMAGE_COLOR,
                    true,
                    0.1f,
                    0.1f,
                    fadeOut,
                    CombatEngineLayers.BELOW_SHIPS_LAYER);

            ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerID",
                    ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHOLD);

        }

        //----------------------------------------------------------------------------HANDLES ARMOR BONUS--------------------------------------------------------------------------------------------
        //We get damage/EMP reduction for our armor depending on flux level, starting at 30% flux and maxing out at 90% flux
        float currentEffect = ((Math.max(0.3f, Math.min(0.9f, ship.getFluxTracker().getFluxLevel())) - 0.3f) / 0.6f);

        //Nia: Basic damage reduction variant. Possibly with a higher bonus to armor damage reduction than hull damage reduction?
        //			Sometimes simplest is best and it does match with the damper-field like visuals.
        //			EMP resist seems like a good addition to this. The Metafalica in particular will like this with how easy those broadside guns get shut down by EMP
		//Nicke: Agreed; using the code, but fixing the values to utilize actual variables
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult("tahlan_FluxArmorID", 1f - (MAX_DAMAGE_REDUCTION*currentEffect));
        ship.getMutableStats().getHullDamageTakenMult().modifyMult("tahlan_FluxArmorID", 1f - (MAX_DAMAGE_REDUCTION*currentEffect));
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("tahlan_FluxArmorID", 1f - (MAX_EMP_REDUCTION*currentEffect));



    }

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean canBeApplied = false;
        return canBeApplied;
    }

    //Adds the description strings
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "Temporal Circuit Grid";
        if (index == 1) return "" + (int)TIME_MULT + "%";
        if (index == 2) return "" + (int)(MAX_DAMAGE_REDUCTION * 100f) + "%";
        if (index == 3) return "" + (int)(MAX_EMP_REDUCTION * 100f) + "%";
        if (index == 4) return "30%";
        if (index == 5) return "90%";
        if (index == 6) return "" + (int)SUPPLIES_MULT + "%";
        return null;
    }
}