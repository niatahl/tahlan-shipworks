## 1. Mission registration & one-time latch

- [x] 1.1 Add a `$tahlan_illustriousRecovered` (and `$tahlan_illustriousRestored`) memory-flag convention; document the keys (e.g. in `TahlanIDs.kt`)
- [x] 1.2 Create `IllustriousRecovery` plugin in `jars/src/org/niatahl/tahlan/campaign/missions/devil/` extending `HubMissionWithBarEvent`, with `create()` returning false when the latch is set or the mission is already active
- [x] 1.3 Register the mission in `data/campaign/person_missions.csv` under tag `tahlan_devil` with a `min rep` set to the COOPERATIVE threshold and non-repeat columns (`reqMissionNone`/low `freq`)
- [x] 1.4 Call `setPersonMissionRef(person, "$tahlan_illustrious_ref")` and gate `create()` on `person.relToPlayer.level >= COOPERATIVE`

## 2. Clue-chain state machine

- [x] 2.1 Define stages: Start → Hop1 → Hop2 → Hop3 → Salvage → Complete, stored in mission/global memory (not plugin fields)
- [x] 2.2 Implement per-hop clue-site location derivation using `BaseHubMission` location-picker helpers with distance/quality constraints (avoid player market / empty systems)
- [x] 2.3 Spawn the Nightwatch dead-drop entity at each hop site
- [x] 2.4 Spawn an escalating faceless Legio guarding fleet per hop (small → medium → large, hop 3 = climax), no named officer/leader
- [x] 2.5 Wire stage advancement: defeating the guard + salvaging the cache advances the stage and reveals the next coordinate as the active waypoint
- [x] 2.6 On hop-3 cache salvage, reveal the wreck location and advance to the Salvage stage

## 3. Wreck & recovery

- [x] 3.1 At the Salvage stage, spawn the drifting Illustrious as a recoverable derelict at the revealed location with NO guarding fleet
- [x] 3.2 Apply exactly 3 D-mods to the spawned hull via `DModManager` and flag it recoverable (it is crewed, not auto-recoverable by default)
- [x] 3.3 On successful salvage, grant a single `tahlan_illustrious` hull to the player and set the `$tahlan_illustriousRecovered` latch; tag the recovered member for later restoration lookup
- [x] 3.4 Complete/close out the mission and intel entry cleanly

## 4. Restoration transaction

- [x] 4.1 Add a `callEvent("restore", ...)` branch on the mission/contact that locates the player's recovered Illustrious, removes all D-mods, deducts the discounted price (`AddRemoveCommodity`), and sets `$tahlan_illustriousRestored`
- [x] 4.2 Compute the discounted restoration price (significant discount off normal restoration cost) and expose it to dialog via `updateInteractionDataImpl()`
- [x] 4.3 Handle the cannot-afford case (no deduction, no D-mod removal) and the ship-not-in-fleet case gracefully

## 5. Dialog & flavor text

- [x] 5.1 Add the quest-offer dialog rows in `data/campaign/rules.csv` gated on `$id == tahlan_devil` + COOPERATIVE, slotted alongside the existing three devil offerings without altering them
- [x] 5.2 Write offer / accept flavor text capturing Louisa's spite-driven motive (deny the Legio, trust in the player)
- [x] 5.3 Write per-hop flavor text (dead-drop discovery, Legio interference, decrypted coordinate) for the 3 clue sites
- [x] 5.4 Write the wreck-discovery flavor text (drifting, silent, the Legio never found it)
- [x] 5.5 Add the restoration-offer dialog rows gated on `$tahlan_illustriousRecovered` and not-yet-restored, with the discounted-price transaction option
- [x] 5.6 Add any required ship/intel descriptions to `data/strings/descriptions.csv`

## 6. Build & verification

- [ ] 6.1 Rebuild `jars/TahlanShipworks.jar` with the new mission class and commit it
- [ ] 6.2 Verify gating: quest hidden below COOPERATIVE, offered at/above it, never re-offered after completion, existing three devil missions still work
- [ ] 6.3 Verify the clue chain end-to-end: 3 escalating guarded fights, coordinate hand-offs, undefended wreck, salvage grants one Illustrious with exactly 3 D-mods
- [ ] 6.4 Verify restoration: discounted price deducted, all D-mods cleared, offered only once, cannot-afford and missing-ship cases handled
- [ ] 6.5 Verify save/load mid-quest preserves stage and spawned entities
