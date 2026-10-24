#version 330
#extension GL_ARB_separate_shader_objects : require

#include <aervium:lod_common.glsl>

uniform sampler2D SceneDepthSampler;

layout(location = 0) out vec4 surfaceData;
layout(location = 1) out vec4 surfaceTint;

const float SURFACE_THICKNESS = 6.0;
const float TINT_PRESENT = 1.0;

vec2 encodeOctahedral(vec3 normal) {
    vec3 folded = normal / (abs(normal.x) + abs(normal.y) + abs(normal.z));
    return folded.xz;
}

void main() {
    vec3 normal = terrainNormal();
    if (terrainWater < 0.5 || normal.y < WATER_TOP || gl_FragCoord.z < texelFetch(SceneDepthSampler, ivec2(gl_FragCoord.xy), 0).r) {
        discard;
    }
    terrainClip();
    surfaceData = vec4(encodeOctahedral(vec3(0.0, 1.0, 0.0)), SURFACE_THICKNESS, terrainLight.y);
    surfaceTint = vec4(vertexColor.rgb * ColorModulator.rgb, TINT_PRESENT);
}
