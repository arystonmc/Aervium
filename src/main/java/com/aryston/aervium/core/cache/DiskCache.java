package com.aryston.aervium.core.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class DiskCache {
    private static final String ENTRY_EXTENSION = ".bin";
    private static final String TEMPORARY_EXTENSION = ".tmp";

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
        try {
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, key, TEMPORARY_EXTENSION);
            Files.write(temporary, content);
            move(temporary, entry(key));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private Path entry(String key) {
        return directory.resolve(key + ENTRY_EXTENSION);
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
