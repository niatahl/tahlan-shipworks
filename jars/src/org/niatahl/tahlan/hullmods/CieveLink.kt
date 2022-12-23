package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.utils.Utils
import org.niatahl.tahlan.utils.Utils.txt


class CieveLink : BaseHullMod() {

    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: ShipAPI.HullSize?, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
        val pad = 10f

        if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_cieveRecruited")) {
            tooltip.addPara(txt("hmcieveunknown"),pad,Misc.getPositiveHighlightColor())
            return
        }

        tooltip.addSectionHeading(txt("hmcieve"), Alignment.MID, pad)
        tooltip.addPara(txt("hmcievetext"),pad,Misc.getEnergyMountColor(), txt("cieve"))

        tooltip.addSectionHeading(txt("hypercoordination"), Alignment.MID, pad)

        val section = tooltip.beginImageWithText("graphics/tahlan/icons/skills/hypercoordination.png", 64f)
        section.addPara(txt("baseEffect"), 0f, Misc.getEnergyMountColor(), txt("baseEffect"))
        section.addPara(txt("hypercoordination_L1_desc"),0f, Misc.getPositiveHighlightColor(), txt("hypercoordination_L1_desc_hl1"))
        if ((TahlanPeople.getPerson(TahlanPeople.CIEVE)?.stats?.getSkillLevel("tahlan_hyperCoordination") ?: 0f) == 2f) {
            section.addPara(txt("eliteEffect"), 10f, Misc.getEnergyMountColor(), txt("eliteEffect"))
            section.addPara(txt("hypercoordination_L2_desc"), 0f, Misc.getPositiveHighlightColor(), txt("hypercoordination_L2_desc_hl1"), txt("hypercoordination_L2_desc_hl2"))
        }
        tooltip.addImageWithText(pad)
    }
}