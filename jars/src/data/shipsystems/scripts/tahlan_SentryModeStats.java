package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_SentryModeStats extends BaseShipSystemScript {

    private static final float HANDLING_MULT = 0.1f;
    private static final float RANGE_BONUS = 300f;
    private static final float SHIELD_DAMAGE_MULT = 0.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getMaxSpeed().modifyMult(id,HANDLING_MULT);
        stats.getAcceleration().modifyMult(id,HANDLING_MULT);
        stats.getDeceleration().modifyMult(id,HANDLING_MULT);
        stats.getMaxTurnRate().modifyMult(id,HANDLING_MULT);
        stats.getTurnAcceleration().modifyMult(id,HANDLING_MULT);
        stats.getBallisticWeaponRangeBonus().modifyFlat(id,RANGE_BONUS);
        stats.getEnergyWeaponRangeBonus().modifyFlat(id,RANGE_BONUS);
        stats.getShieldDamageTakenMult().modifyMult(id,SHIELD_DAMAGE_MULT);

        stats.getEntity().getVelocity().scale(0.9f);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getEnergyWeaponRangeBonus().unmodify(id);
        stats.getBallisticWeaponRangeBonus().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("Sentry Mode active, range increased", false);
        }
        return null;
    }
}
