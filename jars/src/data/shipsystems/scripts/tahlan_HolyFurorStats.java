package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_HolyFurorStats extends BaseShipSystemScript {

    public static final float DAMAGE_BONUS_PERCENT = 50f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
        stats.getEnergyWeaponDamageMult().modifyPercent(id, bonusPercent);

        stats.getShieldDamageTakenMult().modifyMult(id, 1f - 0.5f * effectLevel);

        stats.getMaxSpeed().modifyFlat(id, 50f);
        stats.getAcceleration().modifyFlat(id, 200f);

    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
        if (index == 0) {
            return new StatusData("+" + (int) bonusPercent + "% energy weapon damage" , false);
        } else if (index == 1) {
            return new StatusData("engines and shields boosted", false);
        } else if (index == 2) {
            //return new StatusData("shield damage taken +" + (int) damageTakenPercent + "%", true);
            return null;
        }
        return null;
    }
}
