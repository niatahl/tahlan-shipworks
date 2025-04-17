package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.niatahl.tahlan.utils.TahlanIDs.BLACKWATCH
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.TahlanPeople

class Scathach : BaseHullMod() {
    override fun advanceInCampaign(member: FleetMemberAPI, amount: Float) {
        if (member.captain == null || member.captain.isDefault) {
            member.captain = TahlanPeople.getPerson(TahlanPeople.SCATHACH)
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.fleetCommander == null) return

        if (!ship.fleetCommander.faction.id.equals(BLACKWATCH) && !ship.fleetCommander.faction.id.equals(LEGIO)) return

        ship.mutableStats.apply {
            empDamageTakenMult.modifyMult(id, 0.25f)
            armorDamageTakenMult.modifyMult(id, 0.75f)
            turnAcceleration.modifyMult(id, 1.25f)
            maxTurnRate.modifyMult(id, 1.25f)
            timeMult.modifyMult(id, 1.1f)
            energyWeaponDamageMult.modifyMult(id, 1.2f)
            ballisticWeaponDamageMult.modifyMult(id, 1.2f)
            missileAmmoBonus.modifyMult(id, 10f)
        }
    }
}