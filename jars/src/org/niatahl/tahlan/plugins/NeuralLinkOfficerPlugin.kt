package org.niatahl.tahlan.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl
import java.util.*

class NeuralLinkOfficerPlugin : BaseAICoreOfficerPluginImpl(), AICoreOfficerPlugin {
    override fun createPerson(aiCoreId: String?, factionId: String?, random: Random?): PersonAPI {
        return Global.getSector().playerPerson
    }
}