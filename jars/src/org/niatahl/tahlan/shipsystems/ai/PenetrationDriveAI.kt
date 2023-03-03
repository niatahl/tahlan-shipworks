package org.niatahl.tahlan.shipsystems.ai

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.shipsystems.PenetrationDriveStats
import java.awt.Color

//Base made by Vayra, modified by PureTilt, converted to Kotlin by IntelliJ (cause Nia is a silly person)
class PenetrationDriveAI : ShipSystemAIScript {
    private val flickTimer = IntervalUtil(0.2f, 0.4f)
    private val timer = IntervalUtil(0.75f, 0.75f)
    private val turnMult = HashMap<HullSize, Float>()
    var minPointsToFlank = 0f
    var neededDur = 0f
    var timeElapsed = 0f
    private var ship: ShipAPI? = null
    private var flags: ShipwideAIFlags? = null
    private var engine: CombatEngineAPI? = null
    private var doIFlick = false
    private var targetShip = false

    init {
        turnMult[HullSize.FRIGATE] = 0.1f
        turnMult[HullSize.DESTROYER] = 0.25f
        turnMult[HullSize.CRUISER] = 0.5f
        turnMult[HullSize.CAPITAL_SHIP] = 1f
    }

    override fun init(ship: ShipAPI, system: ShipSystemAPI, flags: ShipwideAIFlags, engine: CombatEngineAPI) {
        this.ship = ship
        this.flags = flags
        this.engine = engine
    }

    // method to check if we're facing within X degrees of target
    private fun rightDirection(ship: ShipAPI?, targetLocation: Vector2f): Boolean {
        val curr = ship!!.location
        val angleToTarget = VectorUtils.getAngle(curr, targetLocation)
        //spawnText(MathUtils.getShortestRotation(angleToTarget, ship.getFacing()) + "", 50f);
        return Math.abs(MathUtils.getShortestRotation(angleToTarget, ship.facing)) <= DEGREES
    }

