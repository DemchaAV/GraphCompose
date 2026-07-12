package com.demcha.compose.document.exceptions;

/**
 * Raised when a document operation needs a fixed-layout render/measurement
 * backend but none is registered on the classpath.
 *
 * <p>Since GraphCompose 2.0 the engine and document model ship without a bundled
 * backend: rendering and text measurement are provided by a separate artifact
 * (for example {@code io.github.demchaav:graph-compose-render-pdf}) discovered at
 * runtime through {@link java.util.ServiceLoader}. Calling a convenience output
 * method — {@code toPdfBytes()}, {@code buildPdf()}, {@code toImages()} — or
 * requesting {@code layoutSnapshot()} without such an artifact on the classpath
 * fails with this exception. The message names the artifact to add.</p>
 *
 * <p>This is an unchecked exception: a missing backend is a build/classpath
 * configuration problem, not a per-call argument error, so user code is not
 * forced to catch it. The explicit {@code render(FixedLayoutBackend)} /
 * {@code export(SemanticBackend)} paths never throw it — they use a
 * caller-supplied backend instead of the ServiceLoader lookup.</p>
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
