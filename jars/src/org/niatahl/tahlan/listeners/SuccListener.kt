package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken

class SuccListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {
    override fun reportFleetJumped(
        fleet: CampaignFleetAPI,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        val system = to?.destination?.starSystem
        if (system != null && system.id.equals("Rubicon")) {
            fleet.setLocation(system.star.location.x, system.star.location.y)
        }
    }

    companion object {
        fun register() {
            Global.getSector().addTransientListener(SuccListener(false))
        }
    }
}