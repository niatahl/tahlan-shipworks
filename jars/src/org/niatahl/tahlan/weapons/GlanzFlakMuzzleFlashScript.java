// By Nicke535
// Spawns particles from a weapon in different firing states, as determined by the user.
package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlanzFlakMuzzleFlashScript implements EveryFrameWeaponEffectPlugin {

    /*

        HOW TO USE:
        USED_IDS specifies which IDs to use for the rest of the script; any ID is valid EXCEPT the unique ID "default". Each ID should only be used once on the same weapon
        The script will spawn one particle "system" for each ID in this list, with the specific attributes of that ID.

        All the different Maps<> specify the attributes of each of the particle "systems"; they MUST have something defined as "default", and can have specific fields for specific IDs
        in the USED_IDS list; any field not filled in for a specific ID will revert to "default" instead.

    */
    private static final List<String> USED_IDS = new ArrayList<>();
    static {
        USED_IDS.add("SMOKE_1");
        USED_IDS.add("SMOKE_3");
        USED_IDS.add("SMOKE_5");
        USED_IDS.add("FLASH_CORE_1");
        USED_IDS.add("FLASH_FRINGE_1");
    }

    //The amount of particles spawned immediately when the weapon reaches full charge level
    //  -For projectile weapons, this is when the projectile is actually fired
    //  -For beam weapons, this is when the beam has reached maximum brightness
    private static final Map<String, Integer> ON_SHOT_PARTICLE_COUNT = new HashMap<>();
    static {
        ON_SHOT_PARTICLE_COUNT.put("default", 10);
        ON_SHOT_PARTICLE_COUNT.put("FLASH_FRINGE_1", 1);
        ON_SHOT_PARTICLE_COUNT.put("FLASH_CORE_1", 1);
        ON_SHOT_PARTICLE_COUNT.put("SMOKE_5", 5);
    }

    //How many particles are spawned each second the weapon is firing, on average
    private static final Map<String, Float> PARTICLES_PER_SECOND = new HashMap<>();
    static {
        PARTICLES_PER_SECOND.put("default", 0f);
    }

    //Does the PARTICLES_PER_SECOND field get multiplied by the weapon's current chargeLevel?
    private static final Map<String, Boolean> AFFECTED_BY_CHARGELEVEL = new HashMap<>();
    static {
        AFFECTED_BY_CHARGELEVEL.put("default", false);
    }

    //When are the particles spawned (only used for PARTICLES_PER_SECOND)? Valid values are "CHARGEUP", "FIRING", "CHARGEDOWN", "READY" (not on cooldown or firing) and "COOLDOWN".
    //  Multiple of these values can be combined via "-" inbetween; "CHARGEUP-CHARGEDOWN" is for example valid
    private static final Map<String, String> PARTICLE_SPAWN_MOMENT = new HashMap<>();
    static {
        PARTICLE_SPAWN_MOMENT.put("default", "FIRING");
    }

    //If this is set to true, the particles spawn with regard to *barrel*, not *center*. Only works for ALTERNATING barrel types on weapons: for LINKED barrels you
    //  should instead set up their coordinates manually with PARTICLE_SPAWN_POINT_TURRET and PARTICLE_SPAWN_POINT_HARDPOINT
    private static final Map<String, Boolean> SPAWN_POINT_ANCHOR_ALTERNATION = new HashMap<>();
    static {
        SPAWN_POINT_ANCHOR_ALTERNATION.put("default", true);
    }

    //The position the particles are spawned (or at least where their arc originates when using offsets) compared to their weapon's center [or shot offset, see
    //SPAWN_POINT_ANCHOR_ALTERNATION above], if the weapon is a turret (or HIDDEN)
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_TURRET = new HashMap<>();
    static {
        PARTICLE_SPAWN_POINT_TURRET.put("default", new Vector2f(0f, 0f));
    }

    //The position the particles are spawned (or at least where their arc originates when using offsets) compared to their weapon's center [or shot offset, see
    //SPAWN_POINT_ANCHOR_ALTERNATION above], if the weapon is a hardpoint
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_HARDPOINT = new HashMap<>();
    static {
        PARTICLE_SPAWN_POINT_HARDPOINT.put("default", new Vector2f(0f, 0f));
    }

    //Which kind of particle is spawned (valid values are "SMOOTH", "BRIGHT" and "SMOKE")
    private static final Map<String, String> PARTICLE_TYPE = new HashMap<>();
    static {
        PARTICLE_TYPE.put("default", "SMOKE");
        PARTICLE_TYPE.put("FLASH_FRINGE_1", "BRIGHT");
        PARTICLE_TYPE.put("FLASH_CORE_1", "BRIGHT");
    }

    //What color does the particles have?
    private static final Map<String, Color> PARTICLE_COLOR = new HashMap<>();
    static {
        PARTICLE_COLOR.put("default", new Color(235,245,255, 125));
        PARTICLE_COLOR.put("SMOKE_3", new Color(235,255,245, 105));
        PARTICLE_COLOR.put("FLASH_FRINGE_1", new Color(219, 148, 255));
        PARTICLE_COLOR.put("FLASH_CORE_1", new Color(255, 224, 202));
    }

    //What's the smallest size the particles can have?
    private static final Map<String, Float> PARTICLE_SIZE_MIN = new HashMap<>();
    static {
        PARTICLE_SIZE_MIN.put("default", 6f);
        PARTICLE_SIZE_MIN.put("FLASH_FRINGE_1", 80f);
        PARTICLE_SIZE_MIN.put("FLASH_CORE_1", 40f);
    }

    //What's the largest size the particles can have?
    private static final Map<String, Float> PARTICLE_SIZE_MAX = new HashMap<>();
    static {
        PARTICLE_SIZE_MAX.put("default", 10f);
        PARTICLE_SIZE_MAX.put("FLASH_FRINGE_1", 80f);
        PARTICLE_SIZE_MAX.put("FLASH_CORE_1", 40f);
    }

    //What's the lowest velocity a particle can spawn with (can be negative)?
    private static final Map<String, Float> PARTICLE_VELOCITY_MIN = new HashMap<>();
    static {
        PARTICLE_VELOCITY_MIN.put("default", 10f);
        PARTICLE_VELOCITY_MIN.put("FLASH_FRINGE_1", 0f);
        PARTICLE_VELOCITY_MIN.put("FLASH_CORE_1", 0f);
    }

    //What's the highest velocity a particle can spawn with (can be negative)?
    private static final Map<String, Float> PARTICLE_VELOCITY_MAX = new HashMap<>();
    static {
        PARTICLE_VELOCITY_MAX.put("default", 30f);
        PARTICLE_VELOCITY_MAX.put("FLASH_FRINGE_1", 0f);
        PARTICLE_VELOCITY_MAX.put("FLASH_CORE_1", 0f);
        PARTICLE_VELOCITY_MAX.put("SMOKE_5", 5f);
    }

    //The shortest duration a particle will last before completely fading away
    private static final Map<String, Float> PARTICLE_DURATION_MIN = new HashMap<>();
    static {
        PARTICLE_DURATION_MIN.put("default", 0.5f);
        PARTICLE_DURATION_MIN.put("FLASH_FRINGE_1", 0.2f);
        PARTICLE_DURATION_MIN.put("FLASH_CORE_1", 0.15f);
    }

    //The longest duration a particle will last before completely fading away
    private static final Map<String, Float> PARTICLE_DURATION_MAX = new HashMap<>();
    static {
        PARTICLE_DURATION_MAX.put("default", 1f);
        PARTICLE_DURATION_MAX.put("FLASH_FRINGE_1", 0.3f);
        PARTICLE_DURATION_MAX.put("FLASH_CORE_1", 0.15f);
    }

    //The shortest along their velocity vector any individual particle is allowed to spawn (can be negative to spawn behind their origin point)
    private static final Map<String, Float> PARTICLE_OFFSET_MIN = new HashMap<>();
    static {
        PARTICLE_OFFSET_MIN.put("default", 0f);
        PARTICLE_OFFSET_MIN.put("FLASH_FRINGE_1", 0f);
        PARTICLE_OFFSET_MIN.put("FLASH_CORE_1", 0f);
    }

    //The furthest along their velocity vector any individual particle is allowed to spawn (can be negative to spawn behind their origin point)
    private static final Map<String, Float> PARTICLE_OFFSET_MAX = new HashMap<>();
    static {
        PARTICLE_OFFSET_MAX.put("default", 20f);
        PARTICLE_OFFSET_MAX.put("SMOKE_3", 30f);
        PARTICLE_OFFSET_MAX.put("FLASH_FRINGE_1", 0f);
        PARTICLE_OFFSET_MAX.put("FLASH_CORE_1", 0f);
        PARTICLE_OFFSET_MAX.put("SMOKE_5", 5f);
    }

    //The width of the "arc" the particles spawn in; affects both offset and velocity. 360f = full circle, 0f = straight line
    private static final Map<String, Float> PARTICLE_ARC = new HashMap<>();
    static {
        PARTICLE_ARC.put("default", 10f);
        PARTICLE_ARC.put("SMOKE_3", 5f);
        PARTICLE_ARC.put("FLASH_FRINGE_1", 0f);
        PARTICLE_ARC.put("FLASH_CORE_1", 0f);
        PARTICLE_ARC.put("SMOKE_5", 180f);
    }

    //The offset of the "arc" the particles spawn in, compared to the weapon's forward facing.
    //  For example: 90f = the center of the arc is 90 degrees clockwise around the weapon, 0f = the same arc center as the weapon's facing.
    private static final Map<String, Float> PARTICLE_ARC_FACING = new HashMap<>();
    static {
        PARTICLE_ARC_FACING.put("default", 0f);
    }

    //How far away from the screen's edge the particles are allowed to spawn. Lower values mean better performance, but
    //too low values will cause pop-in of particles. Generally, the longer the particle's lifetime, the higher this
    //value should be
    private static final Map<String, Float> PARTICLE_SCREENSPACE_CULL_DISTANCE = new HashMap<>();
    static {
        PARTICLE_SCREENSPACE_CULL_DISTANCE.put("default", 600f);
    }


    //-----------------------------------------------------------You don't need to touch stuff beyond this point!------------------------------------------------------------


    //These ones are used in-script, so don't touch them!
    private boolean hasFiredThisCharge = false;
    private int currentBarrel = 0;
    private boolean shouldOffsetBarrelExtra = false;

    //Instantiator
    public GlanzFlakMuzzleFlashScript() {}

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run while paused, or without a weapon
        if (weapon == null || amount <= 0f) {return;}

        //Saves handy variables used later
        float chargeLevel = weapon.getChargeLevel();
        String sequenceState = "READY";
        if (chargeLevel > 0 && (!weapon.isBeam() || weapon.isFiring())) {
            if (chargeLevel >= 1f) {
                sequenceState = "FIRING";
            } else if (!hasFiredThisCharge) {
                sequenceState = "CHARGEUP";
            } else {
                sequenceState = "CHARGEDOWN";
            }
        } else if (weapon.getCooldownRemaining() > 0) {
            sequenceState = "COOLDOWN";
        }

        //Adjustment for burst beams, since they are a pain
        if (weapon.isBurstBeam() && sequenceState.contains("CHARGEDOWN")) {
            chargeLevel = Math.max(0f, Math.min(Math.abs(weapon.getCooldownRemaining()-weapon.getCooldown()) / weapon.getSpec().getDerivedStats().getBurstFireDuration(), 1f));
        }

        //The sequenceStates "CHARGEDOWN" and "COOLDOWN" counts its barrel as 1 earlier than usual, due to code limitations
        shouldOffsetBarrelExtra = sequenceState.contains("CHARGEDOWN") || sequenceState.contains("COOLDOWN");

        //We go through each of our particle systems and handle their particle spawning
        for (String ID : USED_IDS) {
            //Screenspace check: simplified but should do the trick 99% of the time
            float screenspaceCullingDistance = PARTICLE_SCREENSPACE_CULL_DISTANCE.get("default");
            if (PARTICLE_SCREENSPACE_CULL_DISTANCE.keySet().contains(ID)) { screenspaceCullingDistance = PARTICLE_SCREENSPACE_CULL_DISTANCE.get(ID); }
            if (!engine.getViewport().isNearViewport(weapon.getLocation(), screenspaceCullingDistance)) {continue;}
            //Store all the values used for this check, and use default values if we don't have specific values for our ID specified
            //Note that particle count, specifically, is not declared here and is only used in more local if-cases
            boolean affectedByChargeLevel = AFFECTED_BY_CHARGELEVEL.get("default");
            if (AFFECTED_BY_CHARGELEVEL.keySet().contains(ID)) { affectedByChargeLevel = AFFECTED_BY_CHARGELEVEL.get(ID); }

            String particleSpawnMoment = PARTICLE_SPAWN_MOMENT.get("default");
            if (PARTICLE_SPAWN_MOMENT.keySet().contains(ID)) { particleSpawnMoment = PARTICLE_SPAWN_MOMENT.get(ID); }

            boolean spawnPointAnchorAlternation = SPAWN_POINT_ANCHOR_ALTERNATION.get("default");
            if (SPAWN_POINT_ANCHOR_ALTERNATION.keySet().contains(ID)) { spawnPointAnchorAlternation = SPAWN_POINT_ANCHOR_ALTERNATION.get(ID); }

            //Here, we only store one value, depending on if we're a hardpoint or not
            Vector2f particleSpawnPoint = PARTICLE_SPAWN_POINT_TURRET.get("default");
            if (weapon.getSlot().isHardpoint()) {
                particleSpawnPoint = PARTICLE_SPAWN_POINT_HARDPOINT.get("default");
                if (PARTICLE_SPAWN_POINT_HARDPOINT.keySet().contains(ID)) { particleSpawnPoint = PARTICLE_SPAWN_POINT_HARDPOINT.get(ID); }
            } else {
                if (PARTICLE_SPAWN_POINT_TURRET.keySet().contains(ID)) { particleSpawnPoint = PARTICLE_SPAWN_POINT_TURRET.get(ID); }
            }

            String particleType = PARTICLE_TYPE.get("default");
            if (PARTICLE_TYPE.keySet().contains(ID)) { particleType = PARTICLE_TYPE.get(ID); }

            Color particleColor = PARTICLE_COLOR.get("default");
            if (PARTICLE_COLOR.keySet().contains(ID)) { particleColor = PARTICLE_COLOR.get(ID); }

            float particleSizeMin = PARTICLE_SIZE_MIN.get("default");
            if (PARTICLE_SIZE_MIN.keySet().contains(ID)) { particleSizeMin = PARTICLE_SIZE_MIN.get(ID); }
            float particleSizeMax = PARTICLE_SIZE_MAX.get("default");
            if (PARTICLE_SIZE_MAX.keySet().contains(ID)) { particleSizeMax = PARTICLE_SIZE_MAX.get(ID); }

            float particleVelocityMin = PARTICLE_VELOCITY_MIN.get("default");
            if (PARTICLE_VELOCITY_MIN.keySet().contains(ID)) { particleVelocityMin = PARTICLE_VELOCITY_MIN.get(ID); }
            float particleVelocityMax = PARTICLE_VELOCITY_MAX.get("default");
            if (PARTICLE_VELOCITY_MAX.keySet().contains(ID)) { particleVelocityMax = PARTICLE_VELOCITY_MAX.get(ID); }

            float particleDurationMin = PARTICLE_DURATION_MIN.get("default");
            if (PARTICLE_DURATION_MIN.keySet().contains(ID)) { particleDurationMin = PARTICLE_DURATION_MIN.get(ID); }
            float particleDurationMax = PARTICLE_DURATION_MAX.get("default");
            if (PARTICLE_DURATION_MAX.keySet().contains(ID)) { particleDurationMax = PARTICLE_DURATION_MAX.get(ID); }

            float particleOffsetMin = PARTICLE_OFFSET_MIN.get("default");
            if (PARTICLE_OFFSET_MIN.keySet().contains(ID)) { particleOffsetMin = PARTICLE_OFFSET_MIN.get(ID); }
            float particleOffsetMax = PARTICLE_OFFSET_MAX.get("default");
            if (PARTICLE_OFFSET_MAX.keySet().contains(ID)) { particleOffsetMax = PARTICLE_OFFSET_MAX.get(ID); }

            float particleArc = PARTICLE_ARC.get("default");
            if (PARTICLE_ARC.keySet().contains(ID)) { particleArc = PARTICLE_ARC.get(ID); }
            float particleArcFacing = PARTICLE_ARC_FACING.get("default");
            if (PARTICLE_ARC_FACING.keySet().contains(ID)) { particleArcFacing = PARTICLE_ARC_FACING.get(ID); }
            //---------------------------------------END OF DECLARATIONS-----------------------------------------

            //First, spawn "on full firing" particles, since those ignore sequence state
            if (chargeLevel >= 1f && !hasFiredThisCharge) {
                //Count spawned particles: only trigger if the spawned particles are more than 0
                float particleCount = ON_SHOT_PARTICLE_COUNT.get("default");
                if (ON_SHOT_PARTICLE_COUNT.keySet().contains(ID)) { particleCount = ON_SHOT_PARTICLE_COUNT.get(ID); }

                if (particleCount > 0) {
                    spawnParticles(engine, weapon, particleCount, particleType, spawnPointAnchorAlternation, particleSpawnPoint, particleColor, particleSizeMin, particleSizeMax, particleVelocityMin, particleVelocityMax,
                            particleDurationMin, particleDurationMax, particleOffsetMin, particleOffsetMax, particleArc, particleArcFacing);
                }
            }

            //Then, we check if we should spawn particles over duration; only spawn if our spawn moment is in the declaration
            if (particleSpawnMoment.contains(sequenceState)) {
                //Get how many particles should be spawned this frame
                float particleCount = PARTICLES_PER_SECOND.get("default");
                if (PARTICLES_PER_SECOND.keySet().contains(ID)) { particleCount = PARTICLES_PER_SECOND.get(ID); }
                particleCount *= amount;
                if (affectedByChargeLevel && (sequenceState.contains("CHARGEUP") || sequenceState.contains("CHARGEDOWN"))) { particleCount *= chargeLevel; }
                if (affectedByChargeLevel && sequenceState.contains("COOLDOWN")) { particleCount *= (weapon.getCooldownRemaining()/weapon.getCooldown()); }

                //Then, if the particle count is greater than 0, we actually spawn the particles
                if (particleCount > 0f) {
                    spawnParticles(engine, weapon, particleCount, particleType, spawnPointAnchorAlternation, particleSpawnPoint, particleColor, particleSizeMin, particleSizeMax,
                            particleVelocityMin, particleVelocityMax, particleDurationMin, particleDurationMax, particleOffsetMin, particleOffsetMax,
                            particleArc, particleArcFacing);
                }
            }
        }

        //If this was our "reached full charge" frame, register that
        if (chargeLevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;
        }

        //Increase our current barrel if we have <= 0 chargeLevel OR have ceased firing for now, if we alternate, and have fired at least once since we last increased it
        //Also make sure the barrels "loop around", and reset our hasFired variable
        if (hasFiredThisCharge && (chargeLevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
            currentBarrel++;

            //We can *technically* have different barrel counts for hardpoints, hiddens and turrets, so take that into account
            int barrelCount = weapon.getSpec().getTurretAngleOffsets().size();
            if (weapon.getSlot().isHardpoint()) {
                barrelCount = weapon.getSpec().getHardpointAngleOffsets().size();
            } else if (weapon.getSlot().isHidden()) {
                barrelCount = weapon.getSpec().getHiddenAngleOffsets().size();
            }

            if (currentBarrel >= barrelCount) {
                currentBarrel = 0;
            }
        }
    }


    //Shorthand function for actually spawning the particles
    private void spawnParticles (CombatEngineAPI engine, WeaponAPI weapon, float count, String type, boolean anchorAlternation, Vector2f spawnPoint, Color color, float sizeMin, float sizeMax,
                                 float velocityMin, float velocityMax, float durationMin, float durationMax,
                                 float offsetMin, float offsetMax, float arc, float arcFacing) {
        //First, ensure we take barrel position into account if we use Anchor Alternation (note that the spawn location is actually rotated 90 degrees wrong, so we invert their x and y values)
        Vector2f trueCenterLocation = new Vector2f(spawnPoint.y, spawnPoint.x);
        float trueArcFacing = arcFacing;
        int trueCurrentBarrel = currentBarrel;
        if (currentBarrel > 0 && shouldOffsetBarrelExtra) { trueCurrentBarrel -= 1; }
        if (anchorAlternation) {
            if (weapon.getSlot().isHardpoint()) {
                if (currentBarrel <= 0 && shouldOffsetBarrelExtra) { trueCurrentBarrel = weapon.getSpec().getHardpointAngleOffsets().size()-1; }
                trueCenterLocation.x += weapon.getSpec().getHardpointFireOffsets().get(currentBarrel).x;
                trueCenterLocation.y += weapon.getSpec().getHardpointFireOffsets().get(currentBarrel).y;
                trueArcFacing += weapon.getSpec().getHardpointAngleOffsets().get(currentBarrel);
            } else if (weapon.getSlot().isTurret()) {
                if (currentBarrel <= 0 && shouldOffsetBarrelExtra) { trueCurrentBarrel = weapon.getSpec().getTurretAngleOffsets().size()-1; }
                trueCenterLocation.x += weapon.getSpec().getTurretFireOffsets().get(currentBarrel).x;
                trueCenterLocation.y += weapon.getSpec().getTurretFireOffsets().get(currentBarrel).y;
                trueArcFacing += weapon.getSpec().getTurretAngleOffsets().get(currentBarrel);
            } else {
                if (currentBarrel <= 0 && shouldOffsetBarrelExtra) { trueCurrentBarrel = weapon.getSpec().getHiddenAngleOffsets().size()-1; }
                trueCenterLocation.x += weapon.getSpec().getHiddenFireOffsets().get(currentBarrel).x;
                trueCenterLocation.y += weapon.getSpec().getHiddenFireOffsets().get(currentBarrel).y;
                trueArcFacing += weapon.getSpec().getHiddenAngleOffsets().get(currentBarrel);
            }
        }

        //Then, we offset the true position and facing with our weapon's position and facing, while also rotating the position depending on facing
        trueArcFacing += weapon.getCurrAngle();
        trueCenterLocation = VectorUtils.rotate(trueCenterLocation, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        trueCenterLocation.x += weapon.getLocation().x;
        trueCenterLocation.y += weapon.getLocation().y;

        //Then, we can finally start spawning particles
        float counter = count;
        while (Math.random() < counter) {
            //Ticks down the counter
            counter--;

            //Gets a velocity for the particle
            float arcPoint = MathUtils.getRandomNumberInRange(trueArcFacing-(arc/2f), trueArcFacing+(arc/2f));
            Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(velocityMin, velocityMax),
                    arcPoint);

            //Gets a spawn location in the cone, depending on our offsetMin/Max
            Vector2f spawnLocation = MathUtils.getPointOnCircumference(trueCenterLocation, MathUtils.getRandomNumberInRange(offsetMin, offsetMax),
                    arcPoint);

            //Gets our duration
            float duration = MathUtils.getRandomNumberInRange(durationMin, durationMax);

            //Gets our size
            float size = MathUtils.getRandomNumberInRange(sizeMin, sizeMax);

            //Finally, determine type of particle to actually spawn and spawns it
            switch (type) {
                case "SMOOTH":
                    engine.addSmoothParticle(spawnLocation, velocity, size, 1f, duration, color);
                    break;
                case "SMOKE":
                    engine.addSmokeParticle(spawnLocation, velocity, size, 1f, duration, color);
                    break;
                default:
                    engine.addHitParticle(spawnLocation, velocity, size, 10f, duration, color);
                    break;
            }
        }
    }
}