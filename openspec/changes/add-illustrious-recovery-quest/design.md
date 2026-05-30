## Context

Louisa Ferre (`tahlan_devil`, Blackwatch) already exposes three *repeatable* generic person-missions registered in `data/campaign/person_missions.csv` under tag `tahlan_devil`, implemented as plugins in `jars/src/org/niatahl/tahlan/campaign/missions/devil/` and surfaced via dialog rows in `data/campaign/rules.csv` gated on `$id == tahlan_devil`. The closest reference is `DaemonCoreSale.kt`, which extends `HubMissionWithBarEvent`, uses `setPersonMissionRef`, reads `person.relToPlayer.level`, exposes data to rules via `updateInteractionDataImpl()`, and performs a credits/commodity transaction in `callEvent(...)`.

The Illustrious (`tahlan_illustrious`) is a finished, deliberately-unobtainable capital (`no_bp_drop`, `HIDE_IN_CODEX`). This change adds a fourth, **one-time, multi-stage** offering that reuses this exact infrastructure but adds a clue-chain state machine, derived clue/wreck locations, guarding-fleet spawns, a final derelict spawn, a completion latch, and a follow-up restoration transaction.

This change is data + one new JVM class; it must be compiled into `jars/TahlanShipworks.jar`.

## Goals / Non-Goals

**Goals:**
- A one-time, COOPERATIVE-gated questline that ends with the player owning a 3-D-mod Illustrious.
- A 3-hop guarded clue chain with escalating Legio opposition, climaxing at hop 3, terminating in a peaceful salvage.
- A one-time paid (discounted) restoration brokered by Louisa that clears all D-mods.
- Reuse the established devil-mission pattern so the new offering sits naturally beside the existing three.

**Non-Goals:**
- No changes to the three existing devil missions beyond adding a sibling dialog option.
- No new ship art, hull, variants, or blueprint changes — the Illustrious asset is reused as-is.
- No involvement of Scathach or any named/faced Legio leadership; guards are anonymous Legio fleets.
- No guardian at the wreck site; the only combat is in the clue chain.
- No procedural-content framework beyond what vanilla `BaseHubMission` already provides.

## Decisions

### Decision: Implement as a `HubMissionWithBarEvent` subclass mirroring `DaemonCoreSale`
A single new Kotlin class in `campaign/missions/devil/` (e.g. `IllustriousRecovery`) extending `HubMissionWithBarEvent`, registered in `person_missions.csv` under `tahlan_devil` with a `min rep` column set to the COOPERATIVE threshold. Rationale: this is the proven path for Louisa's offerings and gives us vanilla stage/trigger/coordinate/derelict helpers for free. *Alternative considered*: a bespoke `InteractionDialogPlugin` scripted sequence — rejected as far more code for behavior the HubMission API already provides, and it would not integrate with the contact/bar offer flow the other devil missions use.

### Decision: Stage state machine — Start → Hop1 → Hop2 → Hop3 → Salvage → Complete
Use `BaseHubMission` stages with `setStageOnGlobalFlag`/`beginStageTrigger`-style wiring. Each hop stage spawns its clue entity and a guarding Legio fleet at a derived location via `triggerSpawnFleetAtLocation` (or equivalent), and uses an interaction/salvage `onCompletion` to advance. Defeating the guard + salvaging the cache advances the stage and reveals the next coordinate (set as the active map target / intel waypoint). Rationale: matches the vanilla breadcrumb questline pattern and keeps progress save-safe in mission/global memory.

### Decision: Escalating Legio fleets sized by hop
Hop fleets are built from the Legio faction at increasing FP (small → medium → large), hop 3 being the climax. Guards carry no officer/leader of note (faceless). Rationale: satisfies the "every system guides toward combat" credo and encodes the escalation the spec requires.

### Decision: Wreck delivered as a recoverable derelict with exactly 3 D-mods, peaceful salvage
At the Salvage stage spawn the Illustrious as a derelict/salvageable entity (no guarding fleet). Apply exactly 3 D-mods via `DModManager` and flag it recoverable so the player salvages it directly. Rationale: the hull is crewed (won't auto-recover like a drone), so recoverability is set explicitly for this encounter; 3 D-mods matches the "long-adrift with old battle damage" precedent. *Alternative considered*: granting the ship straight to the fleet via `AddShip` on dialog — rejected because salvaging the physical wreck is the intended payoff beat.

### Decision: One-time enforcement via global latch `$tahlan_illustriousRecovered`
`create()` returns `false` if the latch is set (or the mission is already active/done), mirroring how `DaemonSurplusShipHull` guards on `$tahlan_triggered`. The latch is set on recovery. Rationale: simplest save-safe non-repeat guard; `person_missions.csv` `reqMissionNone`/`freq` columns back it up.

### Decision: Restoration as a post-quest `callEvent` transaction on Louisa
After the latch is set, a rules.csv dialog option (gated on `$id == tahlan_devil` and `$tahlan_illustriousRecovered`, and not-yet-restored) invokes a `callEvent("restore", ...)` branch — like `DaemonCoreSale`'s `transact` — that locates the player's Illustrious, removes all D-mods, deducts the discounted price via `AddRemoveCommodity`, and sets a `$tahlan_illustriousRestored` flag. Rationale: reuses the established transaction idiom; keeps the gold-sink in dialog where the player expects it.

### Decision: Reputation/faction model
Gate on `person.relToPlayer.level >= COOPERATIVE`. Blackwatch and Legio are separate factions in code (`BLACKWATCH` vs `LEGIO`), so spawning hostile Legio guards creates no reputation conflict for a player allied to Louisa.

## Risks / Trade-offs

- **Derived clue/wreck locations could land somewhere degenerate** (player's own market, an empty/cramped system) → use vanilla `BaseHubMission` location-picker helpers with distance/criteria constraints, as the Galatia/breadcrumb missions do.
- **Crewed hull may resist normal recovery flow** (it's not `AUTOMATED_RECOVERABLE`) → explicitly flag the spawned wreck recoverable for this encounter and verify in-game salvage grants exactly one hull with 3 D-mods.
- **Restoration must target the *right* hull** if the player owns multiple ships or has renamed/modified it → identify the recovered member by hull id (and ideally a tagged member id stored at recovery) rather than by slot, and no-op gracefully if it's no longer in the fleet.
- **Save-load mid-quest** could desync stage vs spawned entities → store stage in mission/global memory (not plugin fields) and let vanilla HubMission persistence handle entity references, per the established pattern.
- **Latch must be set exactly once and only on success** → set `$tahlan_illustriousRecovered` at the salvage-completion step, not at accept, so an abandoned quest can still be retried (subject to design intent) while a completed one never re-offers.
- **Jar rebuild required** → the new class won't take effect until `jars/TahlanShipworks.jar` is rebuilt and committed.
