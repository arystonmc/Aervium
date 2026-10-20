#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:fog.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 TileOffset;
layout(location = 3) in vec2 FadeRange;
layout(location = 4) in vec4 TransitionBounds;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec3 cameraRelativePosition;
layout(location = 4) flat out vec2 terrainLight;
layout(location = 5) flat out vec2 terrainFade;
layout(location = 6) flat out vec4 terrainBounds;
layout(location = 7) flat out float terrainWater;

const float BYTE_MAX = 255.0;
const float SKY_LIGHT_MAX = 15.0;
const float BLOCK_LIGHT_MAX = 7.0;
const uint SKY_SHIFT = 4u;
const uint BLOCK_SHIFT = 1u;
const uint BLOCK_MASK = 7u;
const uint WATER_MASK = 1u;

void main() {
    vec3 position = Position + TileOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    sphericalVertexDistance = fog_spherical_distance(position);
    cylindricalVertexDistance = fog_cylindrical_distance(position);
    vertexColor = vec4(Color.rgb, 1.0);
    cameraRelativePosition = position;
    uint metadata = uint(round(Color.a * BYTE_MAX));
    terrainLight = vec2(float((metadata >> BLOCK_SHIFT) & BLOCK_MASK) / BLOCK_LIGHT_MAX, float(metadata >> SKY_SHIFT) / SKY_LIGHT_MAX);
    terrainWater = float(metadata & WATER_MASK);
    terrainFade = FadeRange;
    terrainBounds = TransitionBounds;
}
