## Context

The `redesign-legio-siege` change (complete, 42/42) built a fleet-only siege driven by `SiegeManager`
(`BaseCampaignEventListener` + `EveryFrameScript`) with a two-value model — `siegeHealth` (drops on
kills → BROKEN) and `commandCR` (strain/recovery, withdrawal floor). It already tracks a Nex-only
`captureProgress` and exposes a text intel (`SiegeIntel : BaseIntelPlugin`) whose only signal is a
CR-derived stage word. The vanilla colony-crisis event framework
(`com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel`, modelled by `HostileActivityEventIntel`)
provides a richer, idiomatic UI: a horizontal progress bar with discrete stage markers, per-stage prose,
and two factor tables ("Monthly" / "Recent one-time"). This change adopts that UI and, to make the bar
mean the same thing in both modes, unifies the siege's progress onto a single meter — which also gives
the no-Nex path a natural "success" trigger, the hook for the new lasting-consequence requirement.

Constraints: no CLI build (IntelliJ artifacts only); reuse vanilla `events` sprites (no new art); all
player-facing prose via `Utils.txt`; respect save/load (state in `SiegeManager`/`SiegeData`, intel
persisted by the IntelManager); LunaLib/Nexerelin stay soft, guarded.

## Goals / Non-Goals

**Goals:**
- Render the siege as a colony-crisis event: a bar filling toward the siege's goal, stage markers,
  factor tables, bounty — driven by the existing `SiegeManager` state, not by the event framework's own
  economy-tick loop.
- Unify `captureProgress` into one subjugation meter that advances in both Nex and no-Nex, is multiplied
  down by command CR, and is knocked back by fleet kills — so the bar is honest in both modes.
- Give a successful no-Nex siege a lasting consequence on the primary target market: a half-siege scar
  condition plus core-industry disruption for the scar's duration.
- Keep every tuning value a named `SiegeConfig` constant; derive the scar from the siege penalty.

**Non-Goals:**
- No change to the health/CR/withdrawal/bounty model, the Nex capture/garrison path, fleet spawning,
  target selection, or teardown.
- No new ship art or sprites; reuse vanilla `events` sprites.
- Not letting the event framework drive progress or fire consequences — `SiegeManager` stays the single
  source of truth.
- No scar on non-target markets (the blockade already starves them) and no scar in the Nex-capture case
  (the capture *is* the outcome).

## Decisions

### Decision: `SiegeManager` authoritative, `SiegeIntel` is a `BaseEventIntel` renderer
`SiegeIntel` extends `BaseEventIntel` but does not use the framework's progress driver. `SiegeManager`
pushes the bar value each BESIEGING tick via a new `intel.syncProgress(siege)`; every `EventFactor`
returns `getProgress() == 0` so it never mutates `progress`; `reportEconomyTick(int)` is overridden to a
no-op. *Rationale*: the manager is freshly built and tuned and ticks every frame, computing CR recovery,
accessibility-scaled pressure, and raid cadence continuously; re-deriving monthly deltas for the
framework's economy-tick model would be a re-tune and a bug surface for zero gameplay gain. The manager
already has push hooks (`intel?.updateStage/addPlayerBounty/resolve`). *Alternative considered*: move
progression into `reportEconomyTick`/`notifyStageReached` (factors drive progress) — rejected: it forks
the tuned resolution logic.

### Decision: One subjugation meter (`captureProgress`), driving the bar in both modes
Keep the field name `captureProgress` (avoids a serialized-field rename) but widen its meaning to "how
subjugated the system is," 0..`CAPTURE_PROGRESS_MAX`. In `advanceHealthModel` it advances in **both**
modes while the command fleet is present:
`captureProgress += CAPTURE_PROGRESS_PER_DAY_BASE * pressureMult * commandCR * days`. The new
`* commandCR` is the CR brake the UI shows. On reaching the cap it branches: Nex + capturable →
`attemptNexCapture()` (unchanged); else → `applyNoNexAftermath()` then `resolveSiege(SUCCEEDED)`.
*Rationale*: the bar reads identically in both modes and the no-Nex path gains a real success trigger.
*Alternative considered*: keep no-Nex on a timer and make the bar a clock — rejected: a clock can't be
knocked back by kills, so the bar would be cosmetic in no-Nex.

### Decision: Drop the fixed no-Nex lifetime; both modes are pure races
Remove the no-Nex 6-month `LIFTED`-on-expiry trigger (and the Nex-protected-target timer fallback). The
meter only climbs while the command fleet is present, and removing the command fleet also halts regen, so
the residual is always mopped to BROKEN — there is no stalemate to time out. The old "6 months" migrates
into the meter fill-rate. `LIFTED` is retained only for clean teardown (toggle off) and the rare
Nex-protected edge. *Rationale*: symmetry between modes is the whole point of the accumulator. *Risk
note*: see balance below.

