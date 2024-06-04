package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.niatahl.tahlan.utils.TahlanPeople

class Scathach : BaseHullMod() {
    override fun advanceInCampaign(member: FleetMemberAPI, amount: Float) {
        if (member.captain == null || member.captain.isDefault) {
            member.captain = TahlanPeople.getPerson(TahlanPeople.SCATHACH)
        }
    }
}