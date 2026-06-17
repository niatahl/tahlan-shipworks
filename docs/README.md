# Tahlan Shipworks — Design Documentation

Design notes for Tahlan Shipworks: the *why* behind content and systems that isn't
obvious from the code or data alone. Code structure lives in `CLAUDE.md` and
`.claude/rules/`; this folder records **intent** — progression design, balance
rationale, thematic goals, and the non-obvious mechanics that tie them together.

When a design decision is made or uncovered (a tuning choice, a gating mechanism, a
deliberate "this looks like a bug but isn't"), note it here so it survives in one place.

## Index

- [Legio Daemon Bounty Chain](design/legio-daemon-bounties.md) — the three-stage
  Legio Infernalis bounty progression, its memkey gating, fleet-composition philosophy,
  and the Manannan reward mechanic.
- [Dreamweaver Shimmer](design/dreamweaver-shimmer.md) — the cosmetic phase-linked
  shader overlay on the Dreamweaver (`tahlan_nxa`), and the rendering technique behind it.
- [Mod-wide globals: split singletons](design/mod-globals-architecture.md) — where cross-cutting
  flags/settings/ids/registries live (`ModCompat`, `TahlanSettings`, `TahlanRegistry`, `TahlanIDs`).
