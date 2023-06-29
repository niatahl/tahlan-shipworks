package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.niatahl.tahlan.utils.TahlanIDs.NEURALLINK_COMM
import org.niatahl.tahlan.utils.TahlanIDs.SOTF_SIERRA

class DigitalSoulScript : EveryFrameScript {

    private val aiBoats = HashMap<FleetMemberAPI, String>()

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        val digitalSoul = Global.getSector().playerPerson.stats.hasSkill("tahlan_digitalSoul")
        val cargo = Global.getSector().playerFleet.cargo
        // delete all player cores
        removeCore(cargo)
        // if we don't have the skill, we're done here
        if (!digitalSoul) return

        Global.getSector().playerPerson.aiCoreId = null

        // add core if we aren't in a cargo tab
        if (Global.getSector().campaignUI.currentCoreTab == CoreUITabId.FLEET || Global.getSector().campaignUI.currentCoreTab == CoreUITabId.REFIT) addCore(cargo)

        // AI core restoration cause it goes poof if you move the player to a ship that already has an AI core installed
        aiBoats.forEach { member ->
            if (member.key.captain.isPlayer) Global.getSector().playerFleet.cargo.addCommodity(member.value, 1f)
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

    private fun addCore(cargo: CargoAPI) {
        cargo.addCommodity(NEURALLINK_COMM, 1f)
    }

    private fun removeCore(cargo: CargoAPI) {
        cargo.stacksCopy.forEach { stack ->
            if (stack.isCommodityStack && stack.commodityId == NEURALLINK_COMM) {
                val amt = stack.size
                cargo.removeCommodity(NEURALLINK_COMM, amt)
            }
        }
    }

    companion object {
        fun register() {
            if (!Global.getSector().hasTransientScript(DigitalSoulScript::class.java)) {
                Global.getSector().addTransientScript(DigitalSoulScript())
            }
        }
    }
}