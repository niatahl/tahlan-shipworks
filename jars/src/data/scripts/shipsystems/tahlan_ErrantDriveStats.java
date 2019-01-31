package data.scripts.shipsystems;


import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.VectorUtils;


public class tahlan_ErrantDriveStats extends BaseShipSystemScript {

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 100);
    private static final float AFTERIMAGE_THRESHOLD = 0.2f;
    public static final float MAX_TIME_MULT = 2f;

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
            float speed = ship.getVelocity().length();
            if (speed <= 0.1f) {
                ship.getVelocity().set(VectorUtils.getDirectionalVector(ship.getLocation(), ship.getVelocity()));
            }
            if (speed < 900f) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed + driftamount * 3600f);
            }
        } else if (state == State.ACTIVE) {
            float speed = ship.getVelocity().length();
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
        //stats.getMaxSpeed().unmodify(id);
        //stats.getAcceleration().unmodify(id);
        //stats.getDeceleration().unmodify(id);
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


