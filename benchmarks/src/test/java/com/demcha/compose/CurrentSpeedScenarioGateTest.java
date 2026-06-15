package com.demcha.compose;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that every CurrentSpeed latency scenario is covered by a SMOKE gate
 * threshold.
 *
 * <p>The smoke perf gate silently ignores a scenario that has no configured
 * threshold (by design — see
 * {@link CurrentSpeedBenchmarkPerfGateTest#ignoresScenariosWithoutAConfiguredThreshold()}).
 * That defensive behaviour means a newly added scenario would escape the gate
 * unnoticed. This test makes the omission fail loudly instead: adding a scenario
 * to {@code SCENARIO_DEFS} without a matching {@code SMOKE} threshold breaks the
 * build.</p>
 */
class CurrentSpeedScenarioGateTest {

    @Test
    void everyScenarioHasASmokeGateThreshold() {
        var gated = CurrentSpeedBenchmark.BenchmarkProfile.SMOKE.smokeThresholds().keySet();

        List<String> ungated = CurrentSpeedBenchmark.scenarioNames().stream()
                .filter(name -> !gated.contains(name))
                .toList();

        assertThat(ungated)
                .as("CurrentSpeed scenarios missing a SMOKE gate threshold")
                .isEmpty();
    }
}
