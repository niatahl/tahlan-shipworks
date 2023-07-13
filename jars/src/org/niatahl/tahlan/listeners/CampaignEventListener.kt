package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import org.niatahl.tahlan.plugins.TahlanModPlugin
import org.niatahl.tahlan.utils.TahlanIDs

class CampaignEventListener(permaRegister: Boolean = false) : BaseCampaignEventListener(permaRegister) {
    override fun reportFleetSpawned(fleet: CampaignFleetAPI) {
        if (fleet.isPlayerFleet) return

        if (!fleet.faction.id.contains("tahlan_legio")) return

        fleet.membersWithFightersCopy
            .filter { !it.isFighterWing && it.variant.hasHullMod(TahlanIDs.DAEMONIC_HEART) }
            .forEach {
                LegioFleetInflationListener.addDaemonCore(it)
                if (TahlanModPlugin.ENABLE_ADAPTIVEMODE) {
                    LegioFleetInflationListener.clearSMods(it)
                    LegioFleetInflationListener.addSMods(it)
                }
            }
    }
}