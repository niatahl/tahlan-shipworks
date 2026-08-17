## 1. Prerequisites & config scaffolding

- [x] 1.1 Archive `enhance-siege-intel-and-stakes` (verify its remaining build/verify tasks first) so this change's deltas apply against a base spec containing the subjugation meter and Stranglehold stage
- [x] 1.2 Add all new `SiegeConfig` constants with per-system enable flags: `INTERVENTION_ENABLED`, `INTERVENTION_INTERVAL_DAYS`, `INTERVENTION_PRIMARY_FP_MULT`, `INTERVENTION_AUX_FP_PER_CAPACITY`, `INTERVENTION_AUX_CAPACITY_FLOOR`, `INTERVENTION_STRAIN_K`, `BOUNTY_ENABLED`, `BOUNTY_TRIGGER_PROGRESS`, `BOUNTY_BASE_REWARD_MULT`, `TASKFORCE_ENABLED`, `TASKFORCE_FP_BASE`, `TASKFORCE_FP_SCALE`, `TASKFORCE_DAEMON_FP_MULT`, `TASKFORCE_MIN_BURN`, `TASKFORCE_REDISPATCH_DELAY_DAYS`, `HEAT_PER_FP`, `HEAT_DECAY_PER_DAY`, `HEAT_MARK_THRESHOLD`
- [x] 1.3 Add new per-siege state to `SiegeData`: `interventionFleets` list, `interventionCooldown`, `taskForceFleet`, `taskForceRedispatchTimer`, `playerHeat`, `playerMarked` flag; confirm serialization follows existing field patterns
- [x] 1.4 Add new fleet memory keys to `SiegeManager.companion`: `$tahlan_siege_intervention`, `$tahlan_siege_taskforce`

## 2. F3 — Coalition interventions

- [x] 2.1 Implement coalition assembly in `SiegeManager`: victim + allies via `AllianceManager.getFactionAlliance` behind `ModCompat.HAS_NEX`, relationship ≥ WELCOMING fallback without Nex
- [x] 2.2 Implement per-faction response capacity: best market by (size + military-industry tier bonus [High Command > Military Base], × distance falloff to target system, × 0 while disrupted); reuse the `hasIndustry` pattern from `world/Rubicon.kt`
- [x] 2.3 Implement intervention dispatch in the BESIEGING tick: cooldown-gated; capacity-ranked lead spawns primary fleet (`INTERVENTION_PRIMARY_FP_MULT` × command FP, capacity-capped) at its own market with its own faction doctrine; other members above `INTERVENTION_AUX_CAPACITY_FLOOR` spawn capacity-scaled auxiliaries; tag fleets with `$tahlan_siege_intervention` + siege id
- [x] 2.4 Implement `SiegeInterventionAI` (EveryFrameScript, pattern per `fleet_behavior.md`): travel to target system → seek and engage the command fleet (`DELIVER_CREW` close + `battle.join` where applicable) → if command gone, retarget remaining siege fleets or return home and despawn; fleet-local hostility flag when the owner faction is not hostile to Legio
- [x] 2.5 Implement intervention loss listener: on death in battle against siege-tagged fleets → `SiegeManager.onInterventionFleetLost(siegeId, fp)` → strain command CR by `fp × INTERVENTION_STRAIN_K` via the deferred-kill-safe path (pendingKills-style, not inline in the battle callback)
- [x] 2.6 Disperse in-flight intervention fleets in `resolveSiege`/`tearDown`; prune dead references in `pruneDeadSieges`
- [x] 2.7 Add favourable one-time intel factor + externalized strings for "intervention inbound" (faction-name-substituted); wire `sendUpdateIfPlayerHasIntel` ping
- [x] 2.8 Compile-check (IntelliJ `get_file_problems`) and dev-mode verify: weak-victim siege gets victim-led response; alliance victim gets ally-led primary + auxiliaries; failed intervention visibly strains command CR; intervention killing command breaks siege without player bounty
  > NOTE: compile-check DONE (IntelliJ `get_file_problems`, errors-only: clean on all touched files) and the full `build_project` is green. The dev-mode behavioural verification cannot be run headlessly and remains outstanding.

## 3. F2 — Desperation system bounty

