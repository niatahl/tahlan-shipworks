package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class NibelungDecoScript implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        switch (weapon.getShip().getHullSpec().getHullId()) {
            case "tahlan_Nibelung":
                weapon.getAnimation().setFrame(0);
                break;
            case "tahlan_Nibelung_n":
                weapon.getAnimation().setFrame(1);
                break;
            case "tahlan_Nibelung_crg":
                weapon.getAnimation().setFrame(2);
                break;
            case "tahlan_Nibelung_rg":
                weapon.getAnimation().setFrame(3);
                break;
        }
    }
}
