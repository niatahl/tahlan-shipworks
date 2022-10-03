package org.niatahl.tahlan.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import static org.niatahl.tahlan.shipsystems.StormDriveStats.SPEED_BOOST;

//Script by Vayra, as is evident by the commentary
public class StormDriveAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    // only check every half-second (for optimization and, hopefully, synchronization)
    private final IntervalUtil timer = new IntervalUtil(0.5f, 0.5f);

    // setup
    private static final float DEGREES = 69f; // (haha nice)
    private static final float SCAN_RANGE = 1000f; // how far ahead of us to scan for (and avoid) targets
    private static final float WOUNDED_DAMAGE_THRESHOLD = 500f; // how much hull damage does a proj have to do before we are scared of it

    // list of flags to check for using TOWARDS target, using AWAY from target, and NOT USING
    private static final ArrayList<AIFlags> TOWARDS = new ArrayList<>();
    private static final ArrayList<AIFlags> AWAY = new ArrayList<>();
    private static final ArrayList<AIFlags> CON = new ArrayList<>();

    static {
        TOWARDS.add(AIFlags.PURSUING);
        TOWARDS.add(AIFlags.HARASS_MOVE_IN);
        AWAY.add(AIFlags.RUN_QUICKLY);
        AWAY.add(AIFlags.TURN_QUICKLY);
        AWAY.add(AIFlags.NEEDS_HELP);
        CON.add(AIFlags.BACK_OFF);
        CON.add(AIFlags.BACK_OFF_MIN_RANGE);
        CON.add(AIFlags.BACKING_OFF);
        CON.add(AIFlags.DO_NOT_PURSUE);
        CON.add(AIFlags.KEEP_SHIELDS_ON);
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
    }

    // method to check if we're facing within X degrees of target
    private boolean rightDirection(ShipAPI ship, Vector2f targetLocation) {
        Vector2f curr = ship.getLocation();
        float angleToTarget = VectorUtils.getAngle(curr, targetLocation);
        return (Math.abs(MathUtils.getShortestRotation(angleToTarget, ship.getFacing())) <= DEGREES);
    }

    private boolean nothingCanStopMe(ShipAPI ship) {

        // setup
        Vector2f curr = ship.getLocation();
        float facing = ship.getFacing();
        boolean hasTakenHullDamage = ship.getHullLevel() < 0.99f;
        boolean safe = true;

        // scan everything within x range
        List<CombatEntityAPI> consider = CombatUtils.getEntitiesWithinRange(curr, SCAN_RANGE);
        for (CombatEntityAPI test : consider) {
            float angle = VectorUtils.getAngle(curr, test.getLocation());

            // ignore everything outside of a y degree cone
            if (MathUtils.getShortestRotation(angle, facing) > DEGREES) {
                continue;
            }

            if (test instanceof DamagingProjectileAPI) {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) test;
                float armor = ship.getArmorGrid().getArmorRating();
                float armorDamage = proj.getDamageAmount();
                float hullDamage = proj.getDamageAmount();
                switch (proj.getDamageType()) {
                    case KINETIC:
                        armorDamage *= 0.5f;
                        break;
                    case FRAGMENTATION:
                        armorDamage *= 0.25f;
                        hullDamage *= 0.25f;
                        break;
                    case HIGH_EXPLOSIVE:
                        armorDamage *= 2f;
                        break;
                }

                if (armorDamage >= armor) {
                    safe = false;
                }
                if (hasTakenHullDamage && hullDamage >= WOUNDED_DAMAGE_THRESHOLD) {
                    safe = false;
                }
                
            } else if (test instanceof ShipAPI) {
                ShipAPI other = (ShipAPI) test;
                HullSize size = ship.getHullSize();
                HullSize otherSize = other.getHullSize();
                
                if (otherSize.compareTo(size) >= 1) {
                    safe = false;
                }
                if (hasTakenHullDamage && otherSize.compareTo(size) >= 0) {
                    safe = false;
                }
                
            }
        }

        return safe;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

        // don't check if paused
        if (engine.isPaused()) {
            return;
        }

        // don't check if timer not up
        timer.advance(amount);
        if (!timer.intervalElapsed()) {
            return;
        }

        // don't use if can't use
        if (!AIUtils.canUseSystemThisFrame(ship)) {
            return;
        }
        
        // don't use if unsafe
        if (!nothingCanStopMe(ship)) {
            return;
        }

        // setup variables
        boolean useMe = false;
        Vector2f targetLocation = null;
        AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);
        float speed = SPEED_BOOST;

        // First priority: use to retreat if ordered to retreat. Overrides/ignores the "useMe" system and AI flag checks.
        if (assignment != null && assignment.getType() == CombatAssignmentType.RETREAT) {
            if (ship.getOwner() == 1 || (ship.getOwner() == 0 && engine.getFleetManager(FleetSide.PLAYER).getGoal() == FleetGoal.ESCAPE)) {
                targetLocation = new Vector2f(ship.getLocation().x, ship.getLocation().y + 800f); // if ship is enemy OR in "escape" type battle, target loc is UP
            } else {
                targetLocation = new Vector2f(ship.getLocation().x, ship.getLocation().y - 800f); // if ship is player's, target loc is DOWN
            }
            if (rightDirection(ship, targetLocation)) {
                ship.useSystem();
            }

            return;  // prevents the AI from activating the ship's system while retreating and facing the wrong direction
            // thanks, Starsector forums user Morathar
        }

        // if we have an assignment, set our target loc to it
        // otherwise, if we have a hostile target, set our target loc to intercept it
        if (assignment != null && assignment.getTarget() != null) {
            targetLocation = assignment.getTarget().getLocation();
        } else if (target != null && target.getOwner() != ship.getOwner()) {
            targetLocation = AIUtils.getBestInterceptPoint(ship.getLocation(), ship.getVelocity().length() + speed, target.getLocation(), target.getVelocity());
        }

        if (targetLocation == null) {
            return;
        } else if (rightDirection(ship, targetLocation)) {
            useMe = true;
        }

        for (AIFlags f : TOWARDS) {
            if (flags.hasFlag(f) && rightDirection(ship, targetLocation)) {
                useMe = true;
            }
        }

        for (AIFlags f : AWAY) {
            if (flags.hasFlag(f) && !rightDirection(ship, targetLocation)) {
                useMe = true;
            }
        }

        for (AIFlags f : CON) {
            if (flags.hasFlag(f)) {
                useMe = false;
            }
        }

        if (useMe) {
            ship.useSystem();
        }

    }
}
