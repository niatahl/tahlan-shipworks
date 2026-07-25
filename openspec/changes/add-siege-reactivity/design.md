## Context

The siege system (post-`redesign-legio-siege` + `enhance-siege-intel-and-stakes`) is a Pattern-A
manager (`SiegeManager`: permanent `BaseCampaignEventListener` + `EveryFrameScript`) with per-siege
state in `SiegeData`, a two-value health model (siege health + command CR), a unified subjugation
meter, and a colony-crisis-style `BaseEventIntel`. Kill/despawn events route through
`SiegeFleetListener` → `onSiegeFleetKilled` / `onSiegeFleetDespawned`, and per-fleet behavior lives
in `EveryFrameScript` assignment AIs (`SiegeAssignmentAI`, `SiegeBlockadeAI`) that find the manager
via `SiegeManager.get()` (scripts-list scan, memory/persistentData as caches).

This change adds three reactive systems on that substrate. Key existing levers reused:

- `onSiegeFleetKilled(siegeId, fp, isCommand, playerInvolved)` — the command-kill path (60% health
  chunk) and the `playerInvolved` signal (heat accrual point).
- `STRAIN_K`-style CR strain — the mechanism by which failed interventions soften the siege.
- `syncProgress` / one-time factors on `SiegeIntel` — announcement surface for interventions,
  marking, and huntsman replacement.
- The Blackwatch spawn trick (`params.factionId = roster faction; modeOverride = PRIORITY_THEN_ALL;
  fleet.setFaction(LEGIO, true)`) — reused verbatim for the task force's two rosters.

Verified vanilla APIs (0.98a sources): `SystemBountyIntel(market, baseReward, commerceMode)` with a
built-in player-faction guard, per-hull-size payouts × player involvement, a `MilitaryResponseScript`
side effect, auto-termination on market-faction change, and public `reset()`/`setBaseBounty()`;
`SystemBountyManager.getInstance()` with public `addActive()` and `getActive(market)` (registering
through the manager keeps its own market-dedup honest). Verified Nex APIs (source clone):
`AllianceManager.getFactionAlliance(id)` / `areFactionsAllied(a, b)`.

## Goals / Non-Goals

**Goals:**

- The besieged bloc responds militarily without player involvement (siege outcomes vary even when
  ignored).
- A visible, vanilla-native money incentive pulls uninvolved players into late-stage sieges.
- Sustained player aggression against a siege produces a proportionate, legible elite threat.
- All three systems interlock through existing state (heat is a coalition resource: a hunted player
  distracts the screen; the bounty baits players into huntsman range).
- Config-first tuning; every new constant in `SiegeConfig` with LunaLib exposure where curated.

**Non-Goals:**

- No faction-level diplomacy manipulation (no rep hits, no Nex war declarations) — interventions use
  fleet-local hostility only where needed.
- No cross-system pursuit by the task force (it is siege-attached, not a vengeance fleet).
- No coordination/rendezvous logic for coalition fleets (piecemeal arrival accepted for v1).
- No changes to siege launch cadence, health model, meter mechanics, or resolution pathways.
- No MagicBounty involvement.

## Decisions

### D1. Coalition model: capacity-ranked lead + auxiliaries (F3)

Coalition = victim faction + allies. One rule produces both "weak ally sends a detachment" and
"strong ally extends the nuclear umbrella": rank every member by **response capacity** and let the
top-ranked member send the primary fleet while everyone else sends capacity-scaled auxiliaries.

- **Ally definition:** Nex present → `AllianceManager` alliance membership (behind `HAS_NEX` guard,
  in `ModCompat`-gated code). No Nex → relationship ≥ WELCOMING with the victim. *Rationale: Legio
  is designed around the Nex environment; alliances are Nex's core mechanic; a looser fallback
  suffices outside it.*
- **Capacity:** per faction, the best value over its markets of
  `(market size + military-industry tier bonus) × distance falloff × functioning multiplier`, where
  tier bonus distinguishes High Command > Military Base > none, and a disrupted industry zeroes the
  bonus. *Rationale: local military projection, not global economy — a superpower on the far rim
  should not out-respond the local defender. Precedent: `hasIndustry` checks in `world/Rubicon.kt`.*
