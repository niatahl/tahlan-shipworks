package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global

/**
 * Soft-dependency presence flags. GraphicsLib/IndEvo/LunaLib are detected at application
 * load; Nexerelin is detected at new-game time (its random-sector mode matters there, and
 * the original code probed it in onNewGame - timing preserved).
 *
 * `@JvmField` so Java call sites read these as plain static fields
 * (e.g. `ModCompat.HAS_GRAPHICSLIB`), matching the old TahlanModPlugin access.
 */
object ModCompat {
    @JvmField var HAS_GRAPHICSLIB = false
    @JvmField var HAS_NEX = false
    @JvmField var HAS_INDEVO = false
    @JvmField var HAS_LUNA = false

    private fun enabled(id: String) = Global.getSettings().modManager.isModEnabled(id)

    /** Detect soft deps available at application load. */
    fun detectAtAppLoad() {
        HAS_GRAPHICSLIB = enabled("shaderLib")
        HAS_INDEVO = enabled("IndEvo")
        HAS_LUNA = enabled("lunalib")
    }

    /** Nexerelin presence; probed at new-game time to mirror the original timing. */
    fun detectNexerelin() {
        HAS_NEX = enabled("nexerelin")
    }
}
