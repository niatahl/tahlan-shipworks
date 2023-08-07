package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

class SavethShield : BaseHullMod() {

    private val INNERLARGE = "graphics/tahlan/fx/tahlan_savshield.png"
    private val OUTERLARGE = "graphics/tahlan/fx/tahlan_savshield.png"
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
    }
}