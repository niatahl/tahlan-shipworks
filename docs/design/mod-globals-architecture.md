# Mod-wide globals: split singletons

Where the mod's cross-cutting flags, settings, ids, and runtime registries live, and why
they're split the way they are.

## The split

`TahlanModPlugin`'s companion object used to be a grab-bag of unrelated globals. These are now
separated by concern into dedicated singletons under `org.niatahl.tahlan.utils`:

| Object | Holds | Populated by |
|---|---|---|
| `ModCompat` | soft-dep presence: `HAS_GRAPHICSLIB`, `HAS_NEX`, `HAS_INDEVO`, `HAS_LUNA` | `detectAtAppLoad()` (onApplicationLoad), `detectNexerelin()` (onNewGame) |
| `TahlanSettings` | player toggles: `ENABLE_*`, `INDEVO_MINES/ARTY`, `WEEB_MODE` | `loadFromJson()` (onApplicationLoad), `loadFromLuna()` (new game / load) |
| `TahlanRegistry` | runtime id lists: `SHIELD_HULLMODS`, `DAEMON_SHIPS`, `BLACKWATCH_DAEMONS`, `DAEMON_WINGS`, `DAEMON_WEAPONS` | `collectFromSpecs()` (onApplicationLoad) |
| `TahlanIDs` (existing) | the `*_MISSILE_ID` constants and `SETTINGS_FILE` | compile-time constants |

`TahlanModPlugin` keeps only its own `LOGGER` and the daemon lifecycle helpers
(`enableDaemons`, `awakenLegioHostility`, `triggerDaemonicIncursion`, `addDaemons`,
`removeDaemons`). It now *orchestrates* population (calls `ModCompat.detect*`,
`TahlanSettings.load*`, `TahlanRegistry.collectFromSpecs`) rather than storing the state.

## Key design decisions

- **Split by concern, not one god-object.** A single `TahlanGlobals` would just relocate the
  grab-bag. The four groups are genuinely independent (mod detection vs. settings load vs.
  compile-time ids vs. spec-scanned registries), so they get separate homes.
- **Each object owns its own load/collect logic.** The assignment-heavy loaders (`loadFromJson`,
  `loadFromLuna`, `collectFromSpecs`) live *inside* the objects, where their members are in scope.
  The plugin only calls them. This also sidestepped any imported-property-assignment pitfalls in
  the plugin.
- **`@JvmField` on everything Java reads.** `ModCompat.HAS_GRAPHICSLIB` (RealityShell.java),
  `TahlanSettings.ENABLE_HARDMODE` (LegioTyranny.java), and `TahlanRegistry.SHIELD_HULLMODS`
  (ForcedOverdrive.java) are accessed from Java. A Kotlin `object` property without `@JvmField`
  compiles to a getter and breaks `Foo.FIELD` Java/static-import access — so all moved members
  carry `@JvmField`.
- **`SHIELD_HULLMODS` went to `TahlanRegistry`, not a daemon object.** It's a runtime-collected id
  list like the daemon lists, but isn't daemon-related — so the registry is named generically
  rather than `DaemonRegistry`.
- **Nexerelin detection timing preserved.** `HAS_NEX` is still probed at new-game time
  (`detectNexerelin()`), not at app load, matching the original control flow exactly. (The original
  only set it in `onNewGame`; this was kept as-is rather than "fixed" during a refactor.)
- **No deprecated aliases.** All ~12-15 call sites were migrated directly and the old companion
  fields deleted — clean end state, no forwarding cruft.

## Adding a new global

- A soft-dep flag → `ModCompat` (+ detect it in the right lifecycle method).
- A player setting → `TahlanSettings` (mind the save-lock vs toggle distinction from
  tahlan_settings.json; wire both `loadFromJson` and `loadFromLuna`).
- A fixed id → `TahlanIDs`.
- A spec-scanned id list → `TahlanRegistry`.

If Java reads it, mark it `@JvmField`.

## Build note

Kotlin lives in the committed jar — rebuild `jars/TahlanShipworks.jar` in IntelliJ for these
changes to take effect.
