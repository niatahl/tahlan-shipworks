package org.niatahl.tahlan.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs.CORE_ARCHDAEMON
import org.niatahl.tahlan.utils.TahlanIDs.CORE_DAEMON
import java.awt.Color
import java.text.DecimalFormat
import java.util.*


class DaemonOfficerPlugin: BaseAICoreOfficerPluginImpl(), AICoreOfficerPlugin {

    override fun createPerson(aiCoreId: String, factionId: String, random: Random?): PersonAPI? {
        return when (aiCoreId) {
            CORE_DAEMON -> createDaemon(factionId)
            CORE_ARCHDAEMON -> createArchdaemon(factionId)
            else -> null
        }
    }

    private fun createDaemon(factionId: String): PersonAPI {
        val person = Misc.getAICoreOfficerPlugin(Commodities.BETA_CORE).createPerson(Commodities.BETA_CORE, factionId, Misc.random)
        person.stats.skillsCopy.last().skill.id.also { person.stats.setSkillLevel(it,0f) }
        person.apply {
            setFaction(factionId)
            aiCoreId = CORE_DAEMON
            name.first = "Daemon Core"
            stats.level = 6
            stats.setSkillLevel("tahlan_daemonicCorruption", 1f)
            portraitSprite = Global.getSettings().getSpriteName("portraits","tahlan_daemon")
        }
        return person
    }

    private fun createArchdaemon(factionId: String): PersonAPI {
        val person = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, factionId, Misc.random)
        person.stats.skillsCopy.last().skill.id.also { person.stats.setSkillLevel(it,0f) }
        person.apply {
            setFaction(factionId)
            aiCoreId = CORE_ARCHDAEMON
            name.first = "Archdaemon Core"
            stats.level = 7
            stats.setSkillLevel("tahlan_daemonicCorruption", 2f)
            portraitSprite = Global.getSettings().getSpriteName("portraits","tahlan_archdaemon")
        }
        return person
    }

    override fun createPersonalitySection(person: PersonAPI, tooltip: TooltipMakerAPI?) {
        val opad = 10f
        val text: Color = person.faction.baseUIColor
        val bg: Color = person.faction.darkUIColor

        val autoMultString = DecimalFormat("#.##").format(person.memoryWithoutUpdate.getFloat(AUTOMATED_POINTS_MULT))

        tooltip!!.addPara(
            "Automated ship points multiplier: "
                    + autoMultString + "x", opad, Misc.getHighlightColor(), autoMultString + "x"
        )

        tooltip.addSectionHeading("Personality: " + Misc.getPersonalityName(person), text, bg, Alignment.MID, 20F)
        when (person.personalityAPI.id) {
            Personalities.RECKLESS -> tooltip.addPara(
                "In combat, this daemon is single-minded and determined. " +
                        "In a human captain, their traits might be considered reckless. In a machine, they're terrifying.", opad
            )

            Personalities.AGGRESSIVE -> tooltip.addPara(
                "In combat, this daemon will prefer to engage at a range that allows the use of " +
                        "all of their ship's weapons and will employ any fighters under their command aggressively.", opad
            )

            Personalities.STEADY -> tooltip.addPara(
                "In combat, this daemon will favor a balanced approach with " +
                        "tactics matching the current situation.", opad
            )

            Personalities.CAUTIOUS -> tooltip.addPara(
                "In combat, this daemon will prefer to stay out of enemy range, " +
                        "only occasionally moving in if out-ranged by the enemy.", opad
            )

            Personalities.TIMID -> tooltip.addPara(
                "In combat, this daemon will attempt to avoid direct engagements if at all " +
                        "possible, even if commanding a combat vessel.", opad
            )
        }
    }
}