package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;

import java.util.HashSet;
import java.util.Set;

public class tahlan_ParallelTargetingUnit extends BaseHullMod {

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(2);

    static final float RANGE_BOOST = 200f;
    static final float PD_MINUS = 100f;

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(tahlan_PTUListener.class)) {
            ship.addListener(new tahlan_PTUListener());
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + Math.round(RANGE_BOOST);
        if (index == 1) return "" + Math.round(RANGE_BOOST-PD_MINUS);
        return null;
    }

    // Our range listener
    private static class tahlan_PTUListener implements WeaponRangeModifier {

        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }

        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.getSize() != WeaponAPI.WeaponSize.SMALL || weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                return 0f;
            }

            //Stolen from Nicke. Thx buddy
            float percentRangeIncreases = ship.getMutableStats().getEnergyWeaponRangeBonus().getPercentMod();
            if (ship.hasListenerOfClass(WeaponRangeModifier.class)) {
                for (WeaponRangeModifier listener : ship.getListeners(WeaponRangeModifier.class)) {
                    //Should not be needed, but good practice: no infinite loops allowed here, no
                    if (listener == this) {
                        continue;
                    }
                    percentRangeIncreases += listener.getWeaponRangePercentMod(ship, weapon);
                }
            }

            float baseRangeMod = RANGE_BOOST;
            if (weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)) {
                baseRangeMod -= PD_MINUS;
            }

            return baseRangeMod * (1f + (percentRangeIncreases/100f));
        }
    }
}
