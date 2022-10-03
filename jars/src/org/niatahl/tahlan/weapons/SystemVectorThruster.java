/*
    By Tartiflette
    Ever so slightly modified by Nia
 */
package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
//import data.scripts.plugins.SpriteRenderManager;

public class SystemVectorThruster implements EveryFrameWeaponEffectPlugin {
    
    private boolean runOnce=false, accel=false, turn=false;
    private ShipAPI SHIP;
    private ShipEngineAPI thruster;
    private ShipEngineControllerAPI EMGINES;
    private float time=0, previousThrust=0;
    
    //Smooth thrusting prevents instant changes in directions and levels of thrust, lower is smoother
    private final float FREQ=0.05f, SMOOTH_THRUSTING=0.25f;        
    private float TURN_RIGHT_ANGLE=0, THRUST_TO_TURN=0, NEUTRAL_ANGLE=0, FRAMES=0, OFFSET=0;
    //sprite size, could be scaled with the engine width to allow variable engine length
    private Vector2f size= new Vector2f(8,74);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if(!runOnce){
            runOnce=true;
            
            SHIP=weapon.getShip();
            EMGINES=SHIP.getEngineController();     
            if(weapon.getAnimation()!=null){
                FRAMES = weapon.getAnimation().getNumFrames();
            }
            
            //find the ship engine associated with the deco thruster
            for(ShipEngineAPI e : SHIP.getEngineController().getShipEngines()){
                if(MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)){
                    thruster=e;
                }                
            }
            
            //desync the engines wobble
            OFFSET=(float)(Math.random()*MathUtils.FPI);
            
