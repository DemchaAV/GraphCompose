package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentOutputOptions;

/**
 * Two ServiceLoader fakes that both declare format {@code "dup"} — the shape a
 * classpath takes when a third-party plugin ships a backend for a format the
 * official artifact already provides. Selection must refuse the ambiguity
 * rather than let enumeration order pick the renderer.
 *
 * <p>The format is deliberately not {@code "aaa"} or {@code "zzz"}: those drive
 * {@link BackendProvidersSelectionTest}, and {@code "aaa"} sorts before
 * {@code "dup"}, so the default-selection contract there is unaffected.</p>
 */
final class DuplicateFormatFakeProviders {

    static final String FORMAT = "dup";

    private DuplicateFormatFakeProviders() {
    }

    /** First registered provider for {@link #FORMAT}. */
    public static final class One implements FixedLayoutBackendProvider {

        @Override
        public String format() {
            return FORMAT;
        }

        @Override
        public FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug) {
            throw new UnsupportedOperationException("selection-test fake; never renders");
        }
    }

    /** Second registered provider for {@link #FORMAT}. */
    public static final class Two implements FixedLayoutBackendProvider {

        @Override
        public String format() {
            return FORMAT;
        }

        @Override
        public FixedLayoutRenderer create(DocumentOutputOptions chrome, DocumentDebugOptions debug) {
            throw new UnsupportedOperationException("selection-test fake; never renders");
        }
    }
}
