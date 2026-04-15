package com.demcha.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkDiffToolSelectionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCompareTwoNewestCurrentSpeedRunsFromSameLatestProfile() throws Exception {
        Path suiteDir = Files.createDirectories(tempDir.resolve("current-speed"));
        writeCurrentSpeedRun(suiteDir.resolve("run-20260414-213121.json"), "smoke");
        writeCurrentSpeedRun(suiteDir.resolve("run-20260414-213539.json"), "full");
        writeCurrentSpeedRun(suiteDir.resolve("run-20260414-214100.json"), "smoke");
        writeCurrentSpeedRun(suiteDir.resolve("run-20260414-214500.json"), "full");

        BenchmarkDiffTool.ResolvedRunPair pair = BenchmarkDiffTool.resolveLatestRunPaths(tempDir, "current-speed");

        assertThat(pair.baselinePath().getFileName().toString()).isEqualTo("run-20260414-213539.json");
        assertThat(pair.candidatePath().getFileName().toString()).isEqualTo("run-20260414-214500.json");
    }

    @Test
    void shouldRejectCurrentSpeedSelectionWhenLatestProfileHasOnlyOneRun() throws Exception {
        Path suiteDir = Files.createDirectories(tempDir.resolve("current-speed"));
        writeCurrentSpeedRun(suiteDir.resolve("run-20260414-213121.json"), "smoke");
        writeCurrentSpeedRun(suiteDir.resolve("run-20260414-213539.json"), "full");

        assertThatThrownBy(() -> BenchmarkDiffTool.resolveLatestRunPaths(tempDir, "current-speed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profile 'full'");
    }

    @Test
    void shouldCompareTwoNewestArchitectureComparisonRunsFromSameLatestProfile() throws Exception {
        Path suiteDir = Files.createDirectories(tempDir.resolve("architecture-comparison"));
        writeArchitectureComparisonRun(suiteDir.resolve("run-20260414-213121.json"), "smoke");
        writeArchitectureComparisonRun(suiteDir.resolve("run-20260414-213539.json"), "full");
        writeArchitectureComparisonRun(suiteDir.resolve("run-20260414-214100.json"), "smoke");
        writeArchitectureComparisonRun(suiteDir.resolve("run-20260414-214500.json"), "full");

        BenchmarkDiffTool.ResolvedRunPair pair = BenchmarkDiffTool.resolveLatestRunPaths(tempDir, "architecture-comparison");

        assertThat(pair.baselinePath().getFileName().toString()).isEqualTo("run-20260414-213539.json");
        assertThat(pair.candidatePath().getFileName().toString()).isEqualTo("run-20260414-214500.json");
    }

    @Test
    void shouldRejectArchitectureComparisonSelectionWhenLatestProfileHasOnlyOneRun() throws Exception {
        Path suiteDir = Files.createDirectories(tempDir.resolve("architecture-comparison"));
        writeArchitectureComparisonRun(suiteDir.resolve("run-20260414-213121.json"), "smoke");
        writeArchitectureComparisonRun(suiteDir.resolve("run-20260414-213539.json"), "full");

        assertThatThrownBy(() -> BenchmarkDiffTool.resolveLatestRunPaths(tempDir, "architecture-comparison"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profile 'full'");
    }

    private void writeCurrentSpeedRun(Path path, String profile) throws Exception {
        Files.writeString(path, """
                {
                  "timestamp": "2026-04-14 21:31:21",
                  "profile": "%s",
                  "latency": [],
                  "throughput": []
                }
                """.formatted(profile));
    }

    private void writeArchitectureComparisonRun(Path path, String profile) throws Exception {
        Files.writeString(path, """
                {
                  "timestamp": "2026-04-14 21:31:21",
                  "profile": "%s",
                  "layout": [],
                  "pdf": [],
                  "stages": []
                }
                """.formatted(profile));
    }
}
