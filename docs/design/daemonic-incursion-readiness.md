# Daemonic Incursion — Readiness Scoring

How the natural Legio awakening decides it's time. Implemented in `TahlanTrigger`
(`TahlanModPlugin.kt`), rolled on `reportEconomyMonthEnd`.

## Design intent

The trigger is a **readiness check, not a provocation system**. It is deliberately
independent of the player's relationship with the Legio — no notoriety, no "you poked
them" signals. The sole question is: *is the player established enough to survive the
added challenge of the awakened Legio?* When the answer is yes, the betrayal fires.

The original implementation sampled seven binary conditions (cycle, one size-5 market,
gates, level, two capitals, 5M in the bank, 8 officers) each month and fired at 4. Two
problems drove the redesign:

1. **Snapshot sampling mis-measures readiness.** Readiness is cumulative: a player who
   banks 5M and spends it founding a colony has *converted* readiness into a more durable
   form, yet the old check scored them lower for it. Capitals parked in storage on roll
   day were invisible. Credits fluctuated across the threshold month to month.
2. **Playstyle bias.** "Two capitals" is a proxy for "can field a serious battle line" —
   but an elite phase-frigate fleet, among the strongest playstyles per DP, scored zero
   on it. No-colony players could never earn the market point; story-skippers never earn
   gates. Some playstyles hit the threshold years late or never.

## The scoring model

Score = sum over axes, rolled monthly. Threshold **5** (fast mode **3**).

| Axis | Metric | Points | Latched? |
|---|---|---|---|
| Military — fleet | Fleet-wide DP sum, tiers 60 / 120 / 180 | 0–3 | high-water |
| Military — officers | Sum of non-merc officer levels ≥ 24 | 0–1 | high-water |
| Economic — wealth | Credits ≥ 5,000,000 | 0–1 | high-water |
| Economic — colonies | Sum of player market sizes ≥ 8 | 0–1 | high-water |
| Progression — level | Player level ≥ 13 | 0–1 | monotonic anyway |
| Progression — story | Gates scanned & active | 0–1 | monotonic anyway |
| Time backstop | 1 pt at cycle 210, +1 per 3 cycles, **uncapped** | 0–∞ | monotonic anyway |

Instant trigger unchanged: a `tahlan_*_dmn` hull in the player fleet fires the incursion
immediately — owning a daemon is the strongest possible proof of readiness.

### High-water latching ("broken seals")

The volatile metrics (fleet DP, officer levels, credits, colony size) are stored in
sector memory as high-water marks (`$tahlan_hwFleetDP`, `$tahlan_hwOfficerLevels`,
`$tahlan_hwCredits`, `$tahlan_hwColonySize`) and only ever increase. Readiness once
demonstrated never un-demonstrates. This kills both snapshot problems at once: spending
wealth doesn't lower the score, and hiding ships in storage doesn't dodge the roll.

### DP tiers, benchmarked against the challenge

The thresholds are derived from what the awakened Legio actually fields rather than
picked in the abstract. Daemon DP costs (`ship_data.csv`): frigates 10–12, destroyers
16–18, cruisers 35, capitals 45–65.

- **60 DP** ≈ a solid cruiser squadron — can handle daemon prowler packs.
- **120 DP** ≈ a real battle line, the old "two capitals plus escorts" equivalence
  (2 × ~45 + escorts) — can fight a medium daemon patrol.
- **180 DP** ≈ a war fleet — can face a large patrol (Hel capital + cruiser core +
  screen, ~200+ DP) head-on, given player-side quality.

Because it's a fleet-wide DP sum, a 15-frigate elite doomstack and a two-capital wall
register at their true weight. Capital-counting (and the Metafalica double-count) is
gone — it falls out of DP naturally.

### Axis reachability by playstyle

Every archetype has a path to 5 without contorting:

- **Colonizer** (fleet 2, colonies 1, level 1, time 1) — triggers mid-game, same as before.
- **Elite small-fleet** (fleet 1–2, officers 1, wealth 1, level 1) — now visible; the old
  system could miss this player indefinitely.
- **Capital-less trader magnate** (wealth 1, colonies 1, level 1, time 2+) — triggers late
  but reliably.
- **Hermit** — the uncapped time term alone crosses the threshold by cycle 222 (fast: 216).
  "The Legio doesn't wait forever" is the pacing floor; nobody is structurally exempt
  from the content anymore.

### Time as a growing term

Cycle 210 (~4 years in) is worth 1 point, +1 every 3 cycles, no cap. Two readings, both
intended: past a certain date the player has had every opportunity to get ready, and the
Legio's patience is finite. The growth rate (not the base) is the main lever if the
late-game floor feels too early/late.

## What deliberately stayed

- **Monthly cadence** on `reportEconomyMonthEnd` — cheap, save-safe, no new scripts.
- **Threshold-count shape** with fast mode lowering it (5 → 3 ≈ the old 4 → 2 halving).
- **Daemon-hull instant trigger** and the **planetkiller-gift suppression** logic
  (`$tahlan_gavePKtoLegio` / `$tahlan_pkStrikeResolved`) — see `PlanetkillerStrike.kt`.
- **Silent awakening** — no player-facing message when the seal count crosses; the
  betrayal being discovered in play is intentional.
- **No Legio-relationship signals** — see design intent above.

## Save compatibility

Existing un-triggered saves simply start latching from their current state on the next
month-end roll; no migration needed. Already-triggered saves are unaffected (the roll
short-circuits on `$tahlan_triggered`).

## Tuning knobs

All constants live in `TahlanTrigger`'s companion object: `FLEET_DP_TIERS`,
`OFFICER_LEVEL_SUM`, `WEALTH`, `COLONY_TOTAL_SIZE`, `PLAYER_LEVEL`, `TIME_BASE_CYCLE`,
`TIME_STEP_CYCLES`, `THRESHOLD`, `THRESHOLD_FAST`. The monthly roll logs the full
component breakdown at INFO (`Daemonic Incursion readiness N: ...`), so a playtest
campaign's log shows exactly which axes carried the score.
