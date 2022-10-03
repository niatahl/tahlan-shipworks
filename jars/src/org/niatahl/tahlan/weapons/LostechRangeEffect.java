// by Nia, written in an effort to stop Alfonzo from breaking the game again

package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;

import java.util.HashMap;
import java.util.Map;

public class LostechRangeEffect implements EveryFrameWeaponEffectPlugin {

    private static final Map<String,Float> RANGE_MODIFIERS = new HashMap<>();
    static {
        RANGE_MODIFIERS.put("tahlan_cashmere",-100f);
        RANGE_MODIFIERS.put("tahlan_silk",-100f);
        RANGE_MODIFIERS.put("tahlan_taffeta",-100f);
        RANGE_MODIFIERS.put("tahlan_velvet",-100f);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip() == null) {
            return;
        }

        if (!weapon.getShip().hasListenerOfClass(hmi_VariableRangeListener.class)) {
            weapon.getShip().addListener(new hmi_VariableRangeListener());
        }
    }

    private static class hmi_VariableRangeListener implements WeaponRangeModifier {

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
            if (!RANGE_MODIFIERS.containsKey(weapon.getSpec().getWeaponId())) {
                return 0f;
            }

            //Stolen from Nicke. Thx buddy
            float percentRangeIncreases = 0f;
            if (weapon.getType() == WeaponAPI.WeaponType.ENERGY) {
                percentRangeIncreases = ship.getMutableStats().getEnergyWeaponRangeBonus().getPercentMod();
            } else if (weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                percentRangeIncreases = ship.getMutableStats().getBallisticWeaponRangeBonus().getPercentMod();
            }
            if (ship.hasListenerOfClass(WeaponRangeModifier.class)) {
                for (WeaponRangeModifier listener : ship.getListeners(WeaponRangeModifier.class)) {
                    //Should not be needed, but good practice: no infinite loops allowed here, no
                    if (listener == this) {
                        continue;
                    }
                    percentRangeIncreases += listener.getWeaponRangePercentMod(ship, weapon);
                }
            }

            if (weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.ENERGY) {
                return RANGE_MODIFIERS.get(weapon.getSpec().getWeaponId()) * (1f + (percentRangeIncreases / 100f));
            } else {
                return 0f;
            }
        }
    }
}


