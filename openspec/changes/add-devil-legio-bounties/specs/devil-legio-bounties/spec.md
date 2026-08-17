## ADDED Requirements

### Requirement: Deserter bounties against disowned Legio

Louisa Ferre SHALL offer bounties against Legio Infernalis deserters through her existing custom-bounty offer. Deserters are disowned by the Legio, so these contracts SHALL carry no reputation consequence: the target fleet MUST be flagged as `Factions.PIRATES` and MUST carry `MEMORY_KEY_LOW_REP_IMPACT`.

#### Scenario: Killing a deserter costs no standing

- **WHEN** the player destroys a deserter bounty target's flagship, with the transponder in any state
- **THEN** the bounty completes and pays out
- **AND** the player's standing with `tahlan_legioinfernalis` is unchanged
- **AND** no witness alarm is raised among nearby Legio fleets

#### Scenario: Deserter bounties are available before high trust

- **WHEN** the player's personal reputation with Louisa is at or above the level at which her bounty offer is available
- **THEN** deserter bounties are eligible to appear in the dossier regardless of whether FRIENDLY has been reached

### Requirement: Deserter fleet composition scales with difficulty

Deserter target fleets SHALL be composed from the standard Legio Infernalis roster at lower difficulties and from the Blackwatch roster at the highest difficulties, rather than always from the mission giver's faction.

#### Scenario: Low-difficulty deserter draws rank-and-file ships

- **WHEN** a deserter bounty is created at difficulty 4 or 5
- **THEN** the target fleet is composed from `tahlan_legioinfernalis` at default quality

#### Scenario: High-difficulty deserter draws elite ships

- **WHEN** a deserter bounty is created at difficulty 8 or above
- **THEN** the target fleet is composed from `tahlan_legioelite` at higher quality

### Requirement: Purge bounties against Blackwatch's internal enemies

Louisa Ferre SHALL offer purge bounties against Legio Infernalis targets that Blackwatch wants removed. The target fleet MUST be flagged as `tahlan_legioinfernalis` and MUST NOT carry `MEMORY_KEY_LOW_REP_IMPACT`, so that engaging it incurs the full reputation consequence and the vanilla transponder-witness alarm remains active.

#### Scenario: Engaging a purge target while identified costs Legio standing

- **WHEN** the player engages a purge bounty target with the transponder on
- **THEN** the player's standing with `tahlan_legioinfernalis` decreases

#### Scenario: Nearby Legio fleets can witness the strike

- **WHEN** the player engages a purge target with the transponder off, and another `tahlan_legioinfernalis` fleet in the same location has `COMPOSITION_AND_FACTION_DETAILS` visibility of the target
- **THEN** the witness alarm propagates to that fleet as it does for any full-reputation-impact engagement

### Requirement: Purge bounties are gated on FRIENDLY reputation

Purge bounties SHALL NOT be offered until the player's personal reputation with Louisa Ferre is at or above FRIENDLY. The gate MUST be enforced by the creator's frequency returning zero, so that the creator is excluded from the offer picker rather than appearing and failing.

#### Scenario: Below FRIENDLY the purge creator never appears

- **WHEN** the player's personal reputation with Louisa is below FRIENDLY and a bounty dossier is generated
- **THEN** no purge bounty appears in any of the three difficulty dossiers

#### Scenario: At FRIENDLY the purge creator becomes eligible

- **WHEN** the player's personal reputation with Louisa reaches FRIENDLY and a new bounty dossier is generated
- **THEN** purge bounties are eligible to appear alongside the other creators

### Requirement: Clean-kill payout bonus

A purge bounty completed without the player being identified SHALL pay a credit bonus above its base reward. The player counts as unidentified when the player fleet's transponder is off AND no fleet of the target's faction, other than the target, has `COMPOSITION_AND_FACTION_DETAILS` visibility of the target at the time of the kill.

#### Scenario: Clean kill pays the bonus

- **WHEN** the player destroys a purge target's flagship with the transponder off and no same-faction fleet has visibility of the target
- **THEN** the bounty completes and the credit reward paid is the bonus-inclusive amount

