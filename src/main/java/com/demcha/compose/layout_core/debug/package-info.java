/**
 * Debug-only layout snapshot APIs for inspecting resolved GraphCompose geometry.
 *
 * <p>This package contains immutable snapshot models and extraction utilities
 * that describe the document after layout and pagination have completed, but
 * before PDF rendering begins. The APIs in this package are intended for
 * regression testing, diagnostics, and developer tooling that need stable
 * geometry rather than rendered pixels.</p>
 *
 * <p>The package is intentionally isolated from the normal production PDF
 * pipeline. Standard rendering entry points such as {@code build()},
 * {@code toBytes()}, and {@code toPDDocument()} do not depend on snapshot
 * generation unless a caller explicitly invokes the debug snapshot API.</p>
 */
package com.demcha.compose.layout_core.debug;
