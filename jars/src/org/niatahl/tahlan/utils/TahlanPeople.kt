package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.ImportantPeopleAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import org.niatahl.tahlan.TahlanModPlugin.Companion.WEEB_MODE
import org.niatahl.tahlan.utils.TahlanIDs.BLACKWATCH
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO

object TahlanPeople {
    // Person IDs
    const val CIEVE = "tahlan_cieve"
    const val CHILD = "tahlan_child"
    const val HENRIETTA = "tahlan_henrietta"
    const val DEVIL = "tahlan_devil"

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

        // Henrietta
        if (getPerson(HENRIETTA) == null) {
            val person = Global.getFactory().createPerson().apply {
                id = HENRIETTA
                setFaction(Factions.INDEPENDENT)
                gender = FullName.Gender.FEMALE
                rankId = "tahlan_henrietta"
                postId = "tahlan_henrietta"
                name.first = "Henrietta"
                name.last = "von Regenfels"
                importance = PersonImportance.VERY_HIGH
                portraitSprite = Global.getSettings().getSpriteName("portraits", "tahlan_henrietta")
                setPersonality(Personalities.AGGRESSIVE)
                stats.level = 5
                stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 1f)
                stats.setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1f)
                stats.setSkillLevel(Skills.HELMSMANSHIP, 1f)
                stats.setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1f)
                stats.setSkillLevel("tahlan_raketentanz", 1f)
//                memoryWithoutUpdate.set("\$chatterChar", "henrietta")
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
                importance = PersonImportance.LOW
                portraitSprite = Global.getSettings().getSpriteName("portraits", "tahlan_offspring")
                setPersonality(Personalities.RECKLESS)
                stats.level = 3
                stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f)
                stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
                stats.setSkillLevel(Skills.POINT_DEFENSE, 2f)
                memoryWithoutUpdate.set(FEARLESS, true)
            }
            ip.addPerson(person)
        }

        if (getPerson(DEVIL) == null) {
            val person = Global.getFactory().createPerson().apply {
                id = DEVIL
                setFaction(BLACKWATCH)
                name.first = "Louisa"
                name.last = "Ferre"
                gender = FullName.Gender.FEMALE
                rankId = Ranks.SPACE_CAPTAIN
                postId = Ranks.POST_SPECIAL_AGENT
                importance = PersonImportance.HIGH
                portraitSprite = Global.getSettings().getSpriteName("portraits", "tahlan_devil")
                stats.level = 7
                stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 1f)
                stats.setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1f)
                stats.setSkillLevel(Skills.HELMSMANSHIP, 1f)
                stats.setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1f)
                stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
                stats.setSkillLevel(Skills.POLARIZED_ARMOR, 2f)
                stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 1f)
                tags.add("tahlan_devil")
            }
            ip.addPerson(person)
        }
    }
}