#### Scenario: Identified kill pays base only

- **WHEN** the player destroys a purge target's flagship with the transponder on, or with a same-faction fleet holding visibility of the target
- **THEN** the bounty completes and the credit reward paid is the base amount with no bonus

#### Scenario: Bonus does not apply to deserter bounties

- **WHEN** the player destroys a deserter bounty target's flagship with the transponder off and no witnesses present
- **THEN** the bounty pays its base reward with no clean-kill bonus

### Requirement: Clean-kill verdict is determined at the moment of the kill

The clean-kill determination SHALL be evaluated during battle resolution, while the target fleet and any witnesses still exist, and SHALL be carried forward to reward resolution rather than re-evaluated later.

#### Scenario: Witnesses arriving after the kill do not revoke the bonus

- **WHEN** the player completes a purge target cleanly, and a Legio fleet enters the location afterwards
- **THEN** the clean-kill bonus is still paid

#### Scenario: Running dark never fails the bounty

- **WHEN** the player completes any bounty from Louisa with the transponder off
- **THEN** the bounty registers as completed and is not failed or invalidated by the lack of identification

### Requirement: Purge work accelerates standing with Louisa

Purge bounties SHALL grant substantially greater personal reputation with Louisa Ferre than deserter bounties of equivalent difficulty, making them the faster route from FRIENDLY to COOPERATIVE.

#### Scenario: Purge grants more personal reputation than a deserter hunt

- **WHEN** a purge bounty and a deserter bounty of the same difficulty are both completed
- **THEN** the purge bounty grants the greater personal reputation increase with Louisa

#### Scenario: Reaching COOPERATIVE unlocks her existing high-trust content

- **WHEN** purge work raises the player's personal reputation with Louisa to COOPERATIVE
- **THEN** her existing COOPERATIVE-gated offers become available, unchanged in behavior

### Requirement: Blackwatch standing accrues without granting access

Completing Louisa's bounties SHALL increase the player's reputation with the Blackwatch faction (`tahlan_legioelite`), weighted so that purge work contributes substantially more than deserter work. This standing SHALL NOT gate any content introduced by this change, and the Blackwatch faction SHALL remain hidden from the intel tab.

#### Scenario: Purge work builds Blackwatch standing faster

- **WHEN** a purge bounty and a deserter bounty of the same difficulty are both completed
- **THEN** the purge bounty grants the greater Blackwatch faction reputation increase

#### Scenario: Blackwatch standing unlocks nothing yet

- **WHEN** the player's Blackwatch reputation reaches any level
- **THEN** no offer, item, or dialog option introduced by this change becomes newly available as a result

#### Scenario: Blackwatch remains hidden

- **WHEN** the player opens the faction listing in the intel tab at any point
- **THEN** the Blackwatch faction is not shown

### Requirement: Expanded bounty variety at lower trust

Louisa's bounty offer SHALL draw on additional vanilla bounty creators beyond the four currently registered, so that repeated visits present a wider range of contract types.

#### Scenario: Additional contract types appear in the dossier

- **WHEN** the player views Louisa's bounty dossiers across repeated offers
- **THEN** contract types beyond mercenary, pirate, Pather, and Remnant hunts are presented

#### Scenario: Existing contract types are unchanged

- **WHEN** a mercenary, pirate, Pather, or Remnant bounty is offered
- **THEN** its behavior, payout, and offer text are unchanged from before this change

### Requirement: Offer text in Louisa's voice

Each newly registered bounty creator SHALL have offer text written for Louisa, gated on her person id, following the pattern of her existing bounty offer rows. Purge offer text MUST state the clean-kill condition plainly, so the player knows transponder discipline is rewarded before accepting.

#### Scenario: A purge offer explains the stealth condition

- **WHEN** the player reads a purge bounty offer from Louisa
- **THEN** the text conveys that going unidentified — transponder off and unwitnessed — increases the payout

#### Scenario: Every new creator has Louisa-specific text

- **WHEN** any newly registered creator's bounty is offered by Louisa
- **THEN** offer text specific to her is shown rather than generic fallback text
