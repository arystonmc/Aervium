package com.aryston.aervium.core.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class DiskCache {
    private static final String ENTRY_EXTENSION = ".bin";
    private static final String TEMPORARY_EXTENSION = ".tmp";
    private static final int MOVE_ATTEMPTS = 4;
    private static final long RETRY_DELAY_MILLIS = 15L;

    private final Path directory;

    public DiskCache(Path directory) {
        this.directory = directory;
    }

    public Optional<byte[]> read(String key) {
        try {
            return Optional.of(Files.readAllBytes(entry(key)));
        } catch (NoSuchFileException missing) {
            return Optional.empty();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public void write(String key, byte[] content) {
        Path target = entry(key);
        if (Files.exists(target)) {
            return;
        }
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, key, TEMPORARY_EXTENSION);
            Files.write(temporary, content);
            moveIntoPlace(temporary, target);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        } finally {
            deleteQuietly(temporary);
        }
    }

    private Path entry(String key) {
        return directory.resolve(key + ENTRY_EXTENSION);
    }

    private static void moveIntoPlace(Path from, Path to) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                move(from, to);
                return;
            } catch (FileSystemException blocked) {
                if (Files.exists(to)) {
                    return;
                }
                if (attempt == MOVE_ATTEMPTS) {
                    throw blocked;
                }
                pause(attempt);
            }
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void pause(int attempt) throws IOException {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to write a cache entry", interrupted);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            file.toFile().deleteOnExit();
        }
    }
}
