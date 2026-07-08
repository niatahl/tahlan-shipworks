# Legio Balancing & Challenge Mechanics

How Legio Infernalis fleets scale their difficulty to the player, and the balance intent
behind it. The runtime scaling lives in `LegioFleetInflationListener.kt` (a
`FleetInflationListener` that fires when any non-player `tahlan_legio*` fleet is inflated).
For the daemon *hull* budget philosophy see `.claude/rules/balance.md` (enemy/special tech
types); for the bounty-chain fights see [Legio Daemon Bounty Chain](legio-daemon-bounties.md).

These mechanics only touch fleet members carrying the `DAEMONIC_HEART` built-in (the true
daemons). Everything is gated behind the settings toggles below, so a player who wants a
flat difficulty can opt out.

## Two challenge toggles

Both are read from `TahlanSettings` (LunaLib-exposed where applicable):

- **`ENABLE_ADAPTIVEMODE`** — daemons rematch the player's own S-mod investment (below).
- **`ENABLE_HARDMODE`** — raises the *floor* on daemon-core officer quality, scaling with
  player level (`playerLevel / 3`, min 1) instead of a flat min of 1.

## Adaptive mode — S-mod matching

When adaptive mode is on, each daemon's max-permanent-hullmod budget is set to the player
fleet's S-mod level, and it's handed that many random S-mods (from `SMOD_OPTIONS`, plus the
barcode "Sin" mods once the count exceeds 3). The idea: the more the player min-maxes with
S-mods, the more the daemons do too — the challenge tracks the player's power curve rather
than sitting at a fixed point.

### Fleet-point-weighted average (not a plain average)

The match target is a **fleet-point-weighted average** of S-mods across the player's combat
ships, not a plain per-ship average:

```
avgSMods = round( Σ(sMods_i × fleetPoints_i) / Σ(fleetPoints_i) )
```

A plain average (total S-mods ÷ ship count) let a player dilute a heavily-barcoded capital
by tagging along cheap, unmodded frigates — a capital with 7 S-mods next to a 0-S-mod
frigate averaged to ~3.5 and the daemons under-matched the real threat. Weighting by fleet
points makes the big investment dominate the target, closing that loophole.

**Why fleet points, not deployment points (DP):** FP is the deliberately lighter, more
compressed scale (a capital is only ~5–8× a frigate on FP, far steeper on DP). Using FP
still puts the weight firmly on the big barcoded hulls without flattening frigates out of
the calculation entirely — which matters because this is a *fleet-composition* mechanic:
every hull the player fields should still register in the target, just proportionally to its
size. DP would over-punish and effectively erase small ships from the average. This is an
intentional choice, not an oversight — `.claude/rules/balance.md` notes DP is the truer
size measure for hull *budgets*, but that guidance is about pricing individual hulls, not
about weighting a fleet-wide difficulty average.

Implementation: `LegioFleetInflationListener.addSMods()`. Fighters and civilians are excluded
from both sums; `totalFp` is floored at 1 to avoid divide-by-zero on an all-civilian fleet.

## Hard mode — daemon-core officer floor

`addDaemonCore()` assigns an AI-core officer to each daemon (skipping the flagship and any
existing AI/special officer). The tier is rolled on a d5 minus a hull-size modifier (`MAG`:
frigates roll −2, destroyers −1, so small hulls skew toward weaker cores), then floored at a
minimum:

- **Normal:** min tier 1.
- **Hard mode:** min `playerLevel / 3` (min 1) — so a high-level player faces beta/alpha-core
  daemons as a baseline rather than occasionally.
- **DunScaith daemons** (`tahlan_DunScaith_dmn`) always floor at 3 regardless of mode — they
  are meant to be elite.

Legio-faction daemons draw from a daemon-specific core pool (gamma → `CORE_DAEMON` →
`CORE_ARCHDAEMON`); non-Legio fleets flying daemons fall back to the vanilla gamma/beta/alpha
progression.

## Design intent

Daemons are an out-of-band enemy tech type meant to *exceed* vanilla per-DP (regenerating
armor, low-flux-optimized engines — see `.claude/rules/balance.md`). The adaptive/hard toggles
exist so that edge doesn't go stale against a fully-kitted endgame fleet: the challenge is
designed to climb with the player, and both mechanics are opt-in so players who don't want
that can turn them off.
