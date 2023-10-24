package org.niatahl.tahlan.skills

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.utils.Utils.txt

object Raketentanz   {
    class Level1a: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("RaketentanzL1a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            stats.missileAmmoBonus.modifyMult(id,2f)
            stats.missileAmmoRegenMult.modifyMult(id,2f)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.missileAmmoBonus.unmodify(id)
            stats.missileAmmoRegenMult.unmodify(id)
        }
    }

    class Level1b: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("RaketentanzL1b")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            stats.missileRoFMult.modifyMult(id,2f)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.missileRoFMult.unmodify(id)
        }
    }

    class Level1c: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return txt("RaketentanzL1c")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            stats.missileWeaponDamageMult.modifyMult(id,0.7f)
            stats.missileWeaponFluxCostMod.modifyMult(id, 0.7f)
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.missileWeaponDamageMult.unmodify(id)
            stats.missileWeaponFluxCostMod.unmodify(id)
        }
    }

}