/**
 * Public authoring session API for semantic GraphCompose documents.
 *
 * <p>Classes in this package own the mutable authoring lifecycle: configuration,
 * root registration, layout compilation, snapshot extraction, and backend
 * rendering/export entrypoints.</p>
 *
 * <h2>Thread safety</h2>
 *
 * <p>{@link com.demcha.compose.document.api.DocumentSession} is mutable and
 * <strong>not thread-safe</strong>: a single session must be authored from
 * one thread at a time. This is the only mutable type in the public API.</p>
 *
 * <p>Backends ({@code PdfFixedLayoutBackend}, {@code DocxSemanticBackend},
 * {@code PptxSemanticBackend}) are immutable after construction and may be
 * shared across threads &mdash; each {@code render(...)} or {@code export(...)}
 * call constructs its own per-pass state.</p>
 *
 * <p>{@link com.demcha.compose.font.FontLibrary} and the static
 * image/font caches are backed by concurrent collections and are safe to
 * share across sessions and threads.</p>
 *
 * <h2>Stability contract</h2>
 *
 * <p>Types in this package and in {@link com.demcha.compose.document.dsl},
 * {@link com.demcha.compose.document.node},
 * {@link com.demcha.compose.document.style},
 * {@link com.demcha.compose.document.theme}, and
 * {@link com.demcha.compose.document.templates.api} are public API and follow
 * deprecation discipline. Types annotated with
 * {@link com.demcha.compose.document.api.Internal &#64;Internal}
 * (notably the {@code document.layout} package) are implementation detail
 * and may change without notice.</p>
 */
package com.demcha.compose.document.api;
