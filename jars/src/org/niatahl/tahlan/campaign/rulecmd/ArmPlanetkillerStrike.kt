package org.niatahl.tahlan.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.campaign.PKStrikeConfig
import org.niatahl.tahlan.campaign.PlanetkillerStrikeWatcher
import org.niatahl.tahlan.plugins.TahlanModPlugin
import org.niatahl.tahlan.utils.TahlanIDs

/**
 * Fired from rules.csv when the player hands a Domain planetkiller to the Legio Infernalis.
 * Awakens the Legio (enabling daemons) and arms the delayed planetkiller strike. See
 * [org.niatahl.tahlan.campaign.PlanetkillerStrikeWatcher].
 */
class ArmPlanetkillerStrike : BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: List<Misc.Token?>,
        memoryMap: Map<String, MemoryAPI>
    ): Boolean {
        val sector = Global.getSector()
        val mem = sector.memoryWithoutUpdate

        // Quietly enable the Legio's daemons WITHOUT turning them hostile — the betrayal is delayed
        // until the planetkiller strike actually lands (awakenLegioHostility fires then).
        TahlanModPlugin.enableDaemons()

        // Arm the one-shot strike (the timer always starts now).
        if (!mem.getBoolean(TahlanIDs.PK_STRIKE_ARMED) && !mem.getBoolean(TahlanIDs.PK_STRIKE_FIRED)) {
            mem.set(TahlanIDs.PK_STRIKE_ARMED, true)
            val delay = PKStrikeConfig.DELAY_DAYS_MIN +
                Misc.random.nextFloat() * (PKStrikeConfig.DELAY_DAYS_MAX - PKStrikeConfig.DELAY_DAYS_MIN)
            sector.addScript(PlanetkillerStrikeWatcher(delay))
        }
        return true
    }
}
