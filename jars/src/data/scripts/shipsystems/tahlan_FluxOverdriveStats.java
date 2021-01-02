package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;

import java.awt.*;
import java.util.EnumSet;

public class tahlan_FluxOverdriveStats extends BaseShipSystemScript {

    private static final float WEAPON_BOOST = 0.5f;
    private static final float MOBILITY_BOOST = 0.5f;
    private static final float DEFENSE_BOOST = 0.5f;

    private static final Color ENGINE_COLOR = new Color(255, 44, 0);
    private static final Color JITTER_COLOR = new Color(255, 63, 0, 30);
    private static final Color JITTER_UNDER_COLOR = new Color(255, 63, 0, 100);
    private static final Color GLOW_COLOR = new Color(255, 120, 16);

    private float power = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        power = Math.min((ship.getHardFluxLevel()/0.90f),1f);

        stats.getBallisticRoFMult().modifyMult(id,1f+WEAPON_BOOST*power*effectLevel);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id,1f/(1f+WEAPON_BOOST*power*effectLevel));
        stats.getEnergyWeaponDamageMult().modifyMult(id,1f+WEAPON_BOOST*power*effectLevel);

        stats.getMaxSpeed().modifyMult(id, 1f+MOBILITY_BOOST*power*effectLevel);
        stats.getAcceleration().modifyMult(id,1f+MOBILITY_BOOST*power*effectLevel);

        stats.getHullDamageTakenMult().modifyMult(id,1f-DEFENSE_BOOST*power*effectLevel);
        stats.getArmorDamageTakenMult().modifyMult(id,1f-DEFENSE_BOOST*power*effectLevel);
        stats.getEmpDamageTakenMult().modifyMult(id,1f-DEFENSE_BOOST*power*effectLevel);

        ship.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, power*effectLevel, 0.7f);
        ship.setJitter(id, JITTER_COLOR, 0.7f*power*effectLevel, 3, 5f);
        ship.setJitterUnder(id, JITTER_UNDER_COLOR, 0.7f*power*effectLevel, 20, 10f);

        EnumSet<WeaponType> WEAPON_TYPES = EnumSet.of(WeaponType.BALLISTIC);
        ship.setWeaponGlow(0.5f*power*effectLevel, GLOW_COLOR, WEAPON_TYPES);

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);

        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);

    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if ( index == 0) {
            return new StatusData("Flux Overdrive running at " + (int)(power*effectLevel*100f) + "%", false);
        }
        else if ( index == 1 ) {
            return new StatusData("Weapons and mobility boosted by " + (int)(WEAPON_BOOST*power*effectLevel*100f) + "%", false);
        }
        else if ( index == 2 ) {
            return new StatusData("Damage taken reduced by " + (int)(DEFENSE_BOOST*power*effectLevel*100f) + "%", false);
        }
        return null;
    }

}
