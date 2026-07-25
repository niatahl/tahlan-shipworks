## Why

The Legio siege is currently inert scenery: it presses on its target, but nothing in the world presses back. The besieged faction and its allies do not respond militarily, no incentive pulls uninvolved players in, and a player dismantling the siege faces zero counter-pressure. The siege should behave like a three-body conflict — the besieged bloc fights back on its own, bystander players are baited in with money, and players who hurt the siege become hunted — so it reads as a living war event whether or not the player engages.

## What Changes

- **Coalition intervention fleets (F3).** The besieged faction and its allies (Nexerelin alliance members when Nex is present; relationship ≥ WELCOMING otherwise) mobilize against the siege. The coalition member with the highest *response capacity* (best reachable military-industry market, distance-discounted) sends a primary intervention fleet sized to contest the command fleet; other members send capacity-scaled auxiliary detachments. Distance sets arrival delay, not eligibility. Killing the command fleet routes through the existing kill path (siege likely breaks); intervention fleets that die fighting siege forces strain command CR proportionally to their lost FP (a failed rescue still softens the siege). No faction-level reputation changes — non-hostile edge cases use fleet-local hostility only. Inbound interventions are announced on the siege intel as favourable one-time factors.
- **Desperation system bounty (F2).** When the subjugation meter reaches the Stranglehold threshold, the primary target market posts a **vanilla system bounty** (light `SystemBountyIntel` subclass registered through the vanilla `SystemBountyManager`) — the classic "Bounty Posted" feed item, with the "likely cause" attribution corrected to Legio and an optionally intensity-scaled base reward. Kept alive by periodic reset while the command fleet holds; allowed to lapse naturally through mop-up once the command is gone; vanilla behavior auto-handles the player-faction guard (no bounty posted by player markets) and auto-termination when a Nex capture transfers the market. Stacks with the existing accrued-bounty ledger.
- **Huntsman task force (F1).** Each siege fields one standing elite hunter-killer fleet, spawned alongside the command fleet and travelling with it. Composition is decided at spawn: Blackwatch roster before the Legio awakening (`$tahlan_triggered`), pure daemon roster (`tahlan_legiodaemons`) after — with fleet inflation post-filtered so only high-burn hulls serve (it must actually catch things). In-system it hunts by priority: (1) the player, once *heat* (accrued from player-involved siege-fleet kills, decaying daily) crosses a marking threshold; (2) threats to the siege — intervention fleets and hostile military patrols; (3) otherwise screens the command fleet. It is **not a siege fleet**: its death yields no siege-health damage, CR strain, meter knockback, or accrual bounty, and its hulls are unrecoverable — instead a replacement is dispatched from the siege's source market, making each kill buy a geography-dependent respite window rather than a permanent decapitation. Marking and replacement events are announced on the siege intel.

## Capabilities

### New Capabilities

<!-- none — all three systems are behaviors of the existing siege capability -->

### Modified Capabilities

- `legio-siege`: three ADDED requirements — coalition military intervention against the siege; a desperation-triggered vanilla system bounty on the besieged system; and a standing, regenerating huntsman task force with heat-based player marking. No existing requirements change. **Sequencing note:** the bounty trigger and heat mechanics reference the subjugation meter and Stranglehold stage introduced by `enhance-siege-intel-and-stakes` (in progress, code-complete); that change should be archived before this one's deltas apply so the base spec contains those concepts.

## Impact

- **Code (modified):** `SiegeManager.kt` (coalition assessment + intervention dispatch, bounty trigger/keep-alive, task force spawn/replacement, heat bookkeeping in `SiegeData`, new announcement hooks), `SiegeIntel.kt` (one-time factors for interventions, marking, huntsman replacement), `SiegeConfig.kt` (new tunables for all three systems), `SiegeFleetListener.kt` (unchanged path reused; interventions and task force use separate listeners).
- **Code (added):** `SiegeInterventionAI.kt` (intervention fleet assignment AI), `SiegeTaskForceAI.kt` (priority-targeting hunter AI), `SiegeSystemBountyIntel.kt` (light `SystemBountyIntel` subclass), small fleet-event listeners for intervention losses and task-force death.
- **Data:** new `siege_*` strings for announcements and fleet names in `data/strings/strings.json`.
- **Dependencies:** unchanged — Nexerelin stays a soft, guarded integration (`AllianceManager` alliance lookup behind `HAS_NEX`; relationship fallback without it). LunaLib optional as before. Vanilla `SystemBountyIntel` / `SystemBountyManager` / `BaseEventManager.addActive` are stable vanilla APIs (verified against 0.98a sources).
- **Save compatibility:** additive fields on `SiegeData` and new config/strings only; in-flight saves from the current test line are not preserved (clean-cut policy already in effect for this release line).
- **Balance:** intervention sizing (contest-not-crush), heat threshold/decay, task-force FP and replacement delay, and bounty scaling are all config-first with dev-mode balance passes expected; all three systems lengthen sieges, which (with the new gap-based launch timer) spaces sieges further apart.
- **Build:** rebuild + commit `jars/TahlanShipworks.jar` via IntelliJ (`build_project` regenerates the artifact).
