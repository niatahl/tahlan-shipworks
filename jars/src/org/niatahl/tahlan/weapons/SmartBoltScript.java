//By Nicke535, spawns a chain-lightning at the closest target in the weapon's line of fire
package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SmartBoltScript implements EveryFrameWeaponEffectPlugin {

    public static float TARGET_FIND_STEP_LENGTH = 0.05f;
    public static float LIGHTNING_JUMP_RANGE_PERCENTAGE = 0.25f;

    public static Color LIGHTNING_CORE_COLOR = new Color(200, 250, 255);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(14, 133, 195);

    private float damageThisShot = 0f;
    private List<CombatEntityAPI> alreadyDamagedTargets = new ArrayList<CombatEntityAPI>();
    private float empFactor = 0f;

    private List<DamagingProjectileAPI> registeredLightningProjectiles = new ArrayList<DamagingProjectileAPI>();
    private boolean fireNextFrame = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Wait one frame if we are changing our projectile this frame, and ensure our spawned projectiles loose their collision after one frame (+reduce projectile speed)
        if (!fireNextFrame) {
            for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 100f)) {
                if (proj.getProjectileSpecId() == null) { continue; }
                if (proj.getWeapon() == weapon && !registeredLightningProjectiles.contains(proj)) {
                    proj.setCollisionClass(CollisionClass.GAS_CLOUD); //GAS_CLOUD is essentially NONE, but most people don't ignore that class for targeting
                    registeredLightningProjectiles.add(proj);
                    fireNextFrame = true;
                    return;
                }
            }
            //Cleanup to avoid memory leak
            List<DamagingProjectileAPI> cleanUpList = new ArrayList<>();
            for (DamagingProjectileAPI proj : registeredLightningProjectiles) {
                if (!engine.isEntityInPlay(proj)) { cleanUpList.add(proj); }
            }
            for (DamagingProjectileAPI proj : cleanUpList) {
                registeredLightningProjectiles.remove(proj);
            }
            return;
        }

        //If we actually fire this frame, run the rest of the script
        fireNextFrame = false;
        damageThisShot = weapon.getDamage().getDamage();
        empFactor = weapon.getDerivedStats().getEmpPerShot() / weapon.getDerivedStats().getDamagePerShot();
        alreadyDamagedTargets.clear();

        //Declare a variable for weapon range and position to fire from, so we have a shorthand
        float range = weapon.getRange();
        Vector2f weaponFirePoint = new Vector2f(weapon.getLocation().x, weapon.getLocation().y);
        Vector2f fireOffset = new Vector2f(0f, 0f);
        if (weapon.getSlot().isTurret()) {
            fireOffset.x += weapon.getSpec().getTurretFireOffsets().get(0).x;
            fireOffset.y += weapon.getSpec().getTurretFireOffsets().get(0).y;
        } else if (weapon.getSlot().isHardpoint()) {
            fireOffset.x += weapon.getSpec().getHardpointFireOffsets().get(0).x;
            fireOffset.y += weapon.getSpec().getHardpointFireOffsets().get(0).y;
        }
        fireOffset = VectorUtils.rotate(fireOffset, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        weaponFirePoint.x += fireOffset.x;
        weaponFirePoint.y += fireOffset.y;

        //First, we find the closest target in a line
        CombatEntityAPI firstTarget = null;
        float iter = 0f;
        while (firstTarget == null && iter < 1f) {
            //Gets a point a certain distance away from the weapon
            Vector2f pointToLookAt = Vector2f.add(weaponFirePoint, new Vector2f((float)FastTrig.cos(Math.toRadians(weapon.getCurrAngle())) * iter * range, (float)FastTrig.sin(Math.toRadians(weapon.getCurrAngle())) * iter * range), new Vector2f(0f, 0f));

            List<CombatEntityAPI> targetList = CombatUtils.getEntitiesWithinRange(pointToLookAt, range * TARGET_FIND_STEP_LENGTH * (1f + iter));
            for (CombatEntityAPI potentialTarget : targetList) {
                //Checks for dissallowed targets, and ignores them
                if (!(potentialTarget instanceof ShipAPI) && !(potentialTarget instanceof MissileAPI)) {
                    continue;
                } else if (potentialTarget.getOwner() == weapon.getShip().getOwner()) {
                    continue;
                } else if (MathUtils.getDistance(potentialTarget.getLocation(), weaponFirePoint) - (potentialTarget.getCollisionRadius()*0.9f) > range) {
                    continue;
                }

                //Phased targets, and targets with no collision, are ignored
                if (potentialTarget instanceof ShipAPI) {
                    if (((ShipAPI)potentialTarget).isPhased()) {
                        continue;
                    }
                }
                if (potentialTarget.getCollisionClass().equals(CollisionClass.NONE)) {
                    continue;
                }

                //If we found any applicable targets, pick the closest one
                if (firstTarget == null) {
                    firstTarget = potentialTarget;
                } else if (MathUtils.getDistance(firstTarget, weaponFirePoint) > MathUtils.getDistance(potentialTarget, weaponFirePoint)) {
                    firstTarget = potentialTarget;
                }
            }

            iter += TARGET_FIND_STEP_LENGTH;
        }

        //If we didn't find a target on the line, the shot was a dud: spawn a decorative EMP arc to the end destination
        if (firstTarget == null) {
            Vector2f targetPoint = Vector2f.add(weaponFirePoint, new Vector2f((float)FastTrig.cos(Math.toRadians(weapon.getCurrAngle())) * range, (float)FastTrig.sin(Math.toRadians(weapon.getCurrAngle())) * range), new Vector2f(0f, 0f));
            Global.getCombatEngine().spawnEmpArc(weapon.getShip(), weaponFirePoint, weapon.getShip(), new SimpleEntity(targetPoint),
                    weapon.getDamageType(), //Damage type
                    0f, //Damage
                    0f, //Emp
                    100000f, //Max range
                    "tachyon_lance_emp_impact", //Impact sound
                    MathUtils.getRandomNumberInRange(5f, 7f), // thickness of the lightning bolt
                    LIGHTNING_CORE_COLOR, //Central color
                    LIGHTNING_FRINGE_COLOR //Fringe Color
            );
            return;
        }

        //Initializes values for our loop's first iteration
        CombatEntityAPI currentTarget = firstTarget;
        CombatEntityAPI previousTarget = weapon.getShip();
        Vector2f firingPoint = weaponFirePoint;

        //Run a repeating loop to find new targets and deal damage to them in a chain
        while (damageThisShot > 0f) {
            CombatEntityAPI nextTarget = null;

            //Stores how much damage we have left after this shot
            float tempStorage = damageThisShot - currentTarget.getHitpoints();

            //Finds a new target, in case we are going to overkill our current one
            List<CombatEntityAPI> targetList = CombatUtils.getEntitiesWithinRange(currentTarget.getLocation(), range * LIGHTNING_JUMP_RANGE_PERCENTAGE);
            for (CombatEntityAPI potentialTarget : targetList) {
                //Checks for dissallowed targets, and ignores them
                if (!(potentialTarget instanceof ShipAPI) && !(potentialTarget instanceof MissileAPI)) {
                    continue;
                }
                if (potentialTarget.getOwner() == weapon.getShip().getOwner() || alreadyDamagedTargets.contains(potentialTarget)) {
                    continue;
                }

                //If we found any applicable targets, pick the closest one
                if (nextTarget == null) {
                    nextTarget = potentialTarget;
                } else if (MathUtils.getDistance(nextTarget, currentTarget) > MathUtils.getDistance(potentialTarget, currentTarget)) {
                    nextTarget = potentialTarget;
                }
            }

            //If we didn't find any targets, the lightning stops here
            if (nextTarget == null) {
                tempStorage = 0f;
            }

            //Sets our previous target to our current one (before damaging it, that is)
            CombatEntityAPI tempPreviousTarget = previousTarget;
            previousTarget = currentTarget;

            //If our target is a missile, *and* our EMP is not higher than 1/3rd of the missile's HP, we don't count as an EMP arc hit; increase the EMP resistance by one before firing the arc
            if (currentTarget instanceof MissileAPI) {
                if (damageThisShot * empFactor < currentTarget.getHitpoints()/3f) {
                    ((MissileAPI)currentTarget).setEmpResistance(((MissileAPI)currentTarget).getEmpResistance()+1);
                }
            }

            //Actually spawn the lightning arc
            Global.getCombatEngine().spawnEmpArc(weapon.getShip(), firingPoint, tempPreviousTarget, currentTarget,
                    weapon.getDamageType(), //Damage type
                    damageThisShot, //Damage
                    damageThisShot * empFactor, //Emp
                    100000f, //Max range
                    "tachyon_lance_emp_impact", //Impact sound
                    10f * (damageThisShot / weapon.getDamage().getDamage()), // thickness of the lightning bolt
                    LIGHTNING_CORE_COLOR, //Central color
                    LIGHTNING_FRINGE_COLOR //Fringe Color
            );

            //Adjusts variables for the next iteration
            firingPoint = previousTarget.getLocation();
            damageThisShot = tempStorage;
            alreadyDamagedTargets.add(nextTarget);
            currentTarget = nextTarget;
        }
    }
}
