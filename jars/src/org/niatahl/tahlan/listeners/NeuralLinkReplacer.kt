package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI

class NeuralLinkReplacer : CargoTabListener {


    override fun reportCargoOpened(cargo: CargoAPI) {
        removeCore(cargo)
    }

    override fun reportCargoClosed(cargo: CargoAPI) {
        if (Global.getSector().playerPerson.stats.hasSkill("tahlan_digitalSoul")) {
            addCore(cargo)
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
        const val NEURALLINK_SPECIAL = "tahlan_neurallinkspecial"
        const val NEURALLINK_COMM = "tahlan_neurallink"

        fun register() {
            val manager = Global.getSector().listenerManager
            if (!manager.hasListenerOfClass(NeuralLinkReplacer::class.java)) {
                manager.addListener(NeuralLinkReplacer(), true)
            }
        }
    }
}