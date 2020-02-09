//By Tartiflette
package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class tahlan_rotaryAnimation implements EveryFrameWeaponEffectPlugin{
  
    private float charge = 0f, spinUp=0.01f, delay = 0.1f, timer = 0f;
    private int frame = 0;
    private boolean runOnce = false;
    private AnimationAPI theAnim;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if (engine.isPaused() || weapon.getSlot().isHidden()) {
            return;
        }
        
        if(!runOnce){
            runOnce=true;
            
            theAnim = weapon.getAnimation();

            if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) {
                spinUp = 0.04f;
            } else if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
                spinUp = 0.05f;
            } else {
                spinUp = 0.06f;
            }
//            if (weapon.getId().contentEquals("chaingun") || weapon.getId().contentEquals("ionpulser")) {
//                spinUp *= 3f;
//            }
        }

        float mult = 1f;
        ShipAPI ship = weapon.getShip();
        if (ship != null) {
            if (weapon.getSpec().getType() == WeaponAPI.WeaponType.BALLISTIC) {
                mult *= ship.getMutableStats().getBallisticRoFMult().getModifiedValue();
            } else if (weapon.getSpec().getType() == WeaponAPI.WeaponType.ENERGY) {
                mult *= ship.getMutableStats().getEnergyRoFMult().getModifiedValue();
            } else if (weapon.getSpec().getType() == WeaponAPI.WeaponType.MISSILE) {
                mult *= ship.getMutableStats().getMissileRoFMult().getModifiedValue();
            }
        }

        float minDelay = 1f / (theAnim.getFrameRate() * mult);
        int maxFrame = theAnim.getNumFrames();
        
        timer += amount;
        if (timer >= delay) {
            timer -= delay;
            if (weapon.getChargeLevel() >= charge && weapon.getChargeLevel()>0) {
                                
                delay = Math.max(delay - spinUp, minDelay);                
                
            } else {
                
                delay = Math.min(delay + (delay * 4f * spinUp), 0.1f);
                
            }
            if (delay != 0.1f) {
                frame++;
                if (frame == maxFrame) {
                    frame = 0;
                }
            }
        }
        theAnim.setFrame(frame);

        charge = weapon.getChargeLevel();
    }
}
