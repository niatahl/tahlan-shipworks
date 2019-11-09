package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.EnumSet;

public class tahlan_HolyFurorStats extends BaseShipSystemScript {

    public static final float DAMAGE_BONUS_PERCENT = 1.5f;
    public static final float MAX_TIME_MULT = 1.2f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;

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

        stats.getEnergyWeaponDamageMult().modifyMult(id, bonusPercent);

        stats.getShieldDamageTakenMult().modifyMult(id, 1f - 0.5f * effectLevel);

        stats.getMaxSpeed().modifyFlat(id, 50f);
        stats.getAcceleration().modifyFlat(id, 100f);

        //time acceleration
        float TimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
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
