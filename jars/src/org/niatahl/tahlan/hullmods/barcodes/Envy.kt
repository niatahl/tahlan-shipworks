package org.niatahl.tahlan.hullmods.barcodes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.niatahl.tahlan.utils.Utils.txt
import kotlin.math.roundToInt

class Envy : BaseHullMod() {

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI, id: String) {
        stats.apply {
            maxCombatReadiness.modifyFlat(id, AMOUNT)
        }
    }

    override fun advanceInCampaign(member: FleetMemberAPI, amount: Float) {
        if (member.fleetCommander == null || member.fleetData?.fleet == null) return
        if (member.fleetCommander == Global.getSector().playerPerson || member.fleetData.fleet.isPlayerFleet) {
            member.variant.removePermaMod("tahlan_envy")
            member.variant.removeMod("tahlan_envy")
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> AMOUNT.roundToInt().toString() + txt("%")
            else -> null
        }
    }

    companion object {
        const val AMOUNT = 15f
    }
}