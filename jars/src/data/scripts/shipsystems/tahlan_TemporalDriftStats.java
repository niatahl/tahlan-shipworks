package data.scripts.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
//import data.scripts.tahlan_ModPlugin;
//import org.dark.shaders.distortion.DistortionShader;
//import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class tahlan_TemporalDriftStats extends BaseShipSystemScript {

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 60);
    private static final float AFTERIMAGE_THRESHOLD = 0.4f;
    public static final float DAMAGE_MULT = 2f;
    public static final float DPS_MULT = 0.5f;
    public static final float MAX_TIME_MULT = 20f;
    private static final float RIPPLE_SIZE = 1000f;
    private static final float RIPPLE_INTENSITY = 40f;

    public static final Color JITTER_COLOR = new Color(255, 106, 32, 55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 54, 0, 155);

    public static final float ELECTRIC_SIZE = 80.0f;
    public static final float ELECTRIC_SIZE_IZANAMI = 300.0f;

    public boolean HAS_FIRED_LIGHTNING = false;
    private static final Vector2f ZERO = new Vector2f();
    private boolean runOnce = false;

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
//                if (tahlan_ModPlugin.isGraphicsLibAvailable()) {
//                    RippleDistortion ripple = new RippleDistortion(loc, ZERO);
//                    ripple.setSize(RIPPLE_SIZE);
//                    ripple.setIntensity(RIPPLE_INTENSITY);
//                    ripple.setFrameRate(120f);
//                    DistortionShader.addDistortion(ripple);
//                }
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
                //visual effect
                /*Global.getCombatEngine().spawnExplosion(
                        //where
                        ship.getLocation(),
                        //speed
                        (Vector2f) new Vector2f(0,0),
                        //color
                        JITTER_COLOR,
                        //size
                        (MathUtils.getRandomNumberInRange(75f,100f) / ELECTRIC_SIZE) * actualElectricSize,
                        //duration
                        1.0f
                );*/
            }
        } else {
            HAS_FIRED_LIGHTNING = false;
        }

        //time acceleration
        float TimeMult = (float) Math.pow(MAX_TIME_MULT, effectLevel);
        stats.getTimeMult().modifyMult(id, TimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        //damage taken debuff
        float ActualDamageMult = (float) Math.pow(DAMAGE_MULT, effectLevel);
        stats.getShieldDamageTakenMult().modifyMult(id, ActualDamageMult);
        stats.getArmorDamageTakenMult().modifyMult(id, ActualDamageMult);

        //dps debuff
        float ActualDPSMult = (float) Math.pow(DPS_MULT, effectLevel);
        stats.getEnergyRoFMult().modifyMult(id, ActualDPSMult);
        stats.getBallisticRoFMult().modifyMult(id, ActualDPSMult);
        stats.getBeamWeaponDamageMult().modifyMult(id, ActualDPSMult);


        //For Afterimages
        if (!Global.getCombatEngine().isPaused()) {

            float amount = Global.getCombatEngine().getElapsedInLastFrame() * ship.getMutableStats().getTimeMult().getModifiedValue();
            ship.getMutableStats().getDynamic().getStat("tahlan_AfterimageTracker").modifyFlat("tahlan_AfterimageTrackerNullerID", -1);
            ship.getMutableStats().getDynamic().getStat("tahlan_AfterimageTracker").modifyFlat("tahlan_AfterimageTrackerID",
                    ship.getMutableStats().getDynamic().getStat("tahlan_AfterimageTracker").getModifiedValue() + amount);
            if (ship.getMutableStats().getDynamic().getStat("tahlan_AfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHOLD) {
                ship.addAfterimage(
                        AFTERIMAGE_COLOR,
                        0, //X-location
                        0, //Y-location
                        ship.getVelocity().getX() * (-1f), //X-velocity
                        ship.getVelocity().getY() * (-1f), //Y-velocity
                        2f, //Maximum jitter
                        0.1f, //In duration
                        0f, //Mid duration
                        0.6f, //Out duration
                        true, //Additive blend?
                        true, //Combine with sprite color?
                        false //Above ship?
                );
                ship.getMutableStats().getDynamic().getStat("tahlan_AfterimageTracker").modifyFlat("tahlan_AfterimageTrackerID",
                        ship.getMutableStats().getDynamic().getStat("tahlan_AfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHOLD);
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


