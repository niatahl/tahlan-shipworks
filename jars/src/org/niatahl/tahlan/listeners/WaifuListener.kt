package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.characters.FullName.Gender

class WaifuListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {
    override fun reportFleetSpawned(fleet: CampaignFleetAPI) {
        val bitches = Global.getSector().getFaction(BITCHES)
        if (fleet.faction != bitches) return

        fleet.membersWithFightersCopy
            .filter { !it.captain.isDefault && it.captain.gender == Gender.MALE }
            .forEach { member ->
                val newGal = bitches.createRandomPerson(Gender.FEMALE)
                member.captain.apply {
                    gender = Gender.FEMALE
                    portraitSprite = newGal.portraitSprite
                    name = newGal.name
                }
            }

    }

    companion object {
        const val BITCHES = "selkieknightsidhere"
    }
}