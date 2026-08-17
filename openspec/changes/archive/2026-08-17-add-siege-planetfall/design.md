# Design: add-siege-planetfall

## Context

Under Nexerelin, `SiegeManager.advanceHealthModel` resolves a full subjugation meter by calling
`attemptNexCapture` in the same tick: story-protection recheck, `SectorManager.transferMarket`
(7-arg overload, so Nex posts its generic `MarketTransferIntel`), garrison key written to the
command fleet's memory, `resolveSiege(SUCCEEDED, keepCommandFleet = true)`. The command fleet is
orbiting a jump point at the system fringe (`anchorAtFringe`) when the planet changes hands; the
market transfers pristine; the player has no window to witness or interrupt the climax.

Existing machinery this design builds on:

- **Stage state machine** on `SiegeData` (`INBOUND / BESIEGING / BROKEN / LIFTED / SUCCEEDED`),
  advanced by `advanceHealthModel` and consumed by `pruneDeadSieges`, `maintainPressureConditions`,
  and the reactive systems (F1 huntsmen, F2 bounty, F3 interventions).
- **Fleet-memory signaling** between manager and per-fleet AIs (`FLEET_RETURN_FLAG`,
  `FLEET_GARRISON_MARKET_KEY`), deliberately avoiding direct references for save-safety.
- **Deferred battle-callback resolution** (`pendingKills` → `flushKill`), which already handles
  "command fleet died → resolve immediately" for the INBOUND stage.
- **User decisions locked in conversation:** station is starved out (not fought), interventions
  turn back (navy has given up), command-fleet loss mid-planetfall breaks the siege instantly.

## Goals / Non-Goals

**Goals:**

- A visible, timed final act: the siege force converges on the planet before the capture fires.
- A genuine last-chance window: the player (and only the player) can still break the siege during
  planetfall, and decapitating the landing is decisively rewarded.
- The fiction of a starved, abandoned target: defenders flee, the station stands down, help stops
  coming.
- A single, Legio-voiced success beat and a market that reads as conquered, not administratively
  reassigned.

**Non-Goals:**

- The no-Nex devastation path keeps its instant resolution (explicit user scoping).
- No use of Nex's invasion machinery (`InvasionIntel`, ground battles) — Nex invasions can fail,
  which contradicts the meter-as-guarantee design; the capture pathway stays parallel to Nex's own
  systems, as the existing spec requires.
- No changes to how the meter fills, siege health works, or sieges launch.

## Decisions

### D1: Planetfall is a new `SiegeData.Stage`, not a sub-state of BESIEGING

`Stage.PLANETFALL` sits between BESIEGING and the terminal stages. A new stage (vs. a boolean on
BESIEGING) is chosen because nearly every consumer of stage needs *different* behavior during
planetfall: `pruneDeadSieges` must treat it as live (**trap:** its current filter removes any siege
not INBOUND/BESIEGING as "already resolved" — PLANETFALL must be added to the live set),
`maintainPressureConditions` should keep sweeping it (the system is still strangled),
`flushKill` needs a planetfall-specific decapitation rule, and the reactive systems all stand down.
A distinct enum value makes each site's intent explicit and greppable.

New `SiegeData` fields: `planetfallTimer: Float` (counts down from
`SiegeConfig.PLANETFALL_DURATION_DAYS`) and `defenderSweepCooldown: Float`. Both are primitives on
the serialized nested class — old saves deserialize them as `0f`, which is never read because an
old save can never be in PLANETFALL. No `@Transient`/lazy-getter dance needed (that pattern is only
required for non-null *object* fields on the manager itself).

### D2: Entry point replaces the instant-capture branch

In `advanceHealthModel`, the meter-full Nex-capturable branch (`attemptNexCapture(siege)`) becomes
`beginPlanetfall(siege)`. The no-Nex/blocked/protected branch is untouched. `beginPlanetfall`:

1. Sets stage, arms the timer, pings the intel (`notifyPlanetfall`).
2. Re-tasks the command fleet via a new fleet-memory key `FLEET_PLANETFALL_KEY` (value: target
   planet entity id) — same pattern as the garrison key, read independently by `SiegeAssignmentAI`.
3. Directly re-assigns alive escort/blockade/raid fleets to `ORBIT_AGGRESSIVE` on the target planet
   ("supporting landing operations"), skipping any fleet currently in battle (caught by the next
   sweep). Also sets `FLEET_PLANETFALL_KEY` on them so `SiegeBlockadeAI` stands down (see D4).
4. Flags alive intervention fleets home (`FLEET_RETURN_FLAG`) — their AI already routes them back
   to their own markets.
5. Disrupts the station (see D6) and runs the first defender sweep (see D5).

