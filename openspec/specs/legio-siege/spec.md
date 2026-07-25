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
- **THEN** systems with no non-hidden hostile market, systems already hosting a Legio presence or an active siege, and (under Nexerelin) systems whose only eligible markets are Nex-protected/story markets are excluded

#### Scenario: Targeting follows Legio relations and wars
- **WHEN** the manager weights candidate systems and declares a primary target market
- **THEN** systems hosting markets of factions with worse relations to Legio are weighted proportionally higher — a continuous grade rather than a flat at-war step — and the declared primary target market is the worst-relation hostile market in the chosen system

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

#### Scenario: Decapitation before arrival disbands the expedition
- **WHEN** the command fleet is destroyed while the expedition is still inbound, before the siege is established
- **THEN** the siege resolves as Broken, the remaining escorts disperse home, and any accrued bounty pays out — the mandatory mop-up rule applies only to established sieges

#### Scenario: A stalled mop-up eventually breaks
- **WHEN** the command fleet is gone and no siege fleet has been destroyed for a configured timeout
- **THEN** the siege resolves as Broken, the leaderless residuals having lost cohesion, so a siege can never persist indefinitely

### Requirement: Faction-agnostic, FP-weighted attrition

Losses inflicted on any siege fleet SHALL reduce siege health and strain command CR proportionally to the
fleet points lost, regardless of which faction inflicted them. Losses SHALL be counted per fleet-point as
they occur, including partial losses to a fleet that survives the engagement — not only the destruction of
a whole fleet. Losses to the command fleet itself SHALL strain command CR. Besieged-faction patrols SHALL
contribute identically to the player.

#### Scenario: Player and defender kills count the same
- **WHEN** a siege fleet is destroyed by the player or by a besieged-faction patrol
- **THEN** siege health is reduced and command CR is strained by the same FP-weighted amount in both cases

#### Scenario: Bigger kills hurt more
- **WHEN** comparing the destruction of a high-FP escort to a low-FP picket
- **THEN** the high-FP kill reduces siege health and strains command CR by a proportionally larger amount

#### Scenario: Partial losses count as they happen
- **WHEN** a siege fleet is mauled in an engagement but survives it
- **THEN** the fleet points it lost immediately reduce siege health, strain command CR, and knock the
  subjugation meter back — a fleet ground down over several engagements contributes the same attrition as
  one destroyed outright, and grinding one down without killing it is never free

