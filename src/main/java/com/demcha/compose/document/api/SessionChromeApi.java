package com.demcha.compose.document.api;

import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentProtection;
import com.demcha.compose.document.output.DocumentWatermark;

import java.util.Objects;

/**
 * Fluent facade for the chrome (metadata, watermark, protection,
 * header/footer) configuration of a {@link DocumentSession}. Returned by
 * {@link DocumentSession#chrome()}.
 *
 * <p>This facade exists to slim {@code DocumentSession}'s top-level
 * surface (audit finding H1) by grouping the backend-neutral chrome
 * configuration calls behind a single accessor. The equivalent
 * top-level methods on {@code DocumentSession} continue to work
 * unchanged for backward compatibility — both styles set the same
 * underlying {@code chromeOptions}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * try (DocumentSession session = GraphCompose.document(out).create()) {
 *     session.chrome()
 *             .metadata(DocumentMetadata.builder().title("Q1").build())
 *             .watermark(DocumentWatermark.builder().text("DRAFT").build())
 *             .header(DocumentHeaderFooter.builder().centerText("Quarterly").build())
 *             .footer(DocumentHeaderFooter.builder().centerText("Page {page} of {pages}").build());
 *     template.compose(session, spec);
 *     session.buildPdf();
 * }
 * }</pre>
 *
 * <p>Use {@link #session()} to chain back to the owning session if you
 * prefer a single fluent expression that mixes chrome and authoring
 * calls.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.6.0
 */
public final class SessionChromeApi {

    private final DocumentSession session;
    private final DocumentChromeOptions chromeOptions;

    /**
     * Package-private constructor — instances are created exclusively via
     * {@link DocumentSession#chrome()}.
     */
    SessionChromeApi(DocumentSession session, DocumentChromeOptions chromeOptions) {
        this.session = Objects.requireNonNull(session, "session");
        this.chromeOptions = Objects.requireNonNull(chromeOptions, "chromeOptions");
    }

    /**
     * Configures backend-neutral document metadata. Pass {@code null}
     * to clear.
     *
     * @param metadata canonical metadata, or {@code null} to clear
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionChromeApi metadata(DocumentMetadata metadata) {
        ensureOpen();
        chromeOptions.setMetadata(metadata);
        return this;
    }

    /**
     * Configures a backend-neutral document-wide watermark. Pass
     * {@code null} to clear.
     *
     * @param watermark canonical watermark, or {@code null} to clear
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionChromeApi watermark(DocumentWatermark watermark) {
        ensureOpen();
        chromeOptions.setWatermark(watermark);
        return this;
    }

    /**
     * Configures backend-neutral document protection (passwords and
     * permissions). Pass {@code null} to clear.
     *
     * @param protection canonical protection, or {@code null} to clear
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionChromeApi protect(DocumentProtection protection) {
        ensureOpen();
        chromeOptions.setProtection(protection);
        return this;
    }

    /**
     * Registers a backend-neutral repeating page header.
     *
     * @param header header options
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionChromeApi header(DocumentHeaderFooter header) {
        ensureOpen();
        chromeOptions.addHeader(header);
        return this;
    }

    /**
     * Registers a backend-neutral repeating page footer.
     *
     * @param footer footer options
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionChromeApi footer(DocumentHeaderFooter footer) {
        ensureOpen();
        chromeOptions.addFooter(footer);
        return this;
    }

    /**
     * Removes all previously registered headers and footers.
     *
     * @return this facade for chaining
     * @throws IllegalStateException if the owning session has already been closed
     */
    public SessionChromeApi clearHeadersAndFooters() {
        ensureOpen();
        chromeOptions.clearHeadersAndFooters();
        return this;
    }

    /**
     * Returns the owning {@link DocumentSession}, allowing the chrome
     * call chain to continue with authoring or rendering calls in a
     * single fluent expression.
     *
     * @return owning session
     */
    public DocumentSession session() {
        return session;
    }

    private void ensureOpen() {
        if (session.isClosed()) {
            throw new IllegalStateException("DocumentSession is already closed.");
        }
    }
}
