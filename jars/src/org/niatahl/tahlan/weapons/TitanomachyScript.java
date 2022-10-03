package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicInterference;
import data.scripts.util.MagicLensFlare;

import java.awt.*;

public class TitanomachyScript implements EveryFrameWeaponEffectPlugin {

    private IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if(weapon.getShip().getOriginalOwner()<0 && !weapon.getSlot().isBuiltIn()){

            MagicInterference.ApplyInterference(weapon.getShip().getVariant());

        }

    }
}
