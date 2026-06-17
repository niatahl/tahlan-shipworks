// Dreamweaver (tahlan_nxa) cosmetic shimmer.
//
// Re-renders the ship sprite as a glow confined to its silhouette: scrolling
// procedural value-noise paints drifting "veins" of a violet->cyan dream palette.
// Noise is synthesized from texCoord + iTime, so no external noise texture is
// needed. Driven additively from DreamShimmer.kt; brightness tracks the phase
// cloak via the `intensity` uniform.
//
// Technique adapted in spirit from RAT's Gilgamesh phase-shield overlay; see
// docs/design/dreamweaver-shimmer.md.

uniform sampler2D tex;   // ship's own sprite (unit 0); alpha is the silhouette mask
uniform float iTime;     // scrolling time
uniform float alphaMult; // global fade
uniform float intensity; // phase-linked brightness, set per-frame from Kotlin

vec2 texCoord = gl_TexCoord[0].xy;

float hash(vec2 p) {
	return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

// Smoothed value noise.
float vnoise(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	f = f * f * (3.0 - 2.0 * f);
	float a = hash(i);
	float b = hash(i + vec2(1.0, 0.0));
	float c = hash(i + vec2(0.0, 1.0));
	float d = hash(i + vec2(1.0, 1.0));
	return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
	vec4 color = texture2D(tex, texCoord);

	if (color.a > 0.5) {
		// Two octaves scrolling at different speeds/directions -> drifting filaments.
		float n1 = vnoise(texCoord * 7.0  + vec2(iTime,        iTime * 0.5));
		float n2 = vnoise(texCoord * 13.0 + vec2(-iTime * 0.7, iTime * 1.3));

		// Sharpen into thin shimmering veins rather than a flat wash.
		float shimmer = pow(n1, 4.0) * 1.5 + pow(n2, 6.0) * 2.0;

		// Dreamy violet -> cyan palette, blended by the slower octave.
		vec3 violet = vec3(0.55, 0.35, 1.0);
		vec3 cyan   = vec3(0.30, 0.90, 1.0);
		vec3 tint   = mix(violet, cyan, n2);

		color.rgb = tint;
		color.a   = clamp(shimmer * intensity, 0.0, 1.0);
	} else {
		color.a = 0.0;
	}

	color.a *= alphaMult;
	gl_FragColor = color;
}
