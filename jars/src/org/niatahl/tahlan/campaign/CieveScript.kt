package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.utils.TahlanPeople.CIEVE

class CieveScript : EveryFrameScript {
    override fun advance(amount: Float) {
        val cieve = TahlanPeople.getPerson(CIEVE)
        Global.getSector().playerFleet.fleetData.membersListCopy.forEach { member ->
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_cieveRecruited") && member.variant.hasHullMod("tahlan_cieveLink") && (member.captain == null || member.captain.isDefault)) {
                member.captain = cieve
            } else if (member.captain == cieve && !member.variant.hasHullMod("tahlan_cieveLink")) {
                member.captain = null
            }
        }
    }

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }
}