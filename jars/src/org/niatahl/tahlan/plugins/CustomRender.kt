package org.niatahl.tahlan.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.util.vector.Vector2f
import org.niatahl.tahlan.utils.modify
import org.niatahl.tahlan.utils.random
import java.awt.Color
import java.util.*
import kotlin.math.floor


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

        val toRemove: MutableList<Nebula> = ArrayList()
        nebulaData.forEach { nebula ->
            nebula.lifetime += engine.elapsedInLastFrame
            if (nebula.lifetime > nebula.duration)
                toRemove.add(nebula)
        }
        nebulaData.removeAll(toRemove)
    }

    fun render(layer: CombatEngineLayers, view: ViewportAPI) {
        nebulaData.filter { it.layer == layer }.forEach { renderNebula(it, view) }
    }

    private fun renderNebula(nebula: Nebula, view: ViewportAPI) {
        if (!view.isNearViewport(nebula.location, view.visibleWidth)) return
        val cloudSprite = when (nebula.type) {
            NebulaType.NORMAL -> Global.getSettings().getSprite("misc", "nebula_particles")
            NebulaType.SWIRLY -> Global.getSettings().getSprite("misc", "fx_particles2")
            NebulaType.SPLINTER -> Global.getSettings().getSprite("misc", "fx_particles1")
            NebulaType.DUST -> Global.getSettings().getSprite("misc","dust_particles")
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
            if (nebula.endSizeMult > 1f)
                nebula.size + nebula.size * (nebula.endSizeMult - 1f) * (nebula.lifetime / nebula.duration) * 2f
            else
                nebula.size - nebula.size * (1f - nebula.endSizeMult) * (nebula.lifetime / nebula.duration) * 2f

        cloudSprite.apply{
            color = nebula.color.modify(alpha = alpha)
            setAdditiveBlend()
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
        if (nebula.negative) {
            glBlendEquation(GL_FUNC_REVERSE_SUBTRACT)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE)
        }

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

    private data class Nebula(
        val location: Vector2f,
        val velocity: Vector2f,
        val size: Float,
        val endSizeMult: Float,
        val duration: Float,
        val inFraction: Float,
        val outFraction: Float,
        val color: Color,
        val layer: CombatEngineLayers,
        val type: NebulaType,
        val negative: Boolean
    ) {
        var lifetime = 0f
        val index = (0..11).random()
        val angle = (0f..359f).random()
    }

    companion object {
        private val nebulaData: MutableList<Nebula> = ArrayList()
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
            negative: Boolean = false
        ) {
            val newNebula =
                Nebula(Vector2f(location), Vector2f(velocity), size, endSizeMult, duration, inFraction, outFraction, color, layer, type, negative)
            nebulaData.add(newNebula)
        }
    }

    internal class CustomRenderer
        (private val parentPlugin: CustomRender) : BaseCombatLayeredRenderingPlugin() {
        override fun render(layer: CombatEngineLayers, view: ViewportAPI) {
            val engine = Global.getCombatEngine() ?: return
            parentPlugin.render(layer, view)
        }

        override fun getRenderRadius(): Float {
            return 999999999999999f
        }

        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return EnumSet.allOf(CombatEngineLayers::class.java)
        }
    }
}