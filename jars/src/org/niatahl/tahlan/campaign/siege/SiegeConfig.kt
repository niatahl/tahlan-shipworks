package org.niatahl.tahlan.campaign.siege

object SiegeConfig {
    // --- Spawn cadence ---
    var LAUNCH_INTERVAL_DAYS_MIN = 180f     // fastest possible interval
    var LAUNCH_INTERVAL_DAYS_MAX = 360f     // slowest interval (scaled by frequency mult)

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
    //   * Escort / blockade / raid fleets: every fleet point they LOSE deals uncapped per-FP damage
    //     (fp / HEALTH_PER_FP), floored at 0 — partial losses count as they happen, not only deaths.
    //     Their total is not capped to the remaining share — the floor handles overkill.
    // So removing the command fleet is the single biggest blow but never an instant break: the residual
    // (1 - COMMAND_HEALTH_SHARE) must still be mopped up via escort/blockade/raid kills to reach 0.
    var SIEGE_HEALTH_MAX = 100f
    var COMMAND_HEALTH_SHARE = 0.6f         // command chunk = 60% of max health; removed on kill/withdraw

    // --- Attrition (strain coefficient k) ---
    // Per FP destroyed: siegeHealth -= fp / HEALTH_PER_FP; commandCR -= fp * STRAIN_K
    var HEALTH_PER_FP = 5f                  // 1 FP lost → -0.2 siege health
    // BALANCE-PASS NOTE: strain is now booked on *partial* losses as well as kills, and the command
    // fleet's own losses strain CR too (previously only escort deaths did). Total strain per siege is
    // therefore materially higher than when this value was picked — the pace at which CR reaches
    // COMMAND_CR_WITHDRAWAL_FLOOR needs a play-check, and this may want lowering.
    var STRAIN_K = 0.003f                   // per FP lost, strain commandCR this much

    // --- CR model ---
    var CR_RECOVERY_DELAY_DAYS = 20f        // no losses for this long → start CR recovery
    var CR_RECOVERY_RATE_PER_DAY = 0.015f
    var COMMAND_CR_WITHDRAWAL_FLOOR = 0.25f // rational withdrawal threshold

    // --- Health regen (command fleet present only) ---
    // Actual regen/day = HEALTH_REGEN_PER_DAY_BASE * commandCR
    var HEALTH_REGEN_PER_DAY_BASE = 0.5f

    // --- Stall backstops (a siege must never be able to persist forever) ---
    // Mop-up stall: the command fleet is gone but nobody is finishing off the residual fleets. Reset
    // by every siege-fleet loss, partial ones included (daysSinceLastLoss), so an actively-ground
    // siege never times out.
    // BALANCE-PASS STARTING VALUE.
    var MOPUP_STALL_TIMEOUT_DAYS = 60f
    // Travel timeout: insurance against an expedition that never reaches its target system and would
    // otherwise leave the siege stuck INBOUND (which also blocks all future launches).
    // BALANCE-PASS STARTING VALUE.
    var INBOUND_TIMEOUT_DAYS = 180f

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

    // --- Planetfall (the Nex capture climax) ---
    // Every numeric here is a BALANCE-PASS STARTING VALUE — confirm in a dev-mode pass and record
    // finals in changelog.txt.
    //
    // The window is the player's entire last chance, so it is sized against how far away a fleet
    // that heeded the Stranglehold/Climax warnings can realistically be — not against how long a
    // landing would plausibly take.
    var PLANETFALL_DURATION_DAYS = 6f
    // The victim's own military base keeps spawning patrols throughout the window, so the flee sweep
    // repeats instead of running once at planetfall start.
    var DEFENDER_SWEEP_INTERVAL_DAYS = 2f
    // Station disruption deliberately outlasts the landing: Legio inherits a *recovering* station,
    // which reads as battle-scarred and only partly softens an immediate retake.
    var STATION_DISRUPTION_EXTRA_DAYS = 30f

    // --- Occupation aftermath (applied to a market captured under Nex) ---
    // Shorter than AFTERMATH_DURATION_DAYS on purpose: the no-Nex scar punishes a market that held
    // out, while this only has to make a fresh conquest read as conquered.
    var OCCUPATION_DISRUPTION_DAYS = 60f
    // Stability points of vanilla RecentUnrest, which decays on its own one-point-per-90-days clock.
    // Flat rather than intensity-scaled for now — revisit if captured markets stabilize implausibly fast.
    var OCCUPATION_UNREST_POINTS = 4

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

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Reactive systems. Each gates on its own enable flag so a misbehaving one can be switched
    // off (config or LunaLib) without reverting code. Every numeric below is a
    // BALANCE-PASS STARTING VALUE — confirm in a dev-mode pass and record finals in changelog.txt.
    // ─────────────────────────────────────────────────────────────────────────────────────────

