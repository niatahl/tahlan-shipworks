package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import org.niatahl.tahlan.utils.Utils.txt
import java.awt.Color

class Notorious : BaseHullMod() {
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        if (!ship.variant.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)) {
            ship.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> txt("notable")
            else -> null
        }
    }

    override fun getNameColor(): Color {
        return Color.decode("F3BD7AFF")
    }
}