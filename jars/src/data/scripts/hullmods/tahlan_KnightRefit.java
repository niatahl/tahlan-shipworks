package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class tahlan_KnightRefit extends BaseHullMod {


    public static final float ARMOR_MALUS_FRIGATE = 50f;
    public static final float ARMOR_MALUS_DESTROYER = 100f;
    public static final float ARMOR_MALUS_CRUISER = 200f;
    public static final float ARMOR_MALUS_CAPITAL = 300f;

    public static final float SUPPLIES_MULT = 1.5f;

    public static final float OVERDRIVE_TRIGGER_PERCENTAGE = 0.3f;
    public static final float OVERDRIVE_TIME_MULT = 1.3f;

    public static final float TIME_MULT = 1.1f;
    private static final Color AFTERIMAGE_COLOR = new Color(133, 126, 116, 90);
    private static final float AFTERIMAGE_THRESHOLD = 0.4f;

    private static final Color OVERDRIVE_ENGINE_COLOR = new Color(255, 44, 0);
    private static final Color OVERDRIVE_GLOW_COLOR = new Color(255, 120, 16);
    private static final Color OVERDRIVE_JITTER_COLOR = new Color(255, 63, 0, 20);
    private static final Color OVERDRIVE_JITTER_UNDER_COLOR = new Color(255, 63, 0, 80);

    private float fadeOut = 1f;
    private static final String ke_id = "tahlan_KnightRefitID";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        //Better flux stats
        //stats.getFluxCapacity().modifyMult(id,FLUX_MULT);
        //stats.getFluxDissipation().modifyMult(id,FLUX_MULT);

        switch (hullSize) {
            case FRIGATE:
                stats.getArmorBonus().modifyFlat(ke_id, -ARMOR_MALUS_FRIGATE);
                break;
            case DESTROYER:
                stats.getArmorBonus().modifyFlat(ke_id, -ARMOR_MALUS_DESTROYER);
                break;
            case CRUISER:
                stats.getArmorBonus().modifyFlat(ke_id, -ARMOR_MALUS_CRUISER);
                break;
            case CAPITAL_SHIP:
                stats.getArmorBonus().modifyFlat(ke_id, -ARMOR_MALUS_CAPITAL);
        }

        //stats.getMaxSpeed().modifyMult(id,HANDLING_MULT);
        //stats.getAcceleration().modifyMult(id,HANDLING_MULT);
        //stats.getDeceleration().modifyMult(id,HANDLING_MULT);
        //stats.getTurnAcceleration().modifyMult(id,HANDLING_MULT);

        stats.getSuppliesPerMonth().modifyMult(ke_id, SUPPLIES_MULT);
        stats.getCRLossPerSecondPercent().modifyMult(ke_id, 2f);


    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        //don't run while paused because duh
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //The Great Houses are actually timelords
        boolean player = ship == Global.getCombatEngine().getPlayerShip();

        if (ship.getSystem() != null) {
            if (!ship.getSystem().isActive()) {
                if (ship.getHitpoints() <= ship.getMaxHitpoints() * OVERDRIVE_TRIGGER_PERCENTAGE || ship.getVariant().getHullMods().contains("tahlan_forcedoverdrive")) {

                    if (player) {
                        ship.getMutableStats().getTimeMult().modifyMult(ke_id, OVERDRIVE_TIME_MULT);
                        Global.getCombatEngine().getTimeMult().modifyMult(ke_id, 1f / OVERDRIVE_TIME_MULT);
                    } else {
                        ship.getMutableStats().getTimeMult().modifyMult(ke_id, OVERDRIVE_TIME_MULT);
                        Global.getCombatEngine().getTimeMult().unmodify(ke_id);
                    }

                    if (!(ship.getOriginalOwner() == -1)) {

                        EnumSet<WeaponType> WEAPON_TYPES = EnumSet.of(WeaponType.BALLISTIC, WeaponType.COMPOSITE, WeaponType.MISSILE);
                        ship.setWeaponGlow(0.4f, OVERDRIVE_GLOW_COLOR, WEAPON_TYPES);

                        ship.getEngineController().fadeToOtherColor(this, OVERDRIVE_ENGINE_COLOR, null, 1f, 0.7f);
                        ship.setJitter(ke_id, OVERDRIVE_JITTER_COLOR, 0.5f, 3, 5f);
                        ship.setJitterUnder(ke_id, OVERDRIVE_JITTER_UNDER_COLOR, 0.5f, 20, 10f);

                    }

                    if (player) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(ke_id, "graphics/icons/hullsys/temporal_shell.png", "Temporal Overdrive", "Timeflow at 130%", false);
                    }

                    fadeOut  = 2f;

                } else {

                    if (player) {
                        ship.getMutableStats().getTimeMult().modifyMult(ke_id, TIME_MULT);
                        Global.getCombatEngine().getTimeMult().modifyMult(ke_id, 1f / TIME_MULT);
                        Global.getCombatEngine().maintainStatusForPlayerShip(ke_id, "graphics/icons/hullsys/temporal_shell.png", "Temporal Field", "Timeflow at 110%", false);
                    } else {
                        ship.getMutableStats().getTimeMult().modifyMult(ke_id, TIME_MULT);
                        Global.getCombatEngine().getTimeMult().unmodify(ke_id);
                    }

                    fadeOut = 1f;

                }

                ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerNullerID", -1);
                ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerID",
                        ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() + amount);
                if (ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHOLD) {

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
                            fadeOut,
                            CombatEngineLayers.BELOW_SHIPS_LAYER);

                    ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").modifyFlat("tahlan_KRAfterimageTrackerID",
                            ship.getMutableStats().getDynamic().getStat("tahlan_KRAfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHOLD);

                }

            } else {
                ship.getMutableStats().getTimeMult().unmodify(ke_id);
                Global.getCombatEngine().getTimeMult().unmodify(ke_id);
            }
        }




    }


    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "Temporal Circuit Grid";
        if (index == 1) return "" + Math.round((TIME_MULT - 1f) * 100f) + "%";
        if (index == 2)
            return "" + (int) ARMOR_MALUS_FRIGATE + "/" + (int) ARMOR_MALUS_DESTROYER + "/" + (int) ARMOR_MALUS_CRUISER + "/" + (int) ARMOR_MALUS_CAPITAL;
        if (index == 3) return "" + Math.round((SUPPLIES_MULT - 1f) * 100f) + "%";
        if (index == 4) return "" + Math.round(OVERDRIVE_TRIGGER_PERCENTAGE * 100f) + "%";
        if (index == 5) return "" + Math.round((OVERDRIVE_TIME_MULT - 1f) * 100f) + "%";
        if (index == 6) return "suspended while the ship system is active";
        return null;
    }
}
