package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticBackendProvider;

/**
 * Registers the Word export with the document API, so a session can reach it without
 * naming it.
 *
 * <p>The provider is listed in this artifact's
 * {@code META-INF/services/com.demcha.compose.document.backend.semantic.SemanticBackendProvider}
 * file, and answers {@link #format()} with {@code "docx"}:
 * {@link com.demcha.compose.document.backend.semantic.SemanticBackendProviders} finds it through
 * {@link java.util.ServiceLoader} and matches it by that format. So
 * {@code session.buildDocx(path)}, {@code writeDocx} and {@code toDocxBytes} export to Word once
 * the artifact is on the classpath, and the code that builds the document need not import
 * it.</p>
 *
 * <p>Each call to {@link #create()} returns a new {@link DocxSemanticBackend}, because a
 * backend holds the state of the export it is running. That backend is the no-argument one:
 * no report sink, and live dates in the package. For the {@link DocxExportReport} or
 * byte-identical output, build a backend with {@link DocxSemanticBackend#builder()} and pass it
 * to the session's {@code export(...)}.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}): it implements the semantic discovery seam
 * {@link SemanticBackendProvider}, which is Experimental in its first release — see
 * {@code docs/api-stability.md}.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.5.0
 */
@Beta
public final class DocxBackendProvider implements SemanticBackendProvider {

    /**
     * Creates the provider. Invoked by {@link java.util.ServiceLoader}.
     */
    public DocxBackendProvider() {
    }

    @Override
    public String format() {
        return "docx";
    }

    @Override
    public SemanticBackend<byte[]> create() {
        return new DocxSemanticBackend();
    }
}
