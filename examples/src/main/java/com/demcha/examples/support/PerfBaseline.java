package com.demcha.examples.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.UncheckedIOException;
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
 */
public final class PerfBaseline {

    private static final String RESOURCE = "/baselines/current-speed-full.json";

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
        try (InputStream in = PerfBaseline.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return new PerfBaseline("undated", Map.of());
            }
            JsonNode root = new ObjectMapper().readTree(in);
            Map<String, Scenario> scenarios = new HashMap<>();
            for (JsonNode row : root.path("latency")) {
                scenarios.put(row.path("scenario").asText(),
                        new Scenario(row.path("avgMillis").asDouble(),
                                row.path("docsPerSecond").asDouble()));
            }
            String timestamp = root.path("timestamp").asText("");
            return new PerfBaseline(
                    timestamp.length() >= 10 ? timestamp.substring(0, 10) : "undated",
                    Map.copyOf(scenarios));
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }
}
