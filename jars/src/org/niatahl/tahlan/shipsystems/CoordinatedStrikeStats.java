package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.util.C;

import java.awt.*;
import java.util.EnumSet;

import static com.fs.starfarer.api.impl.combat.RecallDeviceStats.getFighters;

public class CoordinatedStrikeStats extends BaseShipSystemScript {

    static final float DAMAGE_MULT = 1.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        stats.getEnergyWeaponDamageMult().modifyMult(id,DAMAGE_MULT);

        if (effectLevel > 0) {
            float maxRangeBonus = 5f;
            float jitterRangeBonus = effectLevel * maxRangeBonus;
            for (ShipAPI fighter : getFighters(ship)) {
                if (fighter.isHulk()) continue;
                MutableShipStatsAPI fStats = fighter.getMutableStats();
                fStats.getEnergyWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
                fStats.getBallisticWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
                fStats.getMissileWeaponDamageMult().modifyMult(id,DAMAGE_MULT);
                fighter.setWeaponGlow(0.5f, new Color(255, 0, 255, 255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
                Global.getSoundPlayer().playLoop("system_targeting_feed_loop", ship, 1f, 1f, fighter.getLocation(), fighter.getVelocity());

            }
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        stats.getEnergyWeaponDamageMult().unmodify(id);

        for (ShipAPI fighter : getFighters(ship)) {
            if (fighter.isHulk()) continue;
            MutableShipStatsAPI fStats = fighter.getMutableStats();
            fighter.setWeaponGlow(0f, new Color(255, 0, 255, 255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
            fStats.getEnergyWeaponDamageMult().unmodify(id);
            fStats.getBallisticWeaponDamageMult().unmodify(id);
            fStats.getMissileWeaponDamageMult().unmodify(id);
        }

    }

}
