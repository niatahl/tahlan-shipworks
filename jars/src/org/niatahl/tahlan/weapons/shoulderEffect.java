package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;

/**
 *
 * @author Tartiflette
 */
public class shoulderEffect implements EveryFrameWeaponEffectPlugin{

    private boolean runOnce=false;
    private WeaponAPI reference;
    private ShipAPI ship;
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if(!runOnce){
            runOnce=true;
            ship=weapon.getShip();
            for(WeaponAPI w : weapon.getShip().getAllWeapons()){
                if(w!=weapon && MathUtils.isWithinRange(w.getLocation(),weapon.getLocation(),2)){
                    reference=w;
                }
            }
        }
        
        if (engine.isPaused() || reference==null) {
            return;
        }
        
        weapon.setCurrAngle(ship.getFacing() + MathUtils.getShortestRotation(ship.getFacing(),reference.getCurrAngle())*0.6f);
    }
}