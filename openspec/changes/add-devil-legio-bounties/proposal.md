## Why

Every offer Louisa Ferre (`tahlan_devil`) makes is transactional: three repeatable "kill someone / buy something" missions plus the one-time Illustrious quest. Her highest-frequency offer by a wide margin is `tahlan_devilCB` (freq 20), and it draws on only four bounty creators (`CBMerc`, `CBPirate`, `CBPather`, `CBRemnant`) out of the thirteen vanilla ships — so the mission the player sees most often is also the one that repeats fastest.

Worse, none of that work is *Blackwatch* work. Louisa is a Blackwatch Special Agent — internal security for the Legio Infernalis — and nothing she currently asks for reflects that. Her reputation ladder is equally thin: WELCOMING → FRIENDLY → COOPERATIVE presently decides only which tier of daemon core she will sell.

This change turns her bounty table into a trust ladder that ends in the work she would actually be doing: hunting Blackwatch's enemies inside the Legio, deniably, with the player as the instrument.

## What Changes

- **Add `CBLegioDeserter`** (bands 1–2) — genuine Legio deserters who have cut ties and are disowned by the Legio. Pirate-flagged and `LOW_REP_IMPACT` per the vanilla standard, so killing them is consequence-free. Fleet **composition** ladders by difficulty from the standard Legio roster toward the Blackwatch roster (higher-value deserters took better ships with them), fixing the flaw in vanilla `CBDeserter`, which composes from the giver's faction and would make every low-tier deserter arrive daemon-heavy.
- **Add `CBLegioPurge`** (band 3) — Blackwatch's internal enemies among the Legio. Gated at **FRIENDLY** via `getFrequency() → 0f`. Genuinely `tahlan_legioinfernalis`-flagged with **full reputation impact** (the `triggerMakeLowRepImpact()` call is deliberately omitted), which also re-enables the vanilla `TOffAlarm` witness mechanic that flag otherwise disables.
- **Clean-kill payout bonus.** A purge executed with the transponder off and no surviving same-faction witnesses pays a bonus. The verdict is sampled at the moment of the kill — by the time the reward resolves, the target is gone and the witness picture has changed — and carried forward on `CustomBountyData`. Stealth becomes a rewarded skill rather than the avoidance of a penalty a Legio-hostile player does not care about.
- **Add four vanilla creators** to bands 1–2 for variety at near-zero cost: `CBTrader`, `CBDerelict`, `CBPatrol`, `CBRemnantPlus`.
- **Weight the reputation rewards.** Purge work grants substantially higher personal reputation with Louisa, making it the fast lane from FRIENDLY to COOPERATIVE (and thence to archdaemon cores and the Illustrious quest). Blackwatch faction reputation accrues but remains invisible and gates nothing — deliberately banked as groundwork for future Sarah Tann (`tahlan_queen`) content.
- **New rules rows** for each added creator's offer text in Louisa's voice, following the existing `CBPatherDevil` / `CBMercDevil` pattern.

Not changing: the existing four creators, `DaemonCoreSale`, `DaemonSurplusShipHull`, `IllustriousRecovery`, and every current reputation gate are untouched.

## Capabilities

### New Capabilities
- `devil-legio-bounties`: A reputation-tiered bounty progression offered by Louisa Ferre, escalating from consequence-free hunts of disowned Legio deserters to deniable strikes against Blackwatch's internal enemies within the Legio, where reputation exposure is real and transponder discipline is rewarded with a payout bonus.

### Modified Capabilities
<!-- None. Existing devil missions and reputation gates are untouched; this extends the creator list of an existing mission and adds new sibling creators. -->

## Impact

- **Code**: new creators in `jars/src/org/niatahl/tahlan/campaign/missions/devil/` extending `BaseCustomBountyCreator`; `DevilCustomBounty.kt` gains the new creators in `CREATORS` and an override of `reportBattleOccurred` to sample the clean-kill verdict before delegating to `super`.
- **Data**: `data/campaign/rules.csv` — new `*OfferDesc` rows gated on `$id == tahlan_devil score:10` for each added creator, mirroring the existing Devil bounty rows. No `person_missions.csv` change: the new creators ride the existing `tahlan_devilCB` entry.
- **Dependencies**: vanilla `missions/cb` API only (`BaseCustomBountyCreator`, `CBStats`, `CustomBountyData`) plus `HubMissionWithTriggers` fleet triggers. No new mod dependencies.
- **Reputation surfaces**: Blackwatch (`tahlan_legioelite`) remains `showInIntelTab: false`; no faction file is edited. Legio Infernalis standing can now move as a *consequence* of sloppy purge work, which is the intended stake.
- **Save compatibility**: purely additive. New creators only appear in newly generated bounty offers; in-flight bounties and existing saves are unaffected.
