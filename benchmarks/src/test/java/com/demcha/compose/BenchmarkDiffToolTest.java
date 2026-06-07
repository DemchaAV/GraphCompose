package com.demcha.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Exercises the benchmark comparison engine end-to-end through
 * {@link BenchmarkDiffTool#main(String[])}: two report JSONs in, one
 * {@code diffs/<suite>/latest.json} out. Asserts the delta math, the
 * baseline&harr;candidate join (one-sided scenarios are dropped), the
 * divide-by-zero contract, and the profile / schema guards.
 *
 * <p>Mirrors the black-box style of {@link BenchmarkMedianToolTest}: redirect
 * the artifact root via {@code graphcompose.benchmark.root} and read the
 * written report back.</p>
 */
class BenchmarkDiffToolTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final double EPS = 1e-9;

    @TempDir
    Path tempDir;

    @AfterEach
    void clearBenchmarkRoot() {
        System.clearProperty("graphcompose.benchmark.root");
    }

    // ------------------------------------------------------------------
    // current-speed suite
    // ------------------------------------------------------------------

    @Test
    void currentSpeedDiffComputesSignedPercentDeltasPerScenario() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());
        Path baseline = write("baseline.json", currentSpeed("full",
                latency("engine-simple", 10.0, 20.0, 100.0, 1.0, 100.0),
                throughput("invoice-template", 1, 50.0, 20.0)));
        Path candidate = write("candidate.json", currentSpeed("full",
                latency("engine-simple", 20.0, 10.0, 50.0, 2.0, 150.0),
                throughput("invoice-template", 1, 40.0, 25.0)));

        BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()});

        JsonNode diff = readDiff("current-speed");

        JsonNode lat = diff.path("latency").get(0);
        assertThat(lat.path("scenario").asText()).isEqualTo("engine-simple");
        assertThat(lat.path("avgMillisDeltaPct").asDouble()).isCloseTo(100.0, within(EPS));   // 10 -> 20
        assertThat(lat.path("p95MillisDeltaPct").asDouble()).isCloseTo(-50.0, within(EPS));    // 20 -> 10
        assertThat(lat.path("docsPerSecondDeltaPct").asDouble()).isCloseTo(-50.0, within(EPS)); // 100 -> 50
        assertThat(lat.path("avgKilobytesDeltaPct").asDouble()).isCloseTo(100.0, within(EPS));  // 1 -> 2
        assertThat(lat.path("peakHeapMbDeltaPct").asDouble()).isCloseTo(50.0, within(EPS));     // 100 -> 150

        JsonNode thr = diff.path("throughput").get(0);
        assertThat(thr.path("scenario").asText()).isEqualTo("invoice-template");
        assertThat(thr.path("threads").asInt()).isEqualTo(1);
        assertThat(thr.path("docsPerSecondDeltaPct").asDouble()).isCloseTo(-20.0, within(EPS));  // 50 -> 40
        assertThat(thr.path("avgMillisPerDocDeltaPct").asDouble()).isCloseTo(25.0, within(EPS)); // 20 -> 25
    }

    @Test
    void currentSpeedDiffKeepsOnlyScenariosPresentInBothRuns() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());
        Path baseline = write("baseline.json", currentSpeed("full",
                latency("shared", 10.0, 10.0, 100.0, 1.0, 100.0) + ","
                        + latency("only-in-baseline", 10.0, 10.0, 100.0, 1.0, 100.0),
                throughput("shared", 1, 50.0, 20.0) + ","
                        + throughput("only-in-baseline", 2, 80.0, 12.0)));
        Path candidate = write("candidate.json", currentSpeed("full",
                latency("shared", 12.0, 12.0, 90.0, 1.1, 110.0) + ","
                        + latency("only-in-candidate", 5.0, 5.0, 200.0, 0.5, 90.0),
                throughput("shared", 1, 48.0, 21.0) + ","
                        + throughput("only-in-candidate", 4, 95.0, 9.0)));

        BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()});

        JsonNode diff = readDiff("current-speed");
        assertThat(diff.path("latency").size()).isEqualTo(1);
        assertThat(diff.path("latency").get(0).path("scenario").asText()).isEqualTo("shared");
        assertThat(diff.path("throughput").size()).isEqualTo(1);
        assertThat(diff.path("throughput").get(0).path("scenario").asText()).isEqualTo("shared");
    }

    @Test
    void currentSpeedDiffTreatsZeroBaselineAsHundredPercentAndZeroToZeroAsZero() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());
        // avgMillis 0 -> 5 => +100 ; p95 0 -> 0 => 0 ; docsPerSecond 0 -> 0 => 0
        Path baseline = write("baseline.json", currentSpeed("full",
                latency("cold-start", 0.0, 0.0, 0.0, 0.0, 0.0),
                throughput("cold-start", 1, 0.0, 0.0)));
        Path candidate = write("candidate.json", currentSpeed("full",
                latency("cold-start", 5.0, 0.0, 0.0, 0.0, 0.0),
                throughput("cold-start", 1, 0.0, 0.0)));

        BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()});

        JsonNode lat = readDiff("current-speed").path("latency").get(0);
        assertThat(lat.path("avgMillisDeltaPct").asDouble()).isCloseTo(100.0, within(EPS));
        assertThat(lat.path("p95MillisDeltaPct").asDouble()).isCloseTo(0.0, within(EPS));
        assertThat(lat.path("docsPerSecondDeltaPct").asDouble()).isCloseTo(0.0, within(EPS));
    }

    @Test
    void currentSpeedDiffRejectsRunsFromDifferentProfiles() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());
        Path baseline = write("baseline.json", currentSpeed("full",
                latency("engine-simple", 10.0, 10.0, 100.0, 1.0, 100.0),
                throughput("engine-simple", 1, 50.0, 20.0)));
        Path candidate = write("candidate.json", currentSpeed("smoke",
                latency("engine-simple", 11.0, 11.0, 95.0, 1.0, 100.0),
                throughput("engine-simple", 1, 48.0, 21.0)));

        assertThatThrownBy(() -> BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profiles do not match");
    }

    // ------------------------------------------------------------------
    // comparative suite
    // ------------------------------------------------------------------

    @Test
    void comparativeDiffComputesLibraryDeltasAndKeepsSharedLibrariesOnly() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());
        Path baseline = write("baseline.json", comparative(
                library("GraphCompose", 4.0, 0.20) + ","
                        + library("retired-lib", 9.0, 9.0)));
        Path candidate = write("candidate.json", comparative(
                library("GraphCompose", 2.0, 0.30) + ","
                        + library("brand-new-lib", 1.0, 0.05)));

        BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()});

        JsonNode diff = readDiff("comparative");
        assertThat(diff.path("libraries").size()).isEqualTo(1);
        JsonNode lib = diff.path("libraries").get(0);
        assertThat(lib.path("library").asText()).isEqualTo("GraphCompose");
        assertThat(lib.path("avgTimeDeltaPct").asDouble()).isCloseTo(-50.0, within(EPS)); // 4.0 -> 2.0
        assertThat(lib.path("avgHeapDeltaPct").asDouble()).isCloseTo(50.0, within(EPS));  // 0.20 -> 0.30
    }

    // ------------------------------------------------------------------
    // schema guards
    // ------------------------------------------------------------------

    @Test
    void rejectsMismatchedReportSchemas() throws Exception {
        Path baseline = write("baseline.json", currentSpeed("full",
                latency("engine-simple", 10.0, 10.0, 100.0, 1.0, 100.0),
                throughput("engine-simple", 1, 50.0, 20.0)));
        Path candidate = write("candidate.json", comparative(library("GraphCompose", 2.0, 0.15)));

        assertThatThrownBy(() -> BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void rejectsUnknownReportSchema() throws Exception {
        String unknown = """
                {
                  "timestamp": "2026-04-14 21:00:00",
                  "mystery": []
                }
                """;
        Path baseline = write("baseline.json", unknown);
        Path candidate = write("candidate.json", unknown);

        assertThatThrownBy(() -> BenchmarkDiffTool.main(new String[]{baseline.toString(), candidate.toString()}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown benchmark report schema");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Path write(String fileName, String json) throws Exception {
        Path path = tempDir.resolve(fileName);
        Files.writeString(path, json);
        return path;
    }

    private JsonNode readDiff(String suite) throws Exception {
        Path report = tempDir.resolve("diffs").resolve(suite).resolve("latest.json");
        return JSON.readTree(Files.readAllBytes(report));
    }

    private static String currentSpeed(String profile, String latencyItems, String throughputItems) {
        return """
                {
                  "timestamp": "2026-04-14 21:00:00",
                  "profile": "%s",
                  "latency": [%s],
                  "throughput": [%s]
                }
                """.formatted(profile, latencyItems, throughputItems);
    }

    private static String latency(String scenario,
                                  double avgMillis,
                                  double p95Millis,
                                  double docsPerSecond,
                                  double avgKilobytes,
                                  double peakHeapMb) {
        return """
                {
                  "scenario": "%s",
                  "description": "scenario %s",
                  "avgMillis": %s,
                  "p95Millis": %s,
                  "docsPerSecond": %s,
                  "avgKilobytes": %s,
                  "peakHeapMb": %s
                }
                """.formatted(scenario, scenario, avgMillis, p95Millis, docsPerSecond, avgKilobytes, peakHeapMb);
    }

    private static String throughput(String scenario, int threads, double docsPerSecond, double avgMillisPerDoc) {
        return """
                {
                  "scenario": "%s",
                  "threads": %d,
                  "docsPerSecond": %s,
                  "avgMillisPerDoc": %s
                }
                """.formatted(scenario, threads, docsPerSecond, avgMillisPerDoc);
    }

    private static String comparative(String libraryItems) {
        return """
                {
                  "timestamp": "2026-04-14 21:00:00",
                  "libraries": [%s]
                }
                """.formatted(libraryItems);
    }

    private static String library(String library, double avgTimeMs, double avgHeapMb) {
        return """
                {
                  "library": "%s",
                  "avgTimeMs": %s,
                  "avgHeapMb": %s
                }
                """.formatted(library, avgTimeMs, avgHeapMb);
    }
}
