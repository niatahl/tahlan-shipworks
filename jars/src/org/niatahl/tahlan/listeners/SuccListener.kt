package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO

/**
 * The Abyssal Maw at the heart of Rubicon does not merely sit there. A fleet arriving in the system
 * is dragged toward the central black hole, and the larger the fleet, the harder it is pulled in.
 *
 * The pull is not permanent: it fades to nothing over [DECAY_TIME] seconds as the fleet's drive
 * field adjusts to the system. Legio Infernalis fleets are exempt entirely (this is their home, and
 * they are long since used to it), as are fleets under Emergency Burn, which vanilla treats as immune
 * to navigation hazards.
 *
 * Runs as a transient every-frame script (re-registered on each load via [register]).
 */
class SuccListener : EveryFrameScript {

    // Resolved lazily on first advance: the system may not exist yet when the script is constructed.
    private var system: StarSystemAPI? = null
    private var resolved = false

    // Per-fleet seconds spent in-system, by fleet id. Drives the decay; cleared when a fleet leaves
    // so re-entry starts the adjustment over.
    private val timeInSystem = HashMap<String, Float>()

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!resolved) {
            system = Global.getSector().getStarSystem(RUBICON_ID)
            resolved = true
        }
        val system = system ?: return
        val center = (system.center ?: system.star) ?: return
        val target = center.location

        val present = HashSet<String>()
        for (fleet in system.fleets) {
            present.add(fleet.id)

            if (fleet.isStationMode) continue // not an arriving fleet; never pulled, don't track

            // Tick the adjustment timer for every mobile fleet present, immune or not, so the drive
            // field settles on a wall clock rather than only while the fleet is actually being pulled.
            val elapsed = (timeInSystem[fleet.id] ?: 0f) + amount
            timeInSystem[fleet.id] = elapsed

            if (fleet.faction?.id == LEGIO) continue // their home; they are used to the Maw
            if (isImmuneToNavHazards(fleet)) continue // Emergency Burn grants vanilla nav-hazard immunity
            if (elapsed >= DECAY_TIME) continue // drive field has adjusted; no more pull

            val loc = fleet.location
            val dist = Misc.getDistance(loc, target)
            if (dist < MIN_DISTANCE) continue // already in the maw; let the event horizon handle it

            // Pull speed scales with fleet size (clamped so even a doomstack drifts rather than snaps),
            // then fades linearly to zero over DECAY_TIME.
            val decay = 1f - elapsed / DECAY_TIME
            val pull = (BASE_PULL + fleet.fleetPoints * PULL_PER_FP).coerceAtMost(MAX_PULL) * decay
            val step = (pull * amount).coerceAtMost(dist) // never overshoot the center

            val dir = Vector2f.sub(target, loc, null)
            dir.normalise()
            fleet.setLocation(loc.x + dir.x * step, loc.y + dir.y * step)
        }

        // Forget fleets that have left so re-entry starts the adjustment over.
        timeInSystem.keys.retainAll(present)
    }

    private fun isImmuneToNavHazards(fleet: CampaignFleetAPI): Boolean {
        val ability = fleet.getAbility(Abilities.EMERGENCY_BURN)
        return ability != null && ability.isActive
    }

    companion object {
        private const val RUBICON_ID = "Rubicon"
        private const val DECAY_TIME = 10f // seconds before a fleet's drive field fully adjusts
        private const val MIN_DISTANCE = 120f // ~ event-horizon radius; stop nudging inside this
        private const val BASE_PULL = 5f // units/sec floor pull, felt even by the smallest fleets
        private const val PULL_PER_FP = 0.5f // additional units/sec per fleet point
        private const val MAX_PULL = 80f // cap so the largest fleets are dragged, not teleported

        fun register() {
            val sector = Global.getSector()
            if (!sector.hasTransientScript(SuccListener::class.java)) {
                sector.addTransientScript(SuccListener())
            }
        }
    }
}
