package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentOutputOptions;

/**
 * ServiceLoader fake with format {@code "aaa"}. It coexists with the real PDF
 * provider on the qa classpath so the default-selection test proves the
 * no-arg {@link BackendProviders#fixedLayout()} prefers PDF over both
 * classpath enumeration order and lexicographic order; rendering is never
 * exercised.
 */
public final class AlphaFakeFixedLayoutBackendProvider implements FixedLayoutBackendProvider {

    @Override
    public String format() {
        return "aaa";
    }

    @Override
    public FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug) {
        throw new UnsupportedOperationException("selection-test fake; never renders");
    }
}
