package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions.INDEPENDENT
import com.fs.starfarer.api.impl.campaign.ids.Factions.PIRATES
import com.fs.starfarer.api.util.IntervalUtil
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.TahlanPeople.DEVIL

class DevilTravel(market: MarketAPI) : EveryFrameScript {
    var marketId: String = market.id
    var doomed = false

    override fun isDone(): Boolean {
        return doomed
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        val sector = Global.getSector()
        val days = sector.clock.convertToDays(amount)
        timer.advance(days)

        val market = sector.economy.getMarket(marketId)
        val marketRuined = market == null || market.hasCondition(Conditions.DECIVILIZED)
        if (timer.intervalElapsed() || marketRuined) {
            val newHome = sector.economy.marketsCopy
                .filter { factions.contains(it.factionId) && !market.hasCondition(Conditions.DECIVILIZED) }
                .randomOrNull()
            val devil = sector.importantPeople.getPerson(DEVIL)
            market?.commDirectory?.removePerson(devil)
            newHome?.commDirectory?.addPerson(devil)
            doomed = newHome == null
        }
    }

    companion object {
        val timer = IntervalUtil(20f, 40f)
        val factions = listOf(LEGIO, PIRATES, INDEPENDENT)
    }
}