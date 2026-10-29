#include <minecraft:dynamictransforms.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:globals.glsl>

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec3 cameraRelativePosition;
layout(location = 4) flat in vec2 terrainLight;
layout(location = 5) flat in vec2 terrainFade;
layout(location = 6) flat in vec4 terrainBounds;
layout(location = 7) flat in float terrainWater;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

const float CHUNK_BLOCKS = 16.0;
const float COVERAGE_SIZE = 256.0;
const float LIGHTMAP_MAX = 240.0;
const uint HASH_X = 0x9E3779B9u;
const uint HASH_Y = 0x85EBCA6Bu;
const uint HASH_MIX = 0x7FEB352Du;
const uint HASH_MASK = 0xFFFFu;
const float HASH_RANGE = 65536.0;
const ivec2 DETAIL_HASH_OFFSET = ivec2(1048576);
const vec2 BLOCK_HASH_SHIFT = vec2(7919.0, 104729.0);
const float TEXTURE_PIXELS = 16.0;
const float DETAIL_FADE_START = 0.6;
const float DETAIL_FADE_END = 1.8;
const float PIXEL_CONTRAST = 0.24;
const float BLOCK_CONTRAST = 0.1;
const float WATER_TOP = 0.5;
const float WATER_BODY = 0.85;
const float WATER_FRESNEL_MIN = 0.04;
const float WATER_FRESNEL_POWER = 5.0;
const float WATER_REFLECTION = 0.8;
const float WATER_SKY_FLOOR = 0.25;
const float SECONDS_PER_DAY = 1200.0;
const float RIPPLE_SCALE_X = 0.7;
const float RIPPLE_SCALE_Z = 0.9;
const float RIPPLE_SPEED_X = 1.3;
const float RIPPLE_SPEED_Z = 1.1;
const float RIPPLE_STRENGTH = 0.08;
const float RIPPLE_FADE_START = 0.5;
const float RIPPLE_FADE_END = 2.0;

float terrainNoise() {
    uvec2 pixel = uvec2(gl_FragCoord.xy);
    uint value = pixel.x * HASH_X ^ pixel.y * HASH_Y;
    value = (value ^ (value >> 16u)) * HASH_MIX;
    return float(value & HASH_MASK) / HASH_RANGE;
}

void terrainCoverageClip() {
    vec2 position = cameraRelativePosition.xz;
    if (position.x < terrainBounds.x || position.y < terrainBounds.y || position.x > terrainBounds.z || position.y > terrainBounds.w) {
        discard;
    }
    vec4 clip = TextureMat[0];
    if (position.x >= clip.x && position.y >= clip.y && position.x < clip.z && position.y < clip.w) {
        vec2 column = floor((position + TextureMat[1].zw) / CHUNK_BLOCKS);
        ivec2 cell = ivec2(mod(column, COVERAGE_SIZE));
        if (texelFetch(Sampler0, cell, 0).r > 0.0) {
            discard;
        }
    }
}

void terrainClip() {
    terrainCoverageClip();
    float noise = terrainNoise();
    if (noise < terrainFade.x || noise >= terrainFade.y) {
        discard;
    }
}

vec3 terrainNormal() {
    vec3 normal = normalize(cross(dFdx(cameraRelativePosition), dFdy(cameraRelativePosition)));
    return dot(normal, cameraRelativePosition) > 0.0 ? -normal : normal;
}

float terrainFog() {
    float distant = linear_fog_value(cylindricalVertexDistance, FogRenderDistanceStart, FogRenderDistanceEnd);
    return max(linear_fog_value(sphericalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd), distant) * FogColor.a;
}

vec3 terrainWorld() {
    return cameraRelativePosition + TextureMat[2].xyz;
}

float terrainHash(vec2 cell) {
    uvec2 value = uvec2(ivec2(cell) + DETAIL_HASH_OFFSET);
    uint mixed = value.x * HASH_X ^ value.y * HASH_Y;
    mixed = (mixed ^ (mixed >> 16u)) * HASH_MIX;
    return float(mixed & HASH_MASK) / HASH_RANGE;
}

float terrainDetail(vec3 normal) {
    if (TextureMat[2].w < 0.5 || terrainWater > 0.5) {
        return 1.0;
    }
    vec3 world = terrainWorld();
    vec3 axis = abs(normal);
    vec2 surface = axis.y >= axis.x && axis.y >= axis.z ? world.xz : axis.x >= axis.z ? world.zy : world.xy;
    vec2 pixels = surface * TEXTURE_PIXELS;
    float footprint = length(fwidth(pixels));
    float pixelVisibility = 1.0 - smoothstep(DETAIL_FADE_START, DETAIL_FADE_END, footprint);
    float blockVisibility = 1.0 - smoothstep(DETAIL_FADE_START, DETAIL_FADE_END, footprint / TEXTURE_PIXELS);
    float pixel = terrainHash(floor(pixels)) - 0.5;
    float block = terrainHash(floor(surface) + BLOCK_HASH_SHIFT) - 0.5;
    return 1.0 + pixel * PIXEL_CONTRAST * pixelVisibility + block * BLOCK_CONTRAST * blockVisibility;
}

vec3 terrainAlbedo(vec3 normal) {
    return vertexColor.rgb * ColorModulator.rgb * terrainDetail(normal);
}

vec3 waterSurface(vec3 albedo, vec3 light, float shade) {
    vec3 view = normalize(cameraRelativePosition);
    float facing = clamp(abs(view.y), 0.0, 1.0);
    float fresnel = WATER_FRESNEL_MIN + (1.0 - WATER_FRESNEL_MIN) * pow(1.0 - facing, WATER_FRESNEL_POWER);
    vec2 world = terrainWorld().xz;
    float seconds = GameTime * SECONDS_PER_DAY;
    float footprint = length(fwidth(world));
    float rippleVisibility = 1.0 - smoothstep(RIPPLE_FADE_START, RIPPLE_FADE_END, footprint);
    float ripple = sin(world.x * RIPPLE_SCALE_X + seconds * RIPPLE_SPEED_X) * sin(world.y * RIPPLE_SCALE_Z - seconds * RIPPLE_SPEED_Z);
    vec3 body = albedo * WATER_BODY * light * shade * (1.0 + ripple * RIPPLE_STRENGTH * rippleVisibility);
    vec3 sky = FogColor.rgb * max(light, vec3(WATER_SKY_FLOOR));
    return mix(body, sky, fresnel * WATER_REFLECTION);
}

vec4 terrainColor(vec3 normal, float fog) {
    float shade = dot(abs(normal), vec3(0.6, normal.y > 0.0 ? 1.0 : 0.5, 0.8));
    vec3 light = sample_lightmap(Sampler2, ivec2(round(terrainLight * LIGHTMAP_MAX))).rgb;
    vec3 albedo = terrainAlbedo(normal);
    vec3 lit = terrainWater > 0.5 && normal.y > WATER_TOP ? waterSurface(albedo, light, shade) : albedo * light * shade;
    return vec4(mix(lit, FogColor.rgb, fog), 1.0);
}