            //"rest" angle when not in use
            NEUTRAL_ANGLE=weapon.getSlot().getAngle();
            //ideal aim angle to rotate the ship (allows free-form placement on the hull)
            TURN_RIGHT_ANGLE=MathUtils.clampAngle(VectorUtils.getAngle(SHIP.getLocation(), weapon.getLocation()));
            //is the thruster performant at turning the ship? Engines closer to the center of mass will concentrate more on dealing with changes of velocity.
            THRUST_TO_TURN=smooth(MathUtils.getDistance(SHIP.getLocation(), weapon.getLocation())/SHIP.getCollisionRadius());            
        }

        if(engine.isPaused() || SHIP.getOriginalOwner()==-1){
            return;
        }

        //check for death/engine disabled
        //added check for active system
        if(!SHIP.isAlive() || (thruster!=null && thruster.isDisabled()) || !SHIP.getSystem().isActive()){
            weapon.getAnimation().setFrame(0);
            previousThrust=0;
            return;
        }

        //20FPS
        time+=amount;
        if(time>=FREQ){
            time=0;                
            
            //check what the ship is doing
            float accelerateAngle=NEUTRAL_ANGLE;
            float turnAngle=NEUTRAL_ANGLE;
            float thrust=0;
            
            if(EMGINES.isAccelerating()){
                accelerateAngle=180;
                thrust=1.5f;
                accel=true;
            } else if (EMGINES.isAcceleratingBackwards()){
                accelerateAngle=0;
                thrust=1.5f;
                accel=true;
            } else  if (EMGINES.isDecelerating()){
                accelerateAngle=NEUTRAL_ANGLE;
                thrust=0.5f;
                accel=true;
            } else {                
                accel=false;
            }
            
            if (EMGINES.isStrafingLeft()){
                if(thrust==0){
                    accelerateAngle=-90;
                } else {
                    accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, -90)/2 + accelerateAngle;
                }
                thrust=Math.max(1, thrust);
                accel=true;
            } else if (EMGINES.isStrafingRight()){
                if(thrust==0){
                    accelerateAngle=90;
                } else {
                    accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, 90)/2 + accelerateAngle;
                }
                thrust=Math.max(1, thrust);
                accel=true;
            }
            
            if (EMGINES.isTurningRight()){
                turnAngle=TURN_RIGHT_ANGLE;
                thrust=Math.max(1, thrust);
                turn=true;
            } else if (EMGINES.isTurningLeft()){
                turnAngle=MathUtils.clampAngle(180+TURN_RIGHT_ANGLE);
                thrust=Math.max(1, thrust);
                turn=true;
            } else {
                turn=false;
            }
            
            //calculate the corresponding vector thrusting            
            if(thrust>0){
                
                //DEBUG
                Vector2f offset = new Vector2f(weapon.getLocation().x-SHIP.getLocation().x,weapon.getLocation().y-SHIP.getLocation().y);
                VectorUtils.rotate(offset, -SHIP.getFacing(), offset);
                
                if(!turn){
                    //thrust only, easy.
                    thrust(weapon, accelerateAngle, thrust*(SHIP.getMutableStats().getAcceleration().computeMultMod()), SMOOTH_THRUSTING);                    
                } else {
                    if(!accel){                        
                        //turn only, easy too.
                        thrust(weapon, turnAngle, thrust*(SHIP.getMutableStats().getTurnAcceleration().computeMultMod()), SMOOTH_THRUSTING);                          
                        
                    } else {
                        //combined turn and thrust, aka the funky part.
                        
                        //aim-to-mouse clamp, helps to avoid flickering when the ship is almost facing the cursor and not turning much.
                        float clampedThrustToTurn=THRUST_TO_TURN*Math.min(1, Math.abs(SHIP.getAngularVelocity())/10);
                        clampedThrustToTurn=smooth(clampedThrustToTurn);
                        
                        //start from the neutral angle
                        float combinedAngle=NEUTRAL_ANGLE;
                        
                        //adds both thrust and turn angle at their respective thrust-to-turn ratio. Gives a "middleground" angle
                        combinedAngle = MathUtils.clampAngle(combinedAngle + MathUtils.getShortestRotation(NEUTRAL_ANGLE,accelerateAngle));                        
                        combinedAngle = MathUtils.clampAngle(combinedAngle + clampedThrustToTurn*MathUtils.getShortestRotation(accelerateAngle,turnAngle));  
                        
                        //DEBUG
//                        SpriteRenderManager.objectspaceRender(
//                                Global.getSettings().getSprite("fx","bar"),
//                                SHIP,
//                                offset,
//                                new Vector2f(),
//                                new Vector2f(32,32),
//                                new Vector2f(),
//                                combinedAngle,
//                                0,
//                                true,
//                                Color.BLUE,
//                                true,
//                                0,
//                                0,
//                                0.1f,
//                                false
//                        );
                                                
                        //get the total thrust with mults
                        float combinedThrust=thrust;
                        combinedThrust*=(SHIP.getMutableStats().getTurnAcceleration().computeMultMod() + SHIP.getMutableStats().getAcceleration().computeMultMod())/2;
                        
                        //calculate how much appart the turn and thrust angle are
                        //bellow 90 degrees, the engine is kept at full thrust
                        //if they are further appart, the engine is less useful and it's output get reduced
                        float offAxis = Math.abs(MathUtils.getShortestRotation(turnAngle, accelerateAngle));
                        offAxis=Math.max(0, offAxis-90);
                        offAxis/=45;
                        
                        combinedThrust*= 1 - Math.max(0,Math.min(1,offAxis));
                        
                        //combined thrust is finicky, thus twice smoother                        
                        if(FRAMES==0){
                            //non animated weapons like covers are just oriented
                            rotate(weapon, combinedAngle, combinedThrust, SMOOTH_THRUSTING/2);
                        } else {
                            thrust(weapon, combinedAngle, combinedThrust, SMOOTH_THRUSTING/2);
                        }
                        
                        //DEBUG
//                        engine.addHitParticle(MathUtils.getPoint(weapon.getLocation(), 20, SHIP.getFacing()+accelerateAngle), new Vector2f(), 5, 0.5f, 0.05f, Color.yellow);
//                        engine.addHitParticle(MathUtils.getPoint(weapon.getLocation(), 20, SHIP.getFacing()+turnAngle), new Vector2f(), 5, 0.5f, 0.05f, Color.red);
//                        engine.addHitParticle(MathUtils.getPoint(weapon.getLocation(), 20*combinedThrust, SHIP.getFacing()+combinedAngle), new Vector2f(), 5, 0.5f, 0.05f, Color.green);
//                        engine.addFloatingText(weapon.getLocation(), " "+clampedThrustToTurn, 20, Color.yellow, SHIP, 1, 1);
//                        engine.addFloatingText(weapon.getLocation(), " "+offAxis, 20, Color.yellow, SHIP, 1, 1);
                        //DEBUG
                    }
                }
            //DEBUG
//                SpriteRenderManager.objectspaceRender(
//                        Global.getSettings().getSprite("fx","bar"),
//                        SHIP,
//                        offset,
//                        new Vector2f(),
//                        new Vector2f(16,16),
//                        new Vector2f(),
//                        turnAngle,
//                        0,
//                        true,
//                        Color.red,
//                        true,
//                        0,
//                        0,
//                        0.1f,
//                        false
//                );
//                SpriteRenderManager.objectspaceRender(
//                        Global.getSettings().getSprite("fx","bar"),
//                        SHIP,
//                        offset,
//                        new Vector2f(),
//                        new Vector2f(16,16),
//                        new Vector2f(),
//                        accelerateAngle,
//                        0,
//                        true,
//                        Color.green,
//                        true,
//                        0,
//                        0,
//                        0.1f,
//                        false
//                );
            //DEBUG
                
            } else {
                if(FRAMES==0){          
                    //non animated weapons like covers are just oriented          
                    rotate(weapon, NEUTRAL_ANGLE, 0, SMOOTH_THRUSTING);
                } else {
                    thrust(weapon, NEUTRAL_ANGLE, 0, SMOOTH_THRUSTING);
                }
            }
        }
    }
    
    private void rotate(WeaponAPI weapon, float angle, float thrust, float smooth){
        //target angle
        float aim=angle+SHIP.getFacing();
        
        //how far from the target angle the engine is aimed at
        aim=MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);
        
        //engine wooble
        aim+=5*FastTrig.cos(SHIP.getFullTimeDeployed()*5*thrust+OFFSET);
        aim*=smooth;        
        weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle()+aim));
    }
    
    private void thrust(WeaponAPI weapon, float angle, float thrust, float smooth){
        
        //random sprite
        int frame = (int)(Math.random() * (FRAMES - 1)) + 1;
        if(frame==weapon.getAnimation().getNumFrames()){
            frame=1;
        }
        weapon.getAnimation().setFrame(frame);
        SpriteAPI sprite = weapon.getSprite();
        
        
        //target angle
        float aim=angle+SHIP.getFacing();
        float length=thrust;        
        
        //how far from the target angle the engine is aimed at
        aim=MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);
        
        //thrust is reduced while the engine isn't facing the target angle, then smoothed
        length*=Math.max(0,1-(Math.abs(aim)/90));
        length-=previousThrust;
        length*=smooth;
        length+=previousThrust;
        previousThrust=length;
        
        //engine wooble
        aim+=5*FastTrig.cos(SHIP.getFullTimeDeployed()*5*thrust+OFFSET);
        aim*=smooth;        
        weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle()+aim));
        
        //finally the actual sprite manipulation
        float width=length*size.x/2+size.x/2;
        float height=length*size.y+(float)Math.random()*3+3;        
        sprite.setSize(width, height);
        sprite.setCenter(width/2, height/2);
        
        //clamp the thrust then color stuff
        length=Math.max(0, Math.min(1, length));
        sprite.setColor(new Color(1f, 0.5f+length/2, 0.75f+length/4));
    }
        
    //////////////////////////////////////////
    //                                      //
    //           SMOOTH DAT MOVE            //
    //                                      //
    //////////////////////////////////////////
    
    public float smooth (float x){
        return 0.5f - ((float)(Math.cos(x*MathUtils.FPI) /2 ));
    }  
}