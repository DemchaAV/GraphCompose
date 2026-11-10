package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.api.Beta;

/**
 * Service-provider that supplies a semantic export backend to the document API's
 * convenience output methods, without the caller naming a concrete backend type.
 *
 * <p>The fixed-layout half of this has existed since 2.0: a render backend artifact
 * registers a {@code FixedLayoutBackendProvider} and the session's {@code buildPdf} finds
 * it. A semantic backend had no such path — the only way to reach one was to construct it,
 * which means naming its artifact in code and carrying the dependency everywhere the
 * document is built. This is the missing half, and deliberately no more than that: the
 * fixed-layout locator keeps its own contracts and is not reorganised around this one.</p>
 *
 * <p>Implementations are discovered through {@link java.util.ServiceLoader} and resolved by
 * {@link SemanticBackendProviders#forFormat(String)}, which matches {@link #format()}
 * case-insensitively and refuses a classpath carrying two providers for one format.</p>
 *
 * <p><b>Experimental</b> ({@code @Beta}) — see {@code docs/api-stability.md}.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.5.0
 */
@Beta
public interface SemanticBackendProvider {

    /**
     * The output format this provider exports, as the selection key.
     *
     * <p>Matched case-insensitively, so a provider may spell it however reads best.</p>
     *
     * @return a stable format identifier such as {@code "docx"}
     */
    String format();

    /**
     * Creates a backend for one export.
     *
     * <p>Called once per export rather than cached, because a semantic backend holds the
     * state of the export it is running. A provider that returned a shared instance would
     * make two concurrent exports write into each other.</p>
     *
     * @return a configured backend producing the format's bytes
     */
    SemanticBackend<byte[]> create();
}
