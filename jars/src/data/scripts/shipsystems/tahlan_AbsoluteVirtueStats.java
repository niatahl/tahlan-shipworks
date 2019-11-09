package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class tahlan_AbsoluteVirtueStats extends BaseShipSystemScript {

    public static final float FLUX_BONUS_PERCENT = 2f;
    public static final float HARD_FLUX_DISSIPATION_PERCENT = 50f;
    public static final float MAX_TIME_MULT = 1.2f;
    public static final float SHIELD_DAMAGE_MULT = 0.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        float bonusPercent = 1f + (FLUX_BONUS_PERCENT - 1f) * effectLevel;

        ShipAPI ship = null;
        boolean player = false;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        if (ship.getShield() != null) {
            //That shield is going up
            if (ship.getShield().isOff()) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }

            //And it's staying like that
            if (ship.getShield().isOn()) {
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            }
        }

        stats.getFluxDissipation().modifyMult(id, bonusPercent);
        stats.getHardFluxDissipationFraction().modifyFlat(id, (float) HARD_FLUX_DISSIPATION_PERCENT * 0.01f);
        stats.getShieldDamageTakenMult().modifyMult(id, SHIELD_DAMAGE_MULT);

        stats.getMaxSpeed().modifyFlat(id, 100f);
        stats.getAcceleration().modifyFlat(id, 200f);
        stats.getShieldUnfoldRateMult().modifyMult(id, 3f);

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
        } else {
            return;
        }

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldUnfoldRateMult().unmodify(id);
        stats.getHardFluxDissipationFraction().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float bonusPercent = (FLUX_BONUS_PERCENT - 1f) * effectLevel * 100f;
        if (index == 0) {
            return new StatusData("Dissipation boosted by " + (int) bonusPercent + "%", false);
        } else if (index == 1) {
            return new StatusData("Timeflow accelerated", false);
        }
        return null;
    }
}
