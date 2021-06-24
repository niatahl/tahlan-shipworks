package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class tahlan_VolatileDissipation extends BaseHullMod {

    public static final Map<ShipAPI.HullSize, Float> LIGHTNING_RANGE_MAP = new HashMap<ShipAPI.HullSize, Float>();

    static {
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.FRIGATE, 350f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.DESTROYER, 400f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.CRUISER, 450f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 500f);
    }

    public static final Map<ShipAPI.HullSize, Float> LIGHTNING_DAMAGE = new HashMap<ShipAPI.HullSize, Float>();

    static {
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.FRIGATE, 10f);
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.DESTROYER, 20f);
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.CRUISER, 40f);
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.CAPITAL_SHIP, 80f);
    }

    public static final Map<ShipAPI.HullSize, Float> LIGHTNING_EMP = new HashMap<ShipAPI.HullSize, Float>();

    static {
        LIGHTNING_EMP.put(ShipAPI.HullSize.FRIGATE, 20f);
        LIGHTNING_EMP.put(ShipAPI.HullSize.DESTROYER, 40f);
        LIGHTNING_EMP.put(ShipAPI.HullSize.CRUISER, 80f);
        LIGHTNING_EMP.put(ShipAPI.HullSize.CAPITAL_SHIP, 160f);
    }

    public static Color LIGHTNING_CORE_COLOR = new Color(255, 219, 253, 202);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(155, 19, 255, 176);

    private IntervalUtil zapInterval = new IntervalUtil(0.08f, 0.14f);

    private boolean runOnce=false;
    private float maxRange=0;
    private final IntervalUtil timer = new IntervalUtil (0.5f,1.5f);

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

        //Sets our hullsize-dependant variables
        float actualLightningRange = LIGHTNING_RANGE_MAP.get(ship.getHullSize());
        float actualLightingDamage = LIGHTNING_DAMAGE.get(ship.getHullSize());
        float actualLightningEMP = LIGHTNING_EMP.get(ship.getHullSize());

        float fluxLevel = ship.getHardFluxLevel();

        ship.getMutableStats().getFluxDissipation().modifyFlat("volatileDissipationID", 200f * fluxLevel);

        if (ship.getFluxLevel() > 0.75f) {

            zapInterval.advance(amount);
            //Checks if we should send lightning this frame
            if (zapInterval.intervalElapsed()) {


                //Choose a random vent port to send lightning from
                List<WeaponSlotAPI> vents = new ArrayList<WeaponSlotAPI>();
                for (WeaponSlotAPI weaponSlotAPI : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                    if (weaponSlotAPI.isSystemSlot()) {
                        vents.add(weaponSlotAPI);
                    }
                }

                //If we have no vents, we can't do a dangerous overload this frame; ignore the rest of the code
                if (!vents.isEmpty()) {
                    Vector2f sourcePoint = vents.get(MathUtils.getRandomNumberInRange(0, vents.size() - 1)).computePosition(ship);

                    //Then, find all valid targets: we can only shoot missiles, ships and asteroids [including ourselves]
                    List<CombatEntityAPI> validTargets = new ArrayList<CombatEntityAPI>();
                    for (CombatEntityAPI entityToTest : CombatUtils.getEntitiesWithinRange(sourcePoint, actualLightningRange)) {
                        if (entityToTest instanceof ShipAPI || entityToTest instanceof AsteroidAPI || entityToTest instanceof MissileAPI) {
                            //Phased targets, and targets with no collision, are ignored
                            if (entityToTest instanceof ShipAPI) {
                                if (((ShipAPI) entityToTest).isPhased() || entityToTest == ship) {
                                    continue;
                                }
                            }
                            if (entityToTest.getCollisionClass().equals(CollisionClass.NONE)) {
                                continue;
                            }

                            validTargets.add(entityToTest);
                        }
                    }

                    //If we have no valid targets, zap a random point near us
                    if (validTargets.isEmpty()) {
                        validTargets.add(new SimpleEntity(MathUtils.getRandomPointInCircle(sourcePoint, actualLightningRange)));
                    }

                    //And finally, fire at a random valid target
                    CombatEntityAPI target = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));
                    Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, target,
                            DamageType.ENERGY, //Damage type
                            MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightingDamage * fluxLevel, //Damage
                            MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightningEMP * fluxLevel, //Emp
                            100000f, //Max range
                            "tachyon_lance_emp_impact", //Impact sound
                            15f * fluxLevel, // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR //Fringe Color
                    );
                    validTargets.clear();
                }
                vents.clear();
            }
        }

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

            if ( ship.getFluxTracker().getFluxLevel()<0.5 && !AIUtils.getNearbyEnemies(ship, maxRange).isEmpty()) {
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
}

