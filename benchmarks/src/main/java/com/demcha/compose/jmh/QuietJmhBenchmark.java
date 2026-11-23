package com.demcha.compose.jmh;

import com.demcha.compose.BenchmarkSupport;
import org.slf4j.LoggerFactory;

/**
 * Base class of every JMH benchmark and {@code @State} class in this module: it
 * applies the quiet benchmark logging before the forked JVM loads anything that
 * logs.
 *
 * <p>Without it a forked benchmark JVM finds no Logback configuration and falls
 * back to Logback's default, which prints every {@code DEBUG} line the engine
 * emits to the console, inside the measured region. The static initializer here
 * runs before the subclass's own static initializer and before any
 * {@code @Setup} method (a superclass is always initialized first), so it holds
 * however the subclass orders its fields. It also initializes Logback there, so
 * a single-shot benchmark does not time Logback reading its configuration.</p>
 *
 * <p>The opt-outs are the manual harness's, passed to the fork with
 * {@code -jvmArgsAppend}: {@code -Dgraphcompose.benchmark.logging=debug} keeps
 * the engine's logging, and {@code -Dlogback.configurationFile=...} chooses
 * another configuration.</p>
 */
public abstract class QuietJmhBenchmark {

    static {
        BenchmarkSupport.configureQuietLogging();
        LoggerFactory.getILoggerFactory();
    }

    /**
     * Subclass constructor.
     */
    protected QuietJmhBenchmark() {
    }
}
