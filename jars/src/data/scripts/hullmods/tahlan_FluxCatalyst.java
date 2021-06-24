package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

import static data.scripts.utils.tahlan_Utils.txt;

public class tahlan_FluxCatalyst extends BaseHullMod {

    private static final String id = "tahlan_FluxCatalystID";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        boolean player = ship == Global.getCombatEngine().getPlayerShip();

        float flux = ship.getFluxLevel();

        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(id,1f + 2f*Math.min(1f,flux/0.9f));
        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(id,1f + 2f*Math.min(1f,flux/0.9f));

        if (player) {
            Global.getCombatEngine().maintainStatusForPlayerShip("fluxcatalyst_id", "graphics/icons/hullsys/temporal_shell.png", "Flux Catalyst", "Weapon damage increased by " + (int)(Math.min(1f,flux/0.9f)*200f) + "%", false);
        }

    }

    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if ( index == 0 ) return txt("hmd_FluxCatalyst1");
        return null;
    }
}
