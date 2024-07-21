package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import java.util.HashMap;
import java.util.Map;

public class GlanzwurfSwap extends BaseHullMod {
    private final Map<String, Integer> SWITCH_SYSTEM_TO = new HashMap<>();
    private final Map<Integer, String> SWITCH_SYSTEM = new HashMap<>();
    private final Map<Integer, String> SYSTEM_SWITCH = new HashMap<>();

    {
        SWITCH_SYSTEM_TO.put("tahlan_glanzwurf", 1);
        SWITCH_SYSTEM_TO.put("tahlan_glanzwurf_b", 0);

        SWITCH_SYSTEM.put(0, "tahlan_glanzwurf");
        SWITCH_SYSTEM.put(1, "tahlan_glanzwurf_b");

        SYSTEM_SWITCH.put(0, "tahlan_glanzwurf_selectorA");
        SYSTEM_SWITCH.put(1, "tahlan_glanzwurf_selectorB");
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        boolean switchSystem = true;
        for (String hullmod : SWITCH_SYSTEM_TO.keySet()) {
            String selectorHullmod = SYSTEM_SWITCH.get(SWITCH_SYSTEM_TO.get(hullmod));
            if (stats.getVariant().getHullMods().contains(selectorHullmod)) {
                switchSystem = false;
                break;
            }
        }

        if (switchSystem && stats.getEntity() != null && ((ShipAPI) stats.getEntity()).getHullSpec() != null) {
            String hullId = ((ShipAPI) stats.getEntity()).getHullSpec().getHullId();
            if (SWITCH_SYSTEM_TO.containsKey(hullId)) {
                int switchTo = SWITCH_SYSTEM_TO.get(hullId);
                ShipHullSpecAPI ship = Global.getSettings().getHullSpec(SWITCH_SYSTEM.get(switchTo));
                ((ShipAPI) stats.getEntity()).getVariant().setHullSpecAPI(ship);
                stats.getVariant().setHullSpecAPI(ship);
                stats.getVariant().addMod(SYSTEM_SWITCH.get(switchTo));
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return (ship.getHullSpec().getHullId().startsWith("tahlan_glanzwurf"));
    }
}


