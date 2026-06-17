## Context

The legacy siege system (`campaign/siege/`, Java, on vanilla `RaidIntel`/`RouteManager`) is dead code (`LegioSiegeManager` never registered, `ENABLE_SIEGE` commented out), disabled for performance. The cause is that `LegioSiegeBaseIntel` modelled the established base as a hidden **`MarketAPI` + station fleet in hyperspace** — a full economy participant in the single always-near `LocationAPI`. The intended "pressure on the besieged system" pillar was never wired (`getStabilityPenalty`/`getAccessibilityPenalty` are dead), and the expedition is fragilely coupled to vanilla raid internals.

The mod already has the idioms this redesign needs: `KassadariClaim` (a `BaseMarketConditionPlugin` applying accessibility/hazard/immigration penalties) is the model for siege pressure; `.claude/rules/fleet_behavior.md` documents the manager + assignment-AI pattern; `TahlanModPlugin.loadLunaSettings()` + `tahlan_settings.json` is the established settings-with-LunaLib-override path. New code is Kotlin (project direction) and must be compiled into `jars/TahlanShipworks.jar`.

## Goals / Non-Goals

**Goals:**
- Revive the siege as a performant, version-robust feature with **no `MarketAPI`, no hyperspace entity, and no `RaidIntel`/`RouteManager` dependency**.
- Model the siege as a fleet phenomenon anchored by a stacked **command fleet**, with a two-value health model (siege health + command CR) that yields two honest playstyles (decapitation, attrition).
- Make **besieged-faction patrols contribute to attrition identically to the player**, so an underpowered player can win by persistence + allies.
- Finally implement the **pressure** pillar as a market condition on the besieged markets.
- Put **all tuning in named constants**, expose a curated subset as **LunaLib sliders**, and gate the feature behind a `tahlan_settings` toggle.

**Non-Goals:**
- Not keeping any of vanilla `RaidIntel`/`RouteManager`/`AssembleStage`/`TravelStage` machinery.
- No persistent station entity or market of any kind for the besiegers (the perf cause).
- Not authoring new besieged-faction AI — we rely on their existing vanilla patrols engaging hostiles.
- Not a hard LunaLib dependency — constants are the defaults; LunaLib only overrides when present.
- No new ship art; the **command fleet draws from the Blackwatch subfaction** (`tahlan_legioelite` = `TahlanIDs.BLACKWATCH`, the Nightwatch remnant) for a distinctly elite composition, while escort/blockade/raid fleets use the standard Legio faction (`tahlan_legioinfernalis`) — all existing hulls.
- Not wiring the siege into Nexerelin's invasion fleets, faction-war scoring, alliances, or hard-mode mechanics — the Nex capture pathway uses `transferMarket` in parallel, which is the whole point (bypassing invasion).

## Decisions

### Decision: Architecture — custom manager + assignment-AI, no RaidIntel
Follow `fleet_behavior.md` Pattern A: a `SiegeManager` (`BaseCampaignEventListener` + `EveryFrameScript`, `IntervalUtil`-paced, `runWhilePaused=false`) decides when to launch a siege, picks source (largest non-hidden Legio market) and target — a hostile system weighted by market size **and Legio relation** (favouring systems hosting a worst-relation faction's market; if Legio is **at war**, prioritizing a system with an at-war faction's market), excluding systems that already host or neighbour a siege/Legio presence and excluding Nex-protected/story markets — then declares the primary target market (worst-relation faction) within it, scales intensity by campaign progress, and tracks active sieges against a cap. *Rationale*: the `RaidIntel`/`RouteManager` coupling is the single biggest source of fragility and the expedition is just "spawn fleet → travel → convert to blockade," which an assignment-AI does directly. *Alternative considered*: keep `RaidIntel` for the travel intel UI (hybrid) — rejected; it retains half the version-fragility we are removing.

