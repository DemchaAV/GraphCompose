package com.demcha.compose.document.exceptions;

/**
 * Raised when GraphCompose fails to render or write a document through one of
 * the convenience output methods on {@link com.demcha.compose.document.api.DocumentSession}
 * such as {@code buildPdf()}, {@code writePdf(OutputStream)}, and
 * {@code toPdfBytes()}.
 *
 * <p>This is an unchecked exception so user code is not forced to declare or
 * catch a checked {@code Exception} on the most common rendering paths. The
 * underlying I/O or PDFBox failure (typically an {@link java.io.IOException})
 * is preserved as the {@linkplain Throwable#getCause() cause} for diagnosis.</p>
 *
 * <p>Lower-level backend SPIs ({@code FixedLayoutBackend.render(...)},
 * {@code SemanticBackend.export(...)}) continue to declare
 * {@code throws Exception} because backend implementations may legitimately
 * surface checked exceptions; the wrapper exists only on the convenience
 * layer.</p>
 *
 * @since 1.6.0
 */
public class DocumentRenderingException extends RuntimeException {

    /**
     * Creates a rendering exception with a message and a wrapped cause.
     *
     * @param message diagnostic message describing what failed
     * @param cause underlying exception thrown by the rendering pipeline
     */
    public DocumentRenderingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a rendering exception with a message and no wrapped cause.
     *
     * @param message diagnostic message describing what failed
     */
    public DocumentRenderingException(String message) {
        super(message);
    }
}
