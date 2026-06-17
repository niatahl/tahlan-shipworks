package org.niatahl.tahlan.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import org.dark.shaders.util.ShaderLib
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.niatahl.tahlan.utils.ModCompat
import java.util.EnumSet

/**
 * Cosmetic phase-linked shimmer for the Dreamweaver (tahlan_nxa).
 *
 * Each frame it re-renders the hull sprite through a GraphicsLib fragment shader
 * on the ABOVE_SHIPS layer; the shader paints scrolling procedural-noise "veins"
 * confined to the ship silhouette. Brightness tracks the phase cloak, so the ship
 * glows as it slips in and out of phase.
 *
 * Purely visual. Requires GraphicsLib (a soft dep) - without it the hullmod is
 * inert and the ship renders normally. Technique adapted from RAT's Gilgamesh
 * phase-shield overlay; see docs/design/dreamweaver-shimmer.md.
 */
class DreamShimmer : BaseHullMod() {

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        // Combat-only effect; nothing to do in the refit/campaign context.
        if (Global.getCombatEngine() == null) return
        // Shader overlay needs GraphicsLib. Bail cleanly if absent - the ship is unaffected.
        if (!ModCompat.HAS_GRAPHICSLIB) return
        Global.getCombatEngine().addLayeredRenderingPlugin(DreamShimmerRenderer(ship))
    }

    override fun shouldAddDescriptionToTooltip(hullSize: ShipAPI.HullSize?, ship: ShipAPI?, isForModSpec: Boolean) = false

    private class DreamShimmerRenderer(private val ship: ShipAPI) : BaseCombatLayeredRenderingPlugin() {

        private val shader = ShaderLib.loadShader(
            Global.getSettings().loadText("data/shaders/tahlan_baseVertex.shader"),
            Global.getSettings().loadText("data/shaders/tahlan_dreamshimmer.shader")
        )

        init {
            if (shader != 0) {
                GL20.glUseProgram(shader)
                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "tex"), 0)
                GL20.glUseProgram(0)
            }
        }

        override fun getActiveLayers(): EnumSet<CombatEngineLayers> =
            EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER)

        override fun getRenderRadius(): Float = 1_000_000f

        override fun isExpired(): Boolean = !ship.isAlive

        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
            if (shader == 0 || !ship.isAlive) return

            // Phase-linked: gentle baseline shimmer that flares as the ship phases in/out.
            val phaseLevel = ship.phaseCloak?.effectLevel ?: 0f
            val intensity = 0.25f + 0.9f * phaseLevel

            val sprite = ship.spriteAPI

            GL20.glUseProgram(shader)
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "iTime"), Global.getCombatEngine().getTotalElapsedTime(false) / 12f)
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "alphaMult"), 1f)
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "intensity"), intensity)

            // Bind the ship's own texture to unit 0 for the shader's silhouette mask.
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.textureId)

            sprite.setAdditiveBlend()
            sprite.alphaMult = 1f
            sprite.angle = ship.facing - 90f
            sprite.renderAtCenter(ship.location.x, ship.location.y)

            GL20.glUseProgram(0)
        }
    }
}
