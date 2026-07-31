package com.demcha.compose.document.exceptions;

/**
 * Raised when a document operation needs a backend that is not registered on
 * the classpath.
 *
 * <p>Since GraphCompose 2.0 the engine and document model ship without a bundled
 * backend: text measurement and rendering are provided by separate artifacts
 * (for example {@code io.github.demchaav:graph-compose-render-pdf} or
 * {@code io.github.demchaav:graph-compose-render-pptx}) discovered at runtime
 * through {@link java.util.ServiceLoader}. Two distinct lookups can fail, at
 * two different moments.</p>
 *
 * <p><strong>Measurement, at {@code create()}.</strong> Opening a
 * {@code DocumentSession} resolves a {@code FontMetricsProvider}, because the
 * session measures text as it builds. A classpath carrying no provider therefore
 * fails while the session is being constructed — before any content is added and
 * before any output method is reached. {@code graph-compose-render-pdf} is the
 * only shipped artifact that registers one, so a bare {@code graph-compose-core}
 * classpath cannot open a session at all.</p>
 *
 * <p><strong>Format backend, at the output call.</strong> Once the session is
 * open, a convenience output method — {@code toPdfBytes()}, {@code buildPdf()},
 * {@code toImages(int)}, {@code toPptxBytes()}, {@code buildPptx(Path)} —
 * additionally resolves a fixed-layout backend for that format. Asking for PPTX
 * output with only the PDF backend present fails here, at the call.</p>
 *
 * <p>The explicit {@code render(FixedLayoutBackend)} /
 * {@code export(SemanticBackend)} paths skip the second lookup — they use the
 * caller-supplied backend instead of the ServiceLoader — but not the first: the
 * session they render from still had to open, and opening it needed a
 * measurement provider.</p>
 *
 * <p>This is an unchecked exception: a missing backend is a build/classpath
 * configuration problem, not a per-call argument error, so user code is not
 * forced to catch it. The message names the artifact to add.</p>
 *
 * @since 2.0.0
 */
public class MissingBackendException extends IllegalStateException {

    /**
     * Creates a missing-backend exception.
     *
     * @param message diagnostic message naming the artifact to add
     */
    public MissingBackendException(String message) {
        super(message);
    }
}
