#version 450

layout(local_size_x = 64) in;

layout(std430, binding = 0) readonly buffer SectionTable {
    int sectionTable[];
};

layout(std430, binding = 1) readonly buffer VisibleSections {
    ivec2 visibleSections[];
};

layout(std430, binding = 2) writeonly buffer DrawCommands {
    uint drawCommands[];
};

layout(std430, binding = 3) buffer DrawCounts {
    uint drawCounts[];
};

layout(std430, binding = 4) writeonly buffer SectionInstances {
    ivec4 sectionInstances[];
};

layout(std430, binding = 5) readonly buffer GroupRanks {
    int groupRanks[];
};

layout(push_constant) uniform CullParameters {
    ivec4 cameraBlockAndCount;
    vec4 cameraFraction;
    uvec4 limits;
} cull;

const int SECTION_STRIDE = 36;
const int SECTION_HEADER = 4;
const int SLOT_STRIDE = 16;
const int SLOT_GROUP = 0;
const int SLOT_INDEX_COUNT = 1;
const int SLOT_FIRST_INDEX = 2;
const int SLOT_BASE_VERTEX = 3;
const int SLOT_FLAGS = 4;
const int SLOT_QUAD_OFFSETS = 5;
const int SLOT_PLANES = 9;
const int HAS_FACES = 1;
const int BUCKETS = 7;
const int INDICES_PER_QUAD = 6;
const int INTS_PER_COMMAND = 5;
const float EYE_MARGIN = 0.5;
const int EAST = 1;
const int WEST = 2;
const int UP = 3;
const int DOWN = 4;
const int SOUTH = 5;
const int NORTH = 6;

int firstQuad(int slot, int bucket) {
    int packed = sectionTable[slot + SLOT_QUAD_OFFSETS + bucket / 2];
    return (bucket & 1) == 0 ? packed & 0xFFFF : (packed >> 16) & 0xFFFF;
}

float plane(int slot, int bucket) {
    return intBitsToFloat(sectionTable[slot + SLOT_PLANES + bucket]);
}

int visibleBuckets(int slot, vec3 eye) {
    int mask = 1;
    mask |= eye.x > plane(slot, EAST) - EYE_MARGIN ? 1 << EAST : 0;
    mask |= eye.x < plane(slot, WEST) + EYE_MARGIN ? 1 << WEST : 0;
    mask |= eye.y > plane(slot, UP) - EYE_MARGIN ? 1 << UP : 0;
    mask |= eye.y < plane(slot, DOWN) + EYE_MARGIN ? 1 << DOWN : 0;
    mask |= eye.z > plane(slot, SOUTH) - EYE_MARGIN ? 1 << SOUTH : 0;
    mask |= eye.z < plane(slot, NORTH) + EYE_MARGIN ? 1 << NORTH : 0;
    return mask;
}

void emit(int group, int indexCount, int firstIndex, int baseVertex, uint instance) {
    int rank = groupRanks[group];
    if (rank < 0) {
        return;
    }
    uint capacity = cull.limits.x;
    uint draw = atomicAdd(drawCounts[rank], 1u);
    if (draw >= capacity) {
        return;
    }
    uint base = (uint(rank) * capacity + draw) * INTS_PER_COMMAND;
    drawCommands[base] = uint(indexCount);
    drawCommands[base + 1] = 1u;
    drawCommands[base + 2] = uint(firstIndex);
    drawCommands[base + 3] = uint(baseVertex);
    drawCommands[base + 4] = instance;
}

void emitFacing(int slot, int group, int firstIndex, int baseVertex, uint instance, int visible) {
    int runStart = -1;
    for (int bucket = 0; bucket <= BUCKETS; bucket++) {
        bool include = bucket < BUCKETS && (((visible >> bucket) & 1) != 0 || firstQuad(slot, bucket + 1) == firstQuad(slot, bucket));
        if (include) {
            if (runStart < 0) {
                runStart = bucket;
            }
            continue;
        }
        if (runStart >= 0) {
            int first = firstQuad(slot, runStart);
            int quads = firstQuad(slot, bucket) - first;
            if (quads > 0) {
                emit(group, quads * INDICES_PER_QUAD, firstIndex + first * INDICES_PER_QUAD, baseVertex, instance);
            }
            runStart = -1;
        }
    }
}

void main() {
    uint entry = gl_GlobalInvocationID.x;
    if (entry >= uint(cull.cameraBlockAndCount.w)) {
        return;
    }
    ivec2 visible = visibleSections[entry];
    int section = visible.x * SECTION_STRIDE;
    ivec3 origin = ivec3(sectionTable[section], sectionTable[section + 1], sectionTable[section + 2]);
    int slots = sectionTable[section + 3];
    sectionInstances[entry] = ivec4(origin, visible.y);
    uint instance = cull.limits.y + entry;
    vec3 eye = vec3(cull.cameraBlockAndCount.xyz - origin) + cull.cameraFraction.xyz;
    for (int index = 0; index < slots; index++) {
        int slot = section + SECTION_HEADER + index * SLOT_STRIDE;
        int group = sectionTable[slot + SLOT_GROUP];
        int indexCount = sectionTable[slot + SLOT_INDEX_COUNT];
        int firstIndex = sectionTable[slot + SLOT_FIRST_INDEX];
        int baseVertex = sectionTable[slot + SLOT_BASE_VERTEX];
        bool hasFaces = (sectionTable[slot + SLOT_FLAGS] & HAS_FACES) != 0;
        if (hasFaces && cull.limits.z != 0u) {
            emitFacing(slot, group, firstIndex, baseVertex, instance, visibleBuckets(slot, eye));
        } else {
            emit(group, indexCount, firstIndex, baseVertex, instance);
        }
    }
}
