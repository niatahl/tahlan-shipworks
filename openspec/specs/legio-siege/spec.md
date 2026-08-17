# legio-siege Specification

## Purpose

The Legio Infernalis siege system: a fleet-, intel-, and market-condition-driven mechanic by which the Legio Infernalis faction periodically besieges eligible hostile star systems. A siege is anchored by an elite Blackwatch command fleet, applies economic pressure to the besieged markets, tracks a two-value health model (siege health plus command-fleet CR), and fields a standing huntsman task force while the besieged faction's coalition mobilizes intervention fleets and the strangled market posts a desperation bounty. A siege has no fixed lifetime: it is a race between a single subjugation meter filling and the siege force being broken. Full subjugation resolves as a timed planetfall culminating in market capture (with Nexerelin) or as a lingering scar on the target market (without), with breaking the siege — driving siege health to zero — the universal counter in both pathways.

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

### Requirement: Coalition military intervention against the siege

While a siege is in its besieging state, the besieged faction and its allies SHALL mobilize
intervention fleets against the siege command fleet, on a per-siege cooldown, without requiring
player involvement. Allies SHALL be defined as the victim's Nexerelin alliance members when
Nexerelin is enabled, and as factions with relationship ≥ WELCOMING toward the victim otherwise.
The coalition member with the highest response capacity — derived from its best reachable
military-industry market (market size plus a military-industry tier bonus, discounted by distance
to the besieged system, zeroed while the industry is disrupted) — SHALL send the primary
intervention fleet, sized to contest (not reliably crush) the command fleet; other members SHALL
send capacity-scaled auxiliary detachments. Distance SHALL set arrival delay, not eligibility.
Intervention fleets SHALL NOT be siege fleets: their destruction SHALL NOT reduce siege health,
knock back the subjugation meter, or accrue player bounty. An intervention fleet destroyed in
battle against siege fleets SHALL strain command CR proportionally to its fleet points.
Interventions SHALL cause no faction-level reputation or diplomacy changes; where an intervening
ally is not already hostile to Legio, hostility SHALL be fleet-local only.

#### Scenario: Victim with no allies sends its own response

- **WHEN** a siege besieges a faction with no eligible allies and that faction has a functioning
  military-industry market in range
- **THEN** that faction dispatches the primary intervention fleet against the siege command fleet
  from its highest-capacity military market

#### Scenario: Strong ally extends the umbrella

- **WHEN** the besieged faction's coalition contains an ally whose response capacity exceeds the
  victim's own
- **THEN** the ally sends the primary intervention fleet and the victim (and any other members)
  contribute auxiliary detachments scaled to their own capacity

#### Scenario: Ally definition follows Nexerelin when present

- **WHEN** Nexerelin is enabled
- **THEN** coalition membership is determined by the victim's Nexerelin alliance; without Nexerelin,
  factions at relationship ≥ WELCOMING toward the victim qualify

#### Scenario: Distance delays but does not disqualify

- **WHEN** a coalition member's best military market lies far from the besieged system
- **THEN** its fleet still mobilizes, spawning at that market and arriving after correspondingly
  longer travel, and its capacity ranking is discounted by that distance

#### Scenario: Killing the command fleet routes through the normal kill path

- **WHEN** an intervention fleet destroys the siege command fleet
- **THEN** the existing command-kill accounting applies (command health chunk removed, siege
  proceeds toward broken via mop-up), with no player bounty accrued when the player was not involved

#### Scenario: Failed intervention still softens the siege

- **WHEN** an intervention fleet is destroyed in battle against siege fleets
- **THEN** command CR is strained proportionally to the destroyed intervention fleet's fleet points,
  and siege health, the subjugation meter, and the player bounty ledger are unchanged

#### Scenario: No diplomatic side effects

- **WHEN** a coalition member not currently hostile to Legio intervenes
- **THEN** its intervention fleet engages siege fleets via fleet-local hostility only, with no
  faction-level reputation change on either side

#### Scenario: Intervention with no target retargets or goes home

- **WHEN** an intervention fleet's target command fleet is destroyed or withdraws before engagement
- **THEN** the fleet retargets remaining siege fleets or returns to its source market and despawns,
  and on siege resolution all in-flight intervention fleets disperse home

#### Scenario: Inbound intervention announced

- **WHEN** an intervention fleet is dispatched
- **THEN** the siege intel surfaces it as a favourable one-time factor visible to a player holding
  the intel

### Requirement: Desperation system bounty on the besieged system

The primary target market SHALL post a vanilla-style system bounty on hostile fleets when the
siege's subjugation meter reaches a configurable threshold (default: the Stranglehold stage value)
while the command fleet is present — attributed to Legio activity, using the vanilla system-bounty
mechanics for payout, reputation, visibility, and lifecycle. The bounty SHALL be kept alive while
the siege remains in its besieging state with the command fleet present, SHALL stop being refreshed
(and thus lapse within its remaining duration) once the command fleet is removed or the siege
resolves, and SHALL terminate when the market changes ownership. Player-faction markets SHALL post
no bounty. The bounty SHALL stack with the existing accrued-bounty ledger.

