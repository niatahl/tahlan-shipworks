package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.plugins.MagicTrailPlugin

class WingTrailScript : EveryFrameWeaponEffectPlugin {
    private val effectInterval = IntervalUtil(0.05f, 0.05f)
    private var trailID: Float? = null
    private val trailSprite = Global.getSettings().getSprite("fx", "tahlan_trail_smooth")
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        if (engine.isPaused) {
            return
        }
        val ship = weapon.ship
        if (ship.isHulk || !ship.isAlive || ship.isPiece) {
            return
        }

        effectInterval.advance(engine.elapsedInLastFrame)
        val angle = Misc.getAngleInDegrees(Vector2f(ship.velocity))
        val color = weapon.spec.glowColor
        val opacity = weapon.ship.velocity.length() / weapon.ship.maxSpeed
        val startSize = weapon.spec.hardpointAngleOffsets[0]
        val endSize = weapon.spec.hardpointAngleOffsets[1]
        val duration = weapon.spec.hardpointAngleOffsets[2]
        val opacityMult = weapon.spec.hardpointAngleOffsets[3]
        if (effectInterval.intervalElapsed()) {
            if (trailID == null) {
                trailID = MagicTrailPlugin.getUniqueID()
            }
            MagicTrailPlugin.addTrailMemberSimple(
                ship,
                trailID!!,
                trailSprite,
                weapon.location,
                0f,
                angle,
                startSize,
                endSize,
                color,
                opacity * opacityMult,
                0f,
                0f,
                duration,
                true
            )
        }
    }
}