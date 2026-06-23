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

    /**
     * Normalized 0..1 interpolation factor for [intensity] across the full [INTENSITY_BASE, INTENSITY_MAX]
     * range. Scale fleet budgets as `BASE + SCALE * intensityFactor(intensity)` so a fresh campaign sits
     * at base strength and ramps to base+scale at max — anchoring on this (not on a hardcoded 1.0) is
     * what keeps the bottom half of the intensity range from clamping inert.
     */
    fun intensityFactor(intensity: Float): Float =
        ((intensity - INTENSITY_BASE) / (INTENSITY_MAX - INTENSITY_BASE)).coerceIn(0f, 1f)

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
    // Two distinct damage paths reduce siege health, NOT a partitioned pool:
    //   * Command fleet: contributes a flat chunk (SIEGE_HEALTH_MAX * COMMAND_HEALTH_SHARE) that is
    //     removed once, on its removal — whether killed or withdrawn (identical effect, per design).
    //     The command fleet does NOT take the per-FP path.
    //   * Escort / blockade / raid fleets: each death deals uncapped per-FP damage (fp / HEALTH_PER_FP),
    //     floored at 0. Their total is not capped to the remaining share — the floor handles overkill.
    // So removing the command fleet is the single biggest blow but never an instant break: the residual
    // (1 - COMMAND_HEALTH_SHARE) must still be mopped up via escort/blockade/raid kills to reach 0.
    var SIEGE_HEALTH_MAX = 100f
    var COMMAND_HEALTH_SHARE = 0.6f         // command chunk = 60% of max health; removed on kill/withdraw

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

    // --- Subjugation meter ---
    var CAPTURE_PROGRESS_MAX = 100f
    // Rate while besieging; multiplied by pressure factor AND command CR. The old fixed no-Nex
    // 6-month lifetime now lives in this fill-rate (both Nex / no-Nex are pure races, no clock).
    // BALANCE-PASS STARTING VALUE: bumped 0.3 -> 0.6 to absorb the retired no-Nex duration; confirm
    // in a dev-mode pace pass and record the final value in changelog.txt.
    var CAPTURE_PROGRESS_PER_DAY_BASE = 0.6f
    // Per FP of a destroyed escort/blockade/raid fleet, knock the subjugation meter back this much
    // (floored at 0). Scaled by the attrition slider in TahlanSettings.loadFromLuna.
    var CAPTURE_KNOCKBACK_PER_FP = 0.05f

    // --- No-Nex aftermath scar (applied on a successful no-Nex subjugation) ---
    // Scar penalties are SiegeCondition.{ACCESSIBILITY,STABILITY,HAZARD}_MOD * this fraction — i.e.
    // "half a siege", derived live so the scar always tracks the active siege penalty / any slider.
    var AFTERMATH_PENALTY_FRACTION = 0.5f
    // How long the scar condition lingers, and the disruption window for the target's core industries.
    var AFTERMATH_DURATION_DAYS = 120f

    // --- Nex garrison duration (~1 year) ---
    var GARRISON_DURATION_DAYS = 365f

    // --- Raid cadence ---
    var RAID_INTERVAL_DAYS = 25f
    var RAID_FP_BASE = 50f
    var RAID_FP_SCALE = 75f
    var MAX_ACTIVE_RAID_FLEETS = 3

    // --- Blockade interception ---
    // A blockade fleet breaks station to intercept a trade fleet whose destination is the besieged
    // market, or which has closed to within this range of it. Tuned wide enough to interdict before
    // the trader docks, but short enough that the blockade fleet drifts back to its jump point.
    var BLOCKADE_INTERCEPT_RANGE = 2500f
    var BLOCKADE_INTERCEPT_DURATION_DAYS = 10f
    // When true, blockade fleets are flagged hostile to all trade fleets ($cfai flag) so the
    // interception actually engages even neutral-faction traders — without it, INTERCEPT only
    // results in a fight against factions Legio is already hostile to.
    var BLOCKADE_HOSTILE_TO_TRADERS = true
}
