## Context

`DevilCustomBounty` extends vanilla `BaseCustomBounty` and supplies a `CREATORS` list of four `CustomBountyCreator`s. `BaseCustomBounty.pickCreator()` builds a `WeightedRandomPicker` over that list, weighting each entry by `creator.getFrequency(mission, difficulty)` and skipping any creator whose `[getMinDifficulty(), getMaxDifficulty()]` band excludes the rolled difficulty. Three dossiers (LOW / NORMAL / HIGH) are rolled per offer; difficulty adapts to the player's last three completed bounties.

Relevant existing gates on Louisa's table:

| Offer | Gate |
|---|---|
| `DaemonCoreSale` | WELCOMING → BETA, FRIENDLY → +BETA_WITH_GAMMA, COOPERATIVE → +ALPHA |
| `DaemonSurplusShipHull` | `person.relToPlayer.rel >= 0.5f` (FRIENDLY) + `$tahlan_triggered` |
| `IllustriousRecovery` | COOPERATIVE, one-time |

Player↔`tahlan_legioinfernalis` starts at `-0.8` HOSTILE (`FactionRelationPlugin`). Blackwatch (`tahlan_legioelite`) is a separate faction with `showInIntelTab: false`. Louisa's faction is Blackwatch, so `setRepRewardFaction(Float)` — which takes no faction id and credits the mission giver's faction — already targets Blackwatch with no override.

## Goals / Non-Goals

**Goals:**
- Increase the variety of Louisa's most frequent offer without touching the four creators already shipped.
- Give the reputation ladder a second, *work*-shaped axis alongside the existing commerce axis.
- Make transponder discipline a rewarded skill on the high-tier work, not merely the avoidance of a penalty.
- Accrue Blackwatch standing as groundwork for later Sarah Tann content.

**Non-Goals:**
- No courier / extraction / procurement missions — deferred to a separate change.
- No changes to `DaemonCoreSale`, `DaemonSurplusShipHull`, or `IllustriousRecovery`, and no re-gating of already-shipped content (would re-lock content in existing saves).
- No Blackwatch reputation *gates*. The currency accrues invisibly and unlocks nothing in this change.
- No edit to `tahlan_legioelite.faction`; Blackwatch stays out of the intel tab.
- Louisa does not become recruitable, and no Nightwatch hull becomes obtainable here.

## Decisions

### D1 — Two creators, not one parameterised creator

The low and high tiers differ in faction flag, reputation impact, fleet composition source, payout multiplier, and reward weighting. A single creator branching on reputation would carry two disjoint configurations behind one `createBounty()`. Two creators keep each configuration readable and let `getFrequency()` do the gating for free.

*Alternative considered:* one creator with a `Variation` enum, mirroring `DaemonCoreSale.pickVariation()`. Rejected — that pattern fits when variations share a code path and differ only in data. Here almost nothing is shared.

### D2 — Deserters stay pirate-flagged; purges do not

`CBLegioDeserter` keeps vanilla's `triggerSetFleetFaction(Factions.PIRATES)` and `triggerSetStandardAggroPirateFlags()`. These are *legitimate* deserters — the Legio has disowned them, so killing them costs nothing and nobody cares who did it. This is correct as vanilla ships it.

`CBLegioPurge` sets `triggerSetFleetFaction(LEGIO)` and calls `triggerMakeHostile()` **without** `triggerMakeLowRepImpact()`. There is no vanilla helper for "hostile, full consequences" — every `triggerSetStandard*Flags()` variant bundles `LOW_REP_IMPACT`, which is exactly why ordinary bounties are consequence-free. Omitting it is the whole mechanism. `IllustriousRecovery.spawnClueGuard()` already does this, so band 3 follows an established in-repo pattern rather than inventing one.

### D3 — Omitting `LOW_REP_IMPACT` also re-arms the witness system

