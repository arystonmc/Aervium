#version 330
#extension GL_ARB_separate_shader_objects : require

#include <kinetic:lod_common.glsl>

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 geometryNormal;
layout(location = 2) out vec4 geometryLight;
layout(location = 3) out vec4 geometryAlbedo;

void main() {
    vec3 normal = terrainNormal();
    terrainClip();
    float fog = terrainFog();
    fragColor = terrainColor(normal, fog);
    geometryNormal = vec4(normal * 0.5 + 0.5, 1.0);
    geometryLight = vec4(terrainLight, 1.0, 1.0);
    geometryAlbedo = vec4(terrainAlbedo(normal), fog);
}
