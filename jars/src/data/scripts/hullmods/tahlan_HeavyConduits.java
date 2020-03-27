package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class tahlan_HeavyConduits extends BaseHullMod {

	public static final float FLUX_RESISTANCE = 50f;
	public static final float VENT_RATE_BONUS = 50f;
	public static final float SUPPLIES_INCREASE = 100f;

	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);

    static final float ARC_CHANCE_PER_SECOND = 0.99f;
    public static final Map<HullSize, Float> LIGHTNING_RANGE_MAP = new HashMap<HullSize, Float>();
    static {
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.FRIGATE, 400f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.DESTROYER, 500f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.CRUISER, 600f);
        LIGHTNING_RANGE_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 700f);
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

    public static Color LIGHTNING_CORE_COLOR = new Color(255, 219, 253, 202);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(155, 19, 255, 176);

    private IntervalUtil zapInterval = new IntervalUtil(0.2f, 0.3f);

	static {
		BLOCKED_HULLMODS.add("fluxbreakers");
	}

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Sets our hullsize-dependant variables
        float actualLightningRange = LIGHTNING_RANGE_MAP.get(ship.getHullSize());
        float actualLightingDamage = LIGHTNING_DAMAGE.get(ship.getHullSize());
        float actualLightningEMP = LIGHTNING_EMP.get(ship.getHullSize());

        //When overloading... well, all hell breaks loose
        if (ship.getFluxTracker().isVenting()) {

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
                    Vector2f sourcePoint = vents.get(MathUtils.getRandomNumberInRange(0, vents.size()-1)).computePosition(ship);

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
                            MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightingDamage, //Damage
                            MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightningEMP, //Emp
                            100000f, //Max range
                            "tachyon_lance_emp_impact", //Impact sound
                            15f, // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR //Fringe Color
                    );

                }
            }
        }
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
				ship.getVariant().removeMod(tmp);
			}
		}
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - FLUX_RESISTANCE * 0.01f);
		stats.getVentRateMult().modifyPercent(id, VENT_RATE_BONUS);
        stats.getSuppliesPerMonth().modifyPercent(id, SUPPLIES_INCREASE);
        stats.getCRLossPerSecondPercent().modifyPercent(id, SUPPLIES_INCREASE);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) FLUX_RESISTANCE + "%";
		if (index == 1) return "" + (int) VENT_RATE_BONUS + "%";
		if (index == 2) return "" + (int) SUPPLIES_INCREASE + "%";
		if (index == 4) return "Resistant Flux Conduits";
		return null;
	}


}