package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.lazywizard.lazylib.combat.CombatUtils

class TemporalHarmonics : BaseHullMod() {
    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        val engine = Global.getCombatEngine()
        val player = ship == engine.playerShip

        ACTIVE[ship.id]?.mutableStats?.timeMult?.unmodify(EFFECT_ID + ship.id)
        ACTIVE.remove(ship.id)

        var max = 1f
        var target: ShipAPI? = null
        CombatUtils.getShipsWithinRange(ship.location, RANGE)
            .filter { !it.isPhased && it.isAlive && it == ship }
            .forEach { other ->
                val otherMult = other.mutableStats.timeMult.modifiedValue
                if (otherMult > max) {
                    max = otherMult
                    target = other
                }
            }

        max =- 1f
        if (target != null) {
            ship.mutableStats.timeMult.modifyFlat(EFFECT_ID, max / 2f)
            target!!.mutableStats?.timeMult?.modifyFlat(EFFECT_ID + ship.id, -max / 2f)
            ACTIVE[ship.id] = target!!
        }

        if (player)
            engine.timeMult.modifyFlat(EFFECT_ID, -max / 2f)
    }

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        ACTIVE.clear()
    }

    companion object {
        const val EFFECT_ID = "tahlan_temporalHarmonicsMod"
        const val RANGE = 500f
        val ACTIVE = HashMap<String,ShipAPI>()
    }
}