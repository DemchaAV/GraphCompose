package com.demcha.compose.document.backend.fixed.pptx;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Collects what the render log said while one deck was written.
 *
 * <p>Part of this backend's font contract is carried by the log rather than by the
 * file: which family the deck could not embed, and which it carried after all. The
 * tests that read it need the same three steps — attach a {@link ListAppender} to the
 * render logger, read the formatted messages under one key, detach — and had written
 * them out again in three classes, in two different shapes (a {@code @BeforeEach}
 * pair, and a try/finally inside a helper).</p>
 *
 * <p>Attach it in a {@code @BeforeEach} and close it in an {@code @AfterEach}, or wrap
 * one render in try-with-resources; both shapes are in use. Closing detaches, so a
 * test that fails mid-render cannot leak the appender into the next one.</p>
 */
final class RenderLogCapture implements AutoCloseable {

    /** The logger every render backend writes its per-pass notes to. */
    private static final String RENDER_LOGGER = "com.demcha.compose.engine.render";

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender;

    private RenderLogCapture(Logger logger, ListAppender<ILoggingEvent> appender) {
        this.logger = logger;
        this.appender = appender;
    }

    /** Starts collecting the render log, until this capture is closed. */
    static RenderLogCapture attach() {
        Logger logger = (Logger) LoggerFactory.getLogger(RENDER_LOGGER);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return new RenderLogCapture(logger, appender);
    }

    /**
     * The formatted messages logged under one key, in the order they were logged.
     *
     * @param key the leading key of the message, such as {@code render.pptx.font.substitution}
     */
    List<String> messages(String key) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith(key))
                .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }
}
