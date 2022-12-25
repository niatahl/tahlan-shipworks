package org.niatahl.tahlan.listeners

import com.fs.starfarer.api.campaign.CargoAPI

interface CargoTabListener {
    fun reportCargoOpened(cargo: CargoAPI)
    fun reportCargoClosed(cargo: CargoAPI)
}