package org.niatahl.tahlan.utils

import data.scripts.ptes_ModPlugin.FactionMap
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONS
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO

object ExiledSpaceIntegrations {
    fun ToggleDaemons(enable: Boolean) {
        if (enable) {
            FactionMap[LEGIO]?.subFactions?.put(DAEMONS,0.25f)
        } else {
            FactionMap[LEGIO]?.subFactions?.remove(DAEMONS)
        }
    }
}