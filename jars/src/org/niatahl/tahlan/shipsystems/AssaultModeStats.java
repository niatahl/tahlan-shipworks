package org.niatahl.tahlan.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.magiclib.util.MagicUI;

import java.awt.*;

public class AssaultModeStats extends BaseShipSystemScript {

    private static final float WEAPON_BOOST = 0.5f;
    private static final float MOBILITY_MULT = 0.5f;

    //private float shieldArc;
    //private boolean runOnce = true;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Ensures we have a ship
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        /*
        if (runOnce) {
            shieldArc = ship.getShield().getArc();
            runOnce = false;
        }
        */

        //Showing the charge level
        MagicUI.drawSystemBar(ship, Color.CYAN, effectLevel,0);

        if (effectLevel > 0) {
            stats.getBallisticRoFMult().modifyMult(id, 1f + WEAPON_BOOST*effectLevel);
            stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - WEAPON_BOOST*effectLevel);
            stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + WEAPON_BOOST*effectLevel);

            stats.getMaxSpeed().modifyMult(id, 1f - MOBILITY_MULT*effectLevel);
            stats.getAcceleration().modifyMult(id, 1f - MOBILITY_MULT*effectLevel);
            stats.getDeceleration().modifyMult(id, 1f - MOBILITY_MULT*effectLevel);
            stats.getMaxTurnRate().modifyMult(id, 1f - MOBILITY_MULT*effectLevel);
            stats.getTurnAcceleration().modifyMult(id, 1f - MOBILITY_MULT*effectLevel);

            //ship.getShield().setArc(shieldArc*(1f-0.5f*effectLevel));
        }

        //Finds all weapons that needs the system to be active, and activates them (sets their ammo to 1) if our system is fully charged
        if (effectLevel >= 1f) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().hasTag("tahlan_AMWeapon")) {
                    weapon.setAmmo(2);
                }
            }
        } else {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().hasTag("tahlan_AMWeapon")) {
                    weapon.setAmmo(0);
                }
            }
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

        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        /*
        if (!runOnce) {
            ship.getShield().setArc(shieldArc);
            runOnce = true;
        }
        */

        //Finds all weapons that needs the system to be active, and deactivates them (sets their ammo to 0)
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSpec().hasTag("tahlan_AMWeapon")) {
                weapon.setAmmo(0);
            }
        }



    }
}
