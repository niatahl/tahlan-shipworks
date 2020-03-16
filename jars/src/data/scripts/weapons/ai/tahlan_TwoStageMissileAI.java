package data.scripts.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;

import java.awt.Color;
import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

// Used for two stage missiles that perform combat entity substitution for the second stage
public class tahlan_TwoStageMissileAI extends tahlan_BaseMissile {
    private static final float LOCK_ON_MAX_TIME = 3f; // If you want a timer to force it to enter the second stage before target acquired
    private static final float AIM_THRESHOLD = 0.5f; // Don't change this, trust me
    private static final float TRUE_BURN_DELAY_MAX = 0.4f; // Max time until splitting after target acquired, accelerates while counting down
    private static final float TRUE_BURN_DELAY_MIN = 0.3f; // Min time until splitting after target acquired, these help account for the acceleration curve
    private static final float ENGINE_DEAD_TIME_MAX = 0.8f;  // Max time until engine burn starts
    private static final float ENGINE_DEAD_TIME_MIN = 0.1f; // Min time until engine burn starts
    private static final float LEAD_GUIDANCE_FACTOR = 0f;           // Lags behind real intercept point
    private static final float LEAD_GUIDANCE_FACTOR_FROM_ECCM = 0.0f; // How much ECCM reduces the lag, do not go above 1f total, this is a + operation
    private static final float FIRE_INACCURACY = 3f; // Randomized cone arc leading offset
    private static final float FLARE_OFFSET = -9f; // Set to engine location matched to missile projectile file
    private static final Color FLARE_COLOR = new Color(86, 200, 184, 255);
    private static final Color SMOKE_COLOR = new Color(207, 220, 210, 157);
    private static final boolean STAGE_ONE_EXPLODE = false;
    private static final boolean STAGE_ONE_FLARE = true; // Glow particle visual when second stage is litup
    private static final boolean STAGE_ONE_TRANSFER_DAMAGE = true;
    private static final boolean STAGE_ONE_TRANSFER_MOMENTUM = true;
    private static final String STAGE_TWO_WEAPON_ID = "tahlan_dolch_dummy";
    private static final String START_FLY_SOUND_ID = "tahlan_dolch_burn";
    private static final float VELOCITY_DAMPING_FACTOR = 0.15f; // Don't change this, trust me
    private static final Vector2f ZERO = new Vector2f();
    private float lifetimer;
    private float trueBurnDelayTimer;
    private float engineDeadTimer;
    private final float inaccuracy;
    private boolean lockedOn = false;
    private boolean readyToFly = false;
    private boolean hasPlayedSound = false;
    protected final float eccmMult;

