## 1. Config & IDs

- [x] 1.1 Add `SiegeConfig` tunables: `CAPTURE_KNOCKBACK_PER_FP` (~0.05), `AFTERMATH_PENALTY_FRACTION` (0.5), `AFTERMATH_DURATION_DAYS` (~120); wire LunaLib overrides where the existing siege sliders are read, defaulting to the constants
- [x] 1.2 Retune `CAPTURE_PROGRESS_PER_DAY_BASE` `0.3 -> ~0.6` (the no-Nex duration now lives here); leave a comment marking it a balance-pass starting value
- [x] 1.3 Add `SIEGE_AFTERMATH_CONDITION_ID = "tahlan_siegeaftermath"` to `TahlanIDs.kt` (next to `SIEGE_CONDITION_ID`)

## 2. Aftermath market condition

- [x] 2.1 Create `SiegeAftermathCondition` (`BaseMarketConditionPlugin`, mirrors `SiegeCondition.kt`); stat mods = `SiegeCondition.{ACCESSIBILITY_MOD, STABILITY_MOD, HAZARD_MOD} * AFTERMATH_PENALTY_FRACTION`, keyed on the per-instance mod `id`
- [x] 2.2 Self-expire in `advance()`: accumulate `Global.getSector().clock.convertToDays(amount)`, and at `AFTERMATH_DURATION_DAYS` call `market.removeSpecificCondition(condition.idForPluginModifications)`; `isTransient() = false`
- [x] 2.3 Add a `createTooltipAfterDescription` showing the (derived) penalties; reuse the siege icon/desc pattern
- [x] 2.4 Register `tahlan_siegeaftermath` in `data/campaign/market_conditions.csv`, mirroring the `tahlan_siegecondition` row (script class, desc, icon)

## 3. Unify the subjugation meter (SiegeManager)

- [x] 3.1 In `advanceHealthModel`, generalize the subjugation block to BOTH modes: advance `captureProgress` while the command fleet is present using `CAPTURE_PROGRESS_PER_DAY_BASE * pressureMult * commandCR * days` (fall back to `pressureMult = 1` when there is no target market)
- [x] 3.2 On reaching `CAPTURE_PROGRESS_MAX`, branch: Nex + capturable target -> existing `attemptNexCapture()` (unchanged); else -> new `applyNoNexAftermath(siege)` then `resolveSiege(SUCCEEDED)`
- [x] 3.3 Remove the fixed no-Nex 6-month `LIFTED`-on-expiry trigger and the Nex-protected-target timer fallback; keep `LIFTED` only for teardown/edge paths
- [x] 3.4 In `onSiegeFleetKilled`, subtract `fleetFp * CAPTURE_KNOCKBACK_PER_FP` from `captureProgress` (floored at 0) for escort/blockade/raid kills (command kill already freezes the meter via `commandFleetPresent`)
- [x] 3.5 Verify all preserved logic is untouched: health/CR/withdrawal/bounty, Nex capture + garrison, fleet spawning, target selection, teardown

## 4. No-Nex scar (SiegeManager.applyNoNexAftermath)

- [x] 4.1 Implement `applyNoNexAftermath(siege)`, called BEFORE `resolveSiege` (which clears `conditionedMarkets`); act on `primaryTargetMarket` only
- [x] 4.2 Add `tahlan_siegeaftermath` to the target market if absent (`hasCondition` guard)
- [x] 4.3 Disrupt the target's core industries for the scar duration: iterate `getIndustries()`, skip `!canBeDisrupted()` and population/spaceport infrastructure, `setDisrupted(AFTERMATH_DURATION_DAYS * jitter, true)` each (jitter via `StarSystemGenerator.getNormalRandom(random, 1f, 1.25f)`)

## 5. Colony-crisis event intel (SiegeIntel rewrite)

