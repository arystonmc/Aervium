#version 330
#extension GL_ARB_separate_shader_objects : require

#include <aervium:lod_common.glsl>

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 normal = terrainNormal();
    terrainClip();
    fragColor = terrainColor(normal, terrainFog());
}
