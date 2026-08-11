# Proposal: add-siege-planetfall

## Why

Under Nexerelin — the intended way to play Legio content — a successful siege currently ends as an
administrative event: the instant the subjugation meter fills, `SectorManager.transferMarket` flips
the market in the same tick, Nex posts its generic capture intel, and the command fleet never
leaves its jump-point anchor. There is no final assault to witness or interrupt, the planet
transfers in pristine condition, and the climax of a months-long campaign carries no Legio voice.
This change turns the capture into a visible, interruptible planetfall sequence.

## What Changes

- **New PLANETFALL siege stage (Nex path only).** When the subjugation meter fills for a
  Nex-capturable target, the siege enters a timed planetfall window (~5–7 days, tunable) instead of
  transferring the market instantly. The transfer fires only when the window completes with the
  command fleet alive over the planet.
- **Siege force converges on the planet.** The command fleet leaves its fringe anchor and moves to
  orbit the target planet; escort, blockade, and raid fleets are re-tasked to support the landing.
  The huntsman task force follows the command fleet (it already screens it).
- **The defending navy gives up.** Military fleets of the victim faction and its coalition in the
  target system receive flee orders and leave (re-swept periodically to catch fresh patrol spawns).
  Player-faction fleets are exempt; trade fleets and third parties are untouched.
- **The station is starved out.** The target market's orbital station industry is disrupted at
  planetfall start — months of blockade have emptied its magazines — so it cannot contest the
  landing. Legio inherits the recovering station on capture.
- **Interventions turn back.** No new coalition relief waves launch during planetfall, and in-flight
  intervention fleets return home. The player is the only possible savior in the window.
- **Point of no return, with a decapitation out.** The meter stops mattering once planetfall starts
  (no more knockback). Breaking the siege remains possible: siege health reaching zero breaks it,
  and killing or driving off the command fleet mid-planetfall breaks it **instantly** — the landing
  collapses and the remaining fleets scatter home. LIFTED validity checks (peace, deciv, ownership
  change) keep running.
- **Owned, flavored success moment.** Nex's generic `MarketTransferIntel` is suppressed
  (`silent=true` overload); the siege's own SUCCEEDED intel becomes the single dramatic beat, with
  Legio-flavored prose and a distinct tone when the player fought the siege and lost it.
- **Occupation aftermath.** The captured market does not flip pristine: core industries are
  disrupted and the conquest reads as conquered — a natural retake target.
- **Intel presentation.** Feed ping and one-time factor when planetfall begins; CLIMAX-stage strings
  describe the planetfall window instead of an instant capture; the countdown is surfaced.

Not changing: the no-Nex devastation path keeps its current instant resolution (explicitly out of
scope). No breaking changes; save-compat is preserved (old saves can never be in the new stage).

## Capabilities

### New Capabilities

_None — all changes extend the existing `legio-siege` capability._

### Modified Capabilities

- `legio-siege`: The "Dual resolution pathway gated on Nexerelin" requirement changes — the Nex
  capture no longer fires instantly at full subjugation but via a new timed planetfall stage with
  convergence, defender flight, station starve-out, intervention stand-down, an instant-BROKEN
  decapitation rule, a suppressed Nex notification in favor of flavored siege intel, and an
  occupation aftermath on the captured market. The "Progressing intel" requirement gains the
  planetfall announcement/countdown. The "Coalition military intervention" requirement gains the
  planetfall stand-down. The "Tunable constants" requirement gains the new planetfall knobs.

## Impact

- **Code:** `jars/src/org/niatahl/tahlan/campaign/siege/` — `SiegeManager.kt` (stage lifecycle,
  convergence, defender sweep, station disruption, planetfall resolution rules, silent transfer,
  aftermath), `SiegeAssignmentAI.kt` (new PLANETFALL phase via fleet-memory flag),
  `SiegeBlockadeAI.kt` (stand-down from jump-point station / trader interception),
  `SiegeIntel.kt` (planetfall factor, countdown, flavored SUCCEEDED prose), `SiegeConfig.kt`
  (new knobs), `SiegeConsoleCommands.kt` (debug visibility for the new stage).
- **Data:** `data/strings/strings.json` — new `siege_*` keys (assignment labels, intel prose,
  planetfall announcements); all player-facing text externalized per project convention.
- **Dependencies:** Nexerelin soft integration only — one additional `transferMarket` overload
  parameter (`silent`), behind the existing `ModCompat.HAS_NEX` guard. No new hard dependencies.
- **Save compatibility:** New stage enum value and timer field on the serialized manager; old saves
  can never be in PLANETFALL, and any non-null field added to the manager follows the existing
  `@Transient` + lazy-getter pattern.
