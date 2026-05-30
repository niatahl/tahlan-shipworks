## Why

The Illustrious (`tahlan_illustrious`), a unique Nightwatch command-ship, exists as a finished asset but is deliberately unobtainable (`no_bp_drop`, `HIDE_IN_CODEX`) with no path into the player's fleet. It is the kind of singular relic that warrants a bespoke acquisition. Louisa Ferre (`tahlan_devil`), the Blackwatch Special Agent contact, is the natural giver: the Blackwatch are the elite remnant of the old Nightwatch the Illustrious belongs to, and at high trust Louisa would rather see the relic in the player's fleet than fall to the corrupted Legio. This gives a unique hull a memorable, lore-grounded one-time questline that follows the design credo of guiding the player toward naval combat.

## What Changes

- Add a new **one-time** quest offered by Louisa Ferre, gated on COOPERATIVE reputation, that culminates in the player recovering the Illustrious.
- The quest is a **3-hop guarded clue chain**: Louisa points to the start of a trail, not the ship. Each hop is a Nightwatch dead-drop guarded by a faceless Legio fleet (escalating small → medium → large); defeating the guards and salvaging the cache decrypts the next coordinate. The hop-3 fight is the climax.
- The final **wreck site is undefended**; the Illustrious is found drifting and recovered via **peaceful salvage with exactly 3 D-mods**.
- After recovery, Louisa offers a **one-time discounted restoration** that clears all 3 D-mods — she brokers the yard work, the player still pays (a credit sink payoff).
- Enforce single-run via a **global memory latch** (`$tahlan_illustriousRecovered`) so the quest never re-offers once completed.
- Add new dialog/flavor text for the offer, each clue hop, the wreck discovery, and the restoration offer; slot a new dialog option alongside Louisa's existing three (repeatable) missions without altering them.

## Capabilities

### New Capabilities
- `illustrious-recovery-quest`: A one-time, reputation-gated questline given by Louisa Ferre that drives the player through an escalating multi-stage combat clue chain against Legio fleets and ends in salvaging the unique Illustrious capital ship, with a follow-up paid restoration to clear its D-mods.

### Modified Capabilities
<!-- None. Existing devil missions are untouched; this only adds a new sibling offering. -->

## Impact

- **Data**: `data/campaign/person_missions.csv` (register the new one-time mission under tag `tahlan_devil` with a min-rep gate); `data/campaign/rules.csv` (new dialog rows gated on `$id == tahlan_devil` for the offer, hops, wreck, and restoration); `data/strings/descriptions.csv` and/or rules text for new flavor.
- **Code**: new mission plugin in `jars/src/org/niatahl/tahlan/campaign/missions/devil/` mirroring `DaemonCoreSale.kt` (extends `HubMissionWithBarEvent` / `BaseHubMission`), driving stages, clue-site fleet spawns, derived coordinates, the final derelict spawn, the global latch, and the restoration transaction.
- **Assets**: existing `tahlan_illustrious` hull, variants, and descriptions are reused; no new ship art required.
- **Dependencies**: builds only on the existing vanilla HubMission API and the established devil-mission infrastructure; no new mod dependencies.
- **Save compatibility**: purely additive (new mission + memory flag); does not modify existing missions or saved state.
