package com.aryston.kinetic.core.lod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LodDiskCacheTest {
    private static final String WORLD = "world-seed-dimension-packs-generator";
    private static final long KEY = LodStore.key(0, -3, 2);
    @TempDir
    Path directory;

    @Test
    void compressionPreservesLayersSideColoursLightsAndKnownVoid() throws Exception {
        LodDiskCache cache = new LodDiskCache(directory, WORLD);
        LodData data = LodLayersTest.layered(true);
        cache.write(KEY, data);
        LodData restored = cache.read(KEY, true);
        assertArrayEquals(data.samples(), restored.samples());
        assertTrue(data.layers().matches(restored.layers()));
        try (var files = Files.walk(directory)) {
            assertTrue(files.filter(Files::isRegularFile).allMatch(path -> path.getFileName().toString().endsWith(".bin")));
        }
        assertNull(cache.read(KEY, false));
        assertNull(new LodDiskCache(directory, WORLD + "-different-pack").read(KEY, true));
    }

    @Test
    void damagedDataIsRemovedAndCanBeGeneratedAgain() throws Exception {
        LodDiskCache cache = new LodDiskCache(directory, WORLD);
        LodData data = LodLayersTest.layered(false);
        cache.write(KEY, data);
        Path file;
        try (var files = Files.walk(directory)) {
            file = files.filter(Files::isRegularFile).findFirst().orElseThrow();
        }
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length / 2] ^= 1;
        Files.write(file, bytes);
        assertNull(cache.read(KEY, true));
        assertFalse(Files.exists(file));
        cache.write(KEY, data);
        assertArrayEquals(data.samples(), cache.read(KEY, true).samples());
        Files.write(file, new byte[] {1, 2, 3});
        assertNull(cache.read(KEY, true));
        assertFalse(Files.exists(file));
    }

    @Test
    void concurrentWritersPublishWholeFilesWithoutPendingDebris() throws Exception {
        LodData data = LodLayersTest.layered(false);
        try (var workers = Executors.newFixedThreadPool(4)) {
            var jobs = IntStream.range(0, 16).mapToObj(index -> (Callable<Void>) () -> {
                LodDiskCache cache = new LodDiskCache(directory, WORLD);
                cache.write(KEY, data);
                assertArrayEquals(data.samples(), cache.read(KEY, true).samples());
                return null;
            }).toList();
            for (var result : workers.invokeAll(jobs)) {
                result.get();
            }
        }
        try (var files = Files.walk(directory)) {
            assertTrue(files.filter(Files::isRegularFile).allMatch(path -> path.getFileName().toString().endsWith(".bin")));
        }
    }
}
