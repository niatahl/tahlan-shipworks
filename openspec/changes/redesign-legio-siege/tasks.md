## 1. Remove the legacy system

- [x] 1.1 Delete the 9 legacy Java files under `jars/src/org/niatahl/tahlan/campaign/siege/` (`LegioSiegeManager`, `LegioSiegeMissionIntel`, `LegioSiegeBaseIntel`, `LegioSiegeMissionAssignmentAI`, `LegioSiegeMissionStage1Organize`–`Stage5Defend`)
- [x] 1.2 Confirm no remaining references compile against the removed classes (the system was never registered, so this should be self-contained)

## 2. Config & gating

- [x] 2.1 Create a `SiegeConfig` Kotlin `object` holding every tunable: spawn cadence, active-siege cap, command/escort composition + FP, intensity-scaling curve, strain coefficient `k`, regen rate, recovery-delay window, withdrawal CR floor, intel-stage thresholds, raid cadence, bounty values, **no-Nex siege lifetime (default ~6 months)**, and **Nex garrison duration (default ~1 year)**
- [x] 2.2 Add a master-enable **toggle** to `tahlan_settings.json` (not save-locked) and document the key
- [x] 2.3 Add the curated LunaLib sliders to `data/config/LunaSettings.csv` (master enable, frequency, command difficulty/size, attrition strength) and read them in `TahlanModPlugin.loadLunaSettings()` behind `HAS_LUNA`, defaulting to the `SiegeConfig` constants
- [x] 2.4 Replace `currentCycle - 206` scaling with an elapsed-campaign-time / Legio-strength metric in `SiegeConfig`

## 3. Siege manager (fleet_behavior.md Pattern A)

- [x] 3.1 Create `SiegeManager` (`BaseCampaignEventListener` + `EveryFrameScript`, `IntervalUtil`-paced, `runWhilePaused=false`); register it in `TahlanModPlugin.onGameLoad` behind the settings toggle
- [x] 3.2 Implement source picking (largest non-hidden Legio market) and target picking weighted by market size **and Legio relation** (favour worst-relation faction's systems; prioritize an at-war faction's system when Legio is at war), with eligibility/distance constraints (no existing Legio presence or active siege in/adjacent to the system; exclude Nex-protected/story markets); then **declare the primary target market** (worst-relation hostile market) for the siege
- [x] 3.3 Track active sieges, enforce the cap, and prune dead sieges with the triple liveness check + `reportFleetDespawned` backup from `fleet_behavior.md`

## 4. Expedition → besieging (assignment AI)

- [x] 4.1 Spawn the stacked command fleet from the **Blackwatch subfaction** (`tahlan_legioelite` / `TahlanIDs.BLACKWATCH`; capitals, officers, S-mods, aiCores per `fleet_behavior.md`) plus escort fleets from the standard Legio faction (`tahlan_legioinfernalis`), at the source market
- [x] 4.2 Implement the expedition assignment-AI: travel to target; on arrival flip the siege to BESIEGING and anchor the command fleet at the system fringe (`ORBIT_AGGRESSIVE`)
- [x] 4.3 Spawn/maintain blockade fleets at jump points and launch periodic raid sorties against the besieged system's hostile markets

## 5. Two-value health model

- [x] 5.1 Implement per-siege state: `siegeHealth` (0..MAX) and `commandCR` (1.0..floor), driven by a `SiegeConfig`
- [x] 5.2 Attach a `FleetEventListener` to every siege fleet; on any destruction (any killer) reduce siege health and strain command CR by an FP-weighted amount; accrue player bounty share on player-involved kills
- [x] 5.3 Regenerate siege health only while the command fleet is present, with regen strength scaling off command CR
- [x] 5.4 Recover command CR when no loss has occurred within the recovery-delay window; scale the command fleet's effective combat strength off command CR
- [x] 5.5 On command-fleet removal (kill or withdrawal): stop regen, remove its health chunk, keep the siege alive until residual health is mopped to 0
- [x] 5.6 Trigger rational withdrawal (disengage + return home) when command CR drops below the withdrawal floor
- [x] 5.7 End the siege as BROKEN at health 0: disperse/withdraw remaining fleets, remove the condition, resolve intel

## 6. Pressure condition

- [x] 6.1 Create a siege `BaseMarketConditionPlugin` (accessibility/stability/hazard, optional immigration suppression) modelled on `KassadariClaim`; register it in `data/campaign/market_conditions.csv`
- [x] 6.2 Apply it to the target system's non-hidden hostile markets on BESIEGING; remove it cleanly on any siege resolution

## 7. Intel

- [x] 7.1 Create a progressing `SiegeIntel` (`BaseIntelPlugin`) marking the target system; stage = f(commandCR) (Entrenched / Strained / Faltering) with update pings
- [x] 7.2 Carry the destruction bounty and player-share payout; resolve as Broken / Lifted / Succeeded
- [x] 7.3 Implement BROKEN resolution (siege health 0) as the universal counter in both pathways

## 7a. Dual resolution pathway (Nexerelin fork)

