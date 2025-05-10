package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.util.MagicIncompatibleHullmods
import org.niatahl.tahlan.utils.Utils
import org.niatahl.tahlan.utils.Utils.txt
import kotlin.math.roundToInt

class PhaseHarmonics : BaseHullMod() {
    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        val system = ship.system ?: return
        val stats = ship.mutableStats
        if (system.ammo == 0) {
            stats.ventRateMult.modifyMult(ID, VENT_MOD)
            stats.armorDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
            stats.hullDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
            stats.empDamageTakenMult.modifyMult(ID, DAMAGE_MOD)
        } else if (!ship.fluxTracker.isVenting) { // We allow the effect to linger during vent
            stats.ventRateMult.unmodify(ID)
            stats.armorDamageTakenMult.unmodify(ID)
            stats.hullDamageTakenMult.unmodify(ID)
            stats.empDamageTakenMult.unmodify(ID)
        }
    }



    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.variant.hasHullMod(HullMods.PHASE_ANCHOR)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, HullMods.PHASE_ANCHOR, txt("phaseHarmonics"))
        }
        if (ship.variant.hasHullMod(HullMods.ADAPTIVE_COILS)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, HullMods.ADAPTIVE_COILS, txt("phaseHarmonics"))
        }

        val shield = ship.shield ?: return
        shield.setRadius(ship.shieldRadiusEvenIfNoShield,"graphics/tahlan/fx/tahlan_savshield.png","graphics/tahlan/fx/tahlan_tempshield_ring.png")
    }

    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize?, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
        val pad = 10f
        tooltip.addSectionHeading(txt("pharm_weapon"),Alignment.MID,pad)
        val section_wep = tooltip.beginImageWithText("graphics/icons/hullsys/high_energy_focus.png",64f)
        section_wep.addPara(txt("pharm_weapontext1"),pad, Misc.getHighlightColor(), txt("pharm_weapontext1_hl1"))
        section_wep.addPara(txt("pharm_weapontext2"),pad, Misc.getHighlightColor(), txt("pharm_weapontext2_hl1"))
        tooltip.addImageWithText(pad)
        tooltip.addSectionHeading(txt("pharm_regen"),Alignment.MID,pad)
        val section_reg = tooltip.beginImageWithText("graphics/icons/hullsys/construction_swarm.png",64f)
        section_reg.addPara(txt("pharm_regentext1"),pad, Misc.getHighlightColor(), txt("pharm_regentext1_hl1"))
        section_reg.addPara(txt("pharm_regentext2"),pad, Misc.getHighlightColor(), txt("pharm_regentext2_hl1"))
        tooltip.addImageWithText(pad)
        tooltip.addSectionHeading(txt("pharm_defense"),Alignment.MID,pad)
        val section_def = tooltip.beginImageWithText("graphics/icons/hullsys/damper_field.png",64f)
        section_def.addPara(txt("pharm_defensetext1"),pad, Misc.getHighlightColor(), txt("pharm_defensetext1_hl1"), txt("pharm_defensetext1_hl2"))
        section_def.addPara(txt("pharm_defensetext2"),pad, Misc.getHighlightColor(), txt("pharm_defensetext2_hl1"))
        section_def.addPara(txt("pharm_defensetext3"),pad, Misc.getHighlightColor(), txt("pharm_defensetext3_hl1"))
        tooltip.addImageWithText(pad)

    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize, ship: ShipAPI): String? {
        return when (index) {
            0 -> txt("phaseBreaker")
            1 -> "${((1f - DAMAGE_MOD) * 100f).roundToInt()}${txt("%")}"
            2 -> "${((VENT_MOD - 1f) * 100f).roundToInt()}${txt("%")}"
            3 -> txt("phaseAnchor")
            else -> null
        }
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return false
    }

    companion object {
        private const val ID = "tahlan_phaseHarmonics"
        private const val VENT_MOD = 1.5f
        private const val DAMAGE_MOD = 0.8f
    }
}