## ADDED Requirements

### Requirement: Quest availability and reputation gating

The system SHALL offer the Illustrious recovery quest through Louisa Ferre (`tahlan_devil`) only when the player's reputation with her is COOPERATIVE or higher, and SHALL offer it at most once per save.

#### Scenario: Offered at high reputation, never run before
- **WHEN** the player interacts with Louisa Ferre and reputation with her is COOPERATIVE or higher and the global latch `$tahlan_illustriousRecovered` is not set and the quest has not been started
- **THEN** a new dialog option introducing the Illustrious quest is presented alongside her existing offerings

#### Scenario: Hidden below the reputation threshold
- **WHEN** the player interacts with Louisa Ferre and reputation with her is below COOPERATIVE
- **THEN** the Illustrious quest option is not presented

#### Scenario: Never re-offered after completion
- **WHEN** the player has previously recovered the Illustrious (global latch `$tahlan_illustriousRecovered` is set)
- **THEN** the quest is not offered again under any reputation level

#### Scenario: Existing devil missions unaffected
- **WHEN** the Illustrious quest option is added to Louisa's dialog
- **THEN** her three existing repeatable missions (`tahlan_devilCB`, `tahlan_devilDaemonSurplus`, `tahlan_devilDaemonCore`) remain available and unchanged

### Requirement: Three-hop guarded clue chain

The quest SHALL consist of exactly three sequential clue sites, each presented as a Nightwatch dead-drop and each guarded by a faceless Legio fleet that must be defeated before the cache can be salvaged.

#### Scenario: Quest starts pointing at the first clue, not the ship
- **WHEN** the player accepts the quest from Louisa
- **THEN** the player is directed to the first clue site's location and is not given the location of the Illustrious itself

#### Scenario: Clue site is guarded by Legio
- **WHEN** the player arrives at a clue site
- **THEN** a hostile Legio fleet is present guarding the dead-drop, with no faction leader or named Legio character involved

#### Scenario: Escalating opposition across hops
- **WHEN** comparing the guarding fleets of hop 1, hop 2, and hop 3
- **THEN** fleet strength escalates from small (hop 1) to medium (hop 2) to large (hop 3), with hop 3 being the combat climax

#### Scenario: Cache decrypts to the next coordinate
- **WHEN** the player defeats the guarding fleet and salvages the dead-drop at hop 1 or hop 2
- **THEN** the next clue site's coordinate is revealed and the quest advances to the following hop

#### Scenario: Final cache reveals the wreck location
- **WHEN** the player defeats the hop-3 guarding fleet and salvages its cache
- **THEN** the location of the drifting Illustrious is revealed and the quest advances to the salvage stage

### Requirement: Peaceful salvage of the Illustrious

The final wreck site SHALL be undefended, and the Illustrious SHALL be recoverable there via peaceful salvage in a damaged state of exactly three D-mods.

#### Scenario: Wreck site has no guardians
- **WHEN** the player arrives at the revealed wreck location
- **THEN** no hostile fleet is present and the Illustrious is found drifting and silent

#### Scenario: Recovered with exactly three D-mods
- **WHEN** the player salvages the drifting Illustrious
- **THEN** a single `tahlan_illustrious` hull is added to the player's fleet bearing exactly three D-mods

#### Scenario: Quest completion latches
- **WHEN** the Illustrious is recovered
- **THEN** the global latch `$tahlan_illustriousRecovered` is set so the quest cannot be offered or run again

### Requirement: One-time paid restoration offer

After the player has recovered the Illustrious, Louisa Ferre SHALL offer to broker a one-time full restoration that removes all of the ship's D-mods for a discounted price that the player pays.

#### Scenario: Restoration offered after recovery
- **WHEN** the player interacts with Louisa after recovering the Illustrious and the restoration has not yet been performed
- **THEN** a dialog option to have the Illustrious restored is presented

#### Scenario: Player pays a discounted price
- **WHEN** the player accepts the restoration and can afford the discounted price
- **THEN** the price is deducted from the player's credits and all D-mods are removed from the recovered Illustrious

#### Scenario: Cannot afford restoration
- **WHEN** the player accepts the restoration but cannot afford the discounted price
- **THEN** the restoration is not performed and the player retains their credits and the ship's D-mods

#### Scenario: Restoration offered only once
- **WHEN** the restoration has already been performed
- **THEN** the restoration option is no longer presented