- **Distance is delay, not eligibility:** capacity discounts by distance for *ranking*, but any
  member with a qualifying market may respond — farther sources simply arrive later (spawn at their
  own market, travel). *Rationale: the cavalry-over-the-hill rescue is a feature; a hard range gate
  would make umbrella allies binary.*
- **Sizing:** primary sized to *contest* the command fleet (`INTERVENTION_PRIMARY_FP_MULT` ≈
  0.7–1.1 × command FP, capped by capacity); auxiliaries scale from their own capacity
  (`INTERVENTION_AUX_FP_PER_CAPACITY`). *Rationale: a reliably-crushing coalition would trivialize
  sieges and erase the player's role; sometimes the Legio repels the rescue.*
- **Cadence:** proactive — mobilization begins when the siege enters BESIEGING, on a per-siege
  cooldown (`INTERVENTION_INTERVAL_DAYS`), while the coalition retains capacity. Not gated on
  desperation. *Rationale: a strong faction responds because it can; desperation-gating is the
  bounty's identity. Alternative rejected: trigger on meter threshold — would make F2 and F3 fire as
  one lump and leave early sieges unopposed.*
- **Composition:** each fleet spawns with its own faction's doctrine (`FleetParamsV3(market, …)`
  with that faction), contrasting with Legio's Blackwatch.

### D2. Intervention outcome plumbing: reverse-kill CR strain (F3)

Intervention fleets are **not** siege fleets. They carry their own memory tag
(`$tahlan_siege_intervention`) and a dedicated `FleetEventListener`. When one dies in a battle
against siege-tagged fleets, the manager strains command CR proportionally to the dead fleet's FP
(`INTERVENTION_STRAIN_K`, a separate knob from `STRAIN_K`).

*Rationale:* this makes every intervention outcome a gradient rather than a coin flip — kill the
command (existing 60%-chunk path, siege likely breaks), maul it and die (CR strain, siege softened
for the next attacker), get bounced cleanly (nothing). A doomed Hegemony sortie still matters.
*Alternative rejected:* routing interventions through `SiegeFleetListener` — their deaths would then
damage siege health and pay player accrual, which inverts their role (they attack the siege, they
are not part of it).

