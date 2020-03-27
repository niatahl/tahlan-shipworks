package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class tahlan_LostechVentsScript implements EveryFrameWeaponEffectPlugin {

    static final float ARC_CHANCE_PER_SECOND = 0.99f;
    public static final Map<ShipAPI.HullSize, Float> LIGHTNING_RANGE_MAP = new HashMap<ShipAPI.HullSize, Float>();
    static {
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.FRIGATE, 500f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.DESTROYER, 600f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.CRUISER, 800f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 1000f);
    }

    public static final Map<ShipAPI.HullSize, Float> LIGHTNING_DAMAGE = new HashMap<ShipAPI.HullSize, Float>();
    static {
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.FRIGATE, 15f);
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.DESTROYER, 25f);
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.CRUISER, 50f);
        LIGHTNING_DAMAGE.put(ShipAPI.HullSize.CAPITAL_SHIP, 75f);
    }

    public static final Map<ShipAPI.HullSize, Float> LIGHTNING_EMP = new HashMap<ShipAPI.HullSize, Float>();
    static {
        LIGHTNING_EMP.put(ShipAPI.HullSize.FRIGATE, 50f);
        LIGHTNING_EMP.put(ShipAPI.HullSize.DESTROYER, 100f);
        LIGHTNING_EMP.put(ShipAPI.HullSize.CRUISER, 150f);
        LIGHTNING_EMP.put(ShipAPI.HullSize.CAPITAL_SHIP, 200f);
    }

    public static Color LIGHTNING_CORE_COLOR = new Color(255, 246, 249, 222);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(155, 19, 255, 176);

    private IntervalUtil zapInterval = new IntervalUtil(0.4f, 0.6f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();

        //Sets our hullsize-dependant variables
        float actualLightningRange = LIGHTNING_RANGE_MAP.get(ship.getHullSize());
        float actualLightingDamage = LIGHTNING_DAMAGE.get(ship.getHullSize());
        float actualLightningEMP = LIGHTNING_EMP.get(ship.getHullSize());

        //When overloading... well, all hell breaks loose
        if (ship.getFluxTracker().isVenting()) {

            zapInterval.advance(amount);
            //Checks if we should send lightning this frame
            if (zapInterval.intervalElapsed()) {


                    Vector2f sourcePoint = weapon.getLocation();

                    //Then, find all valid targets: we can only shoot missiles, ships and asteroids [including ourselves]
                    List<CombatEntityAPI> validTargets = new ArrayList<CombatEntityAPI>();
                    for (CombatEntityAPI entityToTest : CombatUtils.getEntitiesWithinRange(sourcePoint, actualLightningRange)) {
                        if (entityToTest instanceof ShipAPI || entityToTest instanceof AsteroidAPI || entityToTest instanceof MissileAPI) {
                            //Phased targets, and targets with no collision, are ignored
                            if (entityToTest instanceof ShipAPI) {
                                if (((ShipAPI)entityToTest).isPhased() || entityToTest == ship) {
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
                        validTargets.add(new SimpleEntity(MathUtils.getRandomPointInCircle(sourcePoint,actualLightningRange)));
                    }

                        //And finally, fire at a random valid target
                        CombatEntityAPI target = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));
                        Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, target,
                                DamageType.ENERGY, //Damage type
                                MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightingDamage * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Damage
                                MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightningEMP * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Emp
                                100000f, //Max range
                                "tachyon_lance_emp_impact", //Impact sound
                                14f * actualLightingDamage / 50f, // thickness of the lightning bolt
                                LIGHTNING_CORE_COLOR, //Central color
                                LIGHTNING_FRINGE_COLOR //Fringe Color
                        );


            }
        }
    }
}
