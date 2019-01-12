package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.EnumSet;

public class tahlan_HolyFurorStats extends BaseShipSystemScript {

    public static final float DAMAGE_BONUS_PERCENT = 1.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
        stats.getEnergyWeaponDamageMult().modifyMult(id, bonusPercent);

        stats.getShieldDamageTakenMult().modifyMult(id, 1f - 0.5f * effectLevel);

        stats.getMaxSpeed().modifyFlat(id, 100f);
        stats.getAcceleration().modifyFlat(id, 200f);

        EnumSet<WeaponAPI.WeaponType> WEAPON_TYPES = EnumSet.of(WeaponAPI.WeaponType.BALLISTIC);

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
            return new StatusData("+" + ((DAMAGE_BONUS_PERCENT-1f)*100f) + "% energy weapon damage" , false);
        } else if (index == 1) {
            return new StatusData("engines and shields boosted", false);
        } else if (index == 2) {
            //return new StatusData("shield damage taken +" + (int) damageTakenPercent + "%", true);
            return null;
        }
        return null;
    }
}
