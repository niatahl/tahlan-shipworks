package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.impl.campaign.missions.cb.*


class DevilCustomBounty : BaseCustomBounty() {
    override fun getCreators(): MutableList<CustomBountyCreator> {
        return CREATORS
    }

    override fun updateInteractionDataImpl() {
        super.updateInteractionDataImpl()
        val id = getMissionId()
        if (showData != null && showCreator != null) {
            if (showData.fleet != null) {
                val p = showData.fleet.commander
                set("$" + id + "_targetRank", p.rank)
            }
        }
    }

    companion object {
        val CREATORS = mutableListOf<CustomBountyCreator>(
            CBMerc(),
            CBPirate(),
            CBPather(),
            CBRemnant()
        )
    }
}