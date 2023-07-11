package org.niatahl.tahlan.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL14.*
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.interpolateColor
import org.niatahl.tahlan.utils.modify
import org.niatahl.tahlan.utils.random
import org.niatahl.tahlan.weapons.SpearOnFireEffect
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CustomRender : BaseEveryFrameCombatPlugin() {

    enum class NebulaType {
        NORMAL, SWIRLY, SPLINTER, DUST
    }

    override fun init(engine: CombatEngineAPI) {
        nebulaData.clear()
        val layerRenderer: CombatLayeredRenderingPlugin = CustomRenderer(this)
        engine.addLayeredRenderingPlugin(layerRenderer)
    }

    // Ticking our lifetimes and removing expired
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine()
        if (engine.isPaused) return

        // tick and clean up nebula list
        val nebulaToRemove = ArrayList<Long>()
        nebulaData.forEach { (_, nebula) ->
            nebula.lifetime += engine.elapsedInLastFrame
            if (nebula.lifetime > nebula.duration)
                nebulaToRemove.add(nebula.id)
        }
        nebulaToRemove.forEach { nebulaData.remove(it) }

        // tick and clean up afterimage list
        val afterimageToRemove = ArrayList<Long>()
        afterimageData.forEach { (_, afterimage) ->
            afterimage.lifetime += engine.elapsedInLastFrame
            if (afterimage.lifetime > afterimage.duration)
                afterimageToRemove.add(afterimage.id)
        }
        afterimageToRemove.forEach { afterimageData.remove(it) }

        // clean up spear list
        val projToRemove = ArrayList<DamagingProjectileAPI>()
        effectProjectiles.forEach { if (!engine.isEntityInPlay(it)) projToRemove.add(it) }
        effectProjectiles.removeAll(projToRemove)
    }

    fun render(layer: CombatEngineLayers, view: ViewportAPI) {
        nebulaData
            .filter { (_, nebula) -> nebula.layer == layer }
            .forEach { (_, nebula) -> renderNebula(nebula, view) }

        // afterimages
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) afterimageData.forEach { (_, afterimage) ->
            renderAfterimage(afterimage, view)
        }

        // projectile effects
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) effectProjectiles.forEach { proj ->
            when (proj.projectileSpecId) {
                "tahlan_novaspear_shot", "tahlan_sunspear_shot" -> renderSpear(proj, view)
            }
        }
    }

    private fun renderSpear(proj: DamagingProjectileAPI, view: ViewportAPI) {
        if (!view.isNearViewport(proj.location, view.visibleWidth)) return
        val flare1 = Global.getSettings().getSprite("fx", "tahlan_novaspear_glow")
        val flare2 = Global.getSettings().getSprite("fx", "tahlan_novaspear_glow")
        val scale = if (proj.weapon.size == WeaponAPI.WeaponSize.LARGE) 1f else 0.6f

        flare1.apply {
            setAdditiveBlend()
            color = SpearOnFireEffect.PARTICLE_COLOR.modify(alpha = (80 * proj.brightness).roundToInt().coerceIn(0..255))
            angle = proj.facing - 90f
            setSize(100f * scale, 200f * scale)
        }
        flare1.renderAtCenter(proj.location.x, proj.location.y)

        flare2.apply {
            setAdditiveBlend()
            color = SpearOnFireEffect.GLOW_COLOR.modify(alpha = (80 * proj.brightness).roundToInt().coerceIn(0..255))
            angle = proj.facing - 90f
            setSize(120f * scale, 200f * scale)
        }
        val pos = MathUtils.getRandomPointInCircle(proj.location, 5f)
        flare2.renderAtCenter(pos.x, pos.y)
    }

    private fun renderAfterimage(afterimage: Afterimage, view: ViewportAPI) {
        if (!view.isNearViewport(afterimage.location, view.visibleWidth)) return

        // Sprite offset fuckery - Don't you love trigonometry?
        val sprite = afterimage.sprite

        sprite.angle = afterimage.facing - 90f
        sprite.color = afterimage.colorIn.interpolateColor(afterimage.colorOut, afterimage.lifetime / afterimage.duration)
        sprite.alphaMult = 1 - afterimage.lifetime / afterimage.duration
        sprite.setAdditiveBlend()

        if (!Global.getCombatEngine().isPaused && afterimage.jitter > 0f) {
            afterimage.actualLoc = MathUtils.getRandomPointInCircle(afterimage.location, afterimage.jitter)
        }

        sprite.renderAtCenter(afterimage.actualLoc.x, afterimage.actualLoc.y)
    }

    private fun renderNebula(nebula: Nebula, view: ViewportAPI) {
        if (!view.isNearViewport(nebula.location, view.visibleWidth)) return
        val cloudSprite = when (nebula.type) {
            NebulaType.NORMAL -> Global.getSettings().getSprite("misc", "nebula_particles")
            NebulaType.SWIRLY -> Global.getSettings().getSprite("misc", "fx_particles2")
            NebulaType.SPLINTER -> Global.getSettings().getSprite("misc", "fx_particles1")
            NebulaType.DUST -> Global.getSettings().getSprite("misc", "dust_particles")
        } ?: return

        var alpha = nebula.color.alpha
        if (nebula.lifetime < nebula.duration * nebula.inFraction) {
            alpha = (alpha * (nebula.lifetime / (nebula.duration * nebula.inFraction))).toInt().coerceIn(0, 255)
        } else if (nebula.lifetime > nebula.duration - nebula.duration * nebula.outFraction) {
            alpha =
                (alpha - alpha * ((nebula.lifetime - nebula.duration * (1f - nebula.outFraction)) / (nebula.duration * nebula.outFraction))).toInt()
                    .coerceIn(0, 255)
        }

        val actualSize =
            if (nebula.sqrt) {
                nebula.size + (nebula.endSize - nebula.size) * sqrt(nebula.lifetime / nebula.duration)
            } else {
                nebula.size + (nebula.endSize - nebula.size) * nebula.lifetime / nebula.duration
            }

        val lifeFraction = nebula.lifetime / nebula.duration
        cloudSprite.apply {
            color = nebula.color.interpolateColor(nebula.outColor, lifeFraction).modify(alpha = alpha)
            if (nebula.additive) setAdditiveBlend()
            angle = nebula.angle
            setSize(actualSize * 4f, actualSize * 4f)
        }

        val xIndex: Int = nebula.index % 4
        val yIndex = floor(nebula.index / 4f).toInt()
        var offsetPos = Vector2f(actualSize * (1.5f - xIndex), actualSize * (1.5f - yIndex))
        offsetPos = VectorUtils.rotate(offsetPos, nebula.angle)
        val actualLocation = Vector2f()
        val delta = Vector2f(nebula.velocity)
        Vector2f.add(nebula.location, delta.scale(nebula.lifetime) as Vector2f, actualLocation)

        // OpenGL witchcraft that I don't actually understand
        if (nebula.negative) glBlendEquation(GL_FUNC_REVERSE_SUBTRACT)

        cloudSprite.renderRegionAtCenter(
            actualLocation.x + offsetPos.x,
            actualLocation.y + offsetPos.y,
            0.25f * xIndex,
            0.25f * yIndex,
            0.25f,
            0.25f
        )

        // DO NOT FORGET TO TURN OFF FUNKY MODE
        if (nebula.negative) glBlendEquation(GL_FUNC_ADD)
    }

    data class Nebula(
        val id: Long,
        val location: Vector2f,
        val velocity: Vector2f,
        val size: Float,
        val endSize: Float,
        val duration: Float,
        val inFraction: Float,
        val outFraction: Float,
        val color: Color,
        val layer: CombatEngineLayers,
        val type: NebulaType,
        val negative: Boolean,
        val sqrt: Boolean,
        val outColor: Color,
        val additive: Boolean
    ) {
        var lifetime = 0f
        val index = (0..11).random()
        val angle = (0f..359f).random()
    }

    data class Afterimage(
        val id: Long,
        val sprite: SpriteAPI,
        val location: Vector2f,
        val facing: Float,
        val colorIn: Color,
        val colorOut: Color,
        val duration: Float,
        val jitter: Float
    ) {
        var lifetime = 0f
        var actualLoc = location
    }

    companion object {
        private val nebulaData = HashMap<Long, Nebula>()
        private val afterimageData = HashMap<Long, Afterimage>()
        private val effectProjectiles = ArrayList<DamagingProjectileAPI>()
        fun addProjectile(projectile: DamagingProjectileAPI) {
            effectProjectiles.add(projectile)
        }

        fun addAfterimage(
            ship: ShipAPI,
            colorIn: Color,
            colorOut: Color = colorIn,
            duration: Float,
            jitter: Float = 0f,
            location: Vector2f = Vector2f(ship.location)
        ) = Afterimage(
            id = Misc.random.nextLong(),
            sprite = Global.getSettings().getSprite(ship.hullSpec.spriteName),
            location = location,
            facing = ship.facing,
            colorIn = colorIn,
            colorOut = colorOut,
            duration = duration,
            jitter = jitter
        )
            .also { afterimage ->

                val sprite = ship.spriteAPI
                val offsetX = sprite.width / 2 - sprite.centerX
                val offsetY = sprite.height / 2 - sprite.centerY
                val trueOffsetX = FastTrig.cos(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetX - FastTrig.sin(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetY
                val trueOffsetY = FastTrig.sin(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetX + FastTrig.cos(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetY

                afterimage.location.x += trueOffsetX
                afterimage.location.y += trueOffsetY
                afterimage.actualLoc = afterimage.location

                afterimageData[afterimage.id] = afterimage
            }

        fun addNebula(
            location: Vector2f,
            velocity: Vector2f,
            size: Float,
            endSizeMult: Float,
            duration: Float,
            inFraction: Float,
            outFraction: Float,
            color: Color,
            layer: CombatEngineLayers = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER,
            type: NebulaType = NebulaType.NORMAL,
            negative: Boolean = false,
            expandAsSqrt: Boolean = false,
            outColor: Color = color,
            additive: Boolean = false
        ) = Nebula(
            id = Misc.random.nextLong(),
            location = Vector2f(location),
            velocity = Vector2f(velocity),
            size = size,
            endSize = endSizeMult * size,
            duration = duration,
            inFraction = inFraction,
            outFraction = outFraction,
            color = color,
            layer = layer,
            type = type,
            negative = negative,
            sqrt = expandAsSqrt,
            outColor = outColor,
            additive = additive
        )
            .also { nebula -> nebulaData[nebula.id] = nebula }
    }

    internal class CustomRenderer
        (private val parentPlugin: CustomRender) : BaseCombatLayeredRenderingPlugin() {
        override fun render(layer: CombatEngineLayers, view: ViewportAPI) {
            parentPlugin.render(layer, view)
        }

        override fun getRenderRadius(): Float {
            return 9.9999999E14f
        }

        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return EnumSet.allOf(CombatEngineLayers::class.java)
        }
    }
}