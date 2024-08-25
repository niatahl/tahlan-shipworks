package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;

import java.util.HashMap;
import java.util.Map;

public class StahlherzSwap extends BaseHullMod {
    private final Map<String, Integer> SWITCH_SYSTEM_TO = new HashMap<>();
    private final Map<Integer, String> SWITCH_SYSTEM = new HashMap<>();
    private final Map<Integer, String> SYSTEM_SWITCH = new HashMap<>();

    {
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz", 1);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_b", 2);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_c", 3);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_d", 4);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_e", 5);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_f", 6);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_g", 7);
        SWITCH_SYSTEM_TO.put("tahlan_stahlherz_h", 0);

        SWITCH_SYSTEM.put(0, "tahlan_stahlherz");
        SWITCH_SYSTEM.put(1, "tahlan_stahlherz_b");
        SWITCH_SYSTEM.put(2, "tahlan_stahlherz_c");
        SWITCH_SYSTEM.put(3, "tahlan_stahlherz_d");
        SWITCH_SYSTEM.put(4, "tahlan_stahlherz_e");
        SWITCH_SYSTEM.put(5, "tahlan_stahlherz_f");
        SWITCH_SYSTEM.put(6, "tahlan_stahlherz_g");
        SWITCH_SYSTEM.put(7, "tahlan_stahlherz_h");

        SYSTEM_SWITCH.put(0, "tahlan_stahlherz_selectorA");
        SYSTEM_SWITCH.put(1, "tahlan_stahlherz_selectorB");
        SYSTEM_SWITCH.put(2, "tahlan_stahlherz_selectorC");
        SYSTEM_SWITCH.put(3, "tahlan_stahlherz_selectorD");
        SYSTEM_SWITCH.put(4, "tahlan_stahlherz_selectorE");
        SYSTEM_SWITCH.put(5, "tahlan_stahlherz_selectorF");
        SYSTEM_SWITCH.put(6, "tahlan_stahlherz_selectorG");
        SYSTEM_SWITCH.put(7, "tahlan_stahlherz_selectorH");
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
                if (SWITCH_SYSTEM.get(switchTo).equals("tahlan_stahlherz_g")) {
                    stats.getVariant().addPermaMod(HullMods.FLUX_SHUNT);
                } else {
                    stats.getVariant().removePermaMod(HullMods.FLUX_SHUNT);
                }
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return (ship.getHullSpec().getHullId().startsWith("tahlan_stahlherz"));
    }
}


