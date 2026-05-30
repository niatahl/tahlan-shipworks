## Context

The Traumtänzer derelict is placed by `DerelictsSpawnScript.java`. In the traum branch it is spawned via `addDerelict(...)`, which:
- builds a `WRECK` salvage entity through `BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, DerelictShipData)`,
- when `recoverable`, attaches a stock recovery special via `Misc.setSalvageSpecial(ship, new SalvageSpecialAssigner.ShipRecoverySpecialCreator(...).createSpecial(ship, null))`,
- attaches the Remnant guards via `Misc.setDefenderOverride(ship, defenders)`.

The traum branch already forces `BATTERED` condition and sets the Remnant `DefenderDataOverride`. So the seam for this change is precise: **the salvage-special slot** (`Misc.setSalvageSpecial`) on this one entity, leaving placement, condition, and defenders intact.

Henrietta (`tahlan_henrietta`) already exists and is balanced; the Rosenritter questline only stores the location in `$tahlan_traum_location`. Neither is touched. Event prose is placeholder for hand-rewrite.

## Goals / Non-Goals

**Goals:**
- Swap the Traumtänzer's default recovery for a one-time, two-outcome salvage dilemma.
- Save-ship → recover the BATTERED hull; save-crew → recruit Henrietta + small crew/marines; both permanently latch.
- Keep the Remnant guards as the combat/progression gate.
- Read cold (no questline dependency); optional richer text if the questline is done.

**Non-Goals:**
- No changes to Henrietta (stats/skills/tags), the Traumtänzer hull/variant, the questline scripts, or the derelict's placement/condition/defenders.
- No "save both" path; no mechanical penalty on the dark choice.
- No polished prose (placeholder only).

## Decisions

### Decision: Intercept via a custom salvage special, not a global interaction override
Attach a **custom salvage-special plugin** to the traum entity in the traum branch of `DerelictsSpawnScript`, in place of `ShipRecoverySpecialCreator`. Rationale: the entity is a `WRECK` whose interaction already routes through the salvage-special slot, so replacing that slot is the most localized, idiomatic interception — it inherits the "approach the derelict, choose to salvage" framing for free and touches exactly one entity. *Alternative considered*: route through `CampaignPluginImpl.pickInteractionDialogPlugin` keyed on a memory flag (the boss-FID pattern from `scripted_dialog.md`). Viable and avoids salvage-special registration, but it intercepts the whole interaction rather than just the salvage step and is broader than needed. **Fallback**: if registering a custom salvage special proves awkward against the API, use the `pickInteractionDialogPlugin` + entity-memory-flag approach instead; the outcomes and latch are identical either way. The exact registration mechanism (special id + plugin class wiring, cf. how `ShipRecoverySpecial` is registered) is an implementation detail to confirm during apply.

### Decision: The dilemma plugin owns presentation and both outcomes
The custom special/dialog presents two options and applies the chosen outcome directly:
- **Save ship**: add the Traumtänzer (`tahlan_schneefall_traum`/`..._albtraum` variant as currently spawned) to the player fleet in `BATTERED` condition — reusing the same recovery the stock special would have produced — then set the latch and clear the entity.
- **Save crew**: remove/destroy the derelict entity, add `tahlan_henrietta` as a fleet officer (`Global.getSector().getPlayerFleet().getFleetData().addOfficer(...)` style), add a small crew + marines to cargo, set the latch.
Rationale: keeping both outcomes in one place keeps the mutual exclusivity and the single latch trivially correct.

### Decision: One global latch, checked before presenting
Use a single global memory flag (e.g. `$tahlan_traumResolved`). The dilemma is only offered when unset; both outcomes set it. Rationale: matches the established latch pattern in the codebase (e.g. `$tahlan_triggered`, the Illustrious latch) and makes "resolves exactly once" a one-line guard. The entity is removed/consumed on resolution regardless, so the latch is belt-and-suspenders against re-entry.

### Decision: Cold-readable, questline-aware optional layer
The interaction text stands alone. If a questline-completion flag is present, show extra context, but branch only the *flavor*, never the *options*. Rationale: the derelict is explicitly findable without the questline; the choice must be identical in both cases.

### Decision: Wire questline intel cleanup through the same resolution flag (+ a final-stage check)
The Rosenritter questline (`regaliablueprintscript` → `regaliablueprintintel`) is a single "Abandoned Carrier" intel that walks blueprint stages; at the 4th blueprint it reaches `END_OF_BLUEPRINTS`, calls `endAfterDelay()`, and its text begins pointing at `$tahlan_traum_location`. That completed-but-lingering pointer is the clutter to remove. Two behaviors, both keyed on the dilemma's `$tahlan_traumResolved` latch:
- **Before final stage**: `regaliablueprintintel`'s end-stage text branches on `$tahlan_traumResolved` — if set, it omits the constellation-pointer line (the blueprint completion text still shows). This is a pure, decoupled flag read; no reference from the questline back to the dilemma.
- **After final stage**: on resolution, `TraumDilemma.finish()` looks up the questline intel via `IntelManagerAPI.getFirstIntel(regaliablueprintintel.class)` and, **only if it is at its final stage**, `removeIntel`s it. The final-stage gate is exposed as a public `isAtFinalStage()` accessor on the intel (avoids relying on Java protected-field visibility from Kotlin in another package — they happen to share a package, but the accessor is explicit and safe).
Rationale: one flag drives both directions; the final-stage gate ensures a mid-decryption resolution leaves the still-useful blueprint intel alone (only the eventual pointer is suppressed). Cold finds are a natural no-op (`getFirstIntel` returns null). *Alternative considered*: have the questline script poll for the ship/flag and tear itself down — rejected as more moving parts than a flag read + a one-shot removal at resolution.

### Decision: No mechanical penalty on the dark choice
Saving the ship (killing the crew) applies no rep/CR/morale penalty — narrative weight only. Rationale: matches the stated design intent; isolated as a single easily-flippable spot if that ever changes.

## Risks / Trade-offs

- **Custom salvage-special registration may be fiddly** → the design names a concrete fallback (`pickInteractionDialogPlugin` + entity memory flag) with identical outcomes, so the approach can switch without redesign.
- **"Save ship" must reproduce the stock recovery faithfully** (BATTERED hull, correct variant) → reuse the same `DerelictShipData`/recovery the stock special would have used rather than hand-rolling the add, so condition and variant stay correct.
- **Officer add must be the exact `tahlan_henrietta` instance** (her tuned build) → fetch from `importantPeople`/`TahlanPeople` rather than creating a fresh person, so her balanced skills/tags are preserved.
- **Entity cleanup on resolution** → ensure the derelict is removed/consumed in both branches so a resolved site can't be re-interacted; the latch is the secondary guard.
- **Save/load mid-encounter** → store the latch in global memory (not plugin fields); the entity + special persist via vanilla salvage persistence.
- **Jar rebuild required** → the new plugin won't take effect until `jars/TahlanShipworks.jar` is rebuilt in IntelliJ.
