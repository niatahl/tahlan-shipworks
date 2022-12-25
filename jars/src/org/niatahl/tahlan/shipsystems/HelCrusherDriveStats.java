package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class HelCrusherDriveStats extends BaseShipSystemScript {

    public static final float SPEED_BOOST = 300f;
    public static final float MASS_MULT = 3f;
    public static final float DAMAGE_MULT = 0.5f;
    public static final float RANGE = 900f;
    public static final float ROF_MULT = 0.5f;

    private Float mass = null;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        if (mass == null) {
            mass = ship.getMass();
        }

        ShipAPI target = ship.getShipTarget();
        float turnrate = ship.getMaxTurnRate()*4;

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            ship.setMass(mass);
        } else {
            if (ship.getMass() == mass) {
                ship.setMass(mass * MASS_MULT);
            }
            stats.getMaxSpeed().modifyFlat(id, SPEED_BOOST);
            stats.getAcceleration().modifyFlat(id, SPEED_BOOST);
            stats.getEmpDamageTakenMult().modifyMult(id, DAMAGE_MULT);
            stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
            stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);

            stats.getBallisticRoFMult().modifyMult(id, ROF_MULT);
            stats.getEnergyRoFMult().modifyMult(id, ROF_MULT);

            if(target!=null && ship.getSystem().isActive()){
                //system's aiming effect
                float facing = ship.getFacing();
                facing=MathUtils.getShortestRotation(
                        facing,
                        VectorUtils.getAngle(ship.getLocation(), target.getLocation())
                );
                ship.setAngularVelocity(Math.min(turnrate, Math.max(-turnrate, facing*5)));

            }

        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        if (mass == null) {
            mass = ship.getMass();
        }
        if (ship.getMass() != mass) {
            ship.setMass(mass);
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Engines and Armor boosted", false);
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship);
        if (target != null && target != ship) {
            return "READY";
        }

        if ((target == null || target == ship) && ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }

    protected ShipAPI findTarget(ShipAPI ship) {
        ShipAPI target = ship.getShipTarget();
        if(
                target!=null
                        &&
                        (!target.isDrone()||!target.isFighter())
                        &&
                        MathUtils.isWithinRange(ship, target, RANGE)
                        &&
                        target.getOwner()!=ship.getOwner()
                ){
            return target;
        } else {
            return null;
        }
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        ShipAPI target = findTarget(ship);
        return target != null && target != ship;
    }



}


