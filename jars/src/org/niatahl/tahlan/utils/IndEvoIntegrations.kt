package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import indevo.exploration.minefields.MineBeltTerrainPlugin
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO


object IndEvoIntegrations {

    @JvmStatic
    fun addDefenses() {
        val sector = Global.getSector()
        val lucifron = sector.economy.getMarket("tahlan_rubicon_p03_market") ?: return
        val melchi = sector.economy.getMarket("tahlan_rubicon_p01_market") ?: return
        val adra = sector.economy.getMarket("tahlan_rubicon_outpost_market") ?: return
        if (!lucifron.hasCondition("IndEvo_mineFieldCondition")) {
            lucifron.addCondition("IndEvo_mineFieldCondition")
            melchi.addCondition("IndEvo_mineFieldCondition")
            adra.addCondition("IndEvo_mineFieldCondition")
        }
        // new stuff
        if (Global.getSettings().getIndustrySpec("IndEvo_Artillery_mortar") == null) return
        if (!lucifron.hasCondition("IndEvo_ArtilleryStationCondition")) {
            lucifron.addCondition("IndEvo_ArtilleryStationCondition")
            lucifron.addIndustry("IndEvo_Artillery_mortar")
            melchi.addCondition("IndEvo_ArtilleryStationCondition")
            melchi.addIndustry("IndEvo_Artillery_railgun")
            adra.addCondition("IndEvo_ArtilleryStationCondition")
            adra.addIndustry("IndEvo_Artillery_missile")
        }
        if (!sector.memoryWithoutUpdate.getBoolean("\$tahlan_minesDeployed")) {
            sector.memoryWithoutUpdate["\$tahlan_minesDeployed"] = true
            sector.getStarSystem("Rubicon").jumpPoints.forEach { point ->
                val belt = MineBeltTerrainPlugin.addMineBelt(point, 300f, 150f, 30f, 40f, point.name + " Minefield")
                belt.memoryWithoutUpdate.set("\$IndEvo_PlanetMinefieldKey", lucifron.primaryEntity)
            }
            val star = sector.getStarSystem("Rubicon").star
            val belt = MineBeltTerrainPlugin.addMineBelt(star, 600f, 300f, 60f, 120f, star.name + " Minefield")
            belt.memoryWithoutUpdate.set("\$IndEvo_PlanetMinefieldKey", lucifron.primaryEntity)
        }
    }


    fun upgradeDefenses() {
        val sector = Global.getSector()
        if (!sector.memoryWithoutUpdate.getBoolean("\$tahlan_indEvoUpgraded")) return
        val lucifron = sector.economy.getMarket("tahlan_rubicon_p03_market") ?: return
        val melchi = sector.economy.getMarket("tahlan_rubicon_p01_market") ?: return
        val adra = sector.economy.getMarket("tahlan_rubicon_outpost_market") ?: return
        if (lucifron.hasCondition("IndEvo_ArtilleryStationCondition")) {
            if (lucifron.factionId == LEGIO && lucifron.getIndustry("IndEvo_Artillery_mortar") != null)
                lucifron.getIndustry("IndEvo_Artillery_mortar").isImproved = true
            if (melchi.factionId == LEGIO && melchi.getIndustry("IndEvo_Artillery_railgun") != null)
                melchi.getIndustry("IndEvo_Artillery_railgun").isImproved = true
            if (adra.factionId == LEGIO && adra.getIndustry("IndEvo_Artillery_missile") != null)
                adra.getIndustry("IndEvo_Artillery_missile").isImproved = true
            sector.memoryWithoutUpdate.set("\$tahlan_indEvoUpgraded", true)
        }
    }
}