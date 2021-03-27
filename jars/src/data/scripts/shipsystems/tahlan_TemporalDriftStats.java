package data.scripts.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class tahlan_TemporalDriftStats extends BaseShipSystemScript {

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 60);
    private static final float AFTERIMAGE_THRESHOLD = 4f;
    public static final float DAMAGE_MULT = 2f;
    public static final float DPS_MULT = 0.5f;
    public static final float MAX_TIME_MULT = 20f;

    public static final Color JITTER_COLOR = new Color(255, 106, 32, 55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 54, 0, 125);

    public static final float ELECTRIC_SIZE = 80.0f;
    public static final float ELECTRIC_SIZE_IZANAMI = 300.0f;

    public boolean HAS_FIRED_LIGHTNING = false;
    private static final Vector2f ZERO = new Vector2f();
    private boolean runOnce = false;

    private IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;


        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        //Distortion
        if (!runOnce) {
            runOnce = true;
            Vector2f loc = ship.getLocation();
            if (player) {
                Global.getSoundPlayer().playSound("tahlan_zawarudo", 1f, 1f, loc, ship.getVelocity());
            }
        }

        //Fires lightning at full charge, once
        float actualElectricSize = ELECTRIC_SIZE;
        if (ship.getHullSpec().getHullId().contains("tahlan_Izanami")) {
            actualElectricSize = ELECTRIC_SIZE_IZANAMI;
        }
        if (effectLevel >= 0.8f) {
            if (!HAS_FIRED_LIGHTNING) {
                HAS_FIRED_LIGHTNING = true;
                /*Lightning based code...*/
                float tempCounter = 0;
                while (tempCounter <= (5.0f / ELECTRIC_SIZE) * actualElectricSize) {
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

        //time acceleration
        float TimeMult = 1f + (MAX_TIME_MULT-1f)*effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        //damage taken debuff
        float ActualDamageMult = 1f + (1f-DAMAGE_MULT)*effectLevel;
        stats.getShieldDamageTakenMult().modifyMult(id, ActualDamageMult);
        stats.getArmorDamageTakenMult().modifyMult(id, ActualDamageMult);

        //dps debuff
        float ActualDPSMult = 1f - DPS_MULT*effectLevel;
        stats.getEnergyWeaponDamageMult().modifyMult(id, ActualDPSMult);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, ActualDPSMult);
        stats.getBallisticRoFMult().modifyMult(id, ActualDPSMult);


        //For Afterimages
        if (!Global.getCombatEngine().isPaused()) {

            interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (interval.intervalElapsed()) {

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
        } else {
            return;
        }

        runOnce = false;

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);

        stats.getEnergyRoFMult().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("ZA WARUDO", false);
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


