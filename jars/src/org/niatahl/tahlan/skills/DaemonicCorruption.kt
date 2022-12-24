package org.niatahl.tahlan.skills

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import org.niatahl.tahlan.utils.Utils

object DaemonicCorruption {

    const val DAMAGE_MULT = 1.2f
    const val WEAPON_MULT = 1.1f
    const val SCALE_MULT = 0.05f

    class Level1a: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return Utils.txt("CorruptionLevel1a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String, level: Float) {
            stats.apply {
                energyWeaponDamageMult.modifyMult(id, DAMAGE_MULT)
                ballisticWeaponDamageMult.modifyMult(id, DAMAGE_MULT)
                missileWeaponDamageMult.modifyMult(id, DAMAGE_MULT)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            stats.apply {
                energyWeaponDamageMult.unmodify(id)
                ballisticWeaponDamageMult.unmodify(id)
                missileWeaponDamageMult.unmodify(id)
            }
        }
    }

    class Level1b: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return Utils.txt("CorruptionLevel1b")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String, level: Float) {
            stats.apply {
                armorDamageTakenMult.modifyMult(id, DAMAGE_MULT)
                hullDamageTakenMult.modifyMult(id, DAMAGE_MULT)
                shieldDamageTakenMult.modifyMult(id, DAMAGE_MULT)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            stats.apply {
                armorDamageTakenMult.unmodify(id)
                hullDamageTakenMult.unmodify(id)
                shieldDamageTakenMult.unmodify(id)
            }
        }
    }

    class Level1c: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return Utils.txt("CorruptionLevel1c")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String, level: Float) {
            stats.apply {
                when (hullSize) {
                    ShipAPI.HullSize.CRUISER -> {
                        damageToCapital.modifyMult(id, 1f + SCALE_MULT)
                    }
                    ShipAPI.HullSize.DESTROYER -> {
                        damageToCapital.modifyMult(id, 1f + SCALE_MULT*2f)
                        damageToCruisers.modifyMult(id, 1f + SCALE_MULT)
                    }
                    ShipAPI.HullSize.FRIGATE -> {
                        damageToCapital.modifyMult(id, 1f + SCALE_MULT*3f)
                        damageToCruisers.modifyMult(id, 1f + SCALE_MULT*2f)
                        damageToDestroyers.modifyMult(id, 1f + SCALE_MULT)
                    }
                }
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            stats.apply {
                damageToCapital.unmodify(id)
                damageToCruisers.unmodify(id)
                damageToDestroyers.unmodify(id)
            }
        }
    }

    class Level2a: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return Utils.txt("CorruptionLevel2a")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String, level: Float) {
            stats.apply {
                missileAmmoBonus.modifyPercent(id,50f)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            stats.apply {
                missileAmmoBonus.unmodify(id)
            }
        }
    }

    class Level2b: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return Utils.txt("CorruptionLevel2b")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String, level: Float) {
            stats.apply {
                ballisticRoFMult.modifyMult(id,WEAPON_MULT)
                energyRoFMult.modifyMult(id, WEAPON_MULT)
                missileRoFMult.modifyMult(id, WEAPON_MULT)
                ballisticWeaponRangeBonus.modifyMult(id, WEAPON_MULT)
                energyWeaponRangeBonus.modifyMult(id, WEAPON_MULT)
                missileWeaponRangeBonus.modifyMult(id, WEAPON_MULT)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            stats.apply {
                ballisticRoFMult.unmodify(id)
                energyRoFMult.unmodify(id)
                missileRoFMult.unmodify(id)
                ballisticWeaponRangeBonus.unmodify(id)
                energyWeaponRangeBonus.unmodify(id)
                missileWeaponRangeBonus.unmodify(id)
            }
        }
    }

    class Level2c: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return Utils.txt("CorruptionLevel2c")
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String, level: Float) {
            stats.apply {
                ballisticWeaponRangeBonus.modifyMult(id, WEAPON_MULT)
                energyWeaponRangeBonus.modifyMult(id, WEAPON_MULT)
                missileWeaponRangeBonus.modifyMult(id, WEAPON_MULT)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            stats.apply {
                ballisticWeaponRangeBonus.unmodify(id)
                energyWeaponRangeBonus.unmodify(id)
                missileWeaponRangeBonus.unmodify(id)
            }
        }
    }


}