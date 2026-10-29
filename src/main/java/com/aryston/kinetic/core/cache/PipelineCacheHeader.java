package com.aryston.kinetic.core.cache;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class PipelineCacheHeader {
    public static final int UUID_BYTES = 16;

    private static final int HEADER_VERSION_ONE = 1;
    private static final int HEADER_BYTES = 16 + UUID_BYTES;
    private static final int UUID_OFFSET = 16;

    private PipelineCacheHeader() {
    }

    public static boolean matches(byte[] data, int vendorId, int deviceId, byte[] pipelineCacheUuid) {
        if (data.length < HEADER_BYTES) {
            return false;
        }
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int length = header.getInt();
        int version = header.getInt();
        int vendor = header.getInt();
        int device = header.getInt();
        byte[] uuid = Arrays.copyOfRange(data, UUID_OFFSET, UUID_OFFSET + UUID_BYTES);
        return length >= HEADER_BYTES
            && version == HEADER_VERSION_ONE
            && vendor == vendorId
            && device == deviceId
            && Arrays.equals(uuid, pipelineCacheUuid);
    }
}
