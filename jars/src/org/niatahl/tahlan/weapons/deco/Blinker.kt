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
        if (runOnce) return

        val ship = weapon.ship ?: return

        weapon.sprite.setAdditiveBlend()

        ship.addListener(BlinkerListener(ship))

        runOnce = true
    }

    class BlinkerListener(val ship: ShipAPI) : AdvanceableListener {

        private val intervalA = IntervalUtil(0.1f, 0.1f)
        private val intervalB = IntervalUtil(0.5f, 0.5f)
        private var frameA = 0
        private var frameB = 0

        override fun advance(amount: Float) {
            intervalA.advance(amount)
            intervalB.advance(amount)

            if (!ship.isAlive || ship.fluxTracker.isOverloadedOrVenting) {
                ship.allWeapons
                    .filter { it.spec.weaponId.contains("tahlan_blinker") }
                    .forEach { it.animation.frame = 0 }
                return
            }

            if (intervalA.intervalElapsed()) {

                frameA = if (frameA == 3) {
                    intervalA.setInterval(1.5f, 1.5f)
                    0
                } else {
                    intervalA.setInterval(0.1f, 0.1f)
                    frameA + 1
                }

                ship.allWeapons
                    .filter { it.spec.weaponId.contains("tahlan_blinker_") }
                    .forEach { blink(it, frameA) }
            }

            if (intervalB.intervalElapsed()) {

                frameB = if (frameB == 3) {
                    intervalB.setInterval(1.5f, 1.5f)
                    0
                } else {
                    intervalB.setInterval(0.1f, 0.1f)
                    frameB + 1
                }

                ship.allWeapons
                    .filter { it.spec.weaponId.contains("tahlan_blinkerB_") }
                    .forEach { blink(it, frameB) }

            }
        }

        private fun blink(blinker: WeaponAPI, frame: Int) {
            blinker.animation.frame = frame
            if (HAS_GRAPHICSLIB && (frame == 1 || frame == 3))
                customLight(
                    blinker.location,
                    ship,
                    40f,
                    0.5f,
                    blinker.spec.glowColor,
                    0f,
                    0.5f,
                    0f
                )
        }
    }
}
