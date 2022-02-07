package data.scripts.shipsystems;


import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


public class tahlan_ErrantDriveStats extends BaseShipSystemScript {

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 100);
    private static final float AFTERIMAGE_THRESHOLD = 0.4f;
    public static final float MAX_TIME_MULT = 2f;

    public static final float ELECTRIC_SIZE = 80.0f;
    public static final float ELECTRIC_SIZE_DESTROYER = 100.0f;
    public static final float ELECTRIC_SIZE_CRUISER = 200.0f;
    public static final float ELECTRIC_SIZE_CAPITAL = 300.0f;

    public boolean HAS_FIRED_LIGHTNING = false;

    public static final Color JITTER_COLOR = new Color(255, 106, 32, 55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 54, 0, 125);

    private IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        boolean player;
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
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        float driftamount = engine.getElapsedInLastFrame();

        if (state == State.IN) {

            ship.getMutableStats().getAcceleration().modifyFlat(id, 5000f);
            ship.getMutableStats().getDeceleration().modifyFlat(id, 5000f);

        } else if (state == State.ACTIVE) {

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

        //Fires lightning at full charge, once
        float actualElectricSize = ELECTRIC_SIZE;
        if (ship.getHullSpec().getHullId().contains("tahlan_enforcer_gh")) {
            actualElectricSize = ELECTRIC_SIZE_DESTROYER;
        }
        if (ship.getHullSpec().getHullId().contains("tahlan_Castigator_knight")) {
            actualElectricSize = ELECTRIC_SIZE_CRUISER;
        }
        if (ship.getHullSpec().getHullId().contains("tahlan_vendetta_gh")) {
            actualElectricSize = ELECTRIC_SIZE_CAPITAL;
        }
        if (effectLevel >= 0.9f) {
            if (!HAS_FIRED_LIGHTNING) {
                HAS_FIRED_LIGHTNING = true;
                /*Lightning based code...*/
                float tempCounter = 0;
                while (tempCounter <= (6.0f / ELECTRIC_SIZE) * actualElectricSize) {
                    Global.getCombatEngine().spawnEmpArc(ship, new Vector2f(ship.getLocation().x + MathUtils.getRandomNumberInRange(-actualElectricSize, actualElectricSize), ship.getLocation().y + MathUtils.getRandomNumberInRange(-actualElectricSize, actualElectricSize)), null, ship,
                            DamageType.ENERGY, //Damage type
                            0f, //Damage
                            0f, //Emp
                            100000f, //Max range
                            "tachyon_lance_emp_impact",
                            (10f / ELECTRIC_SIZE) * actualElectricSize, // thickness
                            JITTER_COLOR, //Central color
                            JITTER_UNDER_COLOR //Fringe Color
                    );
                    tempCounter++;
                }
            }
        } else {
            HAS_FIRED_LIGHTNING = false;
        }

        stats.getEmpDamageTakenMult().modifyMult(id, 0.5f * effectLevel);
        stats.getArmorDamageTakenMult().modifyMult(id, 0.5f * effectLevel);
        stats.getHullDamageTakenMult().modifyMult(id, 0.5f * effectLevel);

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
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f,
                        0.1f,
                        1f,
                        CombatEngineLayers.BELOW_SHIPS_LAYER);
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

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        //stats.getMaxTurnRate().unmodify(id);
        //stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Engines and Armor boosted", false);
        }
        return null;
    }


    public float getActiveOverride(ShipAPI ship) {
//		if (ship.getHullSize() == HullSize.FRIGATE) {
//			return 1.25f;
//		}
//		if (ship.getHullSize() == HullSize.DESTROYER) {
//			return 0.75f;
//		}
//		if (ship.getHullSize() == HullSize.CRUISER) {
//			return 0.5f;
//		}
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


