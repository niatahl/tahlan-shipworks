// Minimal pass-through vertex shader: fixed-function transform + texcoord 0.
// Shared by Tahlan fragment-shader overlays (e.g. tahlan_dreamshimmer).

void main() {
	gl_Position = ftransform();
	gl_TexCoord[0] = gl_MultiTexCoord0;
}
