# Delta: legio-siege — add-siege-planetfall

## ADDED Requirements

### Requirement: Planetfall climax under Nexerelin

When the subjugation meter completes under Nexerelin against a capturable target, the siege SHALL
enter a timed planetfall stage instead of transferring the market immediately. The market transfer
SHALL fire only when the planetfall window completes with the command fleet alive over the target
planet, and SHALL be deferred while the command fleet is engaged in battle. During planetfall the
siege force SHALL visibly converge on the target planet, the defending navy SHALL flee the system,
the target market's orbital station SHALL be starved out, and coalition relief SHALL stand down.
The planetfall window SHALL be a point of no return for the subjugation meter: fleet kills SHALL no
longer knock the meter back, and the only ways to stop the capture SHALL be driving siege health to
zero or removing the command fleet — either of which SHALL break the siege instantly. If the target
market has no primary entity to converge on, the siege SHALL fall back to the immediate capture.

#### Scenario: Siege force converges on the planet

- **WHEN** planetfall begins
- **THEN** the command fleet leaves its fringe anchor and moves to orbit the target planet, escort,
  blockade, and raid fleets are re-tasked to orbit the planet in support of the landing, blockade
  fleets cease jump-point interdiction and trader interception, and the huntsman task force follows
  the command fleet

#### Scenario: The defending navy gives up

- **WHEN** planetfall begins, and periodically while it continues
- **THEN** military fleets (patrols and war fleets) in the target system belonging to the victim
  faction or its coalition receive disengage-and-flee orders and leave the system to despawn, while
  trade fleets, third-party factions, station fleets, siege-attached fleets, and fleets currently in
  battle are untouched

#### Scenario: The player's navy never flees

- **WHEN** the besieged market belongs to the player's faction
- **THEN** player-faction fleets are exempt from the flee sweep and continue defending

#### Scenario: The station is starved out

- **WHEN** planetfall begins
- **THEN** the target market's station industry (identified by its station tag, covering modded
  stations) is disrupted for the planetfall window plus a configurable margin, so the landing is
  not contested by the station and Legio inherits a recovering station on capture

#### Scenario: Coalition relief stands down

- **WHEN** planetfall begins
- **THEN** no new intervention waves are mobilized and in-flight intervention fleets turn back to
  their home markets; the desperation bounty is no longer refreshed and lapses on its own duration

#### Scenario: Decapitating the landing breaks the siege instantly

- **WHEN** the command fleet is destroyed, despawns, or hits the CR withdrawal floor during
  planetfall
- **THEN** the siege resolves as Broken immediately — no mop-up phase — and all remaining siege
  fleets disperse home

#### Scenario: Breaking the siege force during planetfall still counts

- **WHEN** siege health reaches zero during the planetfall window
- **THEN** the siege resolves as Broken and no market is transferred

#### Scenario: Capture fires only over a held planet

- **WHEN** the planetfall timer expires while the command fleet is alive
- **THEN** the market transfer executes (with its story-protection recheck), unless the command
  fleet is currently in battle, in which case completion is deferred until the battle resolves

#### Scenario: Validity checks continue during planetfall

- **WHEN** the primary target market decivilizes, leaves the economy, changes to Legio ownership by
  other means, or its faction stops being hostile to Legio during planetfall
- **THEN** the siege resolves as Lifted without capture, exactly as during the besieging stage

#### Scenario: Planetfall tuning is externalized

- **WHEN** a maintainer adjusts the planetfall duration, defender sweep cadence, station disruption
  margin, or occupation aftermath values
- **THEN** the new values take effect from named `SiegeConfig` constants without code changes
  elsewhere, following the existing constants-plus-LunaLib pattern

## MODIFIED Requirements

### Requirement: Dual resolution pathway gated on Nexerelin

A siege's success outcome SHALL depend on Nexerelin presence, both driven by the same subjugation meter.
Breaking the siege (driving siege health to zero) SHALL be the universal counter in both pathways. There
SHALL be no fixed siege lifetime; both pathways are races between the meter filling and the siege being
broken. The Nexerelin capture pathway SHALL bypass a normal Nexerelin invasion and SHALL NOT depend on
Nexerelin's invasion-fleet or faction-war systems. Under Nexerelin, a completed meter SHALL resolve
through the planetfall stage rather than an immediate transfer, and the capture SHALL be announced by
the siege's own intel rather than Nexerelin's generic transfer notification.

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

- **WHEN** the planetfall window completes under Nexerelin with the command fleet alive over the target
- **THEN** the declared primary target market is transferred to Legio via Nexerelin's market-transfer API
  without any invasion fleet or ground battle, Nexerelin's generic transfer notification is suppressed in
  favor of the siege intel's own resolution, and the siege resolves as Succeeded

#### Scenario: The success beat is the siege's own, and knows the player's part

