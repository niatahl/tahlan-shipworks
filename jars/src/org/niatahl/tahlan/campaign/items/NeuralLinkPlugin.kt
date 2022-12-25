package org.niatahl.tahlan.campaign.items

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin

class NeuralLinkPlugin : BaseSpecialItemPlugin() {

    override fun hasRightClickAction(): Boolean {
        return false
    }

    override fun shouldRemoveOnRightClickAction(): Boolean {
        return false
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        return 0
    }
}