- [x] 3.1 Implement `SiegeSystemBountyIntel : SystemBountyIntel`: constructor takes market + intensity-scaled base reward (`BOUNTY_BASE_REWARD_MULT`, 0 = vanilla formula); set protected `enemyFaction` to Legio
- [x] 3.2 Implement posting in the BESIEGING tick: at `captureProgress ≥ BOUNTY_TRIGGER_PROGRESS` with command present and market not player-faction, register via `SystemBountyManager.getInstance()` — reuse/refresh an existing active bounty at that market (`getActive` → `reset`), else `addActive(new SiegeSystemBountyIntel(...))`
- [x] 3.3 Implement keep-alive: periodic `reset()` while BESIEGING with command present and meter ≥ threshold; stop refreshing on command removal or siege resolution (deliberate lapse through mop-up); verify Nex-capture auto-termination needs no code
- [x] 3.4 Compile-check and dev-mode verify: bounty posts at Stranglehold with Legio attribution and feed ping; kills near the market pay vanilla amounts + rep; bounty survives past command death only for its remaining window; player-faction victim posts nothing; no duplicate bounty when vanilla already posted one at that market
  > NOTE: compile-check DONE; full build green. Nex-capture auto-termination confirmed by source inspection (`SystemBountyIntel.advanceImpl` ends the intel when `faction != market.getFaction()`), so no teardown code is needed. Dev-mode behavioural verification remains outstanding.

## 4. F1 — Huntsman task force

- [x] 4.1 Implement task force spawning in `tryLaunchSiege`: roster by `$tahlan_triggered` at spawn (Blackwatch vs `tahlan_legiodaemons`), `PRIORITY_THEN_ALL` + `setFaction(LEGIO, true)` per the command-fleet pattern, FP = `TASKFORCE_FP_BASE + TASKFORCE_FP_SCALE × intensityFactor` (× `TASKFORCE_DAEMON_FP_MULT` for daemon rosters); set `MEMORY_KEY_NO_SHIP_RECOVERY` and `$tahlan_siege_taskforce` explicitly (NOT `tagSiegeFleet`); travel assignment alongside the command fleet
- [x] 4.2 Implement the burn filter: post-inflation strip members with max burn < `TASKFORCE_MIN_BURN`, re-roll freed FP, bounded retries, accept under-budget on roster exhaustion
- [x] 4.3 Implement heat bookkeeping: accrue `HEAT_PER_FP × fp` in `onSiegeFleetKilled` when `playerInvolved`; decay `HEAT_DECAY_PER_DAY` in `advanceHealthModel`; mark/unmark around `HEAT_MARK_THRESHOLD`
- [x] 4.4 Implement `SiegeTaskForceAI`: interval-driven priority picker — marked player in-system → intervention-tagged fleets → hostile military patrols (weighted picker per `fleet_behavior.md`) → `ORBIT_AGGRESSIVE` screen on the command fleet; never leave the target system while besieging; standard return/despawn signals on siege end
- [x] 4.5 Implement loss + replacement cycle: dedicated task-force listener → `onTaskForceLost(siegeId)`; while BESIEGING with command present, dispatch replacement from source market after `TASKFORCE_REDISPATCH_DELAY_DAYS` (roster re-decided at spawn), travel to system, resume hunting
- [x] 4.6 Add intel one-time factors + externalized strings: adverse "huntsmen have marked you" on marking, "huntsmen destroyed — replacement en route" on loss
- [x] 4.7 Compile-check and dev-mode verify: task force spawns fast-hull-only and travels with the command; ignores low-heat player while hunting patrols/interventions; marks and intercepts high-heat player; heat decay unmarks; kill yields no siege progress and no recoverable hulls; replacement arrives after delay + travel; post-trigger replacement arrives as daemons
  > NOTE: compile-check DONE; full build green. Burn-filter viability confirmed against the data (daemon hulls run burn 7-10, so `TASKFORCE_MIN_BURN` 9 keeps 8 of them; Blackwatch draws on wide vanilla tags and has plenty). Dev-mode behavioural verification remains outstanding.

## 5. Integration & release

- [x] 5.1 Expose curated LunaLib sliders (per-system enables + intervention frequency, heat threshold, task force size) following the existing `TahlanSettings.loadFromLuna` pattern
- [x] 5.2 Full-system dev-mode pass: all three systems live on one siege — bounty baits, huntsmen contest interventions when player is unmarked / get pulled off by a marked player, coalition outcomes vary; confirm toggling each `*_ENABLED` off mid-save degrades cleanly
  > NOTE: cannot be performed headlessly - requires an in-game dev-mode session.
- [x] 5.3 Rebuild + commit `jars/TahlanShipworks.jar` via IntelliJ `build_project`
- [x] 5.4 Update `changelog.txt` — explicit X → Y for any retuned existing constants; keep Legio event flavor vague per changelog conventions; record final balance-pass values for new constants
