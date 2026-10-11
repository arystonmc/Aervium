package com.aryston.aervium.core.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheFilesTest {
    private static final int SPIRV_MAGIC = 0x07230203;
    private static final int VENDOR = 0x10DE;
    private static final int DEVICE = 0x28A1;

    @TempDir
    Path folder;

    @Test
    void writesAndReadsEntriesWithoutLeftovers() throws Exception {
        DiskCache cache = new DiskCache(folder.resolve("spirv"));
        assertTrue(cache.read("abc").isEmpty());
        cache.write("abc", new byte[] {1, 2, 3});
        cache.write("abc", new byte[] {4, 5});
        assertArrayEquals(new byte[] {4, 5}, cache.read("abc").orElseThrow());
        try (Stream<Path> files = Files.list(folder.resolve("spirv"))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void acceptsOnlyWellFormedSpirv() {
        assertTrue(SpirvBinary.isValid(spirv(5)));
        assertFalse(SpirvBinary.isValid(new byte[] {3, 2, 35, 7}));
        assertFalse(SpirvBinary.isValid(new byte[21]));
        byte[] truncated = new byte[22];
        System.arraycopy(spirv(5), 0, truncated, 0, 20);
        assertFalse(SpirvBinary.isValid(truncated));
    }

    @Test
    void pipelineCacheDataMustComeFromTheSameDevice() {
        byte[] uuid = new byte[PipelineCacheHeader.UUID_BYTES];
        uuid[3] = 9;
        byte[] data = pipelineCache(VENDOR, DEVICE, uuid);
        assertTrue(PipelineCacheHeader.matches(data, VENDOR, DEVICE, uuid));
        assertFalse(PipelineCacheHeader.matches(data, VENDOR, DEVICE + 1, uuid));
        assertFalse(PipelineCacheHeader.matches(data, VENDOR, DEVICE, new byte[PipelineCacheHeader.UUID_BYTES]));
        assertFalse(PipelineCacheHeader.matches(new byte[8], VENDOR, DEVICE, uuid));
    }

    @Test
    void countsHitsMissesAndTime() {
        CacheStatistics statistics = new CacheStatistics();
        statistics.hit(5);
        statistics.miss(7);
        statistics.miss(1);
        assertEquals(1, statistics.hits());
        assertEquals(2, statistics.misses());
        assertEquals(13, statistics.workNanos());
    }

    private static byte[] spirv(int words) {
        ByteBuffer buffer = ByteBuffer.allocate(words * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(SPIRV_MAGIC);
        return buffer.array();
    }

    private static byte[] pipelineCache(int vendor, int device, byte[] uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(32).putInt(1).putInt(vendor).putInt(device).put(uuid);
        return buffer.array();
    }
}
