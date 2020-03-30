package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
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

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
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

                }
            }
        }
    }
}