    // --- F3: Coalition interventions ---
    var INTERVENTION_ENABLED = true
    // Per-siege cooldown between mobilization waves. Sized against a ~110-day unopposed siege
    // (CAPTURE_PROGRESS_PER_DAY_BASE * typical pressure), so a full siege sees roughly two waves.
    var INTERVENTION_INTERVAL_DAYS = 50f
    // Primary fleet is sized to CONTEST the command fleet, never to reliably crush it — the player
    // must stay relevant. Capped by the lead member's response capacity (see below).
    var INTERVENTION_PRIMARY_FP_MULT = 0.9f
    // Fleet points per point of response capacity. Doubles as the capacity cap on the primary:
    // a weak or distant lead cannot field a full-sized contesting fleet however badly it wants to.
    // Capacity typically runs ~2..11, so this yields ~40..220 FP.
    var INTERVENTION_AUX_FP_PER_CAPACITY = 20f
    // Coalition members below this capacity send nothing — a size-3 rimworld dribbling out two
    // frigates reads as noise, not as a rescue.
    var INTERVENTION_AUX_CAPACITY_FLOOR = 4f
    // Command CR strained per FP of an intervention fleet destroyed fighting the siege. Deliberately
    // far below STRAIN_K: a failed rescue softens the siege for the next attacker, it does not break
    // it. At 150 FP that is ~0.15 CR, so ~2 failed interventions eat half the way to the
    // withdrawal floor.
    var INTERVENTION_STRAIN_K = 0.001f

    // --- F2: Desperation system bounty ---
    var BOUNTY_ENABLED = true
    // Subjugation-meter value at which the target market posts its bounty. Default = the
    // Stranglehold stage marker on the intel bar, so the feed item lands as the colony starts failing.
    var BOUNTY_TRIGGER_PROGRESS = 66f
    // Scales the vanilla base-bounty formula. 0 = pure vanilla amount, no scaling applied at all.
    // The siege additionally scales by intensity on top of this (1x at base intensity, 2x at max).
    var BOUNTY_BASE_REWARD_MULT = 1f
    // How often the posted bounty is refreshed (vanilla duration is 60 days, so this must stay
    // comfortably under it) while the command fleet still holds the system.
    var BOUNTY_KEEPALIVE_INTERVAL_DAYS = 20f

    // --- F1: Huntsman task force ---
    var TASKFORCE_ENABLED = true
    // ~1/2..2/3 of command FP: a real elite threat, not a second command fleet.
    var TASKFORCE_FP_BASE = 80f
    var TASKFORCE_FP_SCALE = 80f
    // Daemon rosters are stronger per FP by design (regen armor, daemoncore). 1.0 accepts that
    // step-up as the intended post-awakening escalation; lower it if testing says it is too much.
    var TASKFORCE_DAEMON_FP_MULT = 1.0f
    // The task force's identity is mechanical: fleet burn is the slowest member, so a single
    // capital would make it uncatchable-by-nobody. 9 keeps frigates/destroyers and the faster
    // cruisers on both rosters (daemon hulls run burn 7..10; Blackwatch draws on wide vanilla tags).
    var TASKFORCE_MIN_BURN = 9f
    // Respite window after a kill = this delay PLUS travel from the siege source market, so the
    // real window is geographic: sieges near Legio space refill fast, deep-rim ones leave weeks.
    var TASKFORCE_REDISPATCH_DELAY_DAYS = 30f
    // Bounded re-rolls when the burn filter strips the fleet under budget.
    var TASKFORCE_BUILD_RETRIES = 4

    // --- F1: Player heat (marking) ---
    // Heat accrues per FP of siege fleet the player is involved in destroying, and decays daily.
    // At threshold 150 / accrual 1.0 the player is marked after roughly 1.5 escort fleets' worth of
    // killing; at decay 3/day a marked player cools off over ~50 quiet days.
    var HEAT_PER_FP = 1f
    var HEAT_DECAY_PER_DAY = 3f
    var HEAT_MARK_THRESHOLD = 150f
    // Hysteresis: once marked, the player stays marked until heat falls to this fraction of the
    // threshold. Without it a player sitting near the line would flicker marked/unmarked every tick.
    var HEAT_UNMARK_FRACTION = 0.5f
}
