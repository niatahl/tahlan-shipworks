# Dreamweaver Shimmer (cosmetic shader overlay)

A purely cosmetic, phase-linked shimmer on the **Dreamweaver** (`tahlan_nxa`): scrolling
procedural-noise "veins" of a violet→cyan palette, confined to the hull silhouette, that
flare as the ship phases in and out.

## What it is

- **Built-in hullmod** `tahlan_dreamshimmer` (`org.niatahl.tahlan.hullmods.DreamShimmer`),
  wired only to `tahlan_nxa` (hidden + hiddenEverywhere in `hull_mods.csv`).
- On ship creation in combat it adds a `BaseCombatLayeredRenderingPlugin` that re-renders
  the hull sprite through a GraphicsLib fragment shader each frame on the `ABOVE_SHIPS` layer.
- **Shaders:** `data/shaders/tahlan_dreamshimmer.shader` (fragment) + `tahlan_baseVertex.shader`
  (shared pass-through vertex).

## Key design decisions

- **Phase-linked, not constant.** Intensity = `0.25 + 0.9 * phaseCloak.effectLevel`, so there's
  a gentle baseline shimmer that flares during the phase transition. Chosen because the Dreamweaver
  is a phase ship (system `tahlan_phasebreaker`, which "counts as a phase cloak"); tying the
  cosmetic to the cloak makes it feel intrinsic. `phaseCloak` is null-guarded so a non-phase hull
  would simply get the constant baseline.
- **Dreamweaver-specific, not a reusable system.** Colors and params are hardcoded in the shader.
  If other hulls ever want a shimmer, generalize then — not pre-emptively.
- **Procedural noise, no texture asset.** Unlike RAT (which ships two noise PNGs), the shimmer
  synthesizes value-noise in the shader from `texCoord + iTime`. Avoids a binary asset, keeps the
  effect fully self-contained, and sidesteps any asset-licensing question.
- **Additive blend.** The overlay adds the tint scaled by the noise alpha, so it reads as a glow
  rather than recoloring the hull.
- **GraphicsLib-gated.** GraphicsLib is a soft dep. The hullmod bails in `applyEffectsAfterShipCreation`
  if `TahlanModPlugin.HAS_GRAPHICSLIB` is false — the ship then renders normally, no shimmer. All
  shader code stays behind that flag.

## Technique provenance

Adapted from RAT (Random Assortment of Things) — the Gilgamesh's phase-shield overlay
(`assortment_of_things.exotech.hullmods.PhaseshiftShield.PhaseshiftShieldRenderer`). We reused only
the *rendering* technique (layered-rendering plugin + re-render-sprite-through-shader, uniforms
`iTime`/`alphaMult`/`intensity`, bind ship texture to unit 0), and dropped all of RAT's gameplay
(shield HP, damage conversion, UI bar). RAT's shader samples scrolling noise *textures*; ours
generates the noise procedurally.

## Tuning knobs

- **Color:** `violet` / `cyan` vec3s in `tahlan_dreamshimmer.shader`.
- **Density/scale:** the `* 7.0` / `* 13.0` texCoord multipliers (octave frequency) and the
  `pow(n, 4.0)` / `pow(n, 6.0)` exponents (vein sharpness).
- **Scroll speed:** the `/ 12f` divisor on `iTime` in `DreamShimmer.kt`.
- **Brightness curve:** the `0.25f + 0.9f * phaseLevel` expression in `DreamShimmer.render()`.

## Build note

The Kotlin lives in the committed jar, so `jars/TahlanShipworks.jar` must be **rebuilt in IntelliJ**
for the hullmod to load in-game. The shader/CSV/`.ship` changes are data and take effect on reload.
