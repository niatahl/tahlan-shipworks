package org.niatahl.tahlan.campaign.items

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.impl.campaign.ids.Skills
import org.niatahl.tahlan.utils.TahlanIDs.DIGITAL_SOUL

class NeuralLinkPlugin : BaseSpecialItemPlugin() {

    override fun hasRightClickAction(): Boolean {
        return true
    }

    override fun performRightClickAction() {
        Global.getSector().playerPerson.stats.setSkillLevel(DIGITAL_SOUL, 1f)
    }

    override fun shouldRemoveOnRightClickAction(): Boolean {
        return true
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        return 0
    }
}