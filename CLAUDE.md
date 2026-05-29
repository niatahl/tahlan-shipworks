# Tahlan Shipworks

A content mod ("a ship pack that suffers from feature creep") for the game **Starsector**.
Mod id `tahlan`, author Nia Tahl. Targets Starsector `0.98a`.

## Environment

- **Game install:** `G:\Starsector`
- **API sources:** `G:\Starsector\starsector-core\starfarer.api.zip` — unzip/browse this for the
  vanilla API surface (`com.fs.starfarer.api.*`) and the bundled `impl` classes that most mod code
  extends or copies from.
- **Local docs:** `.claude/rules/` holds worked references for the trickier game systems
  (see [below](#detailed-subsystem-guides)) — read the relevant one before implementing in that area.

## Languages & build

- Mixed **Kotlin + Java** (~90 Kotlin files, ~220 Java files). New code is usually Kotlin; large
  swaths of older code are still Java. Match the language of the file/package you're editing.
- Source lives under `jars/src/org/niatahl/tahlan/`. There is **no Gradle/Ant build** — the project
  is an IntelliJ module (`tahlan-shipworks.iml`) that compiles to the committed artifact
  `jars/TahlanShipworks.jar`. Rebuild the jar via IntelliJ's build/artifacts, not a CLI tool.
- The game loads the jar named in `mod_info.json` (`jars/TahlanShipworks.jar`), so the jar **must be
  rebuilt and committed** for code changes to take effect in-game.

### Dependencies

- **Hard deps** (declared in `mod_info.json`, required to launch): LazyLib, MagicLib.
- **Soft integrations** (guarded by `isModEnabled` checks, mostly in `utils/*Integrations.kt`):
  Nexerelin/Exerelin, Industrial Evolution (IndEvo), Exiled Space, LunaLib, Second-in-Command,
  GraphicsLib. Code that touches these must stay behind an enablement check — never assume they're
  present.

## Layout

### `jars/src/org/niatahl/tahlan/` — compiled code

- `plugins/` — entry points. `TahlanModPlugin.kt` is the `modPlugin` from `mod_info.json` (onApplicationLoad / onGameLoad wiring, AI registration, integration toggles). `CampaignPluginImpl.kt` registers interaction/battle-creation plugin picks.
- `campaign/` — campaign-side systems: `econ/`, `items/`, `missions/`, `rulecmd/` (rule commands), `siege/`, `submarkets/`.
- `hullmods/` — hullmod effects (incl. `barcodes/`).
- `shipsystems/` — ship-system scripts and their `ai/`.
- `weapons/` — weapon effects, `ai/` (projectile/weapon AI), `deco/`.
- `skills/`, `listeners/`, `world/` (faction relations, system generation: Lethia, Rubicon), `utils/` (`TahlanIDs.kt` for id constants, `TahlanPeople.kt`, integration shims, helpers).

### `data/` — game-data definitions (CSV/JSON/`.ship`/`.variant`)

- `hulls/` (`ship_data.csv`, `.ship`, `skins/`), `weapons/`, `shipsystems/`, `hullmods/`,
  `variants/`, `characters/`, `strings/`.
- `campaign/` — `rules.csv` and campaign config.
- `world/factions/` — faction definitions.
- `missions/` — scripted missions (`tahlan_01_*`, `tahlan_02_*`, `tahlan_test`).
- `config/` — large tree of per-integration config (`exerelin/`, `exoticaFactionConfig/`, `indEvo/`,
  `starship_legends/`, `chatter/`, `modFiles/` with `magicBounty_data.json`, etc.).

### Other

- `graphics/`, `sounds/` — assets.
- `tahlan_settings.json` — user-facing toggles (also exposed via LunaLib; LunaLib values win).
  Controls optional content: Lethia system, Legio Infernalis faction, Daemons, hard/fast/adaptive
  modes, etc.
- `tahlan.version` — version checker manifest. `changelog.txt` — player-facing changelog.

## Conventions

- Namespace all ids, tags, and memory keys to avoid collisions with vanilla and other mods. ID
  constants are centralized in `utils/TahlanIDs.kt`.
- Settings are **save-locked** vs **toggle** (see comments in `tahlan_settings.json`) — respect that
  distinction when adding new ones.
- Prefer library helpers over hand-rolled boilerplate: MagicLib for fleets/bounties, LazyLib for
  combat utility lookups, LunaLib for settings and terser campaign scripting.

## Detailed subsystem guides

`.claude/rules/` contains worked references for the trickier systems — read the relevant one before
implementing in that area:

- `fleets.md` — spawning fleets, assignments, memory flags, library shortcuts.
- `fleet_behavior.md` — fleet managers + assignment-AI state-machine patterns.
- `fleet_interaction_dialog.md` — `FleetInteractionDialogPluginImpl` overrides (boss encounters, etc.).
- `scripted_dialog.md` — Java-authored `InteractionDialogPlugin` setpieces.
- `rules.md` — the `rules.csv` system and custom rule commands.
