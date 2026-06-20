# traumtanzer-dilemma Specification

## Purpose

The Traumtänzer salvage dilemma: a one-time, Remnant-guarded capstone encounter that replaces the derelict's default ship recovery with a hard binary choice — save the ship (recover the Traumtänzer in BATTERED condition, losing the cryopod crew) or save the crew (destroy the ship, recruit Henrietta as an officer plus a small crew/marine bonus). The encounter reads without any dependency on the Rosenritter questline, resolves exactly once, and cleans up the questline's dangling intel pointer.

## Requirements

### Requirement: Combat gate to reach the derelict

The Traumtänzer derelict SHALL remain guarded by its Remnant defender force, so the capstone reward can only be reached by winning a fight. This is unchanged from current behavior and is retained deliberately as a progression gate.

#### Scenario: Guards must be cleared first
- **WHEN** the player arrives at the Traumtänzer derelict
- **THEN** the existing Remnant defender force is present and must be defeated before the salvage interaction can be entered

#### Scenario: Earlygame power-skip is gated
- **WHEN** an under-strength player attempts the encounter
- **THEN** the defenders present a fight commensurate with the reward, preventing a free capstone power boost

### Requirement: Salvage dilemma on interaction

Interacting with the (cleared) Traumtänzer derelict SHALL present a one-time dilemma instead of the default ship recovery: reserve power is sustaining the crew's cryopods while the reactor is failing, and the player must choose to save the ship or save the crew. The presentation SHALL read without any dependency on the Rosenritter questline.

#### Scenario: Dilemma replaces default recovery
- **WHEN** the player interacts with the unresolved Traumtänzer derelict
- **THEN** the custom dilemma is presented offering exactly two outcomes (save the ship, save the crew), and the stock `ShipRecoverySpecial` recovery is not offered

#### Scenario: Reads cold
- **WHEN** the player reaches the derelict without having engaged the Rosenritter questline
- **THEN** the interaction still conveys what the ship is and the reactor/cryopod situation, and the choice is fully usable

#### Scenario: Optional questline-aware context
- **WHEN** the player reaches the derelict having completed the Rosenritter questline
- **THEN** additional context may be shown, but the two outcomes are identical to the cold case

### Requirement: Save-the-ship outcome

Choosing to save the ship SHALL kill the cryopod crew, recover the Traumtänzer in BATTERED condition to the player's fleet, and resolve the encounter permanently.

#### Scenario: Recover the battered ship
- **WHEN** the player chooses to save the ship
- **THEN** the Traumtänzer hull is added to the player's fleet in BATTERED condition and the crew is lost

#### Scenario: No mechanical penalty for the dark choice
- **WHEN** the player chooses to save the ship (killing the crew)
- **THEN** no reputation, CR, or morale penalty is applied; the cost is purely narrative

### Requirement: Save-the-crew outcome

Choosing to save the crew SHALL destroy the ship, recruit Henrietta as a fleet officer, grant a small crew and marine bonus, and resolve the encounter permanently.

#### Scenario: Rescue the crew and recruit Henrietta
- **WHEN** the player chooses to save the crew
- **THEN** the derelict entity is destroyed/removed, `tahlan_henrietta` is added to the player's fleet as an officer, and a small amount of crew and marines are granted

#### Scenario: No ship is granted
- **WHEN** the player chooses to save the crew
- **THEN** the Traumtänzer hull is not added to the player's fleet under any circumstances

### Requirement: One-time, mutually-exclusive resolution

The dilemma SHALL be a hard binary with no "save both" option, and SHALL resolve exactly once, after which it can never be repeated or re-exploited.

#### Scenario: Exactly one reward
- **WHEN** the player commits to either outcome
- **THEN** only that outcome's reward is granted and the other is permanently forgone

#### Scenario: No perfect ending
- **WHEN** the dilemma is presented
- **THEN** there is no skill-, aptitude-, or story-point-gated option that saves both the ship and the crew

#### Scenario: Latched against repeat
- **WHEN** the encounter has been resolved (global flag set)
- **THEN** the derelict no longer presents the dilemma and grants no further reward

### Requirement: Rosenritter questline intel cleanup

The dilemma SHALL be wired to the Rosenritter blueprint questline's intel so the questline never leaves a dangling pointer to a ship that has already been resolved. This must work whether the ship is resolved before or after the questline reaches its final stage, and must be a no-op when the questline was never engaged.

#### Scenario: Ship resolved before the questline's final stage
- **WHEN** the Traumtänzer dilemma is resolved while the blueprint questline has not yet reached its final (pointer) stage
- **THEN** when the questline later reaches its final stage it does not present the "object of interest" constellation pointer (the blueprint arc still completes normally)

#### Scenario: Ship resolved after the pointer intel exists
- **WHEN** the questline has reached its final stage (the Traumtänzer pointer is shown) and the dilemma is then resolved
- **THEN** that questline intel is dropped from the intel log to avoid leaving a permanent pointer to a ship that no longer exists or has already been claimed

#### Scenario: Questline never engaged
- **WHEN** the dilemma is resolved on a cold find with no Rosenritter questline intel present
- **THEN** the cleanup wiring is a no-op and nothing errors

#### Scenario: Mid-questline resolution does not drop the intel
- **WHEN** the dilemma is resolved while the questline is still mid-decryption (blueprints not all recovered)
- **THEN** the questline intel is left intact (it is still serving the blueprint hunt); only the final-stage pointer is suppressed later
