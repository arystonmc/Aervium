package com.aryston.kinetic.debug.benchmark;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public final class BenchmarkFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
    private static final String REPORT_PREFIX = "benchmark-";
    private static final String JSON_EXTENSION = ".json";
    private static final String TEXT_EXTENSION = ".txt";

    private final Path directory;

    public BenchmarkFiles(Path directory) {
        this.directory = directory;
    }

    public Optional<BenchmarkReport> latest() {
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                .filter(BenchmarkFiles::isReport)
                .max(Comparator.comparing(path -> path.getFileName().toString()))
                .flatMap(BenchmarkFiles::read);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public Path write(BenchmarkReport report, String text) throws IOException {
        Files.createDirectories(directory);
        String name = REPORT_PREFIX + report.startedAt();
        Files.writeString(directory.resolve(name + JSON_EXTENSION), toJson(report),StandardCharsets.UTF_8);
        Path textFile = directory.resolve(name + TEXT_EXTENSION);
        Files.writeString(textFile, text, StandardCharsets.UTF_8);
        return textFile;
    }

    static String toJson(BenchmarkReport report) {
        return GSON.toJson(report);
    }

    static BenchmarkReport fromJson(String json) {
        return GSON.fromJson(json, BenchmarkReport.class);
    }

    private static boolean isReport(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(REPORT_PREFIX) && name.endsWith(JSON_EXTENSION);
    }

    private static Optional<BenchmarkReport> read(Path path) {
        try {
            BenchmarkReport report = fromJson(Files.readString(path, StandardCharsets.UTF_8));
            return report != null && report.formatVersion() == BenchmarkReport.FORMAT_VERSION ? Optional.of(report) : Optional.empty();
        } catch (IOException | JsonParseException exception) {
            return Optional.empty();
        }
    }
}
