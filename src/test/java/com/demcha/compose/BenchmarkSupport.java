package com.demcha.compose;

import java.net.URL;

final class BenchmarkSupport {

    private static final String LOGBACK_CONFIG_PROPERTY = "logback.configurationFile";
    private static final String LOGBACK_STATUS_LISTENER_PROPERTY = "logback.statusListenerClass";
    private static final String NOP_STATUS_LISTENER = "ch.qos.logback.core.status.NopStatusListener";

    private BenchmarkSupport() {
    }

    static void configureQuietLogging() {
        // Suppress Logback's own bootstrap/status output for benchmark runs.
        // This keeps console output focused on the benchmark metrics instead of
        // framework initialization noise.
        if (System.getProperty(LOGBACK_STATUS_LISTENER_PROPERTY) == null) {
            System.setProperty(LOGBACK_STATUS_LISTENER_PROPERTY, NOP_STATUS_LISTENER);
        }

        if (System.getProperty(LOGBACK_CONFIG_PROPERTY) != null) {
            return;
        }

        URL config = BenchmarkSupport.class.getClassLoader().getResource("logback-benchmark.xml");
        if (config != null) {
            System.setProperty(LOGBACK_CONFIG_PROPERTY, config.toExternalForm());
        }
    }
}
