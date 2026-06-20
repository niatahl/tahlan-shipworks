## Why

The Legio Infernalis siege system (`jars/src/org/niatahl/tahlan/campaign/siege/`, 9 Java files built on vanilla `RaidIntel`/`RouteManager`) is **dead code**: `LegioSiegeManager` is never instantiated or registered and the `ENABLE_SIEGE` gate is commented out. It was disabled because of **major performance problems**, and the root cause is structural:

- `LegioSiegeBaseIntel` represents the established siege base as a full **`MarketAPI` + station combat fleet floating in hyperspace**. A market is the heaviest object the game offers (monthly econ updates, commodity routing, trade-fleet eligibility, `PATROL_NUM_*` spawns, a persistent station fleet), and **hyperspace is a single always-near `LocationAPI`** whose entity list is touched constantly. One active siege = full economy + fleet sim cost, in the busiest location, forever.
- The intended "the siege pressures the besieged system" pillar (`getStabilityPenalty` / `getAccessibilityPenalty`) was **never wired up** — those methods are dead. So the base only ever existed as a thing to fight, a bounty, and a raid source.
- The whole thing is **fragilely coupled to vanilla `RaidIntel`/`RouteManager` internals** (magic `RouteSegment` durations like `666666f`, protected-method overrides, `getRoutesForSource`) — exactly the code that silently breaks on Starsector version bumps.

This change revives the feature as a clean, performant, Kotlin reimplementation.

## What Changes

- **Remove the hyperspace market and station entity entirely.** The siege becomes a pure **fleet phenomenon** — no `MarketAPI`, no hyperspace entity, nothing for the economy sim to chew on.
- **Drop the `RaidIntel`/`RouteManager` dependency.** Replace with a custom manager + per-fleet assignment-AI state machine (the project's own `.claude/rules/fleet_behavior.md` pattern).
- **A stacked, tough "command fleet" anchors the target system's fringe** and is the heart of the siege — but not the whole of it. The siege carries an **overall health** value (the blockade as a whole), and is fully **broken only when that health reaches zero**.
- **The command fleet is the single biggest contributor to siege health and the only source of its regeneration.** Removing it — by destruction *or* by rational withdrawal — deals the largest hit to siege health and **stops all regeneration**, but residual health (escort/blockade/raid fleets, entrenchment) still has to be **mopped up** to fully break the siege. A straight decapitation rush is the biggest blow, never an instant win; killing vs. driving off the command fleet differs only in the reward (a recoverable wreck / loot / bounty on a kill), not in the siege-health effect.
- **The command fleet's combat readiness (CR) is a second, coupled value** that governs regeneration strength, how hard the command fleet is to kill, and the CR floor at which it rationally withdraws. **Any siege-fleet loss — by the player or by besieged-faction patrols, counted identically — both chips siege health and strains command CR, weighted by the dead fleet's FP.** CR recovers when pressure lets up, so a siege is a tempo war: with the defenders' patrols helping, a weaker player can bleed the command fleet's CR until it withdraws (no head-on kill required) and then finish the residual health, while a stronger player can decapitate it outright and mop up.
- **Implement the never-wired pressure pillar** as a market **condition** on the besieged system's hostile markets (the existing `KassadariClaim` `BaseMarketConditionPlugin` pattern), active for the siege's duration.
- **Dual resolution pathway gated on Nexerelin** (`HAS_NEX`). *Without* Nexerelin a siege has a finite lifetime: the pressure condition strangles the besieged markets (cut off from trade) for its duration, then it lifts with no territorial change. *With* Nexerelin a successful siege culminates in **capturing a market in the besieged system via `SectorManager.transferMarket(...)`, deliberately bypassing a normal Nex invasion** — making sieges Legio's expansion engine. In both pathways, breaking the siege (driving siege health to 0) is the universal counter; under Nex it is what prevents the takeover.
- **Surface state through a progressing intel** whose stages reflect command CR (Entrenched → Strained → Faltering), so the player can read when the head is soft enough to strike; bounty share accrues on player-involved kills.
- **All tuning lives in named constants**; a curated subset is exposed as **LunaLib balance sliders** (guarded by `HAS_LUNA`, constants are the defaults). Gate the whole feature behind a `tahlan_settings` toggle with clean mid-save teardown. Replace the brittle `currentCycle - 206` scaling with an elapsed-campaign-time / Legio-strength metric.

## Capabilities

### New Capabilities
- `legio-siege`: A periodic, scaling Legio Infernalis siege campaign event in which a stacked command fleet blockades a hostile star system from its fringe, pressures the besieged markets via an econ condition, and launches recurring raids — broken by destroying or attriting the command fleet (its CR is the siege's health), with besieged-faction patrols contributing to attrition identically to the player. Implemented as a custom fleet manager + assignment-AI, with no market, no hyperspace entity, and no vanilla `RaidIntel`/`RouteManager` dependency.

### Removed Capabilities
- The hyperspace-market siege base (`LegioSiegeBaseIntel`) and the `RaidIntel`/`RouteManager`-based expedition (`LegioSiegeMission*`) are removed and replaced wholesale.

## Impact

- **Code (removed)**: the 9 Java files under `jars/src/org/niatahl/tahlan/campaign/siege/` (`LegioSiegeManager`, `LegioSiegeMissionIntel`, `LegioSiegeBaseIntel`, `LegioSiegeMissionAssignmentAI`, `LegioSiegeMissionStage1Organize`–`Stage5Defend`).
- **Code (added)**: a new Kotlin siege package — manager (`fleet_behavior.md` Pattern A), expedition assignment-AI, per-siege state object (command-CR accounting + `FleetEventListener`), progressing `SiegeIntel`, a besieged-market `BaseMarketConditionPlugin`, and a central `SiegeConfig` constants holder.
- **Wiring**: register the manager in `TahlanModPlugin.onGameLoad` (behind the settings toggle); add the market condition to `data/campaign/market_conditions.csv`; externalize all player-facing prose to `data/strings/strings.json` via `Utils.txt`.
- **Settings**: a new toggle in `tahlan_settings.json` (toggle, not save-locked) + LunaLib slider entries in `data/config/LunaSettings.csv`, read in `loadLunaSettings()`.
- **Dependencies**: hard deps unchanged (LazyLib, MagicLib). LunaLib and **Nexerelin** stay soft, `isModEnabled`-guarded integrations — the feature works without either (no-Nex = finite-lifetime pressure pathway; Nex = market-capture pathway via `exerelin.campaign.SectorManager`, already on the classpath and detected as `HAS_NEX`).
- **Save compatibility**: since the old system never ran (dead code), there is no live save state to migrate; the new system is purely additive and self-contained, with clean teardown when the toggle is turned off mid-save.
- **Build**: must be compiled into `jars/TahlanShipworks.jar` via IntelliJ artifacts.
