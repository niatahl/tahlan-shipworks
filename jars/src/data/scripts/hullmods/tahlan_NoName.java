package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_NoName extends BaseHullMod {


    public static final float SUPPLIES_MULT = 2f;

    public static final float TIME_MULT = 1.3f;
    private static final Color AFTERIMAGE_COLOR = new Color(133, 126, 116, 102);
    private static final float AFTERIMAGE_THRESHOLD = 0.4f;

    private final String INNERLARGE = "graphics/tahlan/fx/tahlan_tempshield.png";
    private final String OUTERLARGE = "graphics/tahlan/fx/tahlan_tempshield_ring.png";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getSuppliesPerMonth().modifyMult(id, SUPPLIES_MULT);
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 0f);

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNERLARGE, OUTERLARGE);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        //don't run while paused because duh
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        if ( !ship.isAlive() || ship.isPiece() ) {
            return;
        }

        //The Great Houses are actually timelords
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        String id = "tahlan_NoNameID";
        if (player) {
            ship.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / TIME_MULT);
        } else {
            ship.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").modifyFlat("tahlan_NNAfterimageTrackerNullerID", -1);
        ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").modifyFlat("tahlan_NNAfterimageTrackerID",
                ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").getModifiedValue() + amount);
        if (ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHOLD) {

            // Sprite offset fuckery - Don't you love trigonometry?
            SpriteAPI sprite = ship.getSpriteAPI();
            float offsetX = sprite.getWidth()/2 - sprite.getCenterX();
            float offsetY = sprite.getHeight()/2 - sprite.getCenterY();

            float trueOffsetX = (float)FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetX - (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetY;
            float trueOffsetY = (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetX + (float)FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetY;

            MagicRender.battlespace(
                    Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                    new Vector2f(ship.getLocation().getX()+trueOffsetX,ship.getLocation().getY()+trueOffsetY),
                    new Vector2f(0, 0),
                    new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                    new Vector2f(0, 0),
                    ship.getFacing()-90f,
                    0f,
                    AFTERIMAGE_COLOR,
                    true,
                    0.1f,
                    0.1f,
                    1f,
                    CombatEngineLayers.BELOW_SHIPS_LAYER);

            ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").modifyFlat("tahlan_NNAfterimageTrackerID",
                    ship.getMutableStats().getDynamic().getStat("tahlan_NNAfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHOLD);
        }

    }


    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return txt("hmd_KassEng1");
        if (index == 1) return "" + Math.round((TIME_MULT - 1f) * 100f) + txt("%");
        if (index == 2) return txt("hmd_NoName1");
        if (index == 3) return "" + Math.round((SUPPLIES_MULT - 1f) * 100f) + txt("%");
        return null;
    }
}
