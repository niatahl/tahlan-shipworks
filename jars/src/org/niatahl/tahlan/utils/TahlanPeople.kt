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
    // Person IDs
    const val CIEVE = "tahlan_cieve"
    const val CHILD = "tahlan_child"

    //AICore IDs
    const val CORE_DAEMON = "tahlan_daemoncore"
    const val CORE_ARCHDAEMON = "tahlan_archdaemoncore"

    // For fake fearless
    const val FEARLESS = "\$tahlan_persFearless"

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
            val person = Global.getFactory().createPerson().apply {
                id = CIEVE
                setFaction("tahlan_cieveFaction")
                gender = FullName.Gender.FEMALE
                rankId = "tahlan_cieve"
                postId = "tahlan_cieve"
                name.first = "CIEVE"
                name.last = ""
                importance = PersonImportance.VERY_HIGH
                portraitSprite = Global.getSettings().getSpriteName("portraits", "tahlan_cieve_waifu")
                setPersonality(Personalities.RECKLESS)
                stats.level = 2
                stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 1f)
                stats.setSkillLevel("tahlan_hyperCoordination", 1f)
                memoryWithoutUpdate.set("\$chatterChar", "cieve")
            }
            ip.addPerson(person)
        }

        // Allmother's Child, born of the void
        // This is the default lostech ship AI that gets installed when no other officer is assigned
        if (getPerson(CHILD) == null) {
            val person = Global.getFactory().createPerson().apply {
                id = CHILD
                setFaction("tahlan_allmother")
                gender = FullName.Gender.ANY
                rankId = "tahlan_offspring"
                postId = "tahlan_offspring"
                name.first = "VOIDCHILD"
                name.last = ""
                aiCoreId
                importance = PersonImportance.LOW
                portraitSprite = Global.getSettings().getSpriteName("portraits", "tahlan_offspring")
                setPersonality(Personalities.RECKLESS)
                stats.level = 3
                stats.setSkillLevel("combat_endurance", 2f)
                stats.setSkillLevel("impact_mitigation", 2f)
                stats.setSkillLevel("point_defense", 2f)
                memoryWithoutUpdate.set(FEARLESS, true)
            }
            ip.addPerson(person)
        }
    }
}