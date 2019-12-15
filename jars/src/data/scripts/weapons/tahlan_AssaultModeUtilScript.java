//based on the Cassowary's deco weapon, thanks tart
package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class tahlan_AssaultModeUtilScript implements EveryFrameWeaponEffectPlugin {
    
    private ShipAPI ship;
    private ShipSystemAPI system;
    private float shieldArc = 0, time=0;
    private boolean bonus = false, runOnce=false;
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (engine.isPaused()) {return;}
        
        if(!runOnce){
            runOnce=true;
            ship=weapon.getShip();
            system=ship.getSystem();
            shieldArc=ship.getShield().getArc();
        }
        time+=amount;
        
        if(time>=1/30){
            time-=1/30;
            if (system.isActive()) {
                float level = system.getEffectLevel();                
                ship.getShield().setArc(shieldArc*(1f-0.5f*level));
            } else {
                ship.getShield().setArc(shieldArc);
            }
        }
    } 
}