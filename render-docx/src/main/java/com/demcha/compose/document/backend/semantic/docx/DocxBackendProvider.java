package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticBackendProvider;

/**
 * Registers the Word export with the document API, so a session can reach it without
 * naming it.
 *
 * <p>Before this, using the export meant constructing {@code DocxSemanticBackend} — which
 * means importing this artifact in the code that builds the document, and carrying that
 * dependency wherever the document is built. A render backend has not needed that since
 * 2.0; now neither does this one: put the artifact on the classpath and
 * {@code session.buildDocx(path)} finds it.</p>
 *
 * <p>A backend is created per export rather than shared, because it holds the state of the
 * export it is running.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.5.0
 */
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
