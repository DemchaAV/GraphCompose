package com.demcha.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkMedianToolTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path tempDir;

    @AfterEach
    void clearBenchmarkRoot() {
        System.clearProperty("graphcompose.benchmark.root");
    }

    @Test
    void shouldWriteMedianCurrentSpeedAggregateForRepeatedRuns() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());

        Path suiteDir = Files.createDirectories(tempDir.resolve("current-speed"));
        Path runA = suiteDir.resolve("run-20260414-210000.json");
        Path runB = suiteDir.resolve("run-20260414-211000.json");
        Path runC = suiteDir.resolve("run-20260414-212000.json");

        Files.writeString(runA, """
                {
                  "timestamp": "2026-04-14 21:00:00",
                  "profile": "full",
                  "warmupIterations": 12,
                  "measurementIterations": 40,
                  "docsPerThread": 12,
                  "threadCounts": [1, 2],
                  "latency": [
                    {
                      "scenario": "engine-simple",
                      "description": "One-page engine composition",
                      "avgMillis": 10.0,
                      "p50Millis": 9.0,
                      "p95Millis": 12.0,
                      "maxMillis": 13.0,
                      "docsPerSecond": 100.0,
                      "avgKilobytes": 1.0,
                      "peakHeapMb": 110.0
                    }
                  ],
                  "throughput": [
                    {
                      "scenario": "invoice-template",
                      "threads": 1,
                      "totalDocs": 12,
                      "docsPerSecond": 50.0,
                      "avgMillisPerDoc": 20.0
                    }
                  ],
                  "totalBytes": 1000
                }
                """);
        Files.writeString(runB, """
                {
                  "timestamp": "2026-04-14 21:10:00",
                  "profile": "full",
                  "warmupIterations": 12,
                  "measurementIterations": 40,
                  "docsPerThread": 12,
                  "threadCounts": [1, 2],
                  "latency": [
                    {
                      "scenario": "engine-simple",
                      "description": "One-page engine composition",
                      "avgMillis": 20.0,
                      "p50Millis": 19.0,
                      "p95Millis": 22.0,
                      "maxMillis": 23.0,
                      "docsPerSecond": 50.0,
                      "avgKilobytes": 1.5,
                      "peakHeapMb": 120.0
                    }
                  ],
                  "throughput": [
                    {
                      "scenario": "invoice-template",
                      "threads": 1,
                      "totalDocs": 12,
                      "docsPerSecond": 40.0,
                      "avgMillisPerDoc": 25.0
                    }
                  ],
                  "totalBytes": 2000
                }
                """);
        Files.writeString(runC, """
                {
                  "timestamp": "2026-04-14 21:20:00",
                  "profile": "full",
                  "warmupIterations": 12,
                  "measurementIterations": 40,
                  "docsPerThread": 12,
                  "threadCounts": [1, 2],
                  "latency": [
                    {
                      "scenario": "engine-simple",
                      "description": "One-page engine composition",
                      "avgMillis": 30.0,
                      "p50Millis": 29.0,
                      "p95Millis": 32.0,
                      "maxMillis": 33.0,
                      "docsPerSecond": 33.0,
                      "avgKilobytes": 2.0,
                      "peakHeapMb": 130.0
                    }
                  ],
                  "throughput": [
                    {
                      "scenario": "invoice-template",
                      "threads": 1,
                      "totalDocs": 12,
                      "docsPerSecond": 30.0,
                      "avgMillisPerDoc": 30.0
                    }
                  ],
                  "totalBytes": 3000
                }
                """);

        BenchmarkMedianTool.main(new String[]{
                "current-speed",
                runA.toString(),
                runB.toString(),
                runC.toString()
        });

        JsonNode aggregate = JSON.readTree(
                Files.readAllBytes(tempDir.resolve("aggregates/current-speed/full/latest.json")));

        assertThat(aggregate.path("aggregation").asText()).isEqualTo("median");
        assertThat(aggregate.path("sourceCount").asInt()).isEqualTo(3);
        assertThat(aggregate.path("latency").get(0).path("avgMillis").asDouble()).isEqualTo(20.0);
        assertThat(aggregate.path("latency").get(0).path("peakHeapMb").asDouble()).isEqualTo(120.0);
        assertThat(aggregate.path("throughput").get(0).path("docsPerSecond").asDouble()).isEqualTo(40.0);
        assertThat(aggregate.path("totalBytes").asLong()).isEqualTo(2000L);
    }

    @Test
    void shouldWriteMedianComparativeAggregateForRepeatedRuns() throws Exception {
        System.setProperty("graphcompose.benchmark.root", tempDir.toString());

        Path suiteDir = Files.createDirectories(tempDir.resolve("comparative"));
        Path runA = suiteDir.resolve("run-20260414-210000.json");
        Path runB = suiteDir.resolve("run-20260414-211000.json");
        Path runC = suiteDir.resolve("run-20260414-212000.json");

        Files.writeString(runA, """
                {
                  "timestamp": "2026-04-14 21:00:00",
                  "warmupIterations": 50,
                  "measurementIterations": 100,
                  "libraries": [
                    {"library": "GraphCompose", "avgTimeMs": 3.0, "avgHeapMb": 0.10},
                    {"library": "iText 5 (Old)", "avgTimeMs": 2.0, "avgHeapMb": 0.20}
                  ]
                }
                """);
        Files.writeString(runB, """
                {
                  "timestamp": "2026-04-14 21:10:00",
                  "warmupIterations": 50,
                  "measurementIterations": 100,
                  "libraries": [
                    {"library": "GraphCompose", "avgTimeMs": 2.0, "avgHeapMb": 0.15},
                    {"library": "iText 5 (Old)", "avgTimeMs": 1.8, "avgHeapMb": 0.25}
                  ]
                }
                """);
        Files.writeString(runC, """
                {
                  "timestamp": "2026-04-14 21:20:00",
                  "warmupIterations": 50,
                  "measurementIterations": 100,
                  "libraries": [
                    {"library": "GraphCompose", "avgTimeMs": 4.0, "avgHeapMb": 0.12},
                    {"library": "iText 5 (Old)", "avgTimeMs": 1.5, "avgHeapMb": 0.30}
                  ]
                }
                """);

        BenchmarkMedianTool.main(new String[]{
                "comparative",
                runA.toString(),
                runB.toString(),
                runC.toString()
        });

        JsonNode aggregate = JSON.readTree(
                Files.readAllBytes(tempDir.resolve("aggregates/comparative/latest.json")));

        assertThat(aggregate.path("aggregation").asText()).isEqualTo("median");
        assertThat(aggregate.path("sourceCount").asInt()).isEqualTo(3);
        assertThat(aggregate.path("libraries").get(0).path("avgTimeMs").asDouble()).isEqualTo(3.0);
        assertThat(aggregate.path("libraries").get(1).path("avgHeapMb").asDouble()).isEqualTo(0.25);
    }

}
