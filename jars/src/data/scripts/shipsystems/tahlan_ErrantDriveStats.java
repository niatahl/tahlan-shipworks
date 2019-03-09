package data.scripts.shipsystems;


import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


public class tahlan_ErrantDriveStats extends BaseShipSystemScript {

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 100);
    private static final float AFTERIMAGE_THRESHOLD = 0.2f;
    public static final float MAX_TIME_MULT = 2f;

    public static final float ELECTRIC_SIZE = 80.0f;
    public static final float ELECTRIC_SIZE_DESTROYER = 100.0f;
    public static final float ELECTRIC_SIZE_CRUISER = 200.0f;
    public static final float ELECTRIC_SIZE_CAPITAL = 300.0f;

    public boolean HAS_FIRED_LIGHTNING = false;

    public static final Color JITTER_COLOR = new Color(255, 106, 32, 55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 54, 0, 155);

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
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

            float driftamount = engine.getElapsedInLastFrame();

        if (state == State.IN) {
            /*
            float speed = ship.getVelocity().length();
            if (speed <= 0.1f) {
                ship.getVelocity().set(VectorUtils.getDirectionalVector(ship.getLocation(), ship.getVelocity()));
            }
            if (speed < 900f) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed + driftamount * 3600f);
            }
            */



            ship.getMutableStats().getAcceleration().modifyFlat(id,5000f);
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


            //stats.getMaxSpeed().modifyFlat(id, 200f * effectLevel);
            //stats.getAcceleration().modifyFlat(id, 500f * effectLevel);
            //stats.getDeceleration().modifyFlat(id, 500f * effectLevel);
            //stats.getMaxTurnRate().modifyMult(id,1.2f);
            //stats.getTurnAcceleration().modifyMult(id, 1.2f);
            stats.getEmpDamageTakenMult().modifyMult(id, 0.5f * effectLevel);
            stats.getArmorDamageTakenMult().modifyMult(id, 0.5f * effectLevel);
            stats.getHullDamageTakenMult().modifyMult(id, 0.5f * effectLevel);

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


