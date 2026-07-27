package com.demcha.examples.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The committed current-speed baseline, for examples that quote engine speed.
 *
 * <p>Reads {@code baselines/current-speed-full.json} — the same file the local
 * perf gate scores against, put on the classpath by the examples module rather
 * than copied, so there is one number and not two that drift. An example that
 * quotes a figure therefore restates measured data instead of asserting a
 * literal somebody typed once.</p>
 *
 * <p>The figures are one machine's median over several runs. Absolute
 * milliseconds do not travel between machines, so anything rendering them
 * should show {@link #capturedOn()} beside them and let the reader judge.</p>
 *
 * <p>Loading never throws: a resource that is missing, unreadable or malformed
 * yields an empty baseline, and callers render a plain "not measured" rather
 * than a number nobody measured. {@code PerfBaselineTest} keeps that fallback
 * from becoming the normal case by asserting the resource really is on the
 * classpath with the scenarios the examples quote.</p>
 */
public final class PerfBaseline {

    private static final String RESOURCE = "/baselines/current-speed-full.json";

    private static final PerfBaseline EMPTY = new PerfBaseline("undated", Map.of());

    private static final PerfBaseline INSTANCE = load();

    private final String capturedOn;
    private final Map<String, Scenario> scenarios;

    private PerfBaseline(String capturedOn, Map<String, Scenario> scenarios) {
        this.capturedOn = capturedOn;
        this.scenarios = scenarios;
    }

    /**
     * One scenario's measured speed.
     *
     * @param avgMillis     average render latency in milliseconds
     * @param docsPerSecond documents rendered per second
     */
    public record Scenario(double avgMillis, double docsPerSecond) {
    }

    /**
     * The bundled baseline.
     *
     * @return the loaded baseline
     */
    public static PerfBaseline get() {
        return INSTANCE;
    }

    /**
     * The date the baseline was measured, as {@code yyyy-MM-dd}.
     *
     * @return capture date, or {@code "undated"} when the file carries no timestamp
     */
    public String capturedOn() {
        return capturedOn;
    }

    /**
     * Looks up one scenario by its benchmark name.
     *
     * @param scenario scenario name, e.g. {@code invoice-template}
     * @return the scenario's figures, empty when the baseline does not carry it
     */
    public Optional<Scenario> scenario(String scenario) {
        return Optional.ofNullable(scenarios.get(scenario));
    }

    private static PerfBaseline load() {
        // Never throws. This runs in a static initializer, so a malformed file
        // would otherwise fail class-load and take down every example in the
        // module, not just the one quoting a figure. Absent and unreadable
        // degrade the same way, and the caller renders the honest fallback.
        try (InputStream in = PerfBaseline.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return EMPTY;
            }
            JsonNode root = new ObjectMapper().readTree(in);
            Map<String, Scenario> scenarios = new HashMap<>();
            for (JsonNode row : root.path("latency")) {
                String name = row.path("scenario").asText("");
                // Require both figures to be present and numeric: a renamed or
                // dropped field would otherwise read as 0.0 and render a
                // plausible "0.0 ms avg, 0 docs/sec" instead of admitting the
                // measurement is missing.
                if (name.isEmpty()
                    || !row.path("avgMillis").isNumber()
                    || !row.path("docsPerSecond").isNumber()) {
                    continue;
                }
                scenarios.put(name, new Scenario(row.path("avgMillis").asDouble(),
                        row.path("docsPerSecond").asDouble()));
            }
            String timestamp = root.path("timestamp").asText("");
            return new PerfBaseline(
                    timestamp.length() >= 10 ? timestamp.substring(0, 10) : "undated",
                    Map.copyOf(scenarios));
        } catch (IOException | RuntimeException e) {
            return EMPTY;
        }
    }
}
