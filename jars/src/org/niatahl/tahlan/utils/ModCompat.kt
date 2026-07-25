package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global

/**
 * Soft-dependency presence flags, all detected once at application load — the mod list cannot
 * change within a running game, so app load is always safe. (Nexerelin used to be probed only in
 * onNewGame, which left HAS_NEX false for any session that started by *loading* a save and
 * silently disabled every load-time/runtime Nex integration, e.g. the siege capture pathway.)
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
        HAS_NEX = enabled("nexerelin")
        HAS_INDEVO = enabled("IndEvo")
        HAS_LUNA = enabled("lunalib")
    }
}
