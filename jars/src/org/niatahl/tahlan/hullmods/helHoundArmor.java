package org.niatahl.tahlan.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.*;

public class helHoundArmor extends BaseHullMod {
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        boolean active = false;

        if (ship.getParentStation() == null) {
            return;
        }

        if (ship.getParentStation().getPhaseCloak().isActive()) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("tahlan_helHoundArmorDamper",0.33f);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("tahlan_helHoundArmorDamper",0.33f);
            ship.getMutableStats().getEmpDamageTakenMult().modifyMult("tahlan_helHoundArmorDamper",0.33f);
            active = true;
        } else {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("tahlan_helHoundArmorDamper");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("tahlan_helHoundArmorDamper");
            ship.getMutableStats().getEmpDamageTakenMult().unmodify("tahlan_helHoundArmorDamper");
        }

        if (ship.getParentStation().getSystem().isActive()) {
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("tahlan_helHoundArmorCrusher",0.2f);
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult("tahlan_helHoundArmorCrusher",0.2f);
            ship.getMutableStats().getEmpDamageTakenMult().modifyMult("tahlan_helHoundArmorCrusher",0.2f);
            active = true;
        } else {
            ship.getMutableStats().getHullDamageTakenMult().unmodify("tahlan_helHoundArmorCrusher");
            ship.getMutableStats().getArmorDamageTakenMult().unmodify("tahlan_helHoundArmorCrusher");
            ship.getMutableStats().getEmpDamageTakenMult().unmodify("tahlan_helHoundArmorCrusher");
        }

        if (active) {
            ship.setJitter("tahlan_helHoundArmor",new Color(255,140,60,65),1f,2,5);
            ship.setJitterUnder("tahlan_helHoundArmor",new Color(255,80,30,135),1f,25,8);
        }
    }
}