### Decision: The siege is fleets only — command fleet + escorts + sorties
On arrival the **command fleet** anchors the target system's fringe (orbit a jump point / outer anchor, `ORBIT_AGGRESSIVE`), escort/blockade fleets camp jump points, and raid sorties launch periodically against the besieged system's hostile markets via `FleetAssignment` (`RAID_SYSTEM`/`PATROL_SYSTEM` + intercept), all spawned from the manager, not a market. The command fleet is composed from the **Blackwatch subfaction** (`tahlan_legioelite`) — the Nightwatch remnant — so it reads as an elite spearhead, while escorts/sorties draw from the standard Legio faction (`tahlan_legioinfernalis`); a Nex capture transfers the market to `tahlan_legioinfernalis` (the regulars hold the ground the elite took). *Rationale*: removes the economy/hyperspace cost entirely and reads thematically as a blockade *in the besieged system*. *Alternative considered*: a market-less station entity in-system — rejected as unnecessary; nothing requires a persistent entity once the command fleet is the kill target.

### Decision: Two coupled values — Siege Health (meter) and Command CR
- **Siege Health** (`0..MAX`): the siege is fully BROKEN at 0. Reduced (FP-weighted) by destroying any siege fleet, by anyone. The command fleet is the largest single contributor; its removal (kill or withdrawal) removes that chunk. **Regenerated only while the command fleet is present and coordinating**, with regen strength scaling off command CR.
- **Command CR** (`1.0..floor`): strained (FP-weighted) by any siege-fleet loss (the blowback); recovers when pressure lets up (no losses within a recovery-delay window). Governs regen strength, the command fleet's own combat strength (so attrition softens the eventual fight), and the **withdrawal floor** at which the command fleet rationally disengages and returns home.
- **Breaking the siege**: removing the command fleet (killed, or withdrawn at the CR floor) stops all regen and removes its health chunk; the player/defenders then **mop up** residual siege health to 0. Decapitation = biggest blow but still needs mop-up; attrition = bleed CR until withdrawal, then finish.

*Rationale*: keeps the command fleet as the dominant objective and the regen engine while preserving "defeating it is the biggest hit, not the whole job," per the retained earlier decision. *Alternative considered*: collapsing both into a single command-CR currency — rejected: it erased the residual-health/mop-up phase and made decapitation an instant win.

### Decision: Attrition is faction-agnostic and FP-weighted
A `FleetEventListener` on every siege fleet credits its destruction to siege-health reduction and command-CR strain **regardless of killer** (player or besieged-faction patrol), proportional to the dead fleet's FP. Player-involved kills additionally accrue a bounty share (vanilla involvement-fraction pattern). *Rationale*: this is the mechanism that lets a weak player win alongside the defenders and makes strong defenders resolve sieges emergently; FP-weighting "rewards challenge."

### Decision: Pressure via a market condition (KassadariClaim pattern)
While a siege is active, apply a `BaseMarketConditionPlugin` (accessibility down, stability/hazard penalties, optionally suppressed immigration) to the besieged system's hostile markets; remove it cleanly when the siege ends. *Rationale*: cheap, idiomatic (mirrors `KassadariClaim`), processed by the existing econ sim, and finally realizes the pillar the legacy code only stubbed.

### Decision: Progressing intel surfaces command CR
A custom `SiegeIntel` (`BaseIntelPlugin`) marks the target system and advances/regresses through stages keyed off command CR (Entrenched → Strained → Faltering), each stage an update ping, so the player can read the otherwise-invisible CR and know when to strike; it also carries the bounty and resolves as Broken / Lifted / Succeeded. *Rationale*: the progressing intel is how an internal float becomes legible player signposting.

### Decision: All tuning in constants; curated LunaLib sliders; settings-gated
A single `SiegeConfig` (Kotlin `object`/companion) holds every tunable: spawn cadence, command/escort composition + FP, strain coefficient `k`, regen rate, recovery-delay window, withdrawal CR floor, intel-stage thresholds, intensity-scaling curve, bounty values. A **curated subset** (master enable, frequency, command difficulty/size, attrition strength) is exposed as LunaLib sliders read in `loadLunaSettings()` and guarded by `HAS_LUNA`; the constants are the defaults so the feature works without LunaLib. The master enable is a `tahlan_settings.json` **toggle** (not save-locked): turning it off stops new sieges and lets active ones tear down cleanly (despawn fleets, remove conditions, resolve intel). *Rationale*: matches the mod's established settings pattern and the "capture all key values as constants" requirement.

