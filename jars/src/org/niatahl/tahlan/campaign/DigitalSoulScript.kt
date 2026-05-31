package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.niatahl.tahlan.utils.TahlanIDs.DIGITAL_SOUL
import org.niatahl.tahlan.utils.TahlanIDs.NEURALLINK_COMM
import org.niatahl.tahlan.utils.TahlanIDs.SOTF_SIERRA

/**
 * Backs the hidden "Digital Soul" skill: the player character acts as an AI core and can captain
 * automated ships. The mechanism is a stand-in "core" commodity ([NEURALLINK_COMM]) that
 * [org.niatahl.tahlan.plugins.NeuralLinkOfficerPlugin] resolves to the player person, so installing
 * it as a captain reads as "the player is piloting this hull".
 *
 * This script keeps exactly one stand-in core in cargo while the player is on a screen where it can
 * be installed (Fleet / Refit) and strips it everywhere else, so it never lingers or stacks. It must
 * [runWhilePaused] because those screens pause the campaign.
 */
class DigitalSoulScript : EveryFrameScript {

    // Snapshot from the previous frame: automated-ship members and the AI core that was captaining
    // them. Used to refund a displaced core when the player takes over such a ship (see advance).
    private val aiBoats = HashMap<FleetMemberAPI, String>()

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        val cargo = Global.getSector().playerFleet.cargo

        // No skill (or it was lost): make sure the stand-in core never lingers, then bail. Cheap
        // quantity check avoids scanning/allocating cargo stacks every frame for the common case.
        if (!Global.getSector().playerPerson.stats.hasSkill(DIGITAL_SOUL)) {
            ensureCoreCount(cargo, 0)
            return
        }

        // The player IS the core; keep the engine from also treating playerPerson as an AI officer.
        Global.getSector().playerPerson.aiCoreId = null

        // Offer the stand-in core only on screens where it can be slotted as a captain.
        val tab = Global.getSector().campaignUI.currentCoreTab
        val wantCore = tab == CoreUITabId.FLEET || tab == CoreUITabId.REFIT
        ensureCoreCount(cargo, if (wantCore) 1 else 0)

        // AI core restoration: a real core goes poof if the player moves onto a ship that already has
        // one installed. If a member we snapshotted last frame is now player-captained, the player
        // displaced its core - refund it to cargo. (The stand-in core is player-captained, so it is
        // filtered out below and never refunded here.)
        aiBoats.forEach { (member, coreId) ->
            if (member.captain.isPlayer) cargo.addCommodity(coreId, 1f)
        }
        aiBoats.clear()
        Global.getSector().playerFleet.membersWithFightersCopy.forEach { member ->
            if (!member.isFighterWing
                && member.captain != null
                && member.captain.isAICore
                && !member.captain.isPlayer
                && member.captain.id != SOTF_SIERRA
            ) {
                aiBoats[member] = member.captain.aiCoreId
            }
        }
    }

    /** Bring the stand-in core in cargo to exactly [desired], acting only when it is currently off. */
    private fun ensureCoreCount(cargo: CargoAPI, desired: Int) {
        val current = cargo.getCommodityQuantity(NEURALLINK_COMM).toInt()
        if (current == desired) return
        if (current > 0) cargo.removeCommodity(NEURALLINK_COMM, current.toFloat())
        if (desired > 0) cargo.addCommodity(NEURALLINK_COMM, desired.toFloat())
    }

    companion object {
        fun register() {
            if (!Global.getSector().hasTransientScript(DigitalSoulScript::class.java)) {
                Global.getSector().addTransientScript(DigitalSoulScript())
            }
        }
    }
}