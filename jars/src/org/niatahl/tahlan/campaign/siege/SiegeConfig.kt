package org.niatahl.tahlan.campaign.siege

object SiegeConfig {
    // --- Spawn cadence ---
    var LAUNCH_INTERVAL_DAYS_MIN = 180f     // fastest possible interval
    var LAUNCH_INTERVAL_DAYS_MAX = 360f     // slowest interval (scaled by frequency mult)
    var ACTIVE_SIEGE_CAP = 2

    // --- Intensity scaling (elapsed campaign time + Legio strength, replaces currentCycle-206) ---
    var INTENSITY_BASE = 0.5f
    var INTENSITY_PER_YEAR = 0.1f           // +10% per elapsed in-game year
    var INTENSITY_PER_LEGIO_MARKET = 0.05f  // +5% per Legio-owned market
    var INTENSITY_MAX = 2.0f

    // --- Command fleet (Blackwatch) ---
    var COMMAND_FP_BASE = 150f              // fleet points at intensity 1.0
    var COMMAND_FP_SCALE = 150f             // bonus FP at max intensity
    var COMMAND_SMODS_BASE = 1
    var COMMAND_SMODS_MAX = 3

    // --- Escort fleet (standard Legio) ---
    var ESCORT_COUNT_BASE = 2
    var ESCORT_COUNT_MAX = 4
    var ESCORT_FP_BASE = 60f
    var ESCORT_FP_SCALE = 90f

    // --- Two-value health model ---
    var SIEGE_HEALTH_MAX = 100f
    var COMMAND_HEALTH_SHARE = 0.6f         // command fleet = 60% of total health; removed on kill/withdraw
    // Note: escort share = 1 - COMMAND_HEALTH_SHARE, distributed among escort/blockade/raid fleets

    // --- Attrition (strain coefficient k) ---
    // Per FP destroyed: siegeHealth -= fp / HEALTH_PER_FP; commandCR -= fp * STRAIN_K
    var HEALTH_PER_FP = 5f                  // 1 FP killed → -0.2 siege health
    var STRAIN_K = 0.003f                   // per FP killed, strain commandCR this much

    // --- CR model ---
    var CR_RECOVERY_DELAY_DAYS = 20f        // no losses for this long → start CR recovery
    var CR_RECOVERY_RATE_PER_DAY = 0.015f
    var COMMAND_CR_WITHDRAWAL_FLOOR = 0.25f // rational withdrawal threshold

    // --- Health regen (command fleet present only) ---
    // Actual regen/day = HEALTH_REGEN_PER_DAY_BASE * commandCR
    var HEALTH_REGEN_PER_DAY_BASE = 0.5f

    // --- Intel stage thresholds (commandCR) ---
    const val STAGE_ENTRENCHED_MIN_CR = 0.6f
    const val STAGE_STRAINED_MIN_CR = 0.35f
    // CR < STAGE_STRAINED_MIN_CR → Faltering

    // --- Bounty ---
    var COMMAND_FLEET_BOUNTY = 100_000f
    var ESCORT_BOUNTY_PER_FP = 200f

    // --- No-Nex finite lifetime (~6 months) ---
    var SIEGE_LIFETIME_NO_NEX_DAYS = 180f

    // --- Nex capture progress ---
    var CAPTURE_PROGRESS_MAX = 100f
    var CAPTURE_PROGRESS_PER_DAY_BASE = 0.3f   // rate while besieging; multiplied by pressure factor

    // --- Nex garrison duration (~1 year) ---
    var GARRISON_DURATION_DAYS = 365f

    // --- Raid cadence ---
    var RAID_INTERVAL_DAYS = 25f
    var RAID_FP_BASE = 50f
    var RAID_FP_SCALE = 75f
    var MAX_ACTIVE_RAID_FLEETS = 3
}
