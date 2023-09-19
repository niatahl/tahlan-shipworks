//By Nicke535, spawns a chain-lightning at the closest target in the weapon's line of fire
package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lazywizard.lazylib.combat.entities.SimpleEntity
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.modify
import org.niatahl.tahlan.weapons.deco.LostechRangeEffect
import java.awt.Color
import kotlin.math.max

class SilkBoltScript : EveryFrameWeaponEffectPlugin {
    private var damageThisShot = 0f
    private val alreadyDamagedTargets: MutableList<CombatEntityAPI?> = ArrayList()
    private var empFactor = 0f
    private val registeredLightningProjectiles: MutableList<DamagingProjectileAPI> = ArrayList()
    private var fireNextFrame = false
    private val CHARGE_SOUND_ID = "tahlan_sol_dios_loop"
    private var hasFiredThisCharge = false
    private val effectInterval = IntervalUtil(0.1f, 0.2f)
    private var rangeModifier: LostechRangeEffect? = null
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused) {
            return
        }
        if (rangeModifier == null) {
            rangeModifier = LostechRangeEffect()
        }
        rangeModifier!!.advance(amount, engine, weapon)
        val chargelevel = weapon.chargeLevel
        if (hasFiredThisCharge && (chargelevel <= 0f || !weapon.isFiring)) {
            hasFiredThisCharge = false
        }

        //Fire offset location
        val weaponFirePoint = Vector2f(weapon.location.x, weapon.location.y)
        var fireOffset = Vector2f(0f, 0f)
        if (weapon.slot.isTurret) {
            fireOffset.x += weapon.spec.turretFireOffsets[0].x
            fireOffset.y += weapon.spec.turretFireOffsets[0].y
        } else if (weapon.slot.isHardpoint) {
            fireOffset.x += weapon.spec.hardpointFireOffsets[0].x
            fireOffset.y += weapon.spec.hardpointFireOffsets[0].y
        }
        fireOffset = VectorUtils.rotate(fireOffset, weapon.currAngle, Vector2f(0f, 0f))
        weaponFirePoint.x += fireOffset.x
        weaponFirePoint.y += fireOffset.y

        //Chargeup visuals
        if (chargelevel > 0f && !hasFiredThisCharge) {
            Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, weapon, 0.55f + weapon.chargeLevel * 2f, 0.4f + weapon.chargeLevel * 0.4f, weapon.location, Vector2f(0f, 0f))
            effectInterval.advance(engine.elapsedInLastFrame)
            if (effectInterval.intervalElapsed()) {
                val arcPoint = MathUtils.getRandomPointInCone(weaponFirePoint, 75f * chargelevel, weapon.currAngle - 45, weapon.currAngle + 45)
                val arc = engine.spawnEmpArcPierceShields(
                    weapon.ship, weaponFirePoint, weapon.ship,
                    SimpleEntity(arcPoint),
                    DamageType.FRAGMENTATION,
                    0f,
                    0f,
                    75f,
                    null,
                    5f + 5f * chargelevel,
                    LIGHTNING_FRINGE_COLOR,
                    LIGHTNING_CORE_COLOR
                )
            }
        }
        if (chargelevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true
        }

        //Wait one frame if we are changing our projectile this frame, and ensure our spawned projectiles loose their collision after one frame (+reduce projectile speed)
        if (!fireNextFrame) {
            for (proj in CombatUtils.getProjectilesWithinRange(weapon.location, 100f)) {
                if (proj.projectileSpecId == null) {
                    continue
                }
                if (proj.weapon === weapon && !registeredLightningProjectiles.contains(proj)) {
                    proj.collisionClass = CollisionClass.GAS_CLOUD //GAS_CLOUD is essentially NONE, but most people don't ignore that class for targeting
                    registeredLightningProjectiles.add(proj)
                    fireNextFrame = true
                    return
                }
            }
            //Cleanup to avoid memory leak
            val cleanUpList: MutableList<DamagingProjectileAPI> = ArrayList()
            for (proj in registeredLightningProjectiles) {
                if (!engine.isEntityInPlay(proj)) {
                    cleanUpList.add(proj)
                }
            }
            for (proj in cleanUpList) {
                registeredLightningProjectiles.remove(proj)
            }
            return
        }

        //If we actually fire this frame, run the rest of the script
        fireNextFrame = false
        damageThisShot = weapon.damage.damage
        empFactor = weapon.derivedStats.empPerShot / weapon.derivedStats.damagePerShot
        alreadyDamagedTargets.clear()

        //Declare a variable for weapon range and position to fire from, so we have a shorthand
        val range = weapon.range


        //First, we find the closest target in a line
        var firstTarget: CombatEntityAPI? = null
        var iter = 0f
        while (firstTarget == null && iter < 1f) {
            //Gets a point a certain distance away from the weapon
            val pointToLookAt = Vector2f.add(
                weaponFirePoint,
                Vector2f(FastTrig.cos(Math.toRadians(weapon.currAngle.toDouble())).toFloat() * iter * range, FastTrig.sin(Math.toRadians(weapon.currAngle.toDouble())).toFloat() * iter * range),
                Vector2f(0f, 0f)
            )
            val targetList = CombatUtils.getEntitiesWithinRange(pointToLookAt, range * TARGET_FIND_STEP_LENGTH * (1f + iter))
            for (potentialTarget in targetList) {
                //Checks for dissallowed targets, and ignores them
                if (potentialTarget !is ShipAPI && potentialTarget !is MissileAPI) {
                    continue
                } else if (potentialTarget.owner == weapon.ship.owner) {
                    continue
                } else if (MathUtils.getDistance(potentialTarget.location, weaponFirePoint) - potentialTarget.collisionRadius * 0.9f > range) {
                    continue
                }

                //Phased targets, and targets with no collision, are ignored
                if (potentialTarget is ShipAPI) {
                    if (potentialTarget.isPhased) {
                        continue
                    }
                }
                if (potentialTarget.collisionClass == CollisionClass.NONE) {
                    continue
                }

                //If we found any applicable targets, pick the closest one
                if (firstTarget == null) {
                    firstTarget = potentialTarget
                } else if (MathUtils.getDistance(firstTarget, weaponFirePoint) > MathUtils.getDistance(potentialTarget, weaponFirePoint)) {
                    firstTarget = potentialTarget
                }
            }
            iter += TARGET_FIND_STEP_LENGTH
        }

        //If we didn't find a target on the line, the shot was a dud: spawn a decorative EMP arc to the end destination
        if (firstTarget == null) {
            val targetPoint = Vector2f.add(
                weaponFirePoint,
                Vector2f(FastTrig.cos(Math.toRadians(weapon.currAngle.toDouble())).toFloat() * range, FastTrig.sin(Math.toRadians(weapon.currAngle.toDouble())).toFloat() * range),
                Vector2f(0f, 0f)
            )
            Global.getCombatEngine().spawnEmpArc(
                weapon.ship, weaponFirePoint, weapon.ship, SimpleEntity(targetPoint),
                weapon.damageType,  //Damage type
                0f,  //Damage
                0f,  //Emp
                100000f,  //Max range
                "tachyon_lance_emp_impact",  //Impact sound
                MathUtils.getRandomNumberInRange(15f, 20f),  // thickness of the lightning bolt
                LIGHTNING_CORE_COLOR,  //Central color
                LIGHTNING_FRINGE_COLOR //Fringe Color
            )
            return
        }

        //Initializes values for our loop's first iteration
        var currentTarget = firstTarget
        var previousTarget: CombatEntityAPI? = weapon.ship
        var firingPoint: Vector2f? = weaponFirePoint

        //Run a repeating loop to find new targets and deal damage to them in a chain
        while (damageThisShot > 1f) {
            var nextTarget: CombatEntityAPI? = null
            damageThisShot *= 0.5f

            //Stores how much damage we have left after this shot
            var tempStorage = (max((damageThisShot - currentTarget!!.hitpoints).toDouble(), 0.0) + damageThisShot).toFloat()

            //Finds a new target, in case we are going to overkill our current one
            val targetList = CombatUtils.getEntitiesWithinRange(currentTarget.location, range * LIGHTNING_JUMP_RANGE_PERCENTAGE)
            for (potentialTarget in targetList) {
                //Checks for dissallowed targets, and ignores them
                if (potentialTarget !is ShipAPI && potentialTarget !is MissileAPI) {
                    continue
                }
                if (alreadyDamagedTargets.contains(potentialTarget)) {
                    continue
                }

                //If we found any applicable targets, pick the closest one
                if (nextTarget == null) {
                    nextTarget = potentialTarget
                } else if (MathUtils.getDistance(nextTarget, currentTarget) > MathUtils.getDistance(potentialTarget, currentTarget)) {
                    nextTarget = potentialTarget
                }
            }

            //If we didn't find any targets, the lightning stops here
            if (nextTarget == null) {
                tempStorage = 0f
            }

            //Sets our previous target to our current one (before damaging it, that is)
            val tempPreviousTarget = previousTarget
            previousTarget = currentTarget

            //If our target is a missile, *and* our EMP is not higher than 1/3rd of the missile's HP, we don't count as an EMP arc hit; increase the EMP resistance by one before firing the arc
            if (currentTarget is MissileAPI) {
                if (damageThisShot * empFactor < currentTarget.getHitpoints() / 3f) {
                    currentTarget.empResistance = currentTarget.empResistance + 1
                }
            }

            //Actually spawn the lightning arc
            Global.getCombatEngine().spawnEmpArc(
                weapon.ship, firingPoint, tempPreviousTarget, currentTarget,
                weapon.damageType,  //Damage type
                damageThisShot,  //Damage
                damageThisShot * empFactor,  //Emp
                100000f,  //Max range
                "tachyon_lance_emp_impact",  //Impact sound
                10f + 10f * (damageThisShot / weapon.damage.damage),  // thickness of the lightning bolt
                LIGHTNING_CORE_COLOR,  //Central color
                LIGHTNING_FRINGE_COLOR //Fringe Color
            )

            for (i in 0..2) {
                Global.getCombatEngine().spawnEmpArc(
                    weapon.ship, firingPoint, tempPreviousTarget, currentTarget,
                    weapon.damageType,  //Damage type
                    0f,  //Damage
                    100f,  //Emp
                    100000f,  //Max range
                    "tachyon_lance_emp_impact",  //Impact sound
                    5f + 5f * (damageThisShot / weapon.damage.damage),  // thickness of the lightning bolt
                    LIGHTNING_CORE_COLOR.modify(alpha = 120),  //Central color
                    LIGHTNING_FRINGE_COLOR.modify(alpha = 120) //Fringe Color
                )
            }

            //A second decorative arc
            Global.getCombatEngine().spawnEmpArc(
                weapon.ship, firingPoint, tempPreviousTarget, currentTarget,
                weapon.damageType,  //Damage type
                0f,  //Damage
                0f,  //Emp
                100000f,  //Max range
                "tachyon_lance_emp_impact",  //Impact sound
                10f + 10f * (damageThisShot / weapon.damage.damage),  // thickness of the lightning bolt
                ARC_CORE_COLOR,  //Central color
                ARC_FRINGE_COLOR //Fringe Color
            )

            for (i in 0..2) {
                Global.getCombatEngine().spawnEmpArc(
                    weapon.ship, firingPoint, tempPreviousTarget, currentTarget,
                    weapon.damageType,  //Damage type
                    0f,  //Damage
                    0f,  //Emp
                    100000f,  //Max range
                    "tachyon_lance_emp_impact",  //Impact sound
                    10f + 10f * (damageThisShot / weapon.damage.damage),  // thickness of the lightning bolt
                    ARC_CORE_COLOR.modify(alpha = 120),  //Central color
                    ARC_FRINGE_COLOR.modify(alpha = 120) //Fringe Color
                )
            }

            //Adjusts variables for the next iteration
            firingPoint = previousTarget.location
            damageThisShot = tempStorage
            alreadyDamagedTargets.add(nextTarget)
            currentTarget = nextTarget
        }
    }

    companion object {
        var TARGET_FIND_STEP_LENGTH = 0.05f
        var LIGHTNING_JUMP_RANGE_PERCENTAGE = 0.50f
        var LIGHTNING_CORE_COLOR = Color(239, 255, 212)
        var LIGHTNING_FRINGE_COLOR = Color(126, 195, 0)
        private val ARC_FRINGE_COLOR = Color(185, 52, 255)
        private val ARC_CORE_COLOR = Color(255, 212, 215)
    }
}
