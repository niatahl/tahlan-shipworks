package org.niatahl.tahlan.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.campaign.DevilTravel
import java.util.logging.Logger

class StartDevilTravel : BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: List<Misc.Token?>,
        memoryMap: Map<String, MemoryAPI>
    ): Boolean {
        Global.getSector().addScript(DevilTravel(dialog.interactionTarget.market))
        return true
    }
}