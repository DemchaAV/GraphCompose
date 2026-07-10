package com.demcha.compose;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkSupportTest {

    private static final String BENCHMARK_LOGGING_PROPERTY = "graphcompose.benchmark.logging";
    private static final String LOGBACK_CONFIG_PROPERTY = "logback.configurationFile";
    private static final String LOGBACK_STATUS_LISTENER_PROPERTY = "logback.statusListenerClass";
    private static final String NOP_STATUS_LISTENER = "ch.qos.logback.core.status.NopStatusListener";

    private String originalBenchmarkLogging;
    private String originalLogbackConfig;
    private String originalStatusListener;

    @BeforeEach
    void captureProperties() {
        originalBenchmarkLogging = System.getProperty(BENCHMARK_LOGGING_PROPERTY);
        originalLogbackConfig = System.getProperty(LOGBACK_CONFIG_PROPERTY);
        originalStatusListener = System.getProperty(LOGBACK_STATUS_LISTENER_PROPERTY);

        System.clearProperty(BENCHMARK_LOGGING_PROPERTY);
        System.clearProperty(LOGBACK_CONFIG_PROPERTY);
        System.clearProperty(LOGBACK_STATUS_LISTENER_PROPERTY);
    }

    @AfterEach
    void restoreProperties() {
        restore(BENCHMARK_LOGGING_PROPERTY, originalBenchmarkLogging);
        restore(LOGBACK_CONFIG_PROPERTY, originalLogbackConfig);
        restore(LOGBACK_STATUS_LISTENER_PROPERTY, originalStatusListener);
    }

    @Test
    void configuresQuietLogbackByDefault() {
        BenchmarkSupport.configureQuietLogging();

        assertThat(System.getProperty(LOGBACK_CONFIG_PROPERTY))
                .contains("logback-benchmark.xml");
        assertThat(System.getProperty(LOGBACK_STATUS_LISTENER_PROPERTY))
                .isEqualTo(NOP_STATUS_LISTENER);
    }

    @Test
    void leavesLogbackConfigurationUnsetWhenBenchmarkLoggingIsDebug() {
        System.setProperty(BENCHMARK_LOGGING_PROPERTY, "debug");

        BenchmarkSupport.configureQuietLogging();

        assertThat(System.getProperty(LOGBACK_CONFIG_PROPERTY)).isNull();
        assertThat(System.getProperty(LOGBACK_STATUS_LISTENER_PROPERTY))
                .isEqualTo(NOP_STATUS_LISTENER);
    }

    @Test
    void leavesLogbackConfigurationUnsetWhenBenchmarkLoggingIsTrue() {
        System.setProperty(BENCHMARK_LOGGING_PROPERTY, "true");

        BenchmarkSupport.configureQuietLogging();

        assertThat(System.getProperty(LOGBACK_CONFIG_PROPERTY)).isNull();
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
