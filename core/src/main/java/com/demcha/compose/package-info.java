/**
 * Top-level public entrypoint package for GraphCompose.
 *
 * <p>Ownership: this package should stay intentionally small. It exposes
 * {@code GraphCompose.document(...)} as the canonical way for applications to
 * create {@code DocumentSession} instances.</p>
 * <p>Extension rules: add new authoring concepts under {@code document.*};
 * add engine internals under {@code engine.*}; avoid placing feature-specific
 * implementation code in this root package.</p>
 */
package com.demcha.compose;
