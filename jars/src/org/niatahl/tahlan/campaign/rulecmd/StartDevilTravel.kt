package org.niatahl.tahlan.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.campaign.DevilTravel

class StartDevilTravel : BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: List<Misc.Token?>,
        memoryMap: Map<String, MemoryAPI>
    ): Boolean {
        val mem = Global.getSector().memoryWithoutUpdate
        if (!mem.getBoolean("\$tahlan_devilTravelStarted")) {
            mem.set("\$tahlan_devilTravelStarted", true)
            Global.getSector().addScript(DevilTravel(dialog.interactionTarget.market))
        }
        return true
    }
}