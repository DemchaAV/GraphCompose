package com.demcha.compose;

import java.net.URL;

final class BenchmarkSupport {

    private static final String LOGBACK_CONFIG_PROPERTY = "logback.configurationFile";

    private BenchmarkSupport() {
    }

    static void configureQuietLogging() {
        if (System.getProperty(LOGBACK_CONFIG_PROPERTY) != null) {
            return;
        }

        URL config = BenchmarkSupport.class.getClassLoader().getResource("logback-benchmark.xml");
        if (config != null) {
            System.setProperty(LOGBACK_CONFIG_PROPERTY, config.toExternalForm());
        }
    }
}
