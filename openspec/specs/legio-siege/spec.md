# legio-siege Specification

## Purpose

The Legio Infernalis siege system: a fleet-, intel-, and market-condition-driven mechanic by which the Legio Infernalis faction periodically besieges eligible hostile star systems. A siege is anchored by an elite Blackwatch command fleet, applies economic pressure to the besieged markets, tracks a two-value health model (siege health plus command-fleet CR), and resolves either as a finite-lifetime pressure event (without Nexerelin) or as a hybrid market capture (with Nexerelin), with breaking the siege the universal counter in both pathways.

## Requirements

### Requirement: No market or hyperspace entity for the besiegers

The siege SHALL be represented entirely as fleets, intel, and market conditions. It SHALL NOT create a `MarketAPI` for the besieging force, and SHALL NOT place any persistent siege entity (station or otherwise) in hyperspace.

#### Scenario: No besieger market exists
- **WHEN** a siege is active
- **THEN** no `MarketAPI` has been created to represent the besieging base, and the besieging force participates in no economy update, commodity routing, or trade-fleet generation

#### Scenario: Nothing placed in hyperspace
- **WHEN** a siege is active
- **THEN** no persistent siege-owned entity exists in the hyperspace location; the besieging presence exists as fleets within the target star system

### Requirement: Periodic, scaling siege launches

A siege manager SHALL periodically launch sieges from the largest non-hidden Legio market against an eligible hostile star system, with intensity scaling on campaign progress, subject to an active-siege cap, and only while the feature is enabled.

#### Scenario: Launch from largest Legio market against a hostile system
- **WHEN** the launch interval elapses and the feature is enabled and the active-siege cap is not reached
- **THEN** a siege expedition is spawned from the largest non-hidden Legio market targeting an eligible hostile system (weighted by market size)

#### Scenario: Ineligible targets excluded
- **WHEN** the manager picks a target
- **THEN** systems with no non-hidden hostile market, systems already hosting a Legio presence or an active siege, systems neighbouring one, and (under Nexerelin) systems whose only eligible markets are Nex-protected/story markets are excluded

#### Scenario: Targeting follows Legio relations and wars
- **WHEN** the manager weights candidate systems and declares a primary target market
- **THEN** systems hosting a market of the faction with the worst relation to Legio are favoured, an at-war faction's system is prioritized when Legio is at war, and the declared primary target market is the worst-relation hostile market in the chosen system

#### Scenario: Primary target declared at launch
- **WHEN** a siege is launched
- **THEN** a single primary target market is fixed for the siege, and pressure, raids, and (under Nexerelin) capture all concentrate on it

#### Scenario: Scaling is not tied to the hardcoded start cycle
- **WHEN** siege intensity (command/escort fleet points, escort count, raid cadence) is computed
- **THEN** it scales on elapsed campaign time and/or a Legio-strength metric, not on a hardcoded `currentCycle - 206` offset

#### Scenario: Disabled feature launches nothing
- **WHEN** the siege feature toggle is off
- **THEN** no new siege is launched

### Requirement: Command fleet anchors the siege

Each siege SHALL be anchored by a single stacked, deliberately tough command fleet that travels to the target and holds at the system fringe, supported by escort/blockade fleets and periodic raid sorties.

#### Scenario: Expedition travels then anchors
- **WHEN** a siege is launched
- **THEN** the command fleet (with escorts) spawns at the source market, travels to the target system, and on arrival anchors at the system fringe and the siege enters its besieging state

#### Scenario: Command fleet is an elite Blackwatch composition
- **WHEN** the command fleet is spawned
- **THEN** it is composed from the Blackwatch subfaction (`tahlan_legioelite`) as an elite spearhead, while escort/blockade/raid fleets use the standard Legio faction (`tahlan_legioinfernalis`)

#### Scenario: Blockade and sorties
- **WHEN** a siege is in its besieging state
- **THEN** escort fleets blockade the system's jump points and raid sorties periodically move against the besieged system's hostile markets

### Requirement: Two-value health model with mandatory mop-up

A siege SHALL track an overall **siege health** value and a separate **command-fleet combat-readiness (CR)** value. The siege SHALL be fully broken only when siege health reaches zero. Removing the command fleet SHALL stop all siege-health regeneration but SHALL NOT by itself reduce siege health to zero.

#### Scenario: Command fleet is the biggest health contributor and the regen source
- **WHEN** the command fleet is present and coordinating
- **THEN** siege health regenerates over time (regen strength scaling with command CR), and the command fleet represents the single largest contribution to siege health

#### Scenario: Removing the command fleet stops regeneration
- **WHEN** the command fleet is removed (destroyed or withdrawn)
- **THEN** siege-health regeneration ceases and the command fleet's health contribution is removed

#### Scenario: Decapitation still requires mop-up
- **WHEN** the command fleet is removed while escort/blockade/raid forces remain
- **THEN** the siege is not yet broken, and remaining siege health must be reduced to zero by destroying the residual fleets

#### Scenario: Siege breaks at zero health
- **WHEN** siege health reaches zero
- **THEN** the siege ends as broken: remaining siege fleets disperse/withdraw, the besieged-market condition is removed, and the intel resolves

### Requirement: Faction-agnostic, FP-weighted attrition

Destroying any siege fleet SHALL reduce siege health and strain command CR proportionally to the destroyed fleet's fleet points, regardless of which faction destroyed it. Besieged-faction patrols SHALL contribute identically to the player.

#### Scenario: Player and defender kills count the same
- **WHEN** a siege fleet is destroyed by the player or by a besieged-faction patrol
- **THEN** siege health is reduced and command CR is strained by the same FP-weighted amount in both cases