Fallback: if `primaryTargetMarket?.primaryEntity` is null (no planet to orbit), skip the stage and
call `attemptNexCapture` directly — degrade to current behavior rather than strand the siege.

### D3: Planetfall tick — what keeps running, what stops

A dedicated `advancePlanetfall(siege, days)` branch in the manager's per-siege loop:

| Keeps running | Stops |
|---|---|
| Loss accounting + CR strain (stage-agnostic callbacks) | New raid sorties |
| Siege-health damage; **health regen too** (command coordination) | New intervention waves (F3) |
| LIFTED validity checks (peace / deciv / ownership / Legio-owned) | Bounty keep-alive (F2; lapses on its vanilla duration) |
| Heat decay + marking (F1) | Meter knockback (`CAPTURE_KNOCKBACK_PER_FP`) |
| Pressure conditions on system markets | Meter advancement (already at max) |

Resolution rules during PLANETFALL:

- `siegeHealth <= 0` → BROKEN (unchanged path; `checkBroken` extended to accept PLANETFALL).
- Command fleet destroyed (battle or despawn, via `flushKill`) → **instant BROKEN** — mirrors the
  existing INBOUND decapitation rule. Remaining fleets disperse home via `resolveSiege`.
- Command CR at withdrawal floor → **instant BROKEN**, not `triggerWithdrawal`: driven-off equals
  killed here (per user decision), and the mop-up machinery (health-chunk drop, escort re-anchor,
  withdrawal factor) is meaningless when the whole force is about to scatter anyway.
- Timer expires → `attemptNexCapture` (existing code: story-protection recheck, transfer, garrison
  handoff), **deferred while the command fleet is in a battle** (`commandFleet.battle != null`) so
  the transfer never fires mid-fight the player is winning.

Meter knockback is suppressed by a stage check in `onSiegeFleetLosses` — otherwise the intel bar
would recede while the capture no longer reads it, lying to the player about what helps.

### D4: Fleet AI signaling stays memory-keyed

- `SiegeAssignmentAI` gains `Phase.PLANETFALL`: from BESIEGING, seeing `FLEET_PLANETFALL_KEY` →
  `clearAssignments`, `ORBIT_AGGRESSIVE` over the planet ("making planetfall on X"). The garrison
  check (currently BESIEGING-only) moves so it also runs in PLANETFALL phase — that is the normal
  handoff now. `FLEET_RETURN_FLAG` is already checked before the phase switch, covering BROKEN.
- `SiegeBlockadeAI` checks `FLEET_PLANETFALL_KEY` on its own fleet: when set, prey-picking stops
  (interdiction is moot) and its hold-station anchor becomes the planet instead of the jump point,
  so its assignment-reassertion logic cooperates with the convergence order instead of fighting it.
- Raid fleets carry no AI script; the manager's direct re-assignment in D2 suffices.
- The huntsman task force needs no changes: its idle station is "screen the command fleet," so it
  follows the convergence for free.

### D5: Defender sweep — repeating, narrowly scoped

