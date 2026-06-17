package org.niatahl.tahlan.utils

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import org.json.JSONException
import org.niatahl.tahlan.campaign.siege.SiegeConfig
import java.io.IOException

/**
 * Player-facing toggles. Defaults come from tahlan_settings.json ([loadFromJson], called at
 * application load); when LunaLib is enabled its live values override them ([loadFromLuna],
 * called on new game / game load - LunaLib wins, per the mod's convention).
 *
 * Save-lock vs toggle distinction follows tahlan_settings.json - keep that in mind when adding
 * new settings. `@JvmField` so Java call sites read these as plain static fields.
 */
object TahlanSettings {
    @JvmField var ENABLE_LETHIA = false
    @JvmField var ENABLE_LEGIO = false

    @JvmField var ENABLE_FASTMODE = false
    @JvmField var ENABLE_LIFELESS = false
    @JvmField var ENABLE_LEGIOBPS = false
    @JvmField var ENABLE_DAEMONS = false
    @JvmField var ENABLE_SIEGE = true

    @JvmField var INDEVO_MINES = true
    @JvmField var INDEVO_ARTY = true

    @JvmField var ENABLE_HARDMODE = false
    @JvmField var ENABLE_ADAPTIVEMODE = false
    @JvmField var WEEB_MODE = false

    /** Load defaults from tahlan_settings.json. */
    @Throws(IOException::class, JSONException::class)
    fun loadFromJson() {
        val setting = Global.getSettings().loadJSON(TahlanIDs.SETTINGS_FILE)
        ENABLE_LETHIA = setting.getBoolean("enableLethia")
        ENABLE_LEGIO = setting.getBoolean("enableLegio")
        ENABLE_LIFELESS = setting.getBoolean("enableLifelessShips")
        ENABLE_LEGIOBPS = setting.getBoolean("enableLegioBlueprintLearning")
        ENABLE_HARDMODE = setting.getBoolean("enableHardMode")
        ENABLE_ADAPTIVEMODE = setting.getBoolean("enableAdaptiveMode")
        ENABLE_DAEMONS = setting.getBoolean("enableDaemons")
        ENABLE_FASTMODE = setting.getBoolean("enableFastmode")
        WEEB_MODE = setting.getBoolean("enableWaifu")
        ENABLE_SIEGE = setting.optBoolean("enableSiege", true)
    }

    /** Override from LunaLib live settings (caller confirms LunaLib is enabled). */
    fun loadFromLuna() {
        ENABLE_LETHIA = LunaSettings.getBoolean("tahlan", "tahlan_enable_lethia") ?: true
        ENABLE_LEGIO = LunaSettings.getBoolean("tahlan", "tahlan_enable_legio") ?: true
        ENABLE_FASTMODE = LunaSettings.getBoolean("tahlan", "tahlan_enable_fastmode") ?: false
        ENABLE_DAEMONS = LunaSettings.getBoolean("tahlan", "tahlan_enable_daemons") ?: true
        ENABLE_HARDMODE = LunaSettings.getBoolean("tahlan", "tahlan_enable_hardmode") ?: false
        ENABLE_ADAPTIVEMODE = LunaSettings.getBoolean("tahlan", "tahlan_enable_adaptivemode") ?: true
        ENABLE_LEGIOBPS = LunaSettings.getBoolean("tahlan", "tahlan_enable_legiobps") ?: false
        ENABLE_LIFELESS = LunaSettings.getBoolean("tahlan", "tahlan_enable_lifeless") ?: false
        INDEVO_MINES = LunaSettings.getBoolean("IndEvo", "IndEvo_Enable_minefields") ?: true
        INDEVO_ARTY = LunaSettings.getBoolean("IndEvo", "IndEvo_Enable_Artillery") ?: true

        // Siege balance sliders (guard: caller has already confirmed LunaLib)
        ENABLE_SIEGE = LunaSettings.getBoolean("tahlan", "tahlan_enable_siege") ?: true
        val freqMult = LunaSettings.getDouble("tahlan", "tahlan_siege_frequency")?.toFloat() ?: 1f
        val diffMult = LunaSettings.getDouble("tahlan", "tahlan_siege_difficulty")?.toFloat() ?: 1f
        val attrMult = LunaSettings.getDouble("tahlan", "tahlan_siege_attrition")?.toFloat() ?: 1f
        SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN = (180f / freqMult).coerceIn(30f, 720f)
        SiegeConfig.LAUNCH_INTERVAL_DAYS_MAX = (360f / freqMult).coerceIn(60f, 1440f)
        // "Siege Fleet Size" scales every siege fleet — command, escorts, and raid waves
        SiegeConfig.COMMAND_FP_BASE = 150f * diffMult
        SiegeConfig.COMMAND_FP_SCALE = 150f * diffMult
        SiegeConfig.ESCORT_FP_BASE = 60f * diffMult
        SiegeConfig.ESCORT_FP_SCALE = 90f * diffMult
        SiegeConfig.RAID_FP_BASE = 50f * diffMult
        SiegeConfig.RAID_FP_SCALE = 75f * diffMult
        // "Siege Attrition Strength" — higher = losses hurt more. Strains command CR harder AND
        // drains more siege health per FP killed (inverse: lower HEALTH_PER_FP = more damage/kill).
        SiegeConfig.STRAIN_K = 0.003f * attrMult
        SiegeConfig.HEALTH_PER_FP = (5f / attrMult).coerceAtLeast(0.5f)
    }
}
