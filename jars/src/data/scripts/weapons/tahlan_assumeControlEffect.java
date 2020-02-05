//Code based on Tartiflette's Diable Avionics code
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class tahlan_assumeControlEffect implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce=false, activated=false, restore=false;
    private ShipAPI theShip, theFighter;
    private ShipSystemAPI theSystem;
    private String id = "tahlan_assumeControl";
    private IntervalUtil timer= new IntervalUtil(0.9f,1.1f);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    
        if(engine.isPaused()) return;
        
        if(!runOnce){
            runOnce=true;
            theShip=weapon.getShip();
            theSystem=theShip.getSystem();
        }
        
        if(theSystem.isStateActive() && theShip.isAlive()){
            timer.advance(amount);
            
            if(!activated){
                activated=true;
                //check if the ship is the player ship
                restore=(engine.getPlayerShip()==theShip
                        &&theShip.getAI()==null
                        );
                timer.setElapsed(0.75f);
                
            }
            
            if(timer.intervalElapsed()){
                if(restore && (engine.getPlayerShip()==theShip || !engine.getPlayerShip().isAlive())){
                    ShipAPI leader=null;
                    if(!theShip.getAllWings().isEmpty()){
                        for(FighterWingAPI w : theShip.getAllWings()){
                            if(w.getLeader()!=null && w.getLeader().isAlive()){
                                leader=w.getLeader();
                                Global.getCombatEngine().setPlayerShipExternal(leader);
                                Global.getCombatEngine().getTimeMult().modifyMult(id,0.75f);
                            }
                        }
                    }
                    //if there are no leader left
                    if(leader==null){                    
                        Global.getCombatEngine().setPlayerShipExternal(theShip);
                        Global.getCombatEngine().getTimeMult().unmodify(id);
                        theSystem.deactivate();
                    } else {
                        theFighter=leader;
                    }
                }
            }
            
            if(theFighter!=null){
                //anti hoover
                ShipAPI closest=AIUtils.getNearestEnemy(theFighter);

                if(closest!=null && MathUtils.getDistanceSquared(closest, theFighter)<=0){
                    Vector2f force=MathUtils.getPointOnCircumference(new Vector2f(), closest.getCollisionRadius(), VectorUtils.getAngle(closest.getLocation(), theFighter.getLocation()));
                    Vector2f dist = new Vector2f();
                    Vector2f.sub(closest.getLocation(), theFighter.getLocation(), dist);
                    Vector2f.add(force, dist, force);
                    force.scale(amount*2);

                    Vector2f vel=theFighter.getVelocity();
                    Vector2f.add(vel, force, vel);
                }
            }
        } else {
            if(restore){
                restore=false;
                activated=false;
                //switch back to the ship
                Global.getCombatEngine().getTimeMult().unmodify(id);
                Global.getCombatEngine().setPlayerShipExternal(theShip);
                theFighter=null;
            }
        }        
    }
}