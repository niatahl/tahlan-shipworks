package org.niatahl.tahlan.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Stats
import kotlin.math.roundToInt

object DaemonicWarfare {

    const val FLEET_SIZE = 50f
    const val DEFEND_BONUS = 50

    class Level1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT)
                .modifyFlat(id, FLEET_SIZE / 100f, "Daemonic Warfare")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String = "+${FLEET_SIZE.roundToInt()}% fleet size"

        override fun getEffectPerLevelDescription(): String? = null

        override fun getScopeDescription(): ScopeDescription = ScopeDescription.GOVERNED_OUTPOST
    }

    class Level2 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD)
                .modifyMult(id, 1f + DEFEND_BONUS * 0.01f, "Daemonic Warfare")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id)
        }

        override fun getEffectDescription(level: Float): String = "+$DEFEND_BONUS% effectiveness of ground defenses"

        override fun getEffectPerLevelDescription(): String? = null

        override fun getScopeDescription(): ScopeDescription = ScopeDescription.GOVERNED_OUTPOST
    }
}
