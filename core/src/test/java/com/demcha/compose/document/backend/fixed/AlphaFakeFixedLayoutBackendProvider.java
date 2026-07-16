package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentOutputOptions;

/**
 * ServiceLoader fake with format {@code "aaa"}. Selection tests need several
 * registered providers on the backend-free core test classpath; rendering is
 * never exercised.
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
