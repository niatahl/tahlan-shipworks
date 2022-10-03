package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class InfernalChargeStats extends BaseShipSystemScript {

    private static final float SPEED_BONUS = 150f;
    private static final float ACCELERATION_BONUS = 200f;
    private static final float DAMAGE_MULT = 0.33f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
        stats.getAcceleration().modifyFlat(id, ACCELERATION_BONUS);

        stats.getEmpDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);

    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);;

        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

    }
}
