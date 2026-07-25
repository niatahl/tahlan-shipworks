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

## 6. Wire-up (SiegeManager call sites)

- [x] 6.1 `tryLaunchSiege` passes `ModCompat.HAS_NEX` to the `SiegeIntel` constructor
- [x] 6.2 In `onCommandFleetArrived` and the BESIEGING tick, replace `siege.intel?.updateStage(...)` with `siege.intel?.syncProgress(siege)`
- [x] 6.3 In `onSiegeFleetKilled`, after bounty handling, `siege.intel?.addFactor(SiegeIntel.FleetKillFactor(...))` (via `siege.intel?.addFleetKill(...)`)

## 7. Prose

- [x] 7.1 Add `strings.json` keys (tahlan): stage labels `siege_stage2_{foothold,stranglehold,climax_nex,climax_nonex}`; tooltips `siege_stagetip_*`; prose `siege_stagedesc_*`
- [x] 7.2 Add factor keys `siege_factor_{blockade,raids,intensity,cr,kill_escort,kill_command}` + `siege_factortip_*`; one-time bullets `siege_killbullet_{escort,command}`
- [x] 7.3 Add `siege_intel_resolved_succeeded_nonex` and aftermath keys `siege_aftermath_{name,desc,accessibility,stability}`

## 8. Build & verify

- [x] 8.1 Compile-check via `mcp__ide__getDiagnostics` on `SiegeIntel.kt`, `SiegeManager.kt`, `SiegeAftermathCondition.kt`, `SiegeConfig.kt`; confirm every factor `getProgress()==0`, `reportEconomyTick` no-op, `addStage(START,0)` present, stage ids are an enum
  > NOTE: `getDiagnostics` timed out repeatedly (the IDE LSP does not analyze files that are not open in the editor). A thorough manual review was done instead; needs an in-IDE confirmation.
- [x] 8.2 Rebuild + commit `jars/TahlanShipworks.jar` via IntelliJ artifacts
- [x] 8.3 In-game dev mode (shorten launch interval, raise fill rate): event UI renders — filling red bar, 3 markers + tooltips, per-stage prose, dual factor tables; CR factor shows ×<1 green and slows the projected monthly
- [x] 8.4 Kill an escort -> green one-time row + bar knocks back + bounty accrues; drive CR to floor -> withdrawal -> mop-up -> BROKEN
- [x] 8.5 No-Nex run to climax -> SUCCEEDED; target market gains `tahlan_siegeaftermath` (half-siege penalties) and its core industries show disrupted; condition + disruption clear after `AFTERMATH_DURATION_DAYS`
- [x] 8.6 Nex run to climax -> market transfers, garrison; save/reload mid-siege rebuilds the UI; toggle feature off -> clean teardown
- [x] 8.7 Update `changelog.txt` with explicit stat changes (e.g. `CAPTURE_PROGRESS_PER_DAY_BASE 0.3 -> 0.6`) per changelog conventions

## 9. Review fixes

- [x] 9.1 Fix stat-modifier leak: `SiegeCondition.unapply` + new `SiegeAftermathCondition.unapply` now `unmodifyFlat` accessibility/stability/hazard (`BaseMarketConditionPlugin.unapply` is empty); plus a one-time `sweepLeakedConditionMods()` repair pass for existing dev saves
- [x] 9.2 Fix permanent INBOUND stall: a command fleet lost before arrival now aborts the expedition as `BROKEN` (`flushKill`), and `pruneDeadSieges` resolves the intel on an INBOUND wipe too
- [x] 9.3 Fix withdrawal leaving screening escorts stranded: `triggerWithdrawal` re-anchors escorts whose assignment target *is* the command fleet onto an in-system anchor; add `MOPUP_STALL_TIMEOUT_DAYS` / `INBOUND_TIMEOUT_DAYS` backstops so a siege can never persist forever
- [x] 9.4 Complete the story-protection guard in `isNexProtected`: add `Misc.isStoryCritical` (distinct from `$core_noDeciv`) and Nex's `$nex_npc_no_invade`; documented as unconditional since we bypass `canBeInvaded`
- [x] 9.5 Fix `getOrCreate` registering into `sector.listenerManager` (which never delivers `CampaignEventListener` callbacks) — use `SectorAPI.addListener`/`removeListener` with an `allListeners` double-registration guard
- [x] 9.6 Stop the intel projecting positive monthly progress while the meter is frozen (`dispFrozen`), and surface command-fleet withdrawal as a one-time `CommandWithdrawnFactor` with new `siege_factor{,tip}_withdrawal` strings

## 10. Gameplay & polish fixes

- [x] 10.1 Count partial fleet losses toward attrition: `SiegeFleetListener` tracks a `lastFp` ledger and reports the FP delta per battle; `onSiegeFleetKilled` becomes `onSiegeFleetLosses(siegeId, lostFp, isCommand, destroyed, playerFraction)`, so grinding a fleet down feeds health/CR/meter as it happens instead of only on the killing blow
- [x] 10.2 Strain command CR on command-fleet partial losses (no health damage or meter knockback — the CR drop already brakes the meter); add a BALANCE-PASS note at `STRAIN_K` since total strain sources increased
- [x] 10.3 Scale the bounty by `BattleAPI.getPlayerInvolvementFraction()`: escort losses pay incrementally per FP shed, the command bounty stays a kill-only lump sum; intel factor rows are still emitted on kills only, to avoid row spam
- [x] 10.4 Re-validate the primary target mid-siege in `advanceHealthModel`: a decivilized/depopulated target, one transferred to Legio by other means, or one whose faction is no longer hostile to Legio now resolves the siege as `LIFTED` — no undeserved `SUCCEEDED` climax
- [x] 10.5 Maintain the pressure condition on a slow (2–3 day) interval via `maintainPressureConditions`: drop it from markets that left hostility / changed hands / left the economy, then re-run the idempotent `applyPressureCondition` to pick up newly hostile ones
- [x] 10.6 Replace the dead target weighting in `pickTargetSystem` (a flat ×2 that applied to every candidate, plus a coarse ×1.5 step) with a continuous graded-hostility multiplier `1 + max(0, -worstRel)`
- [x] 10.7 Externalize the last hardcoded player-facing prose: `siege_fleet_command_name` + `siege_assign_{travel,screen,blockade,intercept,raid,besiege,return,garrison}` in `strings.json`, fetched via `Utils.txt`; reword `siege_factortip_kill_escort` (the factor fires on defender kills too)
- [x] 10.8 Give the escort liveness check in `pruneDeadSieges` the missing middle leg (`!containingLocation.fleets.contains(f)`), matching the command-fleet check
- [x] 10.9 Remove dead code: `SiegeConfig.STAGE_{ENTRENCHED,STRAINED}_MIN_CR`, `SiegeData.commandFleetFP`, `FleetKillFactor.addBulletPointForOneTimeFactor` (unreachable for zero-progress one-time factors) and its `siege_killbullet_*` strings, the unused `StageIconSize` import, and the orphaned `siege_{intel_status,stage_entrenched,stage_strained,stage_faltering,intel_garrison}` strings