- **WHEN** a Nexerelin siege capture resolves the intel as Succeeded
- **THEN** the resolution carries Legio-flavored prose as the single notification of the capture, with a
  distinct tone when the player earned bounty fighting this siege and lost it anyway

#### Scenario: The captured market does not flip pristine

- **WHEN** a market is captured by a siege
- **THEN** its core industries are disrupted for a configurable occupation window and the market receives
  recent unrest, so the fresh conquest reads as conquered and remains a natural retake target; the
  non-Nexerelin scar condition itself is NOT applied to the now-Legio market

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
  hard-mode mechanics, and suppressing the generic transfer notification does not alter Nexerelin's
  diplomacy or war bookkeeping

#### Scenario: A target that stops being a valid objective lifts the siege

- **WHEN** the primary target market is decivilized or leaves the economy, is transferred to Legio by other
  means, or its faction is no longer hostile to Legio
- **THEN** the siege resolves as Lifted without capture or scar — none of the outcome having been earned by
  the siege itself

### Requirement: Unified subjugation meter

A siege SHALL track a single subjugation meter that advances toward a configurable maximum in BOTH the
Nexerelin and non-Nexerelin pathways. The meter SHALL advance only while the command fleet is present,
SHALL be multiplied by command CR (so a battered command fleet subjugates more slowly), and SHALL be
reduced when siege fleets are destroyed — except during planetfall, when the meter is complete and
knockback no longer applies. Reaching the maximum SHALL begin the planetfall stage under Nexerelin
against a capturable target, and SHALL resolve the siege as a success otherwise (lasting scar without
Nexerelin). Removing the command fleet SHALL freeze the meter.

#### Scenario: Meter advances in both pathways while the command fleet holds

- **WHEN** a siege is in its besieging state with the command fleet present
- **THEN** the subjugation meter rises over time at a rate scaled by how strangled the primary target
  market is and multiplied by current command CR, regardless of whether Nexerelin is enabled

#### Scenario: Command-fleet removal freezes the meter

- **WHEN** the command fleet is destroyed or withdraws
- **THEN** the subjugation meter stops advancing (and siege-health regeneration stops), so the siege
  proceeds toward breaking via residual mop-up

#### Scenario: Fleet kills knock the meter back

- **WHEN** an escort, blockade, or raid fleet is destroyed while the siege is besieging
- **THEN** the subjugation meter is reduced proportionally to the destroyed fleet's fleet points,
  floored at zero; during planetfall no knockback is applied — breaking the siege force itself is the
  only counter

#### Scenario: Full subjugation resolves the siege as a success

- **WHEN** the subjugation meter reaches its maximum while the command fleet is present
- **THEN** under Nexerelin against a capturable target the siege enters the planetfall stage (success
  following only if the window completes); otherwise the siege resolves as Succeeded immediately,
  applying the lasting scar

### Requirement: Progressing intel reflecting command CR

A siege SHALL present an intel entry, marking the target system, rendered as a colony-crisis-style event:
a progress bar that fills toward the siege's objective, discrete stage markers, and contributing-factor
tables. Rising progress SHALL read as bad for the player. Command CR SHALL be surfaced as an
all-progress brake factor (not the headline stat), and the player's fleet kills SHALL be surfaced as
one-time factors. The start of planetfall SHALL be announced prominently and the remaining window
surfaced as a countdown. The intel SHALL resolve to a terminal outcome.

#### Scenario: Progress bar tracks subjugation toward the goal

- **WHEN** the subjugation meter changes
- **THEN** the intel's progress bar fills (or recedes) toward the climax stage accordingly, with rising
  progress shown as adverse to the player and fleet kills shown as favourable

#### Scenario: Stage markers along the bar

- **WHEN** the player views the siege intel
- **THEN** discrete stage markers (foothold, stranglehold, climax) are shown along the bar with hover
  tooltips and per-stage descriptive text, and the climax stage's text reflects the active pathway —
  describing the planetfall window under Nexerelin, full entrenchment otherwise

#### Scenario: Planetfall is announced and counted down

- **WHEN** planetfall begins
- **THEN** the intel pings the player's feed with an adverse one-time factor announcing the landing, and
  while planetfall continues the intel shows the days remaining until the planet falls

#### Scenario: Contributing factors are shown

- **WHEN** the player views the siege intel
- **THEN** the monthly factors (blockade pressure, raid sorties, intensity) and the command-readiness
  brake are listed with their contributions, and recent fleet kills appear as favourable one-time
  factors; the accrued bounty remains visible

#### Scenario: Terminal resolution

- **WHEN** a siege ends
- **THEN** the intel resolves as Broken (siege health reached zero, or the landing decapitated during
  planetfall), Succeeded (full subjugation — Nexerelin capture after planetfall or non-Nexerelin scar),
  or Lifted (clean teardown / no eligible outcome)
