package com.demcha.compose.document.templates.support.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Shared lifecycle logger for built-in canonical templates.
 *
 * <p>The logger intentionally records only template identifiers, spec types,
 * and timings. It must not log document text, contact values, addresses, or
 * other user-provided content.</p>
 */
public final class TemplateLifecycleLog {
    private static final Logger LOG = LoggerFactory.getLogger("com.demcha.compose.templates.lifecycle");

    private TemplateLifecycleLog() {
    }

    /**
     * Records the start of one template composition pass.
     *
     * @param templateId stable template identifier
     * @param spec template specification object
     * @return monotonic start time used by {@link #success(String, Object, long)}
     */
    public static long start(String templateId, Object spec) {
        long startNanos = System.nanoTime();
        LOG.debug("template.compose.start templateId={} specType={}", templateId, specType(spec));
        return startNanos;
    }

    /**
     * Records successful completion of one template composition pass.
     *
     * @param templateId stable template identifier
     * @param spec template specification object
     * @param startNanos monotonic start time returned by {@link #start(String, Object)}
     */
    public static void success(String templateId, Object spec, long startNanos) {
        LOG.debug(
                "template.compose.end templateId={} specType={} durationMs={}",
                templateId,
                specType(spec),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
    }

    /**
     * Records a failed template composition pass without logging user content.
     *
     * @param templateId stable template identifier
     * @param spec template specification object
     * @param startNanos monotonic start time returned by {@link #start(String, Object)}
     * @param failure failure raised during template composition
     */
    public static void failure(String templateId, Object spec, long startNanos, Throwable failure) {
        LOG.debug(
                "template.compose.failed templateId={} specType={} durationMs={} errorType={}",
                templateId,
                specType(spec),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                failure.getClass().getSimpleName());
    }

    private static String specType(Object spec) {
        return spec == null ? "null" : spec.getClass().getSimpleName();
    }
}
