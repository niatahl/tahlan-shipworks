package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.HashSet;
import java.util.Set;

import static org.niatahl.tahlan.utils.Utils.txt;

public class ParallelTargetingUnit extends BaseHullMod {

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(2);

    static final float RANGE_BOOST = 200f;
    static final float PD_MINUS = 100f;

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(PTUListener.class)) {
            ship.addListener(new PTUListener());
        }
        if (ship.getVariant().hasHullMod("ballistic_rangefinder")) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),"ballistic_rangefinder","tahlan_paralleltargeting");
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + Math.round(RANGE_BOOST);
        if (index == 1) return "" + Math.round(RANGE_BOOST-PD_MINUS);
        if (index == 2) return txt("rangefinder");
        return null;
    }

    // Our range listener
    private static class PTUListener implements WeaponBaseRangeModifier {

        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }

        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.getSize() != WeaponAPI.WeaponSize.SMALL || weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                return 0f;
            }

            float baseRangeMod = RANGE_BOOST;
            if (weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)) {
                baseRangeMod -= PD_MINUS;
            }

            return baseRangeMod;
        }


    }
}