#### Scenario: Command-fleet losses strain its readiness
- **WHEN** the command fleet takes losses but is not destroyed
- **THEN** command CR is strained proportionally to the fleet points it lost, slowing the subjugation
  meter and bringing the rational-withdrawal floor closer, while siege health is unaffected (the command
  fleet's health contribution remains its single all-or-nothing chunk)

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

While a siege is active, the besieged system's hostile markets SHALL carry a siege market condition that
applies economic pressure, periodically re-validated against current hostility and ownership, and removed
cleanly when the siege ends.

#### Scenario: Condition applied during the siege
- **WHEN** a siege enters its besieging state
- **THEN** the target system's non-hidden hostile markets gain a siege condition reducing accessibility/stability (and applying hazard/immigration penalties)

#### Scenario: Markets that leave hostility lose the condition mid-siege
- **WHEN** a conditioned market stops being a valid pressure target during the siege — its faction is no
  longer hostile to Legio, it changes hands to Legio, or it leaves the economy
- **THEN** the siege condition is removed from it on the next re-validation pass, without waiting for the
  siege to end

#### Scenario: Newly hostile markets gain the condition mid-siege
- **WHEN** a market in the besieged system becomes hostile to Legio (or changes hands into a hostile
  faction's column) after the siege was established
- **THEN** it gains the siege condition on the next re-validation pass

#### Scenario: Condition removed on resolution
- **WHEN** a siege ends for any reason
- **THEN** the siege condition is removed from all affected markets

### Requirement: Progressing intel reflecting command CR

A siege SHALL present an intel entry, marking the target system, rendered as a colony-crisis-style event:
a progress bar that fills toward the siege's objective, discrete stage markers, and contributing-factor
tables. Rising progress SHALL read as bad for the player. Command CR SHALL be surfaced as an
all-progress brake factor (not the headline stat), and the player's fleet kills SHALL be surfaced as
one-time factors. The intel SHALL resolve to a terminal outcome.

#### Scenario: Progress bar tracks subjugation toward the goal

- **WHEN** the subjugation meter changes
- **THEN** the intel's progress bar fills (or recedes) toward the climax stage accordingly, with rising
  progress shown as adverse to the player and fleet kills shown as favourable

#### Scenario: Stage markers along the bar

- **WHEN** the player views the siege intel
- **THEN** discrete stage markers (foothold, stranglehold, climax) are shown along the bar with hover
  tooltips and per-stage descriptive text, and the climax stage's text reflects the active pathway
  (market capture under Nexerelin, full entrenchment otherwise)

#### Scenario: Contributing factors are shown

- **WHEN** the player views the siege intel
- **THEN** the monthly factors (blockade pressure, raid sorties, intensity) and the command-readiness
  brake are listed with their contributions, and recent fleet kills appear as favourable one-time
  factors; the accrued bounty remains visible

#### Scenario: Terminal resolution

- **WHEN** a siege ends
- **THEN** the intel resolves as Broken (siege health reached zero), Succeeded (full subjugation —
  Nexerelin capture or non-Nexerelin scar), or Lifted (clean teardown / no eligible outcome)

### Requirement: Dual resolution pathway gated on Nexerelin

A siege's success outcome SHALL depend on Nexerelin presence, both driven by the same subjugation meter.
Breaking the siege (driving siege health to zero) SHALL be the universal counter in both pathways. There
SHALL be no fixed siege lifetime; both pathways are races between the meter filling and the siege being
broken. The Nexerelin capture pathway SHALL bypass a normal Nexerelin invasion and SHALL NOT depend on
Nexerelin's invasion-fleet or faction-war systems.

#### Scenario: Without Nexerelin — full subjugation scars the target market

- **WHEN** Nexerelin is not enabled and the subjugation meter reaches its maximum
- **THEN** the siege resolves as Succeeded and the primary target market receives a lingering,
  self-expiring scar — economic penalties equal to a configurable fraction (half) of the active siege
  penalty, derived live from the siege-penalty values, plus disruption of its core industries — for the
  scar's duration, after which the scar clears automatically; no market ownership changes

#### Scenario: Scar affects only the target market

- **WHEN** a non-Nexerelin siege succeeds
- **THEN** only the declared primary target market is scarred; other hostile markets in the system gain
  no additional penalty beyond the blockade pressure they already endured

#### Scenario: With Nexerelin — capture progress toward takeover

- **WHEN** Nexerelin is enabled and a siege holds while its primary target market is strangled
- **THEN** the subjugation meter accrues toward taking that market, at a rate scaled by how strangled the
  market is and braked by command CR

#### Scenario: With Nexerelin — successful siege captures the target market bypassing invasion

- **WHEN** the subjugation meter completes under Nexerelin against a capturable target
- **THEN** the declared primary target market is transferred to Legio via Nexerelin's market-transfer API
  without any invasion fleet or ground battle, and the siege resolves as Succeeded

#### Scenario: Captured foothold is garrisoned then relinquished

- **WHEN** a market is captured by a siege
- **THEN** the command fleet garrisons it for a configured duration (~1 year), then returns home and
  disbands, leaving the market to ordinary defenses

#### Scenario: Story-protected markets are never captured

- **WHEN** a market would otherwise be targeted or captured but Nexerelin marks it story-relevant /
  protected / non-invadable
- **THEN** it is excluded as a target and, if flagged by capture time, the transfer is aborted; the siege
  instead resolves via the non-Nexerelin scar pathway against that market

#### Scenario: Breaking the siege prevents the success

- **WHEN** the player and/or defenders drive siege health to zero before the subjugation meter fills
- **THEN** the siege is broken, no market is transferred and no scar is applied, and (under Nexerelin) the
  takeover is prevented

#### Scenario: Capture pathway stays parallel to Nex's own systems

- **WHEN** a Nex siege capture occurs
- **THEN** it does not enroll the siege in Nexerelin's invasion, faction-war scoring, alliance, or
  hard-mode mechanics

#### Scenario: A target that stops being a valid objective lifts the siege

- **WHEN** the primary target market is decivilized or leaves the economy, is transferred to Legio by other
  means, or its faction is no longer hostile to Legio
- **THEN** the siege resolves as Lifted without capture or scar — none of the outcome having been earned by
  the siege itself

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

### Requirement: Unified subjugation meter

A siege SHALL track a single subjugation meter that advances toward a configurable maximum in BOTH the
Nexerelin and non-Nexerelin pathways. The meter SHALL advance only while the command fleet is present,
SHALL be multiplied by command CR (so a battered command fleet subjugates more slowly), and SHALL be
reduced when siege fleets are destroyed. Reaching the maximum SHALL resolve the siege as a success
(capture under Nexerelin, lasting scar otherwise). Removing the command fleet SHALL freeze the meter.

#### Scenario: Meter advances in both pathways while the command fleet holds

- **WHEN** a siege is in its besieging state with the command fleet present
- **THEN** the subjugation meter rises over time at a rate scaled by how strangled the primary target
  market is and multiplied by current command CR, regardless of whether Nexerelin is enabled

#### Scenario: Command-fleet removal freezes the meter

- **WHEN** the command fleet is destroyed or withdraws
- **THEN** the subjugation meter stops advancing (and siege-health regeneration stops), so the siege
  proceeds toward breaking via residual mop-up

#### Scenario: Fleet kills knock the meter back

- **WHEN** an escort, blockade, or raid fleet is destroyed
- **THEN** the subjugation meter is reduced proportionally to the destroyed fleet's fleet points,
  floored at zero

#### Scenario: Full subjugation resolves the siege as a success

- **WHEN** the subjugation meter reaches its maximum while the command fleet is present
- **THEN** the siege resolves as Succeeded — capturing the target market under Nexerelin, or applying a
  lasting scar without Nexerelin

