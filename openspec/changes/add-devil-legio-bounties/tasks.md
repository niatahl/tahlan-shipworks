## 1. Groundwork

- [x] 1.1 Read vanilla `missions/cb/CBDeserter.java` and `BaseCustomBountyCreator.java` from `starfarer.api.zip` and confirm the `beginFleet` → triggers → `createFleet` → `setRepChangesBasedOnDifficulty` → `data.baseReward` shape against the current game version
- [x] 1.2 Confirm `BaseCustomBounty.pickCreator()` still weights by `creator.getFrequency(mission, difficulty)` and skips creators outside `[getMinDifficulty(), getMaxDifficulty()]`, so a `0f` frequency is a hard exclusion
- [x] 1.3 Add any new id constants (creator ids, the clean-kill memory/data key) to `utils/TahlanIDs.kt` following the existing namespacing

## 2. Deserter creator (bands 1–2)

- [x] 2.1 Add `CBLegioDeserter` in `campaign/missions/devil/`, modelled on vanilla `CBDeserter`, keeping `triggerSetFleetFaction(Factions.PIRATES)` and `triggerSetStandardAggroPirateFlags()` so the contract stays consequence-free
- [x] 2.2 Replace vanilla's giver-faction composition with the difficulty ladder from design D4: `tahlan_legioinfernalis`/DEFAULT at 4–5, `tahlan_legioinfernalis`/HIGHER at 6–7, `tahlan_legioelite`/HIGHER at 8+
- [x] 2.3 Set `data.repPerson` / `data.repFaction` to the vanilla difficulty-scaled values (design D8) and leave the payout at the `DESERTER_MULT` baseline
- [x] 2.4 Set the fleet name and `getBountyNamePostfix()` so the target reads as a Legio deserter, not a Blackwatch one

## 3. Purge creator (band 3)

- [x] 3.1 Add `CBLegioPurge` in `campaign/missions/devil/`, composing the target fleet from the Blackwatch roster (`tahlan_legioelite`)
- [x] 3.2 Flag the fleet `tahlan_legioinfernalis` via `triggerSetFleetFaction(LEGIO)` and apply `triggerMakeHostile()` **without** `triggerMakeLowRepImpact()` — verify no `triggerSetStandard*Flags()` helper sneaks the flag back in (design D2/D3)
- [x] 3.3 Gate on FRIENDLY by returning `0f` from `getFrequency()` when `person.relToPlayer.level.ordinal < RepLevel.FRIENDLY.ordinal` (design D7)
- [x] 3.4 Set the base payout multiplier to ~1.2× and `data.repPerson` to `VERY_HIGH`, with an elevated `data.repFaction` (design D8)
- [x] 3.5 Choose `getMinDifficulty()`/`getMaxDifficulty()` so purge contracts sit at the upper end of the dossier range and tune `FleetSize`/`FleetQuality` against the daemon-heavy Blackwatch roster

## 4. Clean-kill bonus

- [x] 4.1 Add the clean-kill predicate as a helper: transponder off AND no non-target fleet of the target's faction with `COMPOSITION_AND_FACTION_DETAILS` visibility of the target (design D5)
- [x] 4.2 Override `reportBattleOccurred` in `DevilCustomBounty.kt` to sample the predicate, raise the credit reward, stash the verdict on `CustomBountyData`, then delegate to `super` — in that order (design D6)
- [x] 4.3 Guard the bonus so it applies only to purge bounties, never to deserter or vanilla-creator contracts
- [x] 4.4 Surface the verdict in the completion text / intel so the player can see whether the bonus was earned and why

## 5. Creator registration & variety

- [x] 5.1 Add `CBLegioDeserter` and `CBLegioPurge` to `DevilCustomBounty.CREATORS`
- [x] 5.2 Add vanilla `CBTrader`, `CBDerelict`, `CBPatrol`, and `CBRemnantPlus` to `CREATORS`
- [x] 5.3 Sanity-check the resulting frequency mix so the four originally shipped creators do not become rare; adjust per-creator frequency multipliers rather than removing entries

## 6. Dialog & flavor text

- [x] 6.1 Add `*OfferDesc` rows in `data/campaign/rules.csv` for each newly registered creator, gated on `$id == tahlan_devil score:10`, following the existing `tahlan_CBPatherDevil` / `tahlan_CBMercDevil` pattern
- [x] 6.2 Write deserter offer text — outlaws the Legio already disowned, no one cares who kills them
- [x] 6.3 Write purge offer text that states the clean-kill condition plainly (transponder off, no witnesses), voiced as Louisa charging for cleanup rather than paying a bounty (design D10)
- [x] 6.4 Write offer text for the four added vanilla creators in her register
- [x] 6.5 Add completion text distinguishing a clean kill from an identified one

## 7. Build & verification

- [x] 7.1 Rebuild `jars/TahlanShipworks.jar` with the new creators and commit it
- [ ] 7.2 Verify the FRIENDLY gate: no purge contract appears in any of the three dossiers below FRIENDLY; purge contracts appear at and above it
- [ ] 7.3 Verify deserter contracts cost no Legio standing and raise no witness alarm regardless of transponder state
- [ ] 7.4 Verify purge contracts cost Legio standing when engaged with the transponder on, and cost nothing when killed dark and unwitnessed
- [ ] 7.5 Verify the witness path: kill a purge target dark with another Legio fleet holding visibility, and confirm the alarm propagates and no bonus is paid
- [ ] 7.6 Verify the clean-kill bonus pays the higher amount, that a Legio fleet arriving after the kill does not revoke it, and that running dark never fails a bounty
- [ ] 7.7 Verify deserter fleet composition at low, mid, and high difficulty matches the D4 ladder
- [ ] 7.8 Verify Blackwatch reputation accrues, weighted higher for purge work, and that Blackwatch is still absent from the intel tab
- [ ] 7.9 Verify the four originally shipped creators, `DaemonCoreSale`, `DaemonSurplusShipHull`, and `IllustriousRecovery` are unchanged in behavior
- [ ] 7.10 Verify save/load with a purge bounty accepted but not completed preserves the contract and its reward configuration
- [x] 7.11 Add a changelog entry for the new bounty types