**Hostility:** the victim is hostile to Legio by construction; Nex allies are almost always at war
with Legio via the shared alliance war. Only the non-Nex relationship-ally edge case needs the
fleet-local hostility memory flag (same trick as the blockade's trader hostility). No faction-level
rep changes anywhere — no cascade risk, no interference with Nex diplomacy.

**Lifecycle:** assignment AI (`SiegeInterventionAI`) drives travel → seek command fleet →
engage; if the command fleet is gone before arrival (withdrawn/killed), retarget to remaining
siege fleets or return home and despawn. `resolveSiege`/`tearDown` disperse any in-flight
intervention fleets (tracked per-siege in `SiegeData.interventionFleets`). Announce dispatch on the
intel as a favourable one-time factor.

### D3. Bounty = vanilla `SystemBountyIntel`, light subclass, manager-registered (F2)

At `captureProgress ≥ BOUNTY_TRIGGER_PROGRESS` (default = the Stranglehold stage value, 66) while
BESIEGING with the command present, post a system bounty at the primary target market.

- **`SiegeSystemBountyIntel : SystemBountyIntel`** whose only overrides: set the protected
  `enemyFaction` to Legio (so the intel reads "likely triggered by Legio Infernalis activity" — the
  vanilla nearest-hostile-market heuristic would misattribute, since Legio's markets sit at
  Lucifron) and accept an intensity-scaled `baseReward` (`BOUNTY_BASE_REWARD_MULT`, 0 = vanilla
  default formula). *Alternative rejected: plain `addOrResetBounty(market)` — five lines, but wrong
  attribution, and flavor is the point of this feature.*
- **Register via `SystemBountyManager.getInstance().addActive(intel)`** so the vanilla manager's
  market-dedup sees it and won't double-post at the same market. The intel self-queues in its
  constructor (vanilla behavior).
- **Keep-alive:** while the siege is BESIEGING with the command fleet present and the meter above
  threshold, periodically `reset()` the intel (60-day vanilla duration, elapsed→0). Once the command
  is gone or the siege resolves, stop refreshing — the bounty lapses naturally within its remaining
  window, incidentally paying for mop-up kills. On Nex capture the vanilla
  `faction != market.getFaction()` check auto-terminates it. *Rationale: zero teardown code; the
  lapse window is a feature (paid cleanup).*
- **Free vanilla behavior consciously accepted:** player-faction markets never post (constructor
  guard — matches the design decision exactly); payouts include any poster-hostile kills near the
  market (pirates too — vanilla-normal and understood by players); payouts carry
  `SYSTEM_BOUNTY_REWARD` rep gains with the victim; posting spawns a `MilitaryResponseScript`
  (the victim's local patrols mobilize — a free mini-F3); no rep gate on collection (vanilla
  semantics, predictable).
- **Stacks with the existing accrual ledger.** The accrual is the mod's own reward channel and the
  only one when no bounty posts (player-faction victim). Vanilla players already experience system
  bounties stacking with everything else. If totals run hot, `ESCORT_BOUNTY_PER_FP` /
  `COMMAND_FLEET_BOUNTY` are the tuning knobs.

### D4. Task force: standing threat entity, regenerating, roster-switched (F1)

One hunter-killer fleet per siege, spawned alongside the command fleet, sized
`TASKFORCE_FP_BASE + TASKFORCE_FP_SCALE × intensityFactor` (~½–⅔ of command FP).

- **Not a siege fleet.** No `SiegeFleetListener`; a dedicated listener reports death →
  `onTaskForceLost(siegeId)`. Death yields no siege-health damage, no CR strain, no meter knockback,
  no accrual. `MEMORY_KEY_NO_SHIP_RECOVERY` set explicitly (it is outside `tagSiegeFleet`, and a
  recoverable pure-daemon fleet would be a hull-farming jackpot). *Rationale: if its death advanced
  the siege's defeat, the punishment arm would subsidize the aggression it exists to punish; and CR
  strain at existing `STRAIN_K` magnitudes would let two task-force kills force a withdrawal.*
- **Regenerating:** on loss, a replacement is dispatched from the siege source market after
  `TASKFORCE_REDISPATCH_DELAY_DAYS`, travels to the target system, then resumes hunting. The respite
  window = delay + travel, so it is *geographic* — sieges near Legio space refill in days, deep-rim
  sieges leave weeks. Replacements continue while the siege is BESIEGING with the command present.
  Farm-safe: kills yield salvage-less elite fights; F2 pays only near the besieged market, so
  ambushing replacements in transit or camping the source pays nothing.
- **Roster decided at spawn time:** `$tahlan_triggered` unset → Blackwatch (`tahlan_legioelite`);
  set → pure daemon (`tahlan_legiodaemons`). Same `PRIORITY_THEN_ALL` + `setFaction(LEGIO)` trick as
  the command fleet. Mid-siege trigger flips escalate via the replacement cycle (kill the Blackwatch
  pack; what returns is daemons). Daemons are deliberately stronger per FP (regen armor, daemoncore);
  a `TASKFORCE_DAEMON_FP_MULT` knob allows damping if testing demands.
- **Burn filter (strip-and-refill):** after inflation, remove members whose max burn <
  `TASKFORCE_MIN_BURN`, re-roll the freed FP, bounded retries, accept slightly-under-budget.
  *Rationale: the fleet's identity is mechanical — it must actually catch things (fleet burn = the
  slowest member). Alternative rejected: curated variant tags — total aesthetic control but a
  data-maintenance liability across two rosters.*
- **Priority-targeting AI (`SiegeTaskForceAI`),** interval-driven re-evaluation:
  1. the player, if `heat ≥ HEAT_MARK_THRESHOLD` and the player is in-system;
  2. threats to the siege in-system — intervention-tagged fleets, then hostile military patrols
     (weighted-picker pattern from `fleet_behavior.md`);
  3. idle: screen the command fleet (`ORBIT_AGGRESSIVE`).
  Never leaves the target system while besieging. Travels with the command fleet before arrival;
  disperses home on resolution like other siege fleets.