#### Scenario: Bounty posts at the desperation threshold

- **WHEN** the subjugation meter reaches the trigger threshold while the siege is besieging with the
  command fleet present and the primary target market is not player-owned
- **THEN** a system bounty posted by the target market appears through the vanilla bounty intel
  pipeline, its likely-cause attribution names Legio, and its reward may scale with siege intensity

#### Scenario: Vanilla payout semantics

- **WHEN** the player participates in destroying fleets hostile to the posting faction near the
  besieged market while the bounty is active
- **THEN** payment and reputation follow vanilla system-bounty rules (per-hull-size payout scaled by
  player involvement, reputation gain with the poster), in addition to any siege bounty accrual

#### Scenario: Bounty lapses through mop-up

- **WHEN** the command fleet is destroyed or withdraws while the bounty is active
- **THEN** the bounty is no longer refreshed and expires on its own remaining duration, continuing
  to pay for kills during the mop-up window

#### Scenario: Capture terminates the bounty

- **WHEN** a Nexerelin siege capture transfers the posting market to Legio
- **THEN** the bounty ends (the vanilla owner-change check), and no bounty remains payable by the
  captured market

#### Scenario: Player-faction victim posts nothing

- **WHEN** the siege's primary target market belongs to the player faction
- **THEN** no system bounty is posted for that siege, and the accrued-bounty ledger remains the
  player's only siege payment channel

#### Scenario: No duplicate bounty at the same market

- **WHEN** the siege bounty would post at a market where a vanilla system bounty is already active
- **THEN** the existing bounty is reused/refreshed rather than duplicated, and the vanilla bounty
  manager never posts a second bounty at that market while the siege bounty runs

### Requirement: Standing huntsman task force

Each siege SHALL field exactly one standing hunter-killer task force, spawned alongside the command
fleet at launch and travelling with it. Its roster SHALL be decided at each spawn: the Blackwatch
list before the Legio awakening (`$tahlan_triggered`), the pure daemon list after. Its composition
SHALL be constrained to high-burn hulls (a configurable minimum burn, enforced by post-inflation
filtering) so the fleet can catch its prey. The task force SHALL NOT be a siege fleet: its
destruction SHALL NOT reduce siege health, strain command CR, knock back the subjugation meter, or
accrue player bounty, and its hulls SHALL NOT be recoverable. When destroyed while the siege remains
in its besieging state with the command fleet present, a replacement SHALL be dispatched from the
siege source market after a configurable delay and travel to the target system. While besieging, the
task force SHALL hunt within the target system by priority: the player (only while marked by heat),
then threats to the siege (intervention fleets, hostile military patrols), otherwise screening the
command fleet; it SHALL NOT pursue targets outside the target system.

#### Scenario: Spawned with the command fleet, roster by awakening state

- **WHEN** a siege launches (or a replacement is dispatched) and `$tahlan_triggered` is unset
- **THEN** the task force spawns from the Blackwatch roster; when the flag is set at spawn time it
  spawns from the pure daemon roster instead, flying under Legio colors in both cases

#### Scenario: Only fast hulls serve

- **WHEN** the task force is inflated
- **THEN** members below the configured minimum burn are removed and their fleet points re-rolled
  (bounded retries, accepting a slightly smaller fleet if the roster runs dry), so the resulting
  fleet's burn qualifies it as a hunter-killer

#### Scenario: Heat marks the player

- **WHEN** the player's accumulated heat — gained from player-involved siege-fleet kills scaled by
  fleet points, decaying daily — crosses the marking threshold while the player is in the besieged
  system
- **THEN** the task force prioritizes intercepting the player fleet, and the siege intel announces
  the marking as an adverse one-time factor

#### Scenario: Heat decays back off

- **WHEN** a marked player avoids further siege-fleet kills long enough for heat to decay below the
  threshold
- **THEN** the task force stops prioritizing the player and returns to hunting siege threats

#### Scenario: Hunts threats to the siege

- **WHEN** no marked player is available in-system and intervention fleets or hostile military
  patrols are present
- **THEN** the task force intercepts them by weighted priority; with no targets at all it screens
  the command fleet

#### Scenario: Killing it buys a window, not a decapitation

- **WHEN** the task force is destroyed while the siege is besieging with the command fleet present
- **THEN** siege health, command CR, the subjugation meter, and the accrued bounty are unchanged; no
  hulls are recoverable; a replacement is dispatched from the siege source market after the
  configured delay; and the siege intel announces the replacement as a one-time factor

#### Scenario: Stays with the siege

- **WHEN** the siege is travelling, resolves, or is torn down
- **THEN** the task force travels with the command fleet (before arrival) and disperses home on
  resolution like other siege-attached fleets, never pursuing targets beyond the target system while
  besieging