### Decision: Dual resolution pathway gated on Nexerelin (`HAS_NEX`)
A siege is a race between the player/defenders breaking it (siege health → 0 = BROKEN, the universal counter) and the siege achieving its goal. The goal — and thus what happens if the player *fails* to break it — forks on Nexerelin presence:
- **No Nexerelin**: the siege has a finite **lifetime**. The besieged-market pressure condition is a time-boxed strangle (markets cut off from trade); on expiry the siege **lifts** with no ownership change. Vanilla has no market-transfer mechanic, so there is no territorial outcome — the siege's "win" is purely the economic harm inflicted while it held.
- **With Nexerelin**: the siege accrues **capture progress** while it holds and the target market is strangled; on completion it **succeeds** by capturing a market in the besieged system via `exerelin.campaign.SectorManager.transferMarket(...)`, **deliberately bypassing a normal Nex invasion** (no invasion fleets / ground battles). This makes sieges Legio's expansion engine and gives the player a real reason to break them — losing the race loses a colony.

The Nex pathway is kept **parallel to** Nex's own invasion/war system: use `transferMarket` and respect its bookkeeping, but do not wire the siege into Nex faction-war scoring, alliances, or hard-mode invasion mechanics — bypassing invasion is the point. *Rationale*: matches the established `HAS_NEX` soft-integration pattern (`TahlanModPlugin` already imports `exerelin.campaign.SectorManager`); both pathways reuse the same fleet/health/CR core, forking only on the failure-to-break outcome.

**Settled sub-decisions:**
- *Capture-progress driver*: **hybrid** — progress accrues while the siege holds, with the rate scaled by how strangled the target market is (its dropping accessibility/stability under the pressure condition).
- *Target market*: **declared at siege launch** — the hostile market in the system whose faction has the **worst relation with Legio**. This also feeds **system selection**: weight target systems toward those hosting a worst-relation faction's market, and if Legio is **at war**, prioritize a system with an at-war faction's market. Pressure, raids, and capture all concentrate on this declared market.
- *Command-fleet fate on SUCCEEDED*: **garrison the captured market for ~1 year** (tuning constant), then return home and disband — a window for the player to counterattack the fresh foothold before Legio's hold normalizes.
- *No-Nex lifetime*: **6 months** default (tuning constant); the besieged markets **fully recover** on lift (no lingering scar).
- *Story-market protection*: defer to **Nexerelin's market-capture eligibility** — story-relevant / protected / non-invadable markets are excluded from being a siege target and are re-checked at transfer time so they are never captured (exact Nex protection API is a verify item).
- *Nex API specifics*: exact `transferMarket` signature/args verified in-engine; the transfer stays outside Nex faction-war balance (a deliberate, unique Legio edge).

### Decision: Scaling metric replaces `currentCycle - 206`
Intensity (command/escort FP, escort count, raid cadence, station-equivalent toughness) scales off **elapsed campaign time and/or a Legio-strength metric** (e.g. number/size of Legio markets) rather than the hardcoded start-cycle offset. *Rationale*: the `- 206` constant silently assumes the vanilla start year and breaks for non-standard starts.

## Lifecycle (state machine)

```
  DORMANT ──launch──▶ INBOUND ──arrive──▶ BESIEGING ──┬─ siege health 0 ─────▶ BROKEN  (player/defenders;
 (manager   (spawn cmd  (cmd fleet   (anchor + escorts │                                under Nex, prevents takeover)
  interval)  + escorts,  travels to   + sorties + cond │
             from Legio  target)       + CR/health     │   goal reached / time runs out:
             market)                   + goal clock)    │     no-Nex:  lifetime expires ─▶ LIFTED    (markets recover)
                                                        └     Nex:     capture progress  ─▶ SUCCEEDED (transferMarket →
                                                                       fills                            Legio takes a market,
                                                                                                        bypassing invasion)
```

