package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions.INDEPENDENT
import com.fs.starfarer.api.impl.campaign.ids.Factions.PIRATES
import com.fs.starfarer.api.impl.campaign.ids.Factions.PLAYER
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel.ContactState
import com.fs.starfarer.api.util.IntervalUtil
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.TahlanPeople.DEVIL

/**
 * Keeps Louisa Ferre (tahlan_devil) roaming between Legio/pirate/independent markets, and relocates
 * her the moment her current home decivilizes or is destroyed. Once she's friendly with the player
 * she'll also settle on player-owned colonies.
 *
 * When she's a tracked contact, every move is driven through her [ContactIntel] so the intel entry,
 * comm-directory presence and map marker all stay in sync — hand-shuffling the comm directory (the
 * old behaviour) left the contact pointing at the dead market and re-adding her there. Her importance
 * is preserved across routine roams; vanilla relocateToMarket docks it each call, which is meant as a
 * decivilization penalty, not something that should grind her down every time she wanders.
 *
 * The per-frame ruined-market check means we relocate her before the contact's own ~1-day periodic
 * check can fire "lost contact", so her full roaming faction set is honoured rather than vanilla's
 * same-faction-only fallback.
 */
class DevilTravel(market: MarketAPI) : EveryFrameScript {
    var marketId: String = market.id
    var doomed = false
    val timer = IntervalUtil(20f, 40f)

    override fun isDone(): Boolean = doomed

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val sector = Global.getSector()
        timer.advance(sector.clock.convertToDays(amount))

        val devil = sector.importantPeople.getPerson(DEVIL) ?: run { doomed = true; return }
        val intel = ContactIntel.getContactIntel(devil)

        // Relationship is over for good (deciv with no fallback, or the player deleted her): stop.
        if (intel != null && (intel.state == ContactState.LOST_CONTACT || intel.state == ContactState.LOST_CONTACT_DECIV)) {
            doomed = true
            return
        }

        val current = sector.economy.getMarket(marketId)
        val marketRuined = current == null || current.hasCondition(Conditions.DECIVILIZED)

        // No move due and her home is fine: just keep our id synced to wherever the contact is.
        if (!timer.intervalElapsed() && !marketRuined) {
            intel?.getMapLocation(null)?.market?.let { marketId = it.id }
            return
        }

        // Once she's friendly with the player she'll also hole up on player-owned colonies.
        val eligible =
            if (devil.relToPlayer.level.ordinal >= RepLevel.FRIENDLY.ordinal) factions + PLAYER
            else factions
        val newHome = sector.economy.marketsCopy
            .filter { it.factionId in eligible && !it.hasCondition(Conditions.DECIVILIZED) }
            .randomOrNull()

        if (newHome == null) {
            // Nowhere left to send her.
            if (intel != null) intel.loseContact(null) else current?.commDirectory?.removePerson(devil)
            doomed = true
            return
        }

        if (intel != null) {
            // Drive the move through the contact: it handles the comm directory, person.setMarket,
            // the market person-list and the intel map marker. Preserve importance (see class doc).
            val importance = devil.importance
            intel.relocateToMarket(newHome, false)
            devil.importance = importance
        } else {
            current?.commDirectory?.removePerson(devil)
            newHome.commDirectory.addPerson(devil)
            devil.market = newHome
        }
        sector.importantPeople.getData(devil)?.location?.market = newHome
        marketId = newHome.id
    }

    companion object {
        val factions = listOf(LEGIO, PIRATES, INDEPENDENT)
    }
}
