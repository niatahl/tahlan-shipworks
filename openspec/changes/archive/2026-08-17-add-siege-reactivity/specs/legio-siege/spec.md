## ADDED Requirements

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