    public tahlan_TwoStageMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);

        this.lifetimer = LOCK_ON_MAX_TIME - (float) Math.random() * 0.25f;
        trueBurnDelayTimer = MathUtils.getRandomNumberInRange(TRUE_BURN_DELAY_MIN, TRUE_BURN_DELAY_MAX);
        engineDeadTimer = MathUtils.getRandomNumberInRange(ENGINE_DEAD_TIME_MIN, ENGINE_DEAD_TIME_MAX);

        eccmMult = 0.5f; // How much ECCM reduces FIRE_INACCURACY

        inaccuracy = MathUtils.getRandomNumberInRange(-FIRE_INACCURACY, FIRE_INACCURACY);
    }

    public float getInaccuracyAfterECCM() {
        float eccmEffectMult = 1;
        if (launchingShip != null) {
            eccmEffectMult = 1 - eccmMult * launchingShip.getMutableStats().getMissileGuidance().getModifiedValue();
        }
        if (eccmEffectMult < 0) {
            eccmEffectMult = 0;
        }

        return inaccuracy * eccmEffectMult;
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        if (missile.isFading() || missile.isFizzling()) {
            return;
        }

        //float distance = MathUtils.getDistance(target.getLocation(), missile.getLocation());
  /*      float guidance = LEAD_GUIDANCE_FACTOR;
        if (missile.getSource() != null)
        {
            guidance += Math.min(missile.getSource().getMutableStats().getMissileGuidance().getModifiedValue()
                    - missile.getSource().getMutableStats().getMissileGuidance().getBaseValue(), 1f) * LEAD_GUIDANCE_FACTOR_FROM_ECCM;
        }
*/

        lifetimer -= amount;
        if (lifetimer <= 0f)
        // "Boost" (create new missile)... When the lock on max timer expires edition
        {
            MissileAPI newMissile;
            if (STAGE_ONE_TRANSFER_MOMENTUM) {
                newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(launchingShip,
                        missile.getWeapon(), STAGE_TWO_WEAPON_ID,
                        missile.getLocation(), missile.getFacing(), missile.getVelocity());
                newMissile.setAngularVelocity(missile.getAngularVelocity());
            } else {
                newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(launchingShip,
                        missile.getWeapon(), STAGE_TWO_WEAPON_ID,
                        missile.getLocation(), missile.getFacing(), ZERO);
            }

            newMissile.setFromMissile(true);

            // Transfer any damage the missile has incurred if so desired
            if (STAGE_ONE_TRANSFER_DAMAGE) {
                newMissile.setEmpResistance(missile.getEmpResistance());
                float damageToDeal = missile.getMaxHitpoints() - missile.getHitpoints();
                if (damageToDeal > 0f) {
                    Global.getCombatEngine().applyDamage(newMissile, missile.getLocation(), damageToDeal,
                            DamageType.FRAGMENTATION, 0f, true, false, missile.getSource());
                }
            }

            // GFX on the spot of the switcheroo if desired
            // Remove old missile
            if (STAGE_ONE_EXPLODE) {
                Global.getCombatEngine().addSmokeParticle(missile.getLocation(), missile.getVelocity(), 60f, 0.75f, 0.75f, SMOKE_COLOR);
                Global.getCombatEngine().applyDamage(missile, missile.getLocation(), missile.getHitpoints() * 100f,
                        DamageType.FRAGMENTATION, 0f, false, false, missile);
            } else if (STAGE_ONE_FLARE) {
                Vector2f offset = new Vector2f(FLARE_OFFSET, 0f);
                VectorUtils.rotate(offset, missile.getFacing(), offset);
                Vector2f.add(offset, missile.getLocation(), offset);
                Global.getCombatEngine().addHitParticle(offset, missile.getVelocity(), 100f, 0.5f, 0.25f, FLARE_COLOR);
                Global.getCombatEngine().removeEntity(missile);
            } else {
                Global.getCombatEngine().removeEntity(missile);
            }

            if (!hasPlayedSound) {
                hasPlayedSound = true;
                Global.getSoundPlayer().playSound(START_FLY_SOUND_ID, 1f, 1f, missile.getLocation(), ZERO);
            }
        }

        // Do not fly until we have both aimed correctly and finished engineDeadTimer
        if (!lockedOn || !readyToFly) {
            if (engineDeadTimer > 0f) {
                engineDeadTimer -= amount;
                if (engineDeadTimer <= 0f) {
                    readyToFly = true;
                }
            }

            // If we have a valid target, turn to face desired intercept point
            if (acquireTarget(amount)) {
                Vector2f guidedTarget = interceptAdvanced(missile.getLocation(), 0f,
                        300 * launchingShip.getMutableStats().getMissileAccelerationBonus().getMult(),
                        900 * launchingShip.getMutableStats().getMissileMaxSpeedBonus().getMult(), target.getLocation(), target.getVelocity());
                if (guidedTarget == null) {
                    float distance = MathUtils.getDistance(target.getLocation(), missile.getLocation());
                    Vector2f projection = new Vector2f(target.getVelocity());
                    float scalar = distance / (missile.getVelocity().length() + 1f);
                    projection.scale(scalar);
                    guidedTarget = Vector2f.add(target.getLocation(), projection, null);
                }
                Vector2f.sub(guidedTarget, target.getLocation(), guidedTarget);
                guidedTarget.scale(0.25f);
                Vector2f.add(guidedTarget, target.getLocation(), guidedTarget);

                float angularDistance = MathUtils.getShortestRotation(missile.getFacing(),
                        MathUtils.clampAngle(VectorUtils.getAngle(missile.getLocation(), guidedTarget) + getInaccuracyAfterECCM()));
                float absDAng = Math.abs(angularDistance);

                missile.giveCommand(angularDistance < 0 ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);

                if (absDAng < Math.abs(missile.getAngularVelocity()) * VELOCITY_DAMPING_FACTOR) {
                    missile.setAngularVelocity(angularDistance / VELOCITY_DAMPING_FACTOR);
                }

                if (absDAng <= AIM_THRESHOLD) {
                    lockedOn = true;
                }
            }
        }

        // Pointed at desired intercept point, waiting for boost
        // Accelerate missile in the meantime, this is basically an aim fudger to account for the acceleration curve!
        else if (trueBurnDelayTimer > 0f) {
            trueBurnDelayTimer -= amount;
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        // "Boost" (create new missile)
        else {
            MissileAPI newMissile;
            if (STAGE_ONE_TRANSFER_MOMENTUM) {
                newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(launchingShip,
                        missile.getWeapon(), STAGE_TWO_WEAPON_ID,
                        missile.getLocation(), missile.getFacing(), missile.getVelocity());
                newMissile.setAngularVelocity(missile.getAngularVelocity());
            } else {
                newMissile = (MissileAPI) Global.getCombatEngine().spawnProjectile(launchingShip,
                        missile.getWeapon(), STAGE_TWO_WEAPON_ID,
                        missile.getLocation(), missile.getFacing(), ZERO);
            }

            newMissile.setFromMissile(true);

            // Transfer any damage the missile has incurred if so desired
            if (STAGE_ONE_TRANSFER_DAMAGE) {
                newMissile.setEmpResistance(missile.getEmpResistance());
                float damageToDeal = missile.getMaxHitpoints() - missile.getHitpoints();
                if (damageToDeal > 0f) {
                    Global.getCombatEngine().applyDamage(newMissile, missile.getLocation(), damageToDeal,
                            DamageType.FRAGMENTATION, 0f, true, false, missile.getSource());
                }
            }

            // GFX on the spot of the switcheroo if desired
            // Remove old missile
            if (STAGE_ONE_EXPLODE) {
                Global.getCombatEngine().addSmokeParticle(missile.getLocation(), missile.getVelocity(), 60f, 0.75f, 0.75f, SMOKE_COLOR);
                Global.getCombatEngine().applyDamage(missile, missile.getLocation(), missile.getHitpoints() * 100f,
                        DamageType.FRAGMENTATION, 0f, false, false, missile);
            } else if (STAGE_ONE_FLARE) {
                Vector2f offset = new Vector2f(FLARE_OFFSET, 0f);
                VectorUtils.rotate(offset, missile.getFacing(), offset);
                Vector2f.add(offset, missile.getLocation(), offset);
                Global.getCombatEngine().addHitParticle(offset, missile.getVelocity(), 100f, 0.5f, 0.25f, FLARE_COLOR);
                Global.getCombatEngine().removeEntity(missile);
            } else {
                Global.getCombatEngine().removeEntity(missile);
            }

            if (!hasPlayedSound) {
                hasPlayedSound = true;
                Global.getSoundPlayer().playSound(START_FLY_SOUND_ID, 1f, 1f, missile.getLocation(), ZERO);
            }
        }
    }

    @Override
    protected boolean acquireTarget(float amount) {
        // If our current target is totally invalid, look for a new one
        if (!isTargetValid(target)) {
            if (target instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) target;
                if (ship.isAlive() && ship.isPhased()) {
                    // We were locked onto a ship that has now phased, do not attempt to acquire a new target
                    return false;
                }
            }
            // Look for a target that is not a drone or fighter, if available
            setTarget(findBestTarget(false));
            // No such target, look again except this time we allow drones and fighters
            if (target == null) {
                setTarget(findBestTarget(true));
            }
            if (target == null) {
                return false;
            }
        }

        // If our target is valid but a drone or fighter, see if there's a bigger ship we can aim for instead
        else {
            if (isDroneOrFighter(target)) {
                if (target instanceof ShipAPI) {
                    ShipAPI ship = (ShipAPI) target;
                    if (ship.isAlive() && ship.isPhased()) {
                        // We were locked onto a ship that has now phased, do not attempt to acquire a new target
                        return false;
                    }
                }
                CombatEntityAPI newTarget = findBestTarget();
                if (newTarget != null) {
                    target = newTarget;
                }
            }
        }
        return true;
    }

    @Override
    protected ShipAPI findBestTarget() {
        return findBestTarget(false);
    }

    /**
     * This is some bullshit weighted random picker that favors larger ships
     *
     * @param allowDroneOrFighter True if looking for an alternate target
     *                            (normally it refuses to target fighters or drones)
     * @return
     */
    protected ShipAPI findBestTarget(boolean allowDroneOrFighter) {
        ShipAPI best = null;
        float weight, bestWeight = 0f;
        List<ShipAPI> ships = AIUtils.getEnemiesOnMap(missile);
        int size = ships.size();
        for (int i = 0; i < size; i++) {
            ShipAPI tmp = ships.get(i);
            float mod;
            // This is a valid target if:
            //   It is NOT a (drone or fighter), OR we're in alternate mode
            //   It passes the valid target check
            boolean valid = allowDroneOrFighter || !isDroneOrFighter(target);
            valid = valid && isTargetValid(tmp);
            if (!valid) {
                continue;
            } else {
                switch (tmp.getHullSize()) {
                    default:
                    case FIGHTER:
                        mod = 1f;
                        break;
                    case FRIGATE:
                        mod = 10f;
                        break;
                    case DESTROYER:
                        mod = 50f;
                        break;
                    case CRUISER:
                        mod = 100f;
                        break;
                    case CAPITAL_SHIP:
                        mod = 125f;
                        break;
                }
            }
            weight = (1500f / Math.max(MathUtils.getDistance(tmp, missile.getLocation()), 750f)) * mod;
            if (weight > bestWeight) {
                best = tmp;
                bestWeight = weight;
            }
        }
        return best;
    }

    protected boolean isDroneOrFighter(CombatEntityAPI target) {
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (ship.isFighter() || ship.isDrone()) {
                return true;
            }
        }
        return false;
    }
}