    private fun flankingScore(ship: ShipAPI?, target: ShipAPI?): Float {
        var flankingScore = 10f
        if (target == null || ship == null) return -100f
        if (target.isCapital && !rightDirection(target, ship.location)) return -100f
        if (target.isStation || target.isFighter) return -100f
        if (target.isHulk) return (-100).toFloat()
        if (target.hullLevel < 0.15f) return (-100).toFloat()
        if (target.fleetMember == null) return (-100).toFloat()
        val shipSide = ship.owner.toFloat()
        val targetSide = target.owner.toFloat()

        //how fast we rotate
        val timeToMaxSpeedYou = ship.maxTurnRate / ship.turnAcceleration
        val timeToTurn180You = 180 - ship.maxTurnRate * timeToMaxSpeedYou * 0.5f / ship.maxTurnRate + timeToMaxSpeedYou
        val timeToMaxSpeedTarget = target.maxTurnRate / target.turnAcceleration
        val timeToTurn180Target = 180 - target.maxTurnRate * timeToMaxSpeedTarget * 0.5f / target.maxTurnRate + timeToMaxSpeedTarget
        //Turn advantage add to score
        flankingScore += (timeToTurn180Target - timeToTurn180You) * turnMult[target.hullSize]!!
        val shipPos = ship.location
        val targetPos = target.location
        val moveDir = VectorUtils.getDirectionalVector(shipPos, targetPos)
        val distPastTarget = (ship.collisionRadius + target.collisionRadius) * 0.75f
        val exitPos = Vector2f(targetPos.x + distPastTarget * moveDir.x, targetPos.y + distPastTarget * moveDir.y)
        val checkPos = Vector2f(exitPos.x + 400 * moveDir.x, exitPos.y + 400 * moveDir.y)

        //spawnText("there", CheckPos);
        var enemyScore = 0f
        var allyScore = 0f
        val shipInExitRange = CombatUtils.getShipsWithinRange(checkPos, distPastTarget + 1500)
        for (toCheck in shipInExitRange) {
            if (toCheck.fleetMember == null) continue
            if (toCheck.owner.toFloat() == shipSide) {
                if (toCheck !== ship) {
                    allyScore += toCheck.fleetMember.deploymentPointsCost
                }
            } else if (toCheck.owner.toFloat() == targetSide) {
                enemyScore += toCheck.fleetMember.deploymentPointsCost
            }
        }
        if (ship.fleetMember.deploymentPointsCost > 0) {
            allyScore += ship.fleetMember.deploymentPointsCost
        }
        val totalScore = allyScore - enemyScore
        flankingScore += totalScore

        //how much of target's HP left
        val targetDmgMult = 400 / (target.armorGrid.armorRating + 400)
        val hullPercent = target.hitpoints / targetDmgMult / (target.maxHitpoints / targetDmgMult)
        var hpLeft = hullPercent
        if (target.shield != null) {
            val shieldPercent = (target.maxFlux - target.currFlux) * target.shield.fluxPerPointOfDamage / (target.maxFlux * target.shield.fluxPerPointOfDamage)
            val hullToShieldRatio = (target.maxFlux - target.currFlux) / (target.maxHitpoints / targetDmgMult)
            hpLeft = shieldPercent * hullToShieldRatio + hullPercent * (1 - hullToShieldRatio)
        }
        flankingScore -= target.fleetMember.deploymentPointsCost - target.fleetMember.deploymentPointsCost * hpLeft

        //spawnText(flankingScore + "", 60f);
        return flankingScore
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {

        // don't check if paused
        if (engine!!.isPaused) return
        if (ship!!.system.isStateActive && targetShip) {
            timeElapsed += amount
            if (timeElapsed >= neededDur) {
                ship!!.useSystem()
                targetShip = false
                timeElapsed = 0f
            }
        }

        //Disable system if use it not for movement
        if (doIFlick) {
            flickTimer.advance(amount)
            if (flickTimer.intervalElapsed()) {
                ship!!.useSystem()
                doIFlick = false
                flickTimer.elapsed = 0f
                //spawnText("flick", 0f);
            } else {
                return
            }
        }

        // don't check if timer not up
        timer.advance(amount)
        if (!timer.intervalElapsed()) {
            return
        }

        // don't use if can't use
        if (!AIUtils.canUseSystemThisFrame(ship)) {
            return
        }
        if (!doIFlick && ship!!.engineController.isFlamedOut) {
            ship!!.useSystem()
            doIFlick = true
            //spawnText("DoFlick", 0f);
            return
        }

        // setup variables
        var useMe = false
        var targetLocation: Vector2f? = null
        val assignment = engine!!.getFleetManager(ship!!.owner).getTaskManager(ship!!.isAlly).getAssignmentFor(ship)
        val speed: Float = PenetrationDriveStats.SPEED

        // First priority: use to retreat if ordered to retreat. Overrides/ignores the "useMe" system and AI flag checks.
        if (assignment != null && assignment.type == CombatAssignmentType.RETREAT) {
            targetLocation = if (ship!!.owner == 1 || ship!!.owner == 0 && engine!!.getFleetManager(FleetSide.PLAYER).goal == FleetGoal.ESCAPE) {
                Vector2f(ship!!.location.x, ship!!.location.y + 800f) // if ship is enemy OR in "escape" type battle, target loc is UP
            } else {
                Vector2f(ship!!.location.x, ship!!.location.y - 800f) // if ship is player's, target loc is DOWN
            }
            if (rightDirection(ship, targetLocation)) {
                ship!!.useSystem()
                //spawnText("retreat", 0f);
            }
            return  // prevents the AI from activating the ship's system while retreating and facing the wrong direction
            // thanks, Starsector forums user Morathar
        }
        for (f in CON) {
            if (flags!!.hasFlag(f)) {
                return
            }
        }

        if (target == null) return

        // if we have an assignment, set our target loc to it
        // otherwise, if we have a hostile target, set our target loc to intercept it
        if (assignment != null && assignment.target != null) {
            targetLocation = assignment.target.location
            targetShip = true
        } else if (target.owner != ship!!.owner) {
            targetLocation = AIUtils.getBestInterceptPoint(ship!!.location, ship!!.velocity.length() + speed, target.location, target.velocity)
            targetShip = true
        }
        if (targetLocation == null) {
            return
        }
        neededDur = (MathUtils.getDistance(ship!!.location, targetLocation) + (ship!!.collisionRadius + target.collisionRadius)) / speed
        if (rightDirection(ship, targetLocation) && neededDur <= ship!!.system.chargeActiveDur && flankingScore(ship, target) > minPointsToFlank) {
            useMe = true
            //spawnText("Flank/" + NeededDur, 0f);
        }
        for (f in TOWARDS) {
            if (flags!!.hasFlag(f) && rightDirection(ship, targetLocation)) {
                useMe = true
                //spawnText("towards", 0f);
            }
        }

        if (useMe) {
            ship!!.useSystem()
            if (targetShip) {
                neededDur = (MathUtils.getDistance(ship!!.location, targetLocation) + (ship!!.collisionRadius + target.collisionRadius)) / speed
                if (neededDur > ship!!.system.chargeActiveDur) neededDur = ship!!.system.chargeActiveDur
            }
        }
    }

    fun spawnText(text: String?, offset: Float) {
        engine!!.addFloatingText(Vector2f(ship!!.location.x, ship!!.location.y + offset), text, 60f, Color.WHITE, ship, 0.25f, 0.25f)
    }

    fun spawnText(text: String?, pos: Vector2f?) {
        engine!!.addFloatingText(pos, text, 60f, Color.WHITE, null, 0.25f, 0.25f)
    }

    companion object {
        // setup
        private const val DEGREES = 3f

        // list of flags to check for using TOWARDS target, using AWAY from target, and NOT USING
        private val TOWARDS = ArrayList<AIFlags>()

        //AWAY = new ArrayList<>(),
        private val CON = ArrayList<AIFlags>()

        init {
            TOWARDS.add(AIFlags.PURSUING)
            TOWARDS.add(AIFlags.HARASS_MOVE_IN)
            //AWAY.add(AIFlags.RUN_QUICKLY);
            //AWAY.add(AIFlags.TURN_QUICKLY);
            //AWAY.add(AIFlags.NEEDS_HELP);
            CON.add(AIFlags.BACK_OFF)
            CON.add(AIFlags.BACK_OFF_MIN_RANGE)
            CON.add(AIFlags.BACKING_OFF)
            CON.add(AIFlags.DO_NOT_PURSUE)
            //CON.add(AIFlags.KEEP_SHIELDS_ON);
        }
    }
}