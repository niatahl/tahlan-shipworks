## ADDED Requirements

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

## MODIFIED Requirements

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