`TOffAlarm.notifyNearby()` returns early when the fleet carries `MEMORY_KEY_LOW_REP_IMPACT` and not `SPREAD_TOFF_HOSTILITY_IF_LOW_IMPACT`. Omitting the flag therefore does two things at once: restores the reputation penalty, and switches the alarm back on so nearby same-faction fleets can witness the strike and turn hostile.

This is load-bearing, not incidental — it is what makes stealth a *system* rather than a single boolean, and it is the reason the clean-kill predicate (D5) can reuse the game's own criterion.

### D4 — Deserter fleet composition ladders off difficulty

Vanilla `CBDeserter` composes from `mission.getPerson().getFaction()`. For Louisa that is Blackwatch, whose roster weights `tahlan_daemon` 12 / `tahlan_legio` 6 / `tahlan_nightwatch` 0.5 — so every low-tier deserter would arrive daemon-heavy, which is wrong for "someone bolted with a ship". `triggerCreateFleet()` takes the composition faction as a parameter, so the fix is to pass the right id and ladder it:

```
difficulty 4–5    tahlan_legioinfernalis, DEFAULT quality   rank-and-file who bolted
difficulty 6–7    tahlan_legioinfernalis, HIGHER quality    took a good ship with them
difficulty 8–10   tahlan_legioelite,      HIGHER quality    took something they shouldn't have
```

The escalation doubles as fiction: higher-value deserters are worth more because of what they left with.

### D5 — Clean-kill predicate mirrors `TOffAlarm`

```kotlin
val dark = !Global.getSector().playerFleet.isTransponderOn()
val seen = target.containingLocation.fleets.any {
    it !== target &&
    it.faction === target.faction &&
    it.getVisibilityLevelTo(target) == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS
}
val clean = dark && !seen
```

This is `TOffAlarm.notifyNearby()`'s loop condition with the flag-setting swapped for a boolean. Using the same criterion means the player learns **one** rule governing both the reputation penalty and the bonus: if the alarm would have spread, there is no bonus.

*Alternatives considered:*
- Transponder state alone — trivially gamed by executing the target in front of a Legio armada while dark.
- Adding `!target.knowsWhoPlayerIs()` — the engine's own identification predicate, and it would catch opening comms before shooting. Rejected because it asks about the fleet being destroyed, and a dead crew files no report. Left as a possible later tightening.

### D6 — The verdict is sampled at the kill, carried forward on `CustomBountyData`

Custom bounties self-complete: `BaseCustomBounty.reportBattleOccurred()` sets `$<id>_completed`, the stage advances to COMPLETED, `endSuccessImpl()` calls `creator.notifyCompleted()`, and the reward resolves — all without a return trip to the giver. By the time `notifyCompleted()` runs, the target fleet is gone and the surrounding fleets have moved.

So `DevilCustomBounty` overrides `reportBattleOccurred`, evaluates D5 while the information still exists, raises the credit reward, stashes the verdict on `CustomBountyData` (which carries `custom1`/`custom2`/`custom3` and a `customMap` for exactly this), then delegates to `super`. Order matters: sample → adjust → `super`.

Notably `BaseCustomBounty.reportBattleOccurred()` checks only `playerInvolved` and whether the flagship died — there is no identification check anywhere in the completion path, so running dark never risks failing the bounty.

### D7 — Purge work is gated at FRIENDLY, not COOPERATIVE

Its headline reward is accelerated personal reputation with Louisa; gating it at COOPERATIVE would make the reward circular. FRIENDLY is a rung that already carries content (daemon hulls, gamma packages), so purge work becomes the fast lane to COOPERATIVE and thence to archdaemon cores and the Illustrious quest.

Personal reputation spans FRIENDLY `0.5` → COOPERATIVE `0.75`. At `RepRewards.MEDIUM` (0.03) that is ~9 missions; at `VERY_HIGH` (0.07), ~4. Roughly halving the grind is felt without trivialising the ladder.

### D8 — Reputation weighting must not be left to vanilla

