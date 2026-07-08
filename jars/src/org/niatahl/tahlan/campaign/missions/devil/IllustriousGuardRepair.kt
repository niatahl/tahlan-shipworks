package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_RECOVERED

/**
 * Transient watchdog that heals the "empty system" bug in existing saves.
 *
 * Older builds of [IllustriousRecovery] created each clue guard but never spawned it into its system,
 * so the player was pointed at an empty system with no way to advance. A mission's triggers are
 * serialized with the save and [IllustriousRecovery.create] never re-runs on load, so the defect
 * survives the code fix for the current *and* not-yet-reached clue stages. On an interval this simply
 * asks the active mission to make sure the current clue stage's guard exists;
 * [IllustriousRecovery.ensureCurrentGuardSpawned] is idempotent, so this is a no-op once every guard
 * is in place (including on new games, where the fixed trigger already spawns it).
 *
 * Registered as a transient script (re-added each load, never persisted) via [register].
 */
class IllustriousGuardRepair : EveryFrameScript {

    private val interval = IntervalUtil(0.5f, 1f)

    override fun isDone(): Boolean =
        Global.getSector().memoryWithoutUpdate.getBoolean(ILLUSTRIOUS_RECOVERED)

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        interval.advance(Global.getSector().clock.convertToDays(amount))
        if (!interval.intervalElapsed()) return

        val mission = Global.getSector().intelManager
            .getFirstIntel(IllustriousRecovery::class.java) as? IllustriousRecovery ?: return
        mission.ensureCurrentGuardSpawned()
    }

    companion object {
        /** Idempotent registration; call from onGameLoad. Skips once the ship has been recovered. */
        fun register() {
            val sector = Global.getSector()
            if (sector.memoryWithoutUpdate.getBoolean(ILLUSTRIOUS_RECOVERED)) return
            if (!sector.hasTransientScript(IllustriousGuardRepair::class.java)) {
                sector.addTransientScript(IllustriousGuardRepair())
            }
        }
    }
}