- [x] 7a.1 No-Nex pathway: finite siege lifetime (default ~6 months, `SiegeConfig`); on expiry resolve LIFTED and **fully recover** the besieged markets (no lingering scar)
- [x] 7a.2 Nex pathway (`HAS_NEX`): concentrate pressure/raids/capture on the declared primary target market
- [x] 7a.3 Nex pathway: accrue capture progress while the siege holds, with the **rate scaled by how strangled the target market is** (its reduced accessibility/stability) — hybrid driver
- [x] 7a.4 Nex pathway: on capture completion, transfer the target market to Legio via `exerelin.campaign.SectorManager.transferMarket(...)`, bypassing invasion; resolve SUCCEEDED; then **garrison the captured market for ~1 year, then return home and disband**
- [x] 7a.5 Respect **Nexerelin story-market protection**: exclude protected/story/non-invadable markets at target selection and re-check at transfer time, aborting the capture if protected
- [x] 7a.6 Keep the Nex pathway parallel to Nex's invasion / faction-war / alliance / hard-mode systems (no enrollment); guard every Nex call behind `HAS_NEX`

## 8. Teardown & lifecycle safety

- [x] 8.1 On feature-toggle off mid-save: stop new launches and tear down active sieges cleanly (despawn fleets, remove conditions, resolve intel) with no orphaned state
- [x] 8.2 Verify save/load round-trips (state in fleet/sector memory per `fleet_behavior.md`, not transient plugin fields)

## 9. Prose & build

- [x] 9.1 Externalize all player-facing prose (intel text, stage labels, update strings, condition tooltip) to `data/strings/strings.json` via `Utils.txt`, namespaced (e.g. `siege_*`)
- [ ] 9.2 Compile into `jars/TahlanShipworks.jar` via IntelliJ artifacts
- [ ] 9.3 In-game validation: launch a siege (dev), confirm no market/hyperspace entity created, verify attrition by defender patrols, command CR strain/recovery/withdrawal, decapitation-then-mop-up, pressure condition apply/remove, intel staging, and clean teardown on toggle-off

## 10. Post-implementation review fixes

Defects found in a code review of the (unshipped) implementation; see "Post-implementation corrections" in `design.md`.

- [x] 10.1 (#1) Delete the `isEnded()` override in `SiegeIntel` so `endAfterDelay()` works — the override reports `ended` the instant `outcome` is set, so the base `advance()` (`if (isEnded()) return`) never ticks the lingering timer and the IntelManager drops the entry next frame. Without it the base tracks `ended`/`endingTimeRemaining` correctly and the resolution text + "bounty paid" line survive the standard ~3 days.
- [x] 10.2 (#2) In `SiegeManager.onSiegeFleetKilled`, exclude the command fleet from the per-FP `siegeHealth` damage (and the now-moot CR strain): the command's health contribution is solely the `COMMAND_HEALTH_SHARE` chunk. This makes a kill identical in health effect to a withdrawal (per the design's "killing vs. driving off differs only in reward") and guarantees the escort residual / mop-up phase survives at every intensity (currently a ≥250-FP command kill removes ≥100 health = instant break, the outcome design decision 5.5 rejected). Rewrite the `SiegeConfig` health-model comment to describe the real model — command = a `COMMAND_HEALTH_SHARE` chunk removed on kill/withdrawal; escort/blockade/raid = uncapped per-FP damage floored at 0 — and delete the inaccurate "escort share = 1 − COMMAND_HEALTH_SHARE, distributed among …" wording.
- [x] 10.3 (#3) Normalize intensity-driven scaling against `[INTENSITY_BASE, INTENSITY_MAX]` instead of pivoting on `1f`. Use `factor = ((intensity − INTENSITY_BASE) / (INTENSITY_MAX − INTENSITY_BASE)).coerceIn(0f, 1f)` and `value = BASE + SCALE × factor` for command FP, escort count, escort FP, command S-mods, and raid FP. Restores meaningful early-game scaling (currently intensity 0.5–1.0 all clamp to the floor, so the first ~5 years of ramp are inert); sieges start at base strength and ramp to full over campaign time.
- [x] 10.4 (#4) Make the LunaLib frequency slider live: re-derive the spawn interval from `SiegeConfig.LAUNCH_INTERVAL_DAYS_MIN/MAX` at reset time via `spawnTimer.setInterval(min, max)` (e.g. each `advance` before the elapsed check) rather than freezing it in the field initializer at construction. The manager persists across saves, so the construction-time capture means slider changes never reach an existing save's timer.
- [x] 10.5 (#5) Give the travel escort fleets post-arrival behavior: in `onCommandFleetArrived`, reassign them to `ORBIT_AGGRESSIVE` the command anchor (screen the flagship) instead of leaving them idling on their launch-time 1000-day `GO_TO_LOCATION` at the system center.
- [x] 10.6 (#7) Notify the player on bounty payout (a campaign-UI message or `MessageIntel`), since fixing #1 restores the intel line but a transaction message is the expected idiom; externalize the string to `strings.json` (`siege_*`).
- [ ] 10.7 Rebuild `jars/TahlanShipworks.jar` and fold these into the 9.3 validation pass: confirm decapitation leaves a mop-up residual, kill == withdrawal health effect, intel lingers on resolve with a bounty message, the frequency slider changes cadence on an existing save, and escorts screen the command fleet.
