package data.scripts.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.fs.starfarer.api.util.Misc.ZERO;


public class tahlan_JauntDriveStats extends BaseShipSystemScript {

    private static final Color FLICKER_COLOR = new Color(129, 110, 99, 131);
    private static final Color AFTERIMAGE_COLOR = new Color(129, 80, 64, 69);
    private Color color = new Color(255, 179, 155,255);
    public static final float MAX_TIME_MULT = 5f;

    private IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        float TimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        ship.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);

        float driftamount = engine.getElapsedInLastFrame();

        interval.advance(engine.getElapsedInLastFrame());

        if (interval.intervalElapsed()) {
            ship.addAfterimage(AFTERIMAGE_COLOR,
                    0f,
                    0f,
                    ship.getVelocity().getX() * (-1f),
                    ship.getVelocity().getY() * (-1f),
                    5,0f,0.1f,0.5f,true,true,false);

            for (int i=0;i<5;i++) {
                engine.addNegativeNebulaParticle(
                        MathUtils.getRandomPointInCircle(ship.getLocation(), 20),
                        MathUtils.getRandomPointInCircle(ZERO,50f),
                        MathUtils.getRandomNumberInRange(60f, 120f),
                        0.3f,
                        0.5f,
                        0.5f,
                        MathUtils.getRandomNumberInRange(1.0f, 1.4f),
                        new Color(24, 181, 255)
                );
            }

        }

        if (state == State.IN) {

            ship.getMutableStats().getAcceleration().modifyFlat(id, 5000f);
            ship.getMutableStats().getDeceleration().modifyFlat(id, 5000f);

        } else if (state == State.ACTIVE) {


            ship.setPhased(true);
            ship.setExtraAlphaMult(0.25f);
            ship.setApplyExtraAlphaToEngines(true);



            ship.setJitter(ship,FLICKER_COLOR,0.5f,5,5f,10f);

            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);

            float speed = ship.getVelocity().length();
            if (speed <= 0.1f) {
                ship.getVelocity().set(VectorUtils.getDirectionalVector(ship.getLocation(), ship.getVelocity()));
            }
            if (speed < 900f) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed + driftamount * 3600f);
            }
        } else {
            float speed = ship.getVelocity().length();
            if (speed > ship.getMutableStats().getMaxSpeed().getModifiedValue()) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed - driftamount * 3600f);
            }
        }


    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);

        stats.getTimeMult().unmodify(id);

        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


    public float getActiveOverride(ShipAPI ship) {
        return -1;
    }

    public float getInOverride(ShipAPI ship) {
        return -1;
    }

    public float getOutOverride(ShipAPI ship) {
        return -1;
    }

    public float getRegenOverride(ShipAPI ship) {
        return -1;
    }

    public int getUsesOverride(ShipAPI ship) {
        return -1;
    }
}


