package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class tahlan_VundAutoforge extends BaseHullMod {

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if ( index == 0 ) return "10%";
        if ( index == 1 ) return "per minute";
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
