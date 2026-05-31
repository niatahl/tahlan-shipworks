package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.utils.TahlanPeople.CIEVE

class CieveScript : EveryFrameScript {

    // Runs while paused so refit changes take effect, but a full fleet scan + list copy every frame
    // is wasteful. A short interval still updates captains effectively instantly to the player.
    private val interval = IntervalUtil(0.2f, 0.3f)

    override fun advance(amount: Float) {
        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        val cieve = TahlanPeople.getPerson(CIEVE) ?: return
        // Loop-invariant: read the recruited flag once per pass, not once per member.
        val recruited = Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_cieveRecruited")

        Global.getSector().playerFleet.fleetData.membersListCopy.forEach { member ->
            if (recruited
                && member.variant.hasHullMod("tahlan_cieveLink")
                && (member.captain == null || member.captain.isDefault)
            ) {
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

    companion object {
        fun register() {
            if (!Global.getSector().hasTransientScript(CieveScript::class.java)) {
                Global.getSector().addTransientScript(CieveScript())
            }
        }
    }
}
