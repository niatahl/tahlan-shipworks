# Legio Daemon Bounty Chain

A three-stage MagicLib bounty progression against the Legio Infernalis, escalating from
an intro daemon fight to a climactic boss duel. Defined in
`data/config/modFiles/magicBounty_data.json`.

## The chain

| Order | Bounty key | Name | Giver | Target / flagship | Reward |
|-------|-----------|------|-------|-------------------|--------|
| 1 | `tahlan_daemons` | "Legio rising" | Hegemony | Idris the Great — `tahlan_DunScaith_elite` "Lady Cassandra" | 500k + `tahlan_daemonplating` modspec |
| 2 | `tahlan_daemons_unchained` | "Legio unchained" | Hegemony | Chax Alastor — `tahlan_Dominator_PH_deranged` "The Loyse" | 2M + `tahlan_neurallinkspecial` |
| 3 | `tahlan_daemons_scathach` | "Sovereign of Hel" | independent | Scathach — `tahlan_scathach_wrathbringer` | 2× `tahlan_manannan` (no credits) |

All three are `neutralisation`, `fleet_no_retreat`, AGGRESSIVE, recoverable flagships,
daemon composition (`fleet_composition_faction: tahlan_legiodaemons`, quality 2).

## Gating — how the chain unlocks

The gating relies on a **MagicLib default** that is easy to misread as broken:
`MagicBountyLoader.java` defaults a bounty's completion memkey to `"$" + bountyId` unless
an explicit `job_memKey` is set. None of these bounties set `job_memKey`, so:

- **#1 "Legio rising"** is gated on `$tahlan_triggered` (set by `awakenLegio` in
  `TahlanModPlugin.kt` when the Legio Infernalis is awakened). Completing it auto-sets
  `$tahlan_daemons = true`.
- **#2** and **#3** both gate on `$tahlan_daemons` — so beating Idris unlocks *both* the
  rampage hunt and the Sovereign's invitation simultaneously. (This is intentional; #3's
  prose reads as a sequel to #1, not necessarily after #2.)
- Completing **#3** auto-sets `$tahlan_daemons_scathach = true`, which the Manannan reward
  weapon keys off of (see below).

> **Not a bug:** because these memkeys are constructed at runtime as `"$" + bountyId`,
> they never appear as literal strings in the codebase — grepping for `$tahlan_daemons`
> finds only reads, never a write. The writes happen inside MagicLib on bounty completion.

## Fleet composition philosophy

The three fleets are deliberately differentiated by *variant selection*, not just scaled
size. The roster choices tell a story:

- **#1 "Legio rising" — the sampler.** ~45 escorts spanning nearly the entire daemon
  roster (1–2 of each variant), frigate-heavy, single capital (the flagship). Salted with
  the barcode "Sin" variants (`sloth`, `gluttony`, `greed`, `wrath`, `envy`, `lust`).
  Introduces the daemon bestiary.
- **#2 "Legio unchained" — the berserk horde.** Spammier and cruder: repeated `_overdriven`
  variants (matching the job text about safety overrides), 8 daemonified Mudskipper
  *freighters*, 8 Hounds, +2 Retribution capitals. Sells the "insane rampage" theme.
- **#3 "Sovereign of Hel" — the elite guard.** Adds **phase warfare** (4 Doom, 6 Afflictor)
  absent from the other two — a qualitative escalation. Highest scaling (1.3), unique boss
  flagship, and 4 Sunders carrying the Manannan (foreshadowing the reward).

Progression: sampler → berserk horde → elite phase-capable royal guard.

## Balance intent

**Intentionally brutal optional challenge fights with powerful rewards.** Not tuned as
fleet staples:

- Every escort is a daemon (built-in `daemoncore` + regenerating `daemonarmor`), so real
  durability/threat is understated by ship count and on-sheet FP (see `.claude/rules/balance.md`).
- The presets (46–54 hulls) far exceed `fleet_min_FP: 300`, so that floor never binds — it's
  inert config. Fleet size is driven by the preset list plus `fleet_scaling_multiplier`
  (1.2–1.3) reinforcements scaled to the player's fleet.
- #1 is the steepest relative to its position (first fight offered post-awakening), but this
  is accepted: these are optional, gated behind awakening the Legio, and reward-bearing.

## The Manannan reward mechanic

The #3 reward is 2× `tahlan_manannan` (a weapon). `ManannanEveryFrameEffect.kt` self-destructs
the firing ship **unless** `$tahlan_daemons_scathach` is set — i.e. unless you legitimately
beat Scathach to earn it. This is an **anti-cheese guard**: obtain the weapon any other way
(console, save edit) and it detonates your ship.

The guard only triggers for *player*-owned ships (`weapon.ship.owner` == player fleet manager),
so the enemy `tahlan_sunder_dmn_deathbringer_manannan` ships in fleet #3 fire it normally — you
face the weapon in the fight before you earn the right to use it.
