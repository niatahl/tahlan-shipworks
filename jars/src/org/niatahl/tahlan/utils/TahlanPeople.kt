package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.ImportantPeopleAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Skills
import org.niatahl.tahlan.TahlanModPlugin.Companion.WEEB_MODE

object TahlanPeople {
    // Sierra
    val CIEVE = "tahlan_cieve"

    fun getPerson(id: String): PersonAPI? {
        return Global.getSector().importantPeople.getPerson(id)
    }

    fun synchronise() {
        createCharacters()
        getPerson(CIEVE)?.portraitSprite =
            if (WEEB_MODE) Global.getSettings().getSpriteName("portraits", "tahlan_cieve_waifu")
            else Global.getSettings().getSpriteName("portraits", "tahlan_cieve")
    }

    private fun createCharacters() {
        val ip: ImportantPeopleAPI = Global.getSector().importantPeople

        // Cieve
        if (getPerson(CIEVE) == null) {
            val person: PersonAPI = Global.getFactory().createPerson().apply {
                id = CIEVE
                setFaction("tahlan_cieveFaction")
                gender = FullName.Gender.FEMALE
                rankId = "tahlan_cieve"
                postId = "tahlan_cieve"
                name.first = "CIEVE"
                name.last = ""
                importance = PersonImportance.VERY_HIGH
                portraitSprite = Global.getSettings().getSpriteName("portraits", "tahlan_cieve_waifu")
                // Officer stats
                setPersonality(Personalities.RECKLESS)
                stats.setLevel(1)
                stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 1f)
                stats.setSkillLevel("tahlan_hyperCoordination", 1f)
                getMemoryWithoutUpdate().set("\$chatterChar", "cieve")
            }
            ip.addPerson(person)
        }
    }
}