`setRepChangesBasedOnDifficulty()` keys off difficulty alone and caps `repFaction` at `SMALL` regardless, so a difficulty-9 deserter would pay identical Blackwatch reputation to a difficulty-9 purge — and deserter bounties are infinite and consequence-free. Left as-is, the currency saturates long before any Sarah Tann content exists to spend it on. Each creator therefore sets `data.repPerson` / `data.repFaction` explicitly (both are public fields on `CustomBountyData`, read by `BaseCustomBounty.accept()` via `setRepRewardPerson`/`setRepRewardFaction`).

### D9 — Blackwatch reputation stays invisible

Keeping `showInIntelTab: false` means the accruing standing has no in-game surface. Accepted deliberately: the visible rewards on purge work are credits and Louisa's personal reputation, and those carry the player to COOPERATIVE. Past that point the visible currencies cap out and the invisible one keeps accruing — which is the correct time for a hidden standing to be the reward, because by then the player is doing the work for its own sake.

### D10 — Bonus in mechanics, deduction in voice

Implemented as an additive bonus so the intel entry never shows a number lower than quoted (a deduction reads as a bug). Louisa's dialogue frames it as her charging for cleanup, which supplies the loss-aversion pull without the mechanical surprise.

### Payout and reward summary

| | credit mult | `repPerson` | `repFaction` (Blackwatch) |
|---|---|---|---|
| `CBLegioDeserter` | `DESERTER_MULT` = 1.0 | vanilla by difficulty | vanilla `TINY`→`SMALL` |
| `CBLegioPurge`, seen | ~1.2× | `VERY_HIGH` | elevated |
| `CBLegioPurge`, clean | ~1.5–1.7× | `VERY_HIGH` | elevated |

For scale, the vanilla spread runs `TRADER_MULT` 0.5 → `REMNANT_PLUS_MULT` 3.0; a clean purge lands near a Remnant hunt.

## Risks / Trade-offs

- **Purge reputation damage lands on a faction the player is usually already hostile to** → Accepted and understood: at `-0.8` starting standing the downside is cheap for most runs. The clean-kill *bonus* (D5) is what carries the incentive, precisely so stealth still matters to a Legio-hostile player.

- **Legio-aligned players (Nexerelin) face a real bite** → Intended. This is the run where the content is most interesting, and the choice to run dark or not is a genuine alignment decision.

- **`TOffAlarm` witness spread is a system players may not know exists** → Louisa's offer text must state the rule plainly ("transponder off, no witnesses"). The mechanic is only fair if it is announced.

- **Clean-kill verdict could mis-sample in multi-fleet battles or staged engagements** → The predicate runs once, in `reportBattleOccurred`, against the target's containing location. Verify behaviour when the player retreats and re-engages, and when third-party fleets join the battle.

- **Adding six creators dilutes the four existing ones** → `getFrequency()` weights are tunable per creator; if the shipped four become rare, lower the new creators' frequency multipliers rather than removing them.

- **`CBLegioPurge` fleets built from the Blackwatch roster may be disproportionately lethal** (daemons weighted 12) → Difficulty ladder and `FleetSize`/`FleetQuality` need play-testing against the LOW/NORMAL/HIGH dossier tiers; daemon-heavy fleets already scale further via `LegioFleetInflationListener` in adaptive/hard mode.

- **`RepRewards` deltas are guesses until played** → All values are plain constants in the creator classes; retune against real play rather than locking now.

## Migration Plan

Purely additive. New creators appear only in newly generated offers; in-flight bounties keep the creator they were built with, and no existing save state is read or rewritten. Rollback is removing the entries from `DevilCustomBounty.CREATORS` — the corresponding `rules.csv` rows become inert rather than erroring.

## Open Questions

- Should the clean-kill bonus eventually extend to the deferred courier / extraction / procurement missions, or stay a bounty-only mechanic?
- Does `CBLegioPurge` want a second flavour later (a rogue *Blackwatch* cell composed from Blackwatch's roster, versus a Legio power bloc) to give band 3 two distinct threat shapes?
- What Blackwatch reputation threshold should Sarah Tann content eventually key off? Deliberately unanswered — tune against real accrual once this ships.
