package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

import static data.scripts.utils.tahlan_txt.txt;

public class tahlan_VundAutoforge extends BaseHullMod {

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if ( index == 0 ) return txt("hmd_VundForge1");
        if ( index == 1 ) return txt("hmd_VundForge2");
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
