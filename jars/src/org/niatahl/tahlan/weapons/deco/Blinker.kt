package org.niatahl.tahlan.weapons.deco

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.util.IntervalUtil
import org.niatahl.tahlan.plugins.TahlanModPlugin.Companion.HAS_GRAPHICSLIB
import org.niatahl.tahlan.utils.GraphicLibEffects.customLight

class Blinker : EveryFrameWeaponEffectPlugin {

    var runOnce = false
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val ship = weapon.ship ?: return

        weapon.sprite.setAdditiveBlend()

        ship.addListener(BlinkerListener(ship))
    }

    class BlinkerListener(val ship: ShipAPI) : AdvanceableListener {

        val interval = IntervalUtil(0.5f,0.5f)
        var frame = 0
        override fun advance(amount: Float) {
            interval.advance(amount)

            if (!ship.isAlive || ship.fluxTracker.isOverloadedOrVenting) {
                ship.allWeapons
                    .filter { it.spec.weaponId.contains("tahlan_blinker") }
                    .forEach { it.animation.frame = 0 }
                return
            }

            if (interval.intervalElapsed()) {

                frame = if (frame == 3) {
                    interval.setInterval(1f,1f)
                    0
                } else {
                    interval.setInterval(0.2f,0.2f)
                    frame + 1
                }

                ship.allWeapons
                    .filter { it.spec.weaponId.contains("tahlan_blinker") }
                    .forEach { blinker ->
                        blinker.animation.frame = frame
                        if (frame == 1 && HAS_GRAPHICSLIB)
                            customLight(
                                blinker.location,
                                ship,
                                10f,
                                1f,
                                blinker.spec.glowColor,
                                0f,
                                0.5f,
                                0f
                            )
                    }
            }
        }
    }

}