## Risks / Trade-offs

- **Hand-rolled intel/UI**: dropping `RaidIntel` means re-authoring the intel entry, map marker, and stage updates. Mitigated by `BaseIntelPlugin` providing the scaffolding and the progressing-intel pattern already used elsewhere in the mod.
- **Fleet bookkeeping leaks**: tracking command/escort/raid fleets requires the triple liveness checks and `reportFleetDespawned` pruning from `fleet_behavior.md`; a missed prune can mis-account siege health or strand fleets. Mitigated by following that doc's manager pattern verbatim.
- **CR manipulation fidelity**: applying/recovering command-fleet CR and a rational withdrawal must use the campaign CR/fleet APIs correctly (per-member CR vs. a stat mod); needs an in-game spike to confirm the cleanest lever.
- **Tuning is the feature**: with `k`, regen, recovery window, and withdrawal floor all interacting, the feel is entirely in the numbers — hence everything is a named constant and the key knobs are player sliders.
- **Target-picking degeneracy**: must avoid besieging the player exclusively, decivved systems, or thrashing targets month-to-month; reuse size-weighted, distance/eligibility-constrained picking.

## Post-implementation corrections (review follow-up)

A code review of the first implementation (tasks §1–§9) surfaced six defects, captured as tasks §10. Most are mechanical; three involved a judgment call, settled as follows:

### Decision: Command-fleet kill removes only its health chunk, not chunk + per-FP
The kill handler applied **both** the per-FP `siegeHealth` damage and the `COMMAND_HEALTH_SHARE` chunk to the command fleet, so a high-intensity command kill (≥250 FP) removed ≥100 health — an instant break. That violates two settled decisions at once: 5.5 ("never an instant win," residual must be mopped) and the equality clause ("killing vs. driving off the command fleet differs only in the reward, not in the siege-health effect" — withdrawal already applies the chunk only). Corrected by **excluding the command fleet from the per-FP path**: its health contribution is solely the chunk, matching the withdrawal path and preserving the escort residual at every intensity. *Alternative considered*: cap escort per-FP damage so the "shares" sum exactly — rejected as needless; floor-at-0 already makes uncapped escort chipping behave, and the model is simpler stated as "command = a chunk, escorts = per-FP." The config comment is corrected to say that rather than implying a distributed escort share.

### Decision: Intensity scaling normalizes over [BASE, MAX], anchored at BASE not 1.0
Consumers computed `BASE + SCALE × (intensity − 1f)` clamped to `[BASE, BASE+SCALE]`, so the entire `[INTENSITY_BASE=0.5, 1.0]` sub-range clamped to the floor and the first ~5 years of the time ramp were inert. Corrected to normalize: `factor = (intensity − INTENSITY_BASE)/(INTENSITY_MAX − INTENSITY_BASE)`. This commits to the intended escalation shape — **early sieges sit at base strength and ramp to full over campaign time** — rather than starting strong. *Alternative considered*: raise `INTENSITY_BASE` toward `INTENSITY_MAX` for strong-from-start sieges — rejected; a Legio threat that grows with the campaign is the intent.

### Decision: Travel escorts screen the command fleet on arrival
The launch-time escort fleets had no post-arrival behavior and idled at the system center; the design wanted escort fleets active in-system. Corrected to reassign them to `ORBIT_AGGRESSIVE` the command anchor on `onCommandFleetArrived`. *Alternatives considered*: distribute them to jump points like blockade fleets (spreads force thin, and the per-jump-point blockade population already covers the chokepoints), or fold the two populations into one (loses the "fleet visibly travels in with the command fleet" beat). Screening the flagship concentrates force into a real battle and reads thematically as a guard around the stacked command fleet.
