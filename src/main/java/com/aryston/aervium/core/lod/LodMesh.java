package com.aryston.aervium.core.lod;

import java.nio.ByteBuffer;

public record LodMesh(long key, int version, ByteBuffer vertices, int quads, int opaqueQuads, boolean complete, float minY, float maxY) {
}