- [x] 5.1 Change `SiegeIntel` to extend `BaseEventIntel`; constructor takes `(targetSystem, primaryTarget, hasNex)`; call `setup()`; do not `addIntel` (manager owns lifecycle). Keep the `SiegeStage` and `SiegeOutcome` enums for save-compat
- [x] 5.2 `setup()`: `setMaxProgress(100)`; `addStage(START,0)`, `addStage(FOOTHOLD,33,MEDIUM)`, `addStage(STRANGLEHOLD,66,MEDIUM)`, `addStage(CLIMAX,100,oneOff,LARGE)`; no `setRandomized`
- [x] 5.3 Override `isEventProgressANegativeThingForThePlayer()` = true; `reportEconomyTick(int)` = no-op; `getFactionForUIColors()` = Legio; reuse `events` sprites in `getStageIcon`; keep `getMapLocation`/`getIntelTags`
- [x] 5.4 Implement `getStageLabel` / `getStageTooltipImpl` / `addStageDescriptionText` (CLIMAX text branches on `hasNex`); keep outcome-aware `getName`; remove `createSmallDescription`
- [x] 5.5 Add display-only factors (all `getProgress()==0`): monthly `BlockadePressureFactor`, `RaidSortiesFactor`, `SiegeIntensityFactor`, and `CommandReadinessFactor` (`getAllProgressMult()` = `commandCR`, colored green when < 1); one-time `FleetKillFactor` (green, shown as a knock-back)
- [x] 5.6 Add `syncProgress(siege)`: snapshot manager state, `setProgress(round(captureProgress))`, cache the projected-monthly value for the bar tooltip (fold in / replace `updateStage`)
- [x] 5.7 Keep `addPlayerBounty`/`resolve` (bounty payout + campaign message + `endAfterDelay`); on `SUCCEEDED` `setProgress(100)`; do NOT override `isEnded()`
- [x] 5.8 Add `readResolve()` that rebuilds `setup()` when `stages` is null/empty (save-compat for the superclass change)

## 6. Wire-up (SiegeManager call sites)

- [x] 6.1 `tryLaunchSiege` passes `ModCompat.HAS_NEX` to the `SiegeIntel` constructor
- [x] 6.2 In `onCommandFleetArrived` and the BESIEGING tick, replace `siege.intel?.updateStage(...)` with `siege.intel?.syncProgress(siege)`
- [x] 6.3 In `onSiegeFleetKilled`, after bounty handling, `siege.intel?.addFactor(SiegeIntel.FleetKillFactor(...))` (via `siege.intel?.addFleetKill(...)`)

## 7. Prose

- [x] 7.1 Add `strings.json` keys (tahlan): stage labels `siege_stage2_{foothold,stranglehold,climax_nex,climax_nonex}`; tooltips `siege_stagetip_*`; prose `siege_stagedesc_*`
- [x] 7.2 Add factor keys `siege_factor_{blockade,raids,intensity,cr,kill_escort,kill_command}` + `siege_factortip_*`; one-time bullets `siege_killbullet_{escort,command}`
- [x] 7.3 Add `siege_intel_resolved_succeeded_nonex` and aftermath keys `siege_aftermath_{name,desc,accessibility,stability}`

## 8. Build & verify

- [ ] 8.1 Compile-check via `mcp__ide__getDiagnostics` on `SiegeIntel.kt`, `SiegeManager.kt`, `SiegeAftermathCondition.kt`, `SiegeConfig.kt`; confirm every factor `getProgress()==0`, `reportEconomyTick` no-op, `addStage(START,0)` present, stage ids are an enum
  > NOTE: `getDiagnostics` timed out repeatedly (the IDE LSP does not analyze files that are not open in the editor). A thorough manual review was done instead; needs an in-IDE confirmation.
- [ ] 8.2 Rebuild + commit `jars/TahlanShipworks.jar` via IntelliJ artifacts
- [ ] 8.3 In-game dev mode (shorten launch interval, raise fill rate): event UI renders — filling red bar, 3 markers + tooltips, per-stage prose, dual factor tables; CR factor shows ×<1 green and slows the projected monthly
- [ ] 8.4 Kill an escort -> green one-time row + bar knocks back + bounty accrues; drive CR to floor -> withdrawal -> mop-up -> BROKEN
- [ ] 8.5 No-Nex run to climax -> SUCCEEDED; target market gains `tahlan_siegeaftermath` (half-siege penalties) and its core industries show disrupted; condition + disruption clear after `AFTERMATH_DURATION_DAYS`
- [ ] 8.6 Nex run to climax -> market transfers, garrison; save/reload mid-siege rebuilds the UI; toggle feature off -> clean teardown
- [x] 8.7 Update `changelog.txt` with explicit stat changes (e.g. `CAPTURE_PROGRESS_PER_DAY_BASE 0.3 -> 0.6`) per changelog conventions