On planetfall start and every `DEFENDER_SWEEP_INTERVAL_DAYS` (~2 days; fresh patrols keep spawning
from the market's own military base), sweep fleets in the target system. A fleet gets flee orders
iff **all** hold:

- Faction is the victim's or in `coalitionFactions(victimId)` (existing helper).
- Hostile to Legio; **not** the player faction (the player's navy gives up only when the player
  does — player markets are valid siege targets).
- Military, not civilian: `MEMORY_KEY_PATROL_FLEET` or `MEMORY_KEY_WAR_FLEET`; trade fleets are
  untouched.
- Not a station fleet (`$stationFleet` — that is the station, handled by D6), not carrying
  `FLEET_SIEGE_ID_KEY` (siege/intervention/task-force fleets have their own signaling).
- Not currently in a battle (skip; next sweep catches it).

Flee orders: `MEMORY_KEY_MAKE_ALLOW_DISENGAGE` + `MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED`
(expiring, sized to the planetfall duration), `clearAssignments`, then
`GO_TO_LOCATION_AND_DESPAWN` at the nearest same-faction market **outside** the system, falling
back to the nearest jump point. Despawn-at-destination is what makes the navy *gone* rather than
loitering; vanilla respawn economics replace them later as normal.

Alternative considered: suppressing the market's `MilitaryResponseScript`/patrol spawning directly —
rejected as deeper vanilla surgery with save-attached scripts; the repeating sweep achieves the
visible result with local, reversible state.

### D6: Station starved out via industry disruption

At planetfall start, disrupt the target market's station industry — identified by industry spec tag
(`Industries.TAG_STATION`) rather than an id whitelist, so modded stations are covered — for
`PLANETFALL_DURATION_DAYS + STATION_DISRUPTION_EXTRA_DAYS` (~30 extra). Disruption empties the
station of its fleet, so the converged siege force does not engage it and a Star Fortress cannot
randomly wreck the scripted climax. The disruption carries through the transfer, so Legio inherits
a *recovering* station — reading as battle-scarred, and softening an immediate retake attempt only
partially. `setDisrupted(..., useMax = true)` per the existing aftermath code, never shortening an
existing disruption.

### D7: Owned success moment + occupation aftermath

`attemptNexCapture` switches to the 8-arg `transferMarket` overload with `silent = true`,
suppressing Nex's generic `MarketTransferIntel`. In its place:

- `SiegeIntel.resolve(SUCCEEDED)` prose becomes the single beat: Legio-flavored capture text, with
  a distinct variant when `playerBountyEarned > 0` (the player fought this siege and lost it).
  Strings externalized (`siege_intel_resolved_succeeded*` reworked + new keys).
- Occupation aftermath, applied right after transfer: core-industry disruption reusing the
  `applyNoNexAftermath` loop shape but with its own shorter duration
  (`OCCUPATION_DISRUPTION_DAYS`, ~60), plus vanilla `RecentUnrest` on the market. The no-Nex scar
  *condition* is deliberately **not** applied — its accessibility/stability penalties are designed
  to punish a still-defender-owned market, whereas this market is now Legio's; disruption + unrest
  express "conquered" without a nonsense penalty on the conqueror.

### D8: Intel presentation

- `notifyPlanetfall()`: one-time factor (adverse, negative-highlight) + feed ping — the single
  loudest moment of the event short of resolution.
- `syncProgress` gains planetfall display state (days remaining); `afterStageDescriptions` renders
  a countdown line while in planetfall ("Legio ground forces are landing — the planet falls in
  ~N days").
- CLIMAX stage strings change from "instant capture" to "planetfall window" phrasing; the Nex
  climax stage label becomes "Planetfall". All text in `strings.json` under `tahlan` (`siege_*`).
- `debugDump` (`SiegeConsoleCommands` / manager) reports the new stage, timer, and sweep cooldown.

## Risks / Trade-offs

- [Player fights the command fleet as the timer expires] → capture completion is deferred while the
  command fleet is in battle; the fight always resolves first.
- [Defender sweep yanks a fleet mid-battle] → in-battle fleets are skipped and caught by the next
  sweep; never `clearAssignments` on a fighting fleet (project rule).
- [Fresh patrols spawn during planetfall and attack the landing] → acceptable between sweeps
  (reads as skirmishing stragglers); the ~2-day sweep cadence bounds it.
- [Instant-BROKEN makes planetfall the optimal time to strike] → accepted and intended: the stakes
  are highest there, and reaching planetfall means the player already let the meter fill. The
  command fleet is also strongest-guarded at that moment (whole force converged around it).
- [Suppressing Nex's transfer intel hides the capture from players who ignore siege intel] → the
  SUCCEEDED resolution still pings the feed via `sendUpdateIfPlayerHasIntel`; Nex's diplomacy/war
  bookkeeping is unaffected by `silent`.
- [Station disruption is a real economic hit if the siege is then broken mid-planetfall] → accepted:
  a siege that reached planetfall *should* leave a mark even when heroically broken; duration is a
  config knob if play-testing says it stings too much.
- [New knobs unvalidated] → all new numerics are BALANCE-PASS STARTING VALUES per the config file's
  existing convention; confirm in a dev-mode pass (console `TahlanSiegeStart` + setting
  `captureProgress` near max) and record finals in `changelog.txt`.

## Migration Plan

No migration needed. Save-compat analysis:

- New enum constant on a serialized enum is safe (XStream resolves by name; old saves never
  reference the new constant).
- New primitive fields on `SiegeData` deserialize as `0f`/`false` on old saves and are only read
  in PLANETFALL, which old saves cannot be in.
- Mid-save feature disable (`tearDown`) already handles any live stage via `resolveSiege(LIFTED)`;
  PLANETFALL needs no special teardown (the return/dispersal flags cover the converged fleets).
- Rollback: reverting the jar restores instant capture; a save mid-planetfall would resolve on the
  old code's rules (stage value unknown to old code is the only hazard — **do not** ship this
  change in a point release players are expected to downgrade from; note in changelog).

## Open Questions

- `PLANETFALL_DURATION_DAYS` default (5 vs 7): start at 6, tune in the balance pass against how far
  away a responding player can realistically be after the Stranglehold/Climax warnings.
- Whether `RecentUnrest` magnitude should scale with siege intensity — start flat, revisit if
  captured markets stabilize implausibly fast.
