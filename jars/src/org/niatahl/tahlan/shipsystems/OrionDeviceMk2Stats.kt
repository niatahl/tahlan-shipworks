package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.impl.combat.OrionDeviceStats
import java.awt.Color

class OrionDeviceMk2Stats : OrionDeviceStats() {
    init {
        p = OrionDeviceParams()
        p.shapedExplosionColor = Color(59, 193, 255 , 155)
        p.jitterColor = Color(59, 193, 255 , 55)
        p.bombWeaponId = "tahlan_odmk2_bomblauncher"
    }
}