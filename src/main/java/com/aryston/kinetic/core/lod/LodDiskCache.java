package com.aryston.kinetic.core.lod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.jspecify.annotations.Nullable;

public final class LodDiskCache {
    private static final int MAGIC = 0x41525954;
    private static final int VERSION = 2;
    private static final int COMPRESSION_LEVEL = 1;
    private static final int MAX_PAYLOAD_BYTES = LodTile.SAMPLES * (Long.BYTES + Integer.BYTES + LodLayers.MAX_LAYERS * (Long.BYTES + Short.BYTES + Integer.BYTES)) + Integer.BYTES;
    private static final String HASH = "SHA-256";
    private static final String EXTENSION = ".bin";
    private static final String TEMP_EXTENSION = ".pending";
    private static final long MAX_CACHE_BYTES = 512L * 1024L * 1024L;
    private static final long TRIM_INTERVAL_NANOS = 60_000_000_000L;
    private static final int CACHE_DEPTH = 2;

    private final Path root;
    private long lastTrim;

    public LodDiskCache(Path directory, String identity) {
        root = directory.resolve(hash(identity)).toAbsolutePath().normalize();
    }

    public @Nullable LodData read(long key, boolean exact) throws IOException {
        Path file = file(key, exact);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return readFile(file, key, exact);
        } catch (IOException | IllegalArgumentException failure) {
            Files.deleteIfExists(file);
            return null;
        }
    }

    private static LodData readFile(Path file, long key, boolean exact) throws IOException {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(file))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION || input.readLong() != key) {
                throw new IOException("Invalid cached terrain header");
            }
            long checksum = input.readLong();
            byte[] payload;
            try (InflaterInputStream compressed = new InflaterInputStream(input)) {
                payload = compressed.readNBytes(MAX_PAYLOAD_BYTES + 1);
            }
            CRC32 crc = new CRC32();
            crc.update(payload);
            if (payload.length > MAX_PAYLOAD_BYTES || crc.getValue() != checksum) {
                throw new IOException("Invalid cached terrain checksum");
            }
            LodData data = decode(payload);
            if (exact && !data.exact()) {
                throw new IOException("Incomplete cached terrain");
            }
            return data;
        }
    }

    public void write(long key, LodData data) throws IOException {
        Path file = file(key, data.exact());
        if (Files.isRegularFile(file)) {
            return;
        }
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + "." + UUID.randomUUID() + TEMP_EXTENSION);
        byte[] payload = encode(data);
        CRC32 crc = new CRC32();
        crc.update(payload);
        Deflater deflater = new Deflater(COMPRESSION_LEVEL);
        try {
            try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(temporary))) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeLong(key);
                output.writeLong(crc.getValue());
                DeflaterOutputStream compressed = new DeflaterOutputStream(output, deflater);
                compressed.write(payload);
                compressed.finish();
            }
            try {
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException failure) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException failure) {
                if (!Files.isRegularFile(file)) {
                    throw failure;
                }
            }
            trim();
        } finally {
            deflater.end();
            Files.deleteIfExists(temporary);
        }
    }

    private void trim() throws IOException {
        long now = System.nanoTime();
        if (lastTrim != 0L && now - lastTrim < TRIM_INTERVAL_NANOS) {
            return;
        }
        lastTrim = now;
        ArrayList<CachedFile> files = new ArrayList<>();
        long total = 0L;
        try (var paths = Files.walk(root, CACHE_DEPTH)) {
            for (Path path : paths.filter(path -> path.getFileName().toString().endsWith(EXTENSION) && Files.isRegularFile(path)).toList()) {
                long bytes = Files.size(path);
                files.add(new CachedFile(path, bytes, Files.getLastModifiedTime(path).toMillis()));
                total += bytes;
            }
        }
        files.sort(Comparator.comparingLong(CachedFile::written));
        for (CachedFile file : files) {
            if (total <= MAX_CACHE_BYTES) {
                break;
            }
            Files.deleteIfExists(file.path());
            total -= file.bytes();
        }
    }

    private record CachedFile(Path path, long bytes, long written) {
    }

    private Path file(long key, boolean exact) {
        return root.resolve(exact ? "exact" : "preview").resolve(Long.toUnsignedString(key, 16) + EXTENSION);
    }

    private static byte[] encode(LodData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            for (long sample : data.samples()) {
                output.writeLong(sample);
            }
            LodLayers layers = data.layers();
            output.writeInt(layers == null ? 0 : layers.spans());
            if (layers != null && layers.spans() > 0) {
                for (int column = 0; column < LodTile.SAMPLES; column++) {
                    output.writeInt(layers.end(column) - layers.start(column));
                }
                for (int span = 0; span < layers.spans(); span++) {
                    output.writeLong(layers.surface(span));
                    output.writeShort(layers.bottom(span));
                    output.writeInt(layers.side(span));
                }
            }
        }
        return bytes.toByteArray();
    }

    private static LodData decode(byte[] payload) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            long[] samples = new long[LodTile.SAMPLES];
            for (int column = 0; column < samples.length; column++) {
                samples[column] = input.readLong();
            }
            int spans = input.readInt();
            if (spans < 0 || spans > LodTile.SAMPLES * LodLayers.MAX_LAYERS) {
                throw new IOException("Invalid cached terrain span count");
            }
            LodLayers layers = null;
            if (spans > 0) {
                int[] offsets = new int[LodTile.SAMPLES + 1];
                for (int column = 0; column < LodTile.SAMPLES; column++) {
                    int count = input.readInt();
                    if (count < 0 || count > LodLayers.MAX_LAYERS) {
                        throw new IOException("Invalid cached terrain layer count");
                    }
                    offsets[column + 1] = offsets[column] + count;
                }
                long[] surfaces = new long[spans];
                short[] bottoms = new short[spans];
                int[] sides = new int[spans];
                for (int span = 0; span < spans; span++) {
                    surfaces[span] = input.readLong();
                    bottoms[span] = input.readShort();
                    sides[span] = input.readInt();
                }
                layers = new LodLayers(offsets, surfaces, bottoms, sides);
            }
            if (input.read() != -1) {
                throw new IOException("Trailing cached terrain data");
            }
            return new LodData(samples, layers);
        }
    }

    private static String hash(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(HASH).digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
