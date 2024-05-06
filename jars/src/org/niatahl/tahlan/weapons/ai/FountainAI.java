package org.niatahl.tahlan.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.util.MagicTargeting;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class FountainAI implements MissileAIPlugin, GuidedMissileAI{

    //////////////////////
    //     SETTINGS     //
    //////////////////////
    
    private final float DAMPING=0.1f;
    private final int SEARCH_CONE=360, MAX_SCATTER=15;
    private float  PRECISION_RANGE=500;
    
    //Leading loss without ECCM hullmod. The higher, the less accurate the leading calculation will be.
    //   1: perfect leading with and without ECCM
    //   2: half precision without ECCM
    //   3: a third as precise without ECCM. Default
    //   4, 5, 6 etc : 1/4th, 1/5th, 1/6th etc precision.
    private float ECCM=3f;   //A VALUE BELOW 1 WILL PREVENT THE MISSILE FROM EVER HITTING ITS TARGET!
    
    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target=null;
    private boolean launch=true;
    private float timer=0, check=0f, scatter=0, random, correctAngle;

    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.2f);

    public FountainAI(MissileAPI missile, ShipAPI launchingShip){
        
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        
        this.missile = missile;
        if (missile.getSource().getVariant().getHullMods().contains("eccm")){
            ECCM=1;
        }
        
        //calculate the precision range factor
        PRECISION_RANGE=(float)Math.pow((2*PRECISION_RANGE),2);
        random = (float)Math.random()*2 -1;        
    }

    @Override
    public void advance(float amount) {
        
        //skip the AI if the game is paused
        if (engine.isPaused()) {return;}
        
        //fading failsafe
        if(missile.isFizzling() || missile.isFading()){
            engine.applyDamage(missile, missile.getLocation(), missile.getHitpoints()*2, DamageType.FRAGMENTATION, 0, true, false, missile.getSource());
        }

        //to deal with idiots who spawn missiles with nulled weapon reference
        int range = Math.round(missile.getMaxFlightTime() * missile.getMaxSpeed());
        
        //assigning a target if there is none or it got destroyed
        if (target == null
                || (target instanceof ShipAPI && ((ShipAPI)target).isHulk())
                || !engine.isEntityInPlay(target)
                || target.getCollisionClass()==CollisionClass.NONE
                ){     
            if(Math.random()<0.75){
                setTarget(MagicTargeting.pickTarget(missile,
                            MagicTargeting.targetSeeking.NO_RANDOM,
                            range,
                            SEARCH_CONE,
                            1,1,1,1,1,
                            true
                        )
                );
            } else {                
                setTarget(MagicTargeting.pickTarget(missile,
                                MagicTargeting.targetSeeking.LOCAL_RANDOM,
                                range/2,
                                SEARCH_CONE,
                                0,1,2,3,4,
                                true
                        )
                );
            }
            //forced acceleration by default
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        if (!interval.intervalElapsed()) {
            interval.advance(amount);
            return;
        }
        
        timer+=amount;
        //finding lead point to aim to        
        if(launch || timer>=check){
            launch=false;
            timer -=check;
            //set the next check time
            float dist = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation())/PRECISION_RANGE;
            check = Math.min(
                    0.5f,
                    Math.max(
                            0.1f,
                            dist)
            );
            Vector2f lead = target.getLocation();
            scatter = Math.min(1, dist) * ECCM * MAX_SCATTER * random;        
        
            //best velocity vector angle for interception
            correctAngle = VectorUtils.getAngle(
                            missile.getLocation(),
                    lead
                    );

            //scatter
            correctAngle+=scatter;


            ///////////////////////////////////////////////////////////////////////
            //OVERSTEER

            //velocity angle correction
            float offCourseAngle = MathUtils.getShortestRotation(
                    VectorUtils.getFacing(missile.getVelocity()),
                    correctAngle
                    );

            float correction = MathUtils.getShortestRotation(
                    correctAngle,
                    VectorUtils.getFacing(missile.getVelocity())+180
                    )
                    * 0.1f * //oversteer
                    (float)((FastTrig.sin(MathUtils.FPI/90*(Math.min(Math.abs(offCourseAngle),45))))); //damping when the correction isn't important

            //modified optimal facing to correct the velocity vector angle as soon as possible
            correctAngle = correctAngle+correction;
            ///////////////////////////////////////////////////////////////////////

        }
        //target angle for interception        
        float aimAngle = MathUtils.getShortestRotation( missile.getFacing(), correctAngle);
        
        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }  
        
        // Damp angular velocity if the missile aim is getting close to the targeted angle
        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }
        
        //always accelerate
        missile.giveCommand(ShipCommand.ACCELERATE);  
    }

    @Override
    public CombatEntityAPI getTarget()
    {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target)
    {
        this.target = target;
    }
}