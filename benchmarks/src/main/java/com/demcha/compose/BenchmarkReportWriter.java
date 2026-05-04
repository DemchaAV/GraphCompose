package com.demcha.compose;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes timestamped and latest benchmark artifacts under {@code target/benchmarks}.
 */
final class BenchmarkReportWriter {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private BenchmarkReportWriter() {
    }

    static BenchmarkArtifacts prepare(String suiteName) throws IOException {
        Path directory = root().resolve(suiteName);
        Files.createDirectories(directory);
        return new BenchmarkArtifacts(directory, LocalDateTime.now().format(FILE_TIMESTAMP));
    }

    private static Path root() {
        return Path.of(System.getProperty("graphcompose.benchmark.root", Path.of("target", "benchmarks").toString()));
    }

    static final class BenchmarkArtifacts {
        private final Path directory;
        private final String timestamp;

        private BenchmarkArtifacts(Path directory, String timestamp) {
            this.directory = directory;
            this.timestamp = timestamp;
        }

        Path writeJson(Object value) throws IOException {
            byte[] bytes = JSON.writeValueAsBytes(value);
            Path latest = directory.resolve("latest.json");
            Path archived = directory.resolve("run-" + timestamp + ".json");
            Files.write(latest, bytes);
            Files.write(archived, bytes);
            return archived;
        }

        Path writeCsv(String tableName, List<String> headers, List<List<String>> rows) throws IOException {
            String csv = toCsv(headers, rows);
            Path latest = directory.resolve("latest-" + tableName + ".csv");
            Path archived = directory.resolve(tableName + "-" + timestamp + ".csv");
            Files.writeString(latest, csv, StandardCharsets.UTF_8);
            Files.writeString(archived, csv, StandardCharsets.UTF_8);
            return archived;
        }

        Path directory() {
            return directory;
        }

        String timestamp() {
            return timestamp;
        }

        private String toCsv(List<String> headers, List<List<String>> rows) {
            StringBuilder builder = new StringBuilder();
            builder.append(join(headers)).append('\n');
            for (List<String> row : rows) {
                builder.append(join(row)).append('\n');
            }
            return builder.toString();
        }

        private String join(List<String> values) {
            return values.stream()
                    .map(this::escape)
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
        }

        private String escape(String value) {
            String safe = value == null ? "" : value;
            if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
                return "\"" + safe.replace("\"", "\"\"") + "\"";
            }
            return safe;
        }
    }
}
