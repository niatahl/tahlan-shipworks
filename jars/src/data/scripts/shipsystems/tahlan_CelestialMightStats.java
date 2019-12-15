package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class tahlan_CelestialMightStats extends BaseShipSystemScript {

    public static final float DAMAGE_BONUS_PERCENT = 1.5f;
    public static final float DISSIPATION_MULT = 1.5f;
    public static final float MAX_TIME_MULT = 3f;

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 100);

    private IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        float bonusPercent = 1f + (DAMAGE_BONUS_PERCENT - 1f) * effectLevel;

        stats.getEnergyWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getBallisticWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getBeamWeaponDamageMult().modifyMult(id, bonusPercent);

        stats.getFluxDissipation().modifyMult(id,DISSIPATION_MULT);

        float TimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
        } else {
            stats.getMaxSpeed().modifyFlat(id, 200f);
            stats.getAcceleration().modifyFlat(id, 400f);
            stats.getDeceleration().modifyFlat(id, 400f);
            stats.getTurnAcceleration().modifyMult(id, 2f);
            stats.getMaxTurnRate().modifyMult(id, 2f);
        }

        //For Afterimages
        if (!Global.getCombatEngine().isPaused()) {

            interval.advance(Global.getCombatEngine().getElapsedInLastFrame());

            if (interval.intervalElapsed()) {

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
                        1f,
                        CombatEngineLayers.BELOW_SHIPS_LAYER);
            }
        }

    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);

        stats.getFluxDissipation().unmodify(id);

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
        if (index == 0) {
            return new StatusData("+" + ((DAMAGE_BONUS_PERCENT-1f)*100f) + "% weapon damage" , false);
        } else if (index == 1) {
            return new StatusData("engines and dissipation boosted", false);
        } else if (index == 2) {
            return new StatusData("Timeflow accelerated by " + (int) Math.round((MAX_TIME_MULT*100f)-100f) + "%", false);
        } else if (index == 3) {
            return null;
        }
        return null;
    }
}
