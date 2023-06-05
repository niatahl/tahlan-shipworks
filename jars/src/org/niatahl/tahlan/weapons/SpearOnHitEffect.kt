package org.niatahl.tahlan.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.plugins.MagicTrailPlugin
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.modify
import org.niatahl.tahlan.utils.random
import java.awt.Color
import kotlin.math.roundToInt

class SpearOnHitEffect : OnHitEffectPlugin {
    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI?, point: Vector2f, shieldHit: Boolean, damageResult: ApplyDamageResultAPI, engine: CombatEngineAPI) {
        engine.spawnExplosion(point, Misc.ZERO, projectile.projectileSpec.fringeColor, projectile.projectileSpec.length, 0.3f)
        for (i in 1..3) engine.addSwirlyNebulaParticle(
            projectile.location,
            Misc.ZERO,
            projectile.projectileSpec.length * (i / 4f),
            3f,
            0.1f,
            0.2f,
            1.5f,
            NEBULA_COLOR.modify(red = 30 + i*10),
            true
        )

        //Benediction-esque on-hit effects, but in a very tight cone, and faster
        val spriteToUse = Global.getSettings().getSprite("fx", "tahlan_trail_zappy")
        val count = (projectile.projectileSpec.length / 3).roundToInt()
        for (i1 in 0..count) {
            //Color randomization
            val colorToUse = Misc.interpolateColor(COLOR1, COLOR2, (0f..0.5f).random())
            val id = MagicTrailPlugin.getUniqueID()
            val angle = (1f..360f).random()
            val startSpeed = MathUtils.getRandomNumberInRange(0f, 900f)
            val startAngularVelocity = MathUtils.getRandomNumberInRange(-300f, 300f)
            val startSize = MathUtils.getRandomNumberInRange(17f, 39f)
            val lifetimeMult = MathUtils.getRandomNumberInRange(0.4f, 0.7f)
            for (i2 in 0..69) {
                //This is for "end fizzle"
                val fizzleConstantSpeed = (-20f..20f).random()
                val fizzleConstantAngle = (-40f..40f).random()
                MagicTrailPlugin.addTrailMemberAdvanced(
                    null, id, spriteToUse, projectile.location,
                    startSpeed * (i2.toFloat() / 70f), fizzleConstantSpeed * (1f - i2.toFloat() / 70f),
                    angle, startAngularVelocity * (i2.toFloat() / 70f), fizzleConstantAngle * (1f - i2.toFloat() / 70f), startSize, 0f,
                    colorToUse, colorToUse, 0.45f, 0.1f, 0.1f * (i2.toFloat() / 70f) * lifetimeMult, 0.4f * (i2.toFloat() / 70f) * lifetimeMult,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE, 500f, 600f, 0f, Vector2f(0f, 0f), null, CombatEngineLayers.CONTRAILS_LAYER, 1f
                )
            }
        }
    }

    companion object {
        private val COLOR1 = Color(255, 130, 30)
        private val COLOR2 = Color(255, 246, 234)
        private val NEBULA_COLOR = Color(30, 30, 100)
    }
}