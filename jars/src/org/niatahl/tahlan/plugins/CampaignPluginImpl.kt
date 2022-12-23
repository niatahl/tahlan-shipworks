package org.niatahl.tahlan.plugins

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.CampaignPlugin
import org.niatahl.tahlan.utils.TahlanPeople

class CampaignPluginImpl: BaseCampaignPlugin() {

    override fun pickAICoreOfficerPlugin(commodityId: String): PluginPick<AICoreOfficerPlugin>? {
        return when (commodityId) {
            TahlanPeople.CORE_DAEMON, TahlanPeople.CORE_ARCHDAEMON -> PluginPick<AICoreOfficerPlugin>(DaemonOfficerPlugin(), CampaignPlugin.PickPriority.MOD_SET)
            else -> null
        }
    }

    override fun getId(): String {
        return "TahlanCampaignPluginImpl"
    }
}