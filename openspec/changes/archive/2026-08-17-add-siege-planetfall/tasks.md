# Tasks: add-siege-planetfall

## 1. Config, stage, and state

- [x] 1.1 Add `SiegeConfig` knobs: `PLANETFALL_DURATION_DAYS` (6f), `DEFENDER_SWEEP_INTERVAL_DAYS`
      (2f), `STATION_DISRUPTION_EXTRA_DAYS` (30f), `OCCUPATION_DISRUPTION_DAYS` (60f) — all marked
      BALANCE-PASS STARTING VALUE per file convention
- [x] 1.2 Add `SiegeData.Stage.PLANETFALL` plus `planetfallTimer: Float` and
      `defenderSweepCooldown: Float` fields on `SiegeData` (primitives; safe defaults on old saves)
- [x] 1.3 Add `FLEET_PLANETFALL_KEY` fleet-memory constant to `SiegeManager.companion` (value:
      target planet entity id, same pattern as `FLEET_GARRISON_MARKET_KEY`)
- [x] 1.4 Teach the live-stage filters about PLANETFALL: `pruneDeadSieges` (must NOT reap a
      planetfall siege as "already resolved"), `maintainPressureConditions` (keep sweeping),
      `advanceHealthModel`'s stage gate, and `checkBroken` (accept PLANETFALL)

## 2. Planetfall entry

- [x] 2.1 Replace the meter-full Nex-capturable branch in `advanceHealthModel` with
      `beginPlanetfall(siege)`; keep the no-Nex/blocked/protected branch untouched; fall back to
      immediate `attemptNexCapture` when `primaryTargetMarket?.primaryEntity` is null
- [x] 2.2 `beginPlanetfall`: set stage, arm `planetfallTimer`, set `FLEET_PLANETFALL_KEY` on the
      command fleet, re-assign alive escort/blockade/raid fleets to `ORBIT_AGGRESSIVE` on the
      planet ("supporting landing operations" string), skipping fleets currently in battle, and set
      `FLEET_PLANETFALL_KEY` on them
- [x] 2.3 `beginPlanetfall`: flag alive intervention fleets home via `FLEET_RETURN_FLAG`
- [x] 2.4 `beginPlanetfall`: disrupt the target market's station industry — identify by
      `Industries.TAG_STATION` spec tag, `setDisrupted(duration, true)` for
      `PLANETFALL_DURATION_DAYS + STATION_DISRUPTION_EXTRA_DAYS`
- [x] 2.5 `beginPlanetfall`: run the first defender sweep and call `intel?.notifyPlanetfall()`

## 3. Planetfall tick and resolution rules

- [x] 3.1 Add `advancePlanetfall(siege, days)` branch to the manager loop: tick the timer, keep the
      LIFTED validity checks, CR recovery, health regen, heat decay; do NOT spawn raids, run F3
      interventions, or refresh the F2 bounty
- [x] 3.2 Defender sweep on `defenderSweepCooldown` cadence: military fleets
      (`MEMORY_KEY_PATROL_FLEET` / `MEMORY_KEY_WAR_FLEET`) of the victim faction or
      `coalitionFactions(victimId)` in the target system, hostile to Legio, excluding player
      faction, trade fleets, `$stationFleet`, `FLEET_SIEGE_ID_KEY` carriers, and in-battle fleets →
      set expiring `MEMORY_KEY_MAKE_ALLOW_DISENGAGE` + `MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED`,
      `clearAssignments`, `GO_TO_LOCATION_AND_DESPAWN` at nearest out-of-system same-faction market
      (fallback: nearest jump point)
- [x] 3.3 Point of no return: suppress `CAPTURE_KNOCKBACK_PER_FP` in `onSiegeFleetLosses` while
      stage == PLANETFALL (loss accounting, CR strain, bounty, and heat still apply)
- [x] 3.4 Instant decapitation: in `flushKill`, mirror the INBOUND rule — stage == PLANETFALL and
      `!commandFleetPresent` → `resolveSiege(BROKEN)`; in `advancePlanetfall`, CR at
      `COMMAND_CR_WITHDRAWAL_FLOOR` → `resolveSiege(BROKEN)` directly (no `triggerWithdrawal`)
- [x] 3.5 Completion: timer expired and command fleet alive → `attemptNexCapture(siege)`; defer
      while `commandFleet.battle != null`
- [x] 3.6 Verify `tearDown` and `debugEndAllSieges` handle a PLANETFALL siege cleanly (dispersal
      flags cover converged fleets; no special casing expected — confirm)

## 4. Fleet AI cooperation

- [x] 4.1 `SiegeAssignmentAI`: new `Phase.PLANETFALL` — from BESIEGING, on `FLEET_PLANETFALL_KEY`,
      `clearAssignments` + `ORBIT_AGGRESSIVE` over the planet ("making planetfall on X" string);
      move the garrison-key check so it also runs in PLANETFALL phase (that is the normal handoff
      now); `FLEET_RETURN_FLAG` handling unchanged
- [x] 4.2 `SiegeBlockadeAI`: when own fleet carries `FLEET_PLANETFALL_KEY`, stop prey-picking and
      re-anchor `holdStation` on the planet entity instead of the jump point

## 5. Success moment and aftermath

- [x] 5.1 `attemptNexCapture`: switch to the 8-arg `SectorManager.transferMarket` overload with
      `silent = true` (still inside the existing try/catch + `ModCompat.HAS_NEX` guard)
- [x] 5.2 Occupation aftermath after successful transfer: core-industry disruption reusing the
      `applyNoNexAftermath` loop shape with `OCCUPATION_DISRUPTION_DAYS`, plus vanilla
      `RecentUnrest` on the market; do NOT apply the no-Nex scar condition
- [x] 5.3 `SiegeIntel`: rework SUCCEEDED resolution prose — Legio-flavored capture text as the
      single notification, distinct variant when `playerBountyEarned > 0`

## 6. Intel presentation

- [x] 6.1 `SiegeIntel.notifyPlanetfall()`: adverse one-time factor + `sendUpdateIfPlayerHasIntel`
      feed ping
- [x] 6.2 Sync planetfall display state (days remaining) from the manager; render a countdown line
      in `afterStageDescriptions` while planetfall is underway
- [x] 6.3 Update CLIMAX stage strings for the Nex path: label to "Planetfall", tooltip/description
      describing the timed window instead of an instant capture

## 7. Strings, debug, verification

- [x] 7.1 Add all new `siege_*` keys to `data/strings/strings.json` (assignment labels: planetfall
      / landing support; intel: planetfall factor, countdown, climax stage, SUCCEEDED prose
      variants) — no literals in source, per project convention
- [x] 7.2 Extend `debugDump` with planetfall stage, timer, and sweep cooldown; confirm
      `TahlanSiegeStart` + manually setting `captureProgress` near max reaches planetfall for
      dev-mode testing (add a debug setter if none exists)
- [x] 7.3 Build via IntelliJ MCP `build_project` (regenerates the committed jar) and fix any
      compile errors
- [x] 7.4 Dev-mode pass: force a siege, drive the meter to full, observe convergence / fleeing
      defenders / station stand-down / countdown intel; verify decapitation mid-planetfall resolves
      instant BROKEN and timer completion transfers the market with occupation aftermath and only
      the flavored intel beat
- [x] 7.5 Update `changelog.txt` (vague on Legio event details per changelog style; note the
      no-downgrade caveat for saves mid-planetfall)
