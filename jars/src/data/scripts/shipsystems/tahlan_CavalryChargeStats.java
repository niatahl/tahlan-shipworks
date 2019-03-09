package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_CavalryChargeStats extends BaseShipSystemScript {

    private static final float SPEED_MULT = 1.5f;
    private static final float ROF_MULT = 1.5f;
    private static final float TURNRATE_MULT = 0.2f;
    private static final float DAMAGE_MULT = 0.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        stats.getMaxSpeed().modifyMult(id,SPEED_MULT*effectLevel);
        stats.getAcceleration().modifyMult(id,SPEED_MULT*effectLevel);
        stats.getBallisticRoFMult().modifyMult(id,ROF_MULT*effectLevel);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id,1f/ROF_MULT*effectLevel);
        stats.getMaxTurnRate().modifyMult(id,TURNRATE_MULT*effectLevel);

        stats.getEmpDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);

    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

    }
}