#### Scenario: Bigger kills hurt more
- **WHEN** comparing the destruction of a high-FP escort to a low-FP picket
- **THEN** the high-FP kill reduces siege health and strains command CR by a proportionally larger amount

#### Scenario: Bounty share only on player involvement
- **WHEN** a siege fleet is destroyed in a battle the player was involved in
- **THEN** the player accrues a bounty share scaled by involvement, while non-player kills grant the player no payment

### Requirement: Command CR strain, recovery, and rational withdrawal

Command CR SHALL fall under sustained losses, recover when pressure lets up, scale the command fleet's combat strength, and trigger a rational withdrawal at a configured floor.

#### Scenario: CR recovers when unpressured
- **WHEN** no siege fleet has been lost within the recovery-delay window
- **THEN** command CR recovers over time toward its maximum

#### Scenario: Attrition softens the command fleet
- **WHEN** command CR has been reduced by sustained attrition
- **THEN** the command fleet's effective combat strength is correspondingly reduced, making it killable by a weaker fleet

#### Scenario: Rational withdrawal at the floor
- **WHEN** command CR falls below the withdrawal floor
- **THEN** the command fleet disengages and returns toward a Legio market rather than fighting on, regeneration stops, and the siege proceeds toward breaking via residual mop-up

### Requirement: Pressure on the besieged markets

While a siege is active, the besieged system's hostile markets SHALL carry a siege market condition that applies economic pressure, removed cleanly when the siege ends.

#### Scenario: Condition applied during the siege
- **WHEN** a siege enters its besieging state
- **THEN** the target system's non-hidden hostile markets gain a siege condition reducing accessibility/stability (and applying hazard/immigration penalties)

#### Scenario: Condition removed on resolution
- **WHEN** a siege ends for any reason
- **THEN** the siege condition is removed from all affected markets

### Requirement: Progressing intel reflecting command CR

A siege SHALL present an intel entry marking the target system whose stage reflects command CR and which resolves to a terminal outcome.

#### Scenario: Stage tracks command CR
- **WHEN** command CR changes materially
- **THEN** the intel advances or regresses through stages (Entrenched / Strained / Faltering) and pings an update, giving the player a readable signal of the command fleet's condition

#### Scenario: Terminal resolution
- **WHEN** a siege ends
- **THEN** the intel resolves as Broken (siege health reached zero), Lifted (no-Nex lifetime expired), or Succeeded (Nex market capture)

### Requirement: Dual resolution pathway gated on Nexerelin

A siege's failure-to-break outcome SHALL depend on Nexerelin presence. Breaking the siege (driving siege health to zero) SHALL be the universal counter in both pathways. The Nex capture pathway SHALL bypass a normal Nexerelin invasion and SHALL NOT depend on Nexerelin's invasion-fleet or faction-war systems.

#### Scenario: Without Nexerelin — finite-lifetime pressure
- **WHEN** Nexerelin is not enabled and a siege is established
- **THEN** the siege runs for a finite, configurable lifetime (default ~6 months) during which the besieged markets carry the pressure condition, and on expiry it lifts with no change of market ownership and the markets fully recover (no lingering scar)

#### Scenario: With Nexerelin — hybrid capture progress toward takeover
- **WHEN** Nexerelin is enabled and a siege holds while its primary target market is strangled
- **THEN** capture progress accrues toward taking that market, at a rate scaled by how strangled the market is (its reduced accessibility/stability)

#### Scenario: With Nexerelin — successful siege captures the target market bypassing invasion
- **WHEN** capture progress completes
- **THEN** the declared primary target market is transferred to Legio via Nexerelin's market-transfer API without any invasion fleet or ground battle, and the siege resolves as Succeeded

#### Scenario: Captured foothold is garrisoned then relinquished
- **WHEN** a market is captured by a siege
- **THEN** the command fleet garrisons it for a configured duration (~1 year), then returns home and disbands, leaving the market to ordinary defenses

#### Scenario: Story-protected markets are never captured
- **WHEN** a market would otherwise be targeted or captured but Nexerelin marks it story-relevant / protected / non-invadable
- **THEN** it is excluded as a target and, if flagged by capture time, the transfer is aborted and the siege resolves without taking it

#### Scenario: Breaking the siege prevents the takeover
- **WHEN** the player and/or defenders drive siege health to zero before capture progress completes
- **THEN** the siege is broken, no market is transferred, and (under Nexerelin) the takeover is prevented

#### Scenario: Capture pathway stays parallel to Nex's own systems
- **WHEN** a Nex siege capture occurs
- **THEN** it does not enroll the siege in Nexerelin's invasion, faction-war scoring, alliance, or hard-mode mechanics

### Requirement: Tunable constants, LunaLib sliders, and a settings gate

All siege tuning values SHALL live in named constants. A curated subset SHALL be exposed as LunaLib balance sliders when LunaLib is enabled, with the constants serving as defaults otherwise. The feature SHALL be gated behind a `tahlan_settings` toggle with clean mid-save teardown.

#### Scenario: Works without LunaLib
- **WHEN** LunaLib is not enabled
- **THEN** the siege uses the constant default values and functions normally

#### Scenario: LunaLib overrides curated values
- **WHEN** LunaLib is enabled and a player adjusts an exposed slider (master enable, frequency, command difficulty/size, attrition strength)
- **THEN** that value overrides the corresponding constant default at load

#### Scenario: Disabling mid-save tears down cleanly
- **WHEN** the player turns the siege toggle off during a save with active sieges
- **THEN** no new sieges launch and active sieges tear down cleanly — fleets despawn, market conditions are removed, and intel resolves — without orphaned state
