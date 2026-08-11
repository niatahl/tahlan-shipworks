package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console

/**
 * Console Commands (lw_console) integration for the Legio siege system. These classes are only ever
 * loaded when Console Commands is installed and enabled — it discovers them by scanning
 * data/console/commands.csv — so referencing org.lazywizard.console.* here is safe for users without
 * the mod (the classes simply never load).
 *
 * All logic lives on [SiegeManager]; these are thin front-ends that resolve the live manager, call it,
 * and print the result via [Console.showMessage].
 */

/** TahlanSiegeInfo — print in-depth manager + per-siege state for debugging. */
class TahlanSiegeInfo : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        if (!context.isCampaignAccessible) {
            Console.showMessage("Run TahlanSiegeInfo on the campaign map.")
            return CommandResult.WRONG_CONTEXT
        }
        val mgr = SiegeManager.get()
        if (mgr == null) {
            Console.showMessage("No siege manager found (the siege system has not initialized this game).")
            return CommandResult.SUCCESS
        }
        Console.showMessage(mgr.debugDump())
        return CommandResult.SUCCESS
    }
}

/** TahlanSiegeKill — force-end (lift) all active sieges, dispersing their fleets. */
class TahlanSiegeKill : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        if (!context.isCampaignAccessible) {
            Console.showMessage("Run TahlanSiegeKill on the campaign map.")
            return CommandResult.WRONG_CONTEXT
        }
        val mgr = SiegeManager.get()
        if (mgr == null) {
            Console.showMessage("No siege manager found; nothing to end.")
            return CommandResult.SUCCESS
        }
        val ended = mgr.debugEndAllSieges()
        Console.showMessage(if (ended == 0) "No active sieges to end." else "Ended $ended active siege(s); fleets dispersing home.")
        return CommandResult.SUCCESS
    }
}

/**
 * TahlanSiegeProgress — set the subjugation meter on every active siege. Only the meter is written;
 * the manager's own tick is left to decide what a full meter means, so parking it just short of max
 * is how the planetfall entry path gets exercised in a dev pass.
 */
class TahlanSiegeProgress : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        if (!context.isCampaignAccessible) {
            Console.showMessage("Run TahlanSiegeProgress on the campaign map.")
            return CommandResult.WRONG_CONTEXT
        }
        val value = args.trim().toFloatOrNull()
        if (value == null) {
            Console.showMessage("Usage: TahlanSiegeProgress <0-${SiegeConfig.CAPTURE_PROGRESS_MAX.toInt()}>")
            return CommandResult.BAD_SYNTAX
        }
        val mgr = SiegeManager.get()
        if (mgr == null) {
            Console.showMessage("No siege manager found (the siege system has not initialized this game).")
            return CommandResult.SUCCESS
        }
        Console.showMessage(mgr.debugSetProgress(value))
        return CommandResult.SUCCESS
    }
}

/** TahlanSiegeStart — force-launch a new siege now, bypassing the spawn timer. */
class TahlanSiegeStart : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        if (!context.isCampaignAccessible) {
            Console.showMessage("Run TahlanSiegeStart on the campaign map.")
            return CommandResult.WRONG_CONTEXT
        }

        val query = args.trim()
        var forced: StarSystemAPI? = null
        if (query.isNotEmpty()) {
            forced = findSystem(query)
            if (forced == null) {
                Console.showMessage("No star system matching \"$query\". Omit the argument to auto-pick a target.")
                return CommandResult.ERROR
            }
        }

        val mgr = SiegeManager.getOrCreate()
        Console.showMessage(mgr.debugForceLaunch(forced))
        return CommandResult.SUCCESS
    }

    /** Match a system by base name, then by full name (both case-insensitive). */
    private fun findSystem(query: String): StarSystemAPI? {
        val systems = Global.getSector().starSystems
        return systems.firstOrNull { it.baseName.equals(query, ignoreCase = true) }
            ?: systems.firstOrNull { it.name.equals(query, ignoreCase = true) }
    }
}