- **Heat:** per-siege fields on `SiegeData` — accrues in `onSiegeFleetKilled` when `playerInvolved`
  (`HEAT_PER_FP × fp`), decays in `advanceHealthModel` (`HEAT_DECAY_PER_DAY`), marks at threshold,
  decays back off (marking is not permanent). Crossing announces a red one-time factor on the intel
  ("the huntsmen have marked you"); replacement dispatch announces likewise ("huntsmen destroyed —
  replacement en route"). *Rationale: kills-only heat keeps the consequence legible (the player felt
  themselves earn it) and inert for Legio-friendly or passive players.*

### D5. Interlock (emergent, no dedicated code)

- The task force's priority list makes player heat a coalition resource: a marked player being
  chased leaves the intervention a clean run at the command fleet, and vice versa.
- F2's bounty bait pulls players into F1's hunting ground; the huntsmen are what make the bounty
  dangerous to collect.
- Strong-coalition sieges resolve without the player (low bounty opportunity); friendless victims
  pay strangers the most. Falls out of the fiction; priced by no one.

### D6. Persistence

All new per-siege state lives as plain fields on `SiegeData` (fleet refs, heat, cooldowns,
counters) — serialized with the manager exactly like existing fields. New AIs and listeners follow
the existing pattern: identified by siege id, find the manager via `SiegeManager.get()`, no direct
manager references. No `readResolve` needed (additive fields; clean-cut save policy is in effect for
this release line anyway).

## Risks / Trade-offs

- [Coalition reliably crushes the command fleet → sieges rarely succeed, player role evaporates] →
  primary sized to contest (≤ ~1.1× command FP), capacity caps, cooldown between waves; NPC-vs-NPC
  autoresolve swing acknowledged — tune `INTERVENTION_PRIMARY_FP_MULT` in the dev-mode pass.
- [Piecemeal arrival gets auxiliaries beaten in detail] → accepted for v1 (arguably realistic);
  vanilla `battle.join` naturally combines fleets that arrive together.
- [Nex double-response: Nex's own war machinery + our intervention hit the same siege] → accepted;
  interventions are per-siege-cooldown-bounded and the siege's health model absorbs both identically.
- [Vanilla `SystemBountyManager` internals change in a future Starsector version] → the subclass
  overrides only `enemyFaction` seeding and reward; registration uses two public methods
  (`addActive`, `getActive`); worst case fallback is `addOrResetBounty` with default attribution.
- [Task-force burn filter empties the roster (few fast hulls at some intensity tier)] → bounded
  retries then accept under-budget; floor of `TASKFORCE_MIN_BURN` exposed in config so it can be
  relaxed per-roster.
- [Heat feels opaque to players] → both edges announced on the intel (marked / decayed off is
  implicit by the huntsmen disengaging); threshold and decay in config for feel-tuning.
- [Three systems lengthen sieges → with the gap-based launch timer, sieges become rarer] →
  deliberate: rarer-but-meatier; `LAUNCH_INTERVAL_DAYS_*` sliders remain the pacing lever.
- [`enemyFaction` is protected — engine classloader restrictions] → subclass field access is
  ordinary inheritance (no reflection); verified pattern used by other mods subclassing vanilla
  intel.

## Migration Plan

Implement and land in three independently testable slices: **F3 → F2 → F1** (cheapest-to-deepest;
each slice play-testable in dev mode before the next). All three are additive to `SiegeData`; no
save migration (clean-cut test-release policy). Rollback = feature-flag style: each system gates on
its own config constant (`INTERVENTION_ENABLED`, `BOUNTY_ENABLED`, `TASKFORCE_ENABLED`) so a
misbehaving system can be switched off in `SiegeConfig`/LunaLib without reverting code.

## Open Questions

- Intervention fleet naming/flavor and huntsman fleet naming (externalized strings — Nia's call at
  implementation time).
- Whether auxiliaries should be suppressed entirely below a capacity floor (a size-1 ally sending a
  2-frigate "detachment" may read as noise) — default: small floor, tune in testing.
- `TASKFORCE_DAEMON_FP_MULT` default (1.0 = accept the daemon step-up as intended escalation) —
  confirm in the balance pass.
