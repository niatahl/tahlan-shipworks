package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.niatahl.tahlan.listeners.NeuralLinkReplacer

class DigitalSoulScript : EveryFrameScript {
    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        val digitalSoul =  Global.getSector().playerPerson.stats.hasSkill("tahlan_digitalSoul")
        val cargo = Global.getSector().playerFleet.cargo
        val linkStacks = cargo.stacksCopy.filter { stack ->
            stack.isCommodityStack && stack.commodityId == NeuralLinkReplacer.NEURALLINK_COMM
        }
        if (linkStacks.isNotEmpty() && !digitalSoul) {
            linkStacks.forEach { cargo.removeCommodity(NeuralLinkReplacer.NEURALLINK_COMM, it.size) }
        } else if (linkStacks.isEmpty() && digitalSoul) {
            cargo.addCommodity(NeuralLinkReplacer.NEURALLINK_COMM, 1f)
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