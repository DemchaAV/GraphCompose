package com.demcha.compose;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic regression gate for how much measurement work the layout pass
 * does, driving {@link MeasurementCountBenchmark}'s real compile through a
 * {@link CountingTextMeasurementSystem}.
 *
 * <p>These counts are exact integers, not timings. A wall-clock benchmark can
 * only say "slower on this machine today"; it cannot separate a slower laptop
 * from a slower engine, and an absolute millisecond ceiling generous enough to
 * survive CI variance is generous enough to miss a real regression. Asking the
 * layout pass how many width measurements it requested has neither problem: the
 * answer is identical on every machine and moves only when the engine's
 * behaviour actually changes.</p>
 *
 * <p>Complementary to the layout snapshots rather than a duplicate of them. Two
 * implementations that place fragments identically but measure a different
 * number of times both satisfy every snapshot; only this gate separates
 * them.</p>
 *
 * <p><b>When this fails</b>, the engine changed how it measures text. Decide
 * whether that was intended: fewer requests for the same document is an
 * improvement worth recording, more is a regression worth explaining. Either
 * way the fix is to run {@code MeasurementCountBenchmark} and update the
 * expectation below in the same commit as the behaviour change, so the number
 * moves deliberately.</p>
 *
 * <p>Allocation is deliberately not gated. It is stable to about 0.1% across
 * runs on one JVM, but it is a property of the JDK as much as of the engine, so
 * an expectation recorded on one machine would be a guess about another. The
 * probe still reports it.</p>
 */
class MeasurementCountGateTest {

    /**
     * Exact measurement work per scenario, recorded from
     * {@link MeasurementCountBenchmark} on the 2.1.1 development line.
     */
    private static final Map<String, Work> EXPECTED = expected();

    private static Map<String, Work> expected() {
        Map<String, Work> work = new LinkedHashMap<>();
        work.put("long-text", new Work(4472, 32, 32457, 124, 1, 2));
        work.put("long-token", new Work(99, 55, 7739, 600, 1, 1));
        work.put("accented-latin", new Work(2499, 37, 14393, 123, 1, 1));
        work.put("large-table", new Work(1206, 211, 5916, 13, 0, 6));
        return work;
    }

    /**
     * The measurement work one scenario costs the layout pass.
     *
     * @param widthRequests         total text-width measurements requested
     * @param distinctWidthRequests distinct (style, text) pairs among them
     * @param summedRequestChars    characters across all requests
     * @param maxRequestChars       longest single measured string
     * @param lineMetricsCalls      line-metric lookups
     * @param pages                 pages the document compiled to
     */
    private record Work(long widthRequests,
                        long distinctWidthRequests,
                        long summedRequestChars,
                        long maxRequestChars,
                        long lineMetricsCalls,
                        int pages) {

        static Work of(MeasurementCountBenchmark.Result result) {
            CountingTextMeasurementSystem.Counts counts = result.counts();
            return new Work(counts.widthRequests(),
                    counts.distinctWidthRequests(),
                    counts.summedRequestChars(),
                    counts.maxRequestChars(),
                    counts.lineMetricsCalls(),
                    result.pages());
        }
    }

    @Test
    void everyScenarioMeasuresExactlyAsMuchTextAsRecorded() throws Exception {
        for (var scenario : MeasurementCountBenchmark.scenarios().entrySet()) {
            Work actual = Work.of(
                    MeasurementCountBenchmark.measureScenario(scenario.getKey(), scenario.getValue()));

            assertThat(actual)
                    .describedAs("measurement work for '%s' changed - re-run "
                                 + "MeasurementCountBenchmark and update the expectation "
                                 + "in the same commit if the change was intended",
                            scenario.getKey())
                    .isEqualTo(EXPECTED.get(scenario.getKey()));
        }
    }

    /**
     * A scenario added to the probe without an expectation would be measured and
     * silently ignored, which is the failure mode that lets coverage rot.
     */
    @Test
    void everyProbeScenarioIsGated() {
        assertThat(MeasurementCountBenchmark.scenarios().keySet())
                .describedAs("probe scenarios missing a recorded expectation")
                .containsExactlyInAnyOrderElementsOf(EXPECTED.keySet());
    }
}
