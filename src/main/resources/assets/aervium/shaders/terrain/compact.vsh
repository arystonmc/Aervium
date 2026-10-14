#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:terrainglobals.glsl>
#ifndef MULTIDRAW_TERRAIN
    #include <minecraft:chunksection.glsl>
#endif

layout(location = 0) in uvec4 PositionLight;
layout(location = 1) in vec4 Color;
layout(location = 2) in uvec2 PackedUV0;
#ifdef MULTIDRAW_TERRAIN
layout(location = 4) in ivec3 ChunkPosition;
layout(location = 5) in float ChunkVisibility;
#endif

uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out float chunkVisibility;

const float AERVIUM_POSITION_STEP = 1.0 / 2048.0;
const float AERVIUM_POSITION_ORIGIN = 8.0;
const float AERVIUM_UV_STEP = 1.0 / 65536.0;
const uint AERVIUM_LIGHT_MASK = 0xFFu;
const uint AERVIUM_SKY_LIGHT_SHIFT = 8u;
const float AERVIUM_CHUNK_FULLY_VISIBLE_RANGE = 16.0;

void main() {
    vec3 position = vec3(PositionLight.xyz) * AERVIUM_POSITION_STEP - AERVIUM_POSITION_ORIGIN;
    ivec2 light = ivec2(PositionLight.w & AERVIUM_LIGHT_MASK, PositionLight.w >> AERVIUM_SKY_LIGHT_SHIFT);
    vec3 pos = position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * sample_lightmap(Sampler2, light);
    texCoord0 = vec2(PackedUV0) * AERVIUM_UV_STEP;

    float dist = length(pos);
    chunkVisibility = mix(1.0, ChunkVisibility, clamp((dist - AERVIUM_CHUNK_FULLY_VISIBLE_RANGE) / AERVIUM_CHUNK_FULLY_VISIBLE_RANGE, 0.0, 1.0));
}
