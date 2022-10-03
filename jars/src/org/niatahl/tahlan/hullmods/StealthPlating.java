package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import static org.niatahl.tahlan.utils.Utils.txt;

public class StealthPlating extends BaseHullMod {

    static final float SENSOR_MULT = 0.5f;

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 1) return "" + Math.round((1f-SENSOR_MULT)*100f) + txt("%");
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSensorProfile().modifyMult(id,SENSOR_MULT);
    }
}