### Decision: Kills knock the meter back; CR is the all-progress brake
In `onSiegeFleetKilled`, an escort/blockade/raid kill subtracts `fleetFp * CAPTURE_KNOCKBACK_PER_FP` from
`captureProgress` (floored at 0); a command-fleet kill needs no explicit knock-back because it flips
`commandFleetPresent = false`, freezing the meter. Each kill also pushes a one-time `FleetKillFactor` to
the intel (display-only, `getProgress()==0`, shown green). Command CR appears as a monthly
`CommandReadinessFactor` whose `getAllProgressMult()` returns `commandCR` (green when < 1 — inverted vs
`HAColonyDefensesFactor`, since here a low multiplier helps the player). *Rationale*: realizes the chosen
UX honestly in both modes; the health model (BROKEN path) is untouched, so kills meaningfully feed *both*
meters.

### Decision: No-Nex scar = half a siege, derived live, on the target market only
`applyNoNexAftermath(siege)` runs **before** `resolveSiege` (which clears `conditionedMarkets`) and acts
on `primaryTargetMarket` only:
- Add `tahlan_siegeaftermath` if absent. Its stat mods are
  `SiegeCondition.{ACCESSIBILITY_MOD, STABILITY_MOD, HAZARD_MOD} * AFTERMATH_PENALTY_FRACTION` (0.5),
  read live — the scar is always "half a siege" and tracks any future change or LunaLib slider on the
  siege penalty. `SiegeAftermathCondition` self-expires from `advance()` after `AFTERMATH_DURATION_DAYS`
  via `market.removeSpecificCondition(condition.idForPluginModifications)`, `isTransient() = false` so the
  elapsed counter persists.
- Disrupt the market's **core industries for the full scar duration**: iterate `market.getIndustries()`,
  skip `!canBeDisrupted()` and the population/spaceport infrastructure, and `setDisrupted(dur, true)` each
  with `dur = AFTERMATH_DURATION_DAYS * jitter` (`useMax=true` so an existing disruption is only extended;
  jitter via `StarSystemGenerator.getNormalRandom(random, 1f, 1.25f)` as vanilla raids do).
*Rationale*: one fraction governs the whole scar; disruption lasting the scar duration (rather than a
separate shorter window) makes the consequence read as a single coherent "occupation hangover." Only the
target is scarred — other in-system markets already suffer blockade trade shortages.
*Alternative considered*: weighted top-1–2 industries for a short fixed window — rejected per the
"core industries for the scar duration" decision; harsher and simpler to reason about.

### Decision: Stage model and save-compat for the superclass change
`maxProgress = 100`; stages `START(0)`, `FOOTHOLD(33, MEDIUM)`, `STRANGLEHOLD(66, MEDIUM)`,
`CLIMAX(100, oneOff, LARGE)`; no `setRandomized` (deterministic stages keep icon/tooltip on the
non-random branch). `START` at 0 is mandatory (the bar dereferences the last active stage with no null
guard). CLIMAX prose/labels branch on the stored `hasNex`. Reuse `events` sprites
(`stage_unknown_bad`, `hostile_activity`). Add `readResolve()` (mirroring
`HostileActivityEventIntel.readResolve`) that rebuilds `setup()` when `stages` is null/empty, so an
in-flight siege from an existing save renders rather than crashing. Keep the `SiegeStage` and
`SiegeOutcome` enums (serialized / referenced by the manager).

### Decision: New config, derived and LunaLib-friendly
Add to `SiegeConfig`: `CAPTURE_KNOCKBACK_PER_FP` (~0.05), `AFTERMATH_PENALTY_FRACTION` (0.5),
`AFTERMATH_DURATION_DAYS` (~120), `AFTERMATH_INDUSTRIES_DISRUPTED` (sentinel/`-1` = all core industries,
or omit if "all core" is unconditional). Retune `CAPTURE_PROGRESS_PER_DAY_BASE 0.3 -> ~0.6`. The scar
penalties are *not* separate constants — they are `SiegeCondition` constants × the fraction.

## Risks / Trade-offs

- **Pace re-tuning** → the unified meter + CR brake change how fast a siege subjugates, and the old fixed
  no-Nex duration is gone. Mitigation: `CAPTURE_PROGRESS_PER_DAY_BASE` is the single duration knob; ship
  ~0.6 as a starting value and confirm in a dev-mode balance pass (state X→Y in the changelog).
- **Save-compat on the superclass change** → in-flight sieges from old saves won't auto-populate
  `BaseEventIntel` fields. Mitigation: `readResolve()` rebuilds `setup()`; new sieges construct cleanly;
  enums and `captureProgress` field name preserved.
- **Removing condition mid-iteration** → `SiegeAftermathCondition` self-removes from its own `advance()`.
  This is the established vanilla idiom (`PirateActivity`, `CommRelayCondition`), so it is safe.
- **Core-industry disruption strength** → disrupting all core industries for ~120 days is a heavy blow;
  it is the intended "you let the siege win" penalty and is gated behind full subjugation. Tunable via
  `AFTERMATH_DURATION_DAYS`.
- **Capability not yet canonical** → `legio-siege` lives only in the unarchived `redesign-legio-siege`
  change. Mitigation: archive that change first so this change's spec deltas modify a canonical base.
