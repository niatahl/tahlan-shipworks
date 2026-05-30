## 1. Constants & latch

- [x] 1.1 Add id/flag constants in `utils/TahlanIDs.kt`: the one-time resolution flag (e.g. `$tahlan_traumResolved`), the Traumtänzer derelict variant id, and any entity memory-flag/special id the chosen approach needs
- [x] 1.2 Decide the approach per design.md (custom salvage special vs. `pickInteractionDialogPlugin` fallback) and note the choice in code comments — chose custom salvage special; SalvageSpecialData is self-describing (no config registration needed)

## 2. Dilemma interaction

- [x] 2.1 Implement the custom dilemma plugin (salvage special or `InteractionDialogPlugin`) that presents two options: save the ship / save the crew
- [x] 2.2 Gate presentation on the unset resolution flag; do not present (or grant) anything once resolved
- [x] 2.3 Make the interaction read cold — convey the ship identity + reactor/cryopod situation without questline state
- [x] 2.4 Add the optional questline-aware flavor branch (extra context only; identical options) keyed on the questline-completion flag
- [x] 2.5 All on-screen text is minimal placeholder, clearly marked for hand-rewrite

## 3. Outcomes

- [x] 3.1 Save-the-ship: add the Traumtänzer to the player fleet in BATTERED condition (reuse the stock recovery's `DerelictShipData`/variant so condition + variant are faithful); no rep/CR/morale penalty
- [x] 3.2 Save-the-crew: remove/destroy the derelict entity, add `tahlan_henrietta` as a fleet officer (fetch the existing tuned instance, do not create a fresh person), and grant a small crew + marines bonus
- [x] 3.3 Both outcomes set the one-time resolution flag and consume/remove the entity so the site cannot be re-interacted
- [x] 3.4 Enforce mutual exclusivity: exactly one reward is granted; no "save both" / no skill- or SP-gated perfect ending

## 4. Wiring into placement

- [x] 4.1 In `DerelictsSpawnScript.java` traum branch, attach the custom dilemma special instead of the stock `ShipRecoverySpecialCreator`, leaving placement, BATTERED condition, and the Remnant `DefenderDataOverride` unchanged
- [x] 4.2 Register the custom salvage special (or campaign-plugin pick) per the chosen approach; confirm the registration mechanism against the API — confirmed NONE required: `SalvageSpecialData.createSpecialPlugin()` is self-describing
- [x] 4.3 Verify the Remnant guards still spawn and gate access (combat/progression gate intact) — guards untouched (recoverable flag flipped to false only to suppress the stock recovery special)

## 5. Rosenritter questline intel cleanup

- [x] 5.1 Add a public `isAtFinalStage()` accessor on `regaliablueprintintel` (true at `END_OF_BLUEPRINTS`)
- [x] 5.2 In `regaliablueprintintel`'s end-stage text (`addBulletPoints` + `createSmallDescription`), branch on `$tahlan_traumResolved`: when set, omit the constellation pointer (blueprint completion text still shown)
- [x] 5.3 In `TraumDilemma.finish()`, look up the questline intel via `getFirstIntel(...)` and drop it (`removeIntel`) only if it is at its final stage; no-op when absent or mid-decryption

## 6. Build & verification

- [ ] 6.1 Rebuild `jars/TahlanShipworks.jar` in IntelliJ and commit it
- [ ] 6.2 Verify the gate: defenders present; dilemma only reachable after clearing them
- [ ] 6.3 Verify save-the-ship: grants a BATTERED Traumtänzer, kills the crew, no penalty, latches, entity consumed
- [ ] 6.4 Verify save-the-crew: destroys the ship, recruits the tuned Henrietta + crew/marines, grants no hull, latches, entity consumed
- [ ] 6.5 Verify one-time/exclusive: no repeat after resolution, no "save both", and the cold-find path reads correctly with no questline
- [ ] 6.6 Verify save/load mid-encounter preserves the unresolved state and the latch survives resolution
- [ ] 6.7 Verify intel cleanup: (a) resolve before final stage → no pointer shown at completion; (b) resolve after pointer → intel dropped; (c) cold find → no-op; (d) mid-decryption resolve → intel intact, pointer later suppressed
