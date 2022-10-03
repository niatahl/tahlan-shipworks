package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

import static data.scripts.TahlanUtils.Utils.txt;

/**
 * Gives bonus damage to a ship's energy weapons after venting
 * @author Nicke535
 */
public class tahlan_VentWeaponBoost extends BaseHullMod {

    private static final float BOOST_DURATION = 5f;
    private static final float BOOST_PER_SECOND = 0.2f;

    private static final Color GLOW_COLOR = new Color(202, 58, 255);

    private boolean runOnce=false;
    private float maxRange=0;
    private final IntervalUtil timer = new IntervalUtil (0.5f,1.5f);

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        if (!runOnce){
            runOnce=true;
            List<WeaponAPI> loadout = ship.getAllWeapons();
            if (loadout!=null){
                for (WeaponAPI w : loadout){
                    if (w.getType()!=WeaponAPI.WeaponType.MISSILE){
                        if (w.getRange()>maxRange){
                            maxRange=w.getRange();
                        }
                    }
                }
            }
            timer.randomize();

        }

        //Nothing should happen if we are paused, or our ship is destroyed
        if (Global.getCombatEngine().isPaused() || !ship.isAlive()) {
            return;
        }

        //Gets the custom data for our specific ship
        ShipSpecificData data = (ShipSpecificData) Global.getCombatEngine().getCustomData().get("SPECIAL_VENT_ASSIST_DATA_KEY"+ship.getId());
        if (data == null) {
            data = new ShipSpecificData();
        }

        //Venting : tick up our time spent venting
        if (ship.getFluxTracker().isVenting()) {
            data.timeSpentVenting += amount;
        }

        //Not venting
        else {
            //Just stopped venting : apply the buff the first time and reset vent time
            if (data.timeSpentVenting > 0f) {
                data.buffDurationRemaining = BOOST_DURATION;
                data.currentBuffAmount = Math.min(data.timeSpentVenting*BOOST_PER_SECOND,2f);
                data.timeSpentVenting = 0f;
            }

            //Apply our buff, if our duration is not yet up
            if (data.buffDurationRemaining > 0f) {
                data.buffDurationRemaining -= amount;
                ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(this.getClass().getName()+ship.getId(), 1f + (data.currentBuffAmount));
                ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(this.getClass().getName()+ship.getId(), 1f + (data.currentBuffAmount));
                //If we are the player ship, also display a tooltip showing our current bonus
                if (ship == Global.getCombatEngine().getPlayerShip() && data.currentBuffAmount > 0f) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(this.getClass().getName() + "_TOOLTIP",
                            "graphics/icons/hullsys/high_energy_focus.png", txt("hmd_ventBoost1"),
                            txt("hmd_ventBoost2") + (int)(data.currentBuffAmount*100f) + txt("%"), false);
                }
                EnumSet<WeaponType> WEAPON_TYPES = EnumSet.of(WeaponType.BALLISTIC,WeaponType.ENERGY);
                ship.setWeaponGlow(0.5f+data.currentBuffAmount, GLOW_COLOR, WEAPON_TYPES);
            }

            //If our duration IS up, remove the bonus
            else {
                ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(this.getClass().getName()+ship.getId());
                ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(this.getClass().getName()+ship.getId());
                EnumSet<WeaponType> WEAPON_TYPES = EnumSet.of(WeaponType.BALLISTIC,WeaponType.ENERGY);
                ship.setWeaponGlow(0f, GLOW_COLOR, WEAPON_TYPES);
            }
        }

        //Finally, write the custom data back to the engine
        Global.getCombatEngine().getCustomData().put("SPECIAL_VENT_ASSIST_DATA_KEY"+ship.getId(), data);


        // Venting AI stuff that I borrowed from Tart
        if (ship.getShipAI() == null) {
            return;
        }
        timer.advance(amount);
        if (timer.intervalElapsed()) {
            if (ship.getFluxTracker().isOverloadedOrVenting()) {
                return;
            }
            MissileAPI closest=AIUtils.getNearestEnemyMissile(ship);
            if (closest!=null && MathUtils.isWithinRange(ship, closest,500)){
                return;
            }

            if ( ship.getFluxTracker().getFluxLevel()<0.5 && AIUtils.getNearbyEnemies(ship, maxRange)!=null) {
                return;
            }

            //venting need

            float ventingNeed;
            switch (ship.getHullSize()) {
                case CAPITAL_SHIP:
                    ventingNeed = 2*(float) Math.pow(ship.getFluxTracker().getFluxLevel(),5f);
                    break;
                case CRUISER:
                    ventingNeed = 1.5f*(float) Math.pow(ship.getFluxTracker().getFluxLevel(),4f);
                    break;
                case DESTROYER:
                    ventingNeed = (float) Math.pow(ship.getFluxTracker().getFluxLevel(),3f);
                    break;
                default:
                    ventingNeed = (float) Math.pow(ship.getFluxTracker().getFluxLevel(),2f);
                    break;
            }

            float hullFactor;
            switch (ship.getHullSize()) {
                case CAPITAL_SHIP:
                    hullFactor=(float) Math.pow(ship.getHullLevel(),0.4f);
                    break;
                case CRUISER:
                    hullFactor=(float) Math.pow(ship.getHullLevel(),0.6f);
                    break;
                case DESTROYER:
                    hullFactor=ship.getHullLevel();
                    break;
                default:
                    hullFactor=(float) Math.pow(ship.getHullLevel(),2f);
                    break;
            }

            //situational danger

            float dangerFactor=0;

            List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, 2000f);
            for (ShipAPI enemy : nearbyEnemies) {
                //reset often with timid or cautious personalities
                FleetSide side = FleetSide.PLAYER;
                if (ship.getOriginalOwner()>0){
                    side = FleetSide.ENEMY;
                }
                if(Global.getCombatEngine().getFleetManager(side).getDeployedFleetMember(ship)!=null){
                    PersonalityAPI personality = (Global.getCombatEngine().getFleetManager(side).getDeployedFleetMember(ship)).getMember().getCaptain().getPersonalityAPI();
                    if(personality.getId().equals("timid") || personality.getId().equals("cautious")){
                        if (enemy.getFluxTracker().isOverloaded() && enemy.getFluxTracker().getOverloadTimeRemaining() > ship.getFluxTracker().getTimeToVent()) {
                            continue;
                        }
                        if (enemy.getFluxTracker().isVenting() && enemy.getFluxTracker().getTimeToVent() > ship.getFluxTracker().getTimeToVent()) {
                            continue;
                        }
                    }
                }

                switch (enemy.getHullSize()) {
                    case CAPITAL_SHIP:
                        dangerFactor+= Math.max(0,3f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    case CRUISER:
                        dangerFactor+= Math.max(0,2.25f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    case DESTROYER:
                        dangerFactor+= Math.max(0,1.5f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    case FRIGATE:
                        dangerFactor+= Math.max(0,1f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                        break;
                    default:
                        dangerFactor+= Math.max(0,0.5f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/640000));
                        break;
                }
            }

            float decisionLevel = (ventingNeed*hullFactor+1)/(dangerFactor+1);

            if (decisionLevel >=1.5f || (ship.getFluxTracker().getFluxLevel()>0.1f && dangerFactor ==0)) {
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
    }


    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    /**
     * Class for managing the data we need to track on a per-ship basis
     */
    private class ShipSpecificData {
        private float timeSpentVenting = 0f;
        private float currentBuffAmount = 0f;
        private float buffDurationRemaining = 0f;
    }
}