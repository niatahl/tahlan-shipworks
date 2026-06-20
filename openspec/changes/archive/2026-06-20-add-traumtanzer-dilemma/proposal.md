## Why

The Rosenritter capstone — the unique Traumtänzer derelict — currently resolves as a plain `ShipRecoverySpecial`: clear the Remnant guards, salvage the ship, done. That makes the capstone a single fixed reward (a powerful but unique, off-theme hull) which is a poor fit for themed/faction runs where a lone Rosenritter battlecruiser breaks the fleet's aesthetic and doctrine. The named officer Henrietta von Regenfels is already built and balanced but wired into nothing. This change joins the two: it turns the salvage into a one-time moral dilemma that forks the capstone into two differently-shaped rewards — the ship, or the people (including Henrietta).

## What Changes

- Replace the default recovery on the Traumtänzer derelict (`tahlan_schneefall_traum_albtraum`) with a **custom salvage dilemma** presented on interaction: reserve power is keeping the crew's cryopods alive while the reactor fails, and the two cannot both be saved.
- Add two **mutually-exclusive, irreversible** outcomes:
  - **Save the ship** — cut the cryopods (crew dies), reactor stabilizes → recover the BATTERED Traumtänzer.
  - **Save the crew** — defrost them, reactor collapses → ship destroyed → recruit Henrietta (`tahlan_henrietta`) as a fleet officer + a small crew/marines flavor bonus.
- Add a one-time global resolution flag (e.g. `$tahlan_traumResolved`) so the encounter cannot be repeated or re-exploited.
- The interaction must read **cold** (no questline assumed), with an optional richer text branch if the Rosenritter questline was completed.
- Keep the existing Remnant defender override as a **combat + progression gate** (fight to reach the reward; blocks earlygame power-skipping).

Non-goals / explicitly unchanged: Henrietta's stats, skills, and tags; the Traumtänzer hull/variant; the Rosenritter questline scripts; and the derelict's placement/condition/defenders in `DerelictsSpawnScript`. Only the salvage interaction/outcome behavior changes. Event/dialog prose is placeholder for hand-rewrite by the author.

## Capabilities

### New Capabilities
- `traumtanzer-dilemma`: A one-time, mutually-exclusive salvage choice on the Traumtänzer derelict that forks the Rosenritter capstone into either recovering the unique ship or rescuing its frozen crew (recruiting Henrietta), gated behind the existing Remnant guards and latched so it resolves exactly once.

### Modified Capabilities
<!-- None. The existing Rosenritter questline, Henrietta, and derelict placement are untouched; this only swaps the interaction behavior on the already-placed derelict. -->

## Impact

- **Code**: `jars/src/org/niatahl/tahlan/campaign/DerelictsSpawnScript.java` (traum branch: attach the custom dilemma special instead of the stock `ShipRecoverySpecial`); a new custom salvage-special plugin or scripted `InteractionDialogPlugin` for the dilemma; possibly `plugins/CampaignPluginImpl.kt` if the interaction is routed via `pickInteractionDialogPlugin`. New id/flag constants in `utils/TahlanIDs.kt`.
- **Data**: any salvage-special registration the chosen approach requires (e.g. config CSV/JSON); placeholder strings only.
- **Reuses (unchanged)**: Henrietta (`tahlan_henrietta`), the Traumtänzer hull/variant, the Remnant `DefenderDataOverride`, and the existing placement in `DerelictsSpawnScript`.
- **Save compatibility**: additive interaction behavior + one global flag. Existing saves where the derelict is unspawned/unresolved pick up the new behavior; an already-salvaged derelict is gone and unaffected.
- **Dependencies**: vanilla salvage-special / interaction-dialog API only; no new mod dependencies.
