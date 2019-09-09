package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_CelestialMightStats extends BaseShipSystemScript {

    public static final float DAMAGE_BONUS_PERCENT = 1.5f;
    public static final float DISSIPATION_MULT = 1.5f;
    public static final float MAX_TIME_MULT = 3f;

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

        float bonusPercent = 1f + (DAMAGE_BONUS_PERCENT - 1f) * effectLevel;

        stats.getEnergyWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getBallisticWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getBeamWeaponDamageMult().modifyMult(id, bonusPercent);

        stats.getFluxDissipation().modifyMult(id,DISSIPATION_MULT);

        float TimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, (1f / TimeMult));
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
        } else {
            stats.getMaxSpeed().modifyFlat(id, 200f);
            stats.getAcceleration().modifyFlat(id, 400f);
            stats.getTurnAcceleration().modifyMult(id, 2f);
            stats.getMaxTurnRate().modifyMult(id, 2f);
        }

    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);

        stats.getFluxDissipation().unmodify(id);

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
        if (index == 0) {
            return new StatusData("+" + ((DAMAGE_BONUS_PERCENT-1f)*100f) + "% weapon damage" , false);
        } else if (index == 1) {
            return new StatusData("engines and dissipation boosted", false);
        } else if (index == 2) {
            return new StatusData("Timeflow accelerated by " + (int) Math.round((MAX_TIME_MULT*100f)-100f) + "%", false);
        } else if (index == 3) {
            return null;
        }
        return null;
    }
}
