## Why

The revived Legio siege (`redesign-legio-siege`, now complete) surfaces its status as a single text line — a CR-derived word (Entrenched / Strained / Faltering) plus an accruing bounty. The player can't see *how close the siege is to its objective*, and what pushes it forward or holds it back is invisible. Worse, **without Nexerelin a siege has no lasting consequence**: it runs a fixed 6-month timer and lifts with full market recovery, so ignoring it costs nothing. We want the siege to read like a vanilla **colony-crisis event** (a progress bar racing toward a climax) and to *hurt* if the player lets it win — in both Nexerelin and non-Nexerelin games.

## What Changes

- **Replace the CR-thermometer intel with a colony-crisis-style event UI.** `SiegeIntel` is reworked from a `BaseIntelPlugin` text panel into a `BaseEventIntel` renderer: a progress bar that fills toward the siege's goal, discrete stage markers (Foothold → Stranglehold → Climax) with hover tooltips and per-stage prose, and the two side-by-side factor tables. The bar measures the siege's advance; rising progress is bad for the player (red), the player's fleet kills knock it back (green). Command CR is demoted from "the headline stat" to an **all-progress brake factor**; blockade pressure, raid sorties, and intensity are the monthly push factors; fleet kills are one-time factors. `SiegeManager` stays authoritative and pushes the bar value (the event framework's economy-tick auto-driver is neutralized).
- **Unify the progress model across Nex / no-Nex.** The existing `captureProgress` is generalized into a single **subjugation meter** that advances in *both* modes while the command fleet is present, multiplied by command CR (the brake) and knocked back by fleet kills. Reaching the cap branches: with Nexerelin → capture the target market (unchanged); without Nexerelin (or a Nex-protected target) → apply a lasting scar.
- **BREAKING (vs `redesign-legio-siege` decision 7a.1): drop the fixed no-Nex lifetime and replace "no lingering scar" with a real consequence.** Both modes are now pure races with no clock (the old 6-month value migrates into the meter's fill-rate). On a successful no-Nex siege, the **primary target market only** receives: (1) a lingering, self-expiring "siege aftermath" market condition whose penalties are **half the active siege penalty, derived live** (so the scar always tracks the siege penalty / any future slider on it); and (2) **disruption of its core industries for the full scar duration**.
- **Add the supporting plumbing:** a new self-expiring `SiegeAftermathCondition`, a `tahlan_siegeaftermath` market-condition registration, new `SiegeConfig` tunables, and new externalized strings.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `legio-siege`: The **progressing-intel** requirement changes from a CR-stage thermometer to a colony-crisis event bar (bar = subjugation progress; CR surfaced as an all-progress brake factor; fleet kills as one-time factors). The **dual-resolution** requirement's no-Nex behaviour changes from a finite-lifetime lift with full recovery to an accumulator race with no fixed lifetime, where full subjugation leaves a lingering half-siege scar plus core-industry disruption on the target market. (Capability currently defined in the unarchived `redesign-legio-siege` change; archive that first so this change's deltas apply against a canonical base.)

## Impact

- **Code (rewritten):** `jars/src/org/niatahl/tahlan/campaign/siege/SiegeIntel.kt` (`BaseIntelPlugin` → `BaseEventIntel`).
- **Code (modified):** `SiegeManager.kt` (generalize the subjugation meter to both modes with the CR brake; kill knock-back + one-time factor; `syncProgress`; new `applyNoNexAftermath`; pass `HAS_NEX` to the intel). `SiegeConfig.kt` (new tunables; retune `CAPTURE_PROGRESS_PER_DAY_BASE`). `TahlanIDs.kt` (new condition id).
- **Code (added):** `SiegeAftermathCondition.kt` (self-expiring `BaseMarketConditionPlugin`).
- **Data:** register the condition in `data/campaign/market_conditions.csv`; new `siege_*` keys in `data/strings/strings.json`.
- **APIs reused:** `BaseEventIntel` / `EventFactor` / `BaseFactorTooltip`; `Industry.setDisrupted(...)`; `market.removeSpecificCondition(...)`; vanilla `events` sprite category (no new art).
- **Save compatibility:** changing `SiegeIntel`'s superclass needs a `readResolve()` that rebuilds `setup()` for in-flight sieges; the `SiegeStage`/`SiegeOutcome` enums and the `captureProgress` field name are kept. New condition/config/ids are additive.
- **Dependencies:** unchanged. LunaLib and Nexerelin remain soft, guarded. Feature still works without either.
- **Build:** must rebuild + commit `jars/TahlanShipworks.jar` via IntelliJ (no CLI build); compile-check via `mcp__ide__getDiagnostics`.
- **Balance:** generalizing the meter + adding the CR brake changes siege pace; `CAPTURE_PROGRESS_PER_DAY_BASE 0.3 -> ~0.6` is a starting value for an in-game balance pass.
