package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_RoyalChargeStats extends BaseShipSystemScript {

    private static final float SPEED_BONUS = 300f;
    private static final float TURNRATE_MULT = 0.5f;
    private static final float DAMAGE_MULT = 0.1f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        stats.getMaxSpeed().modifyFlat(id,SPEED_BONUS*effectLevel);
        stats.getAcceleration().modifyFlat(id,SPEED_BONUS*effectLevel);
        stats.getMaxTurnRate().modifyMult(id,TURNRATE_MULT*effectLevel);

        stats.getEmpDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);

    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

    }
}
