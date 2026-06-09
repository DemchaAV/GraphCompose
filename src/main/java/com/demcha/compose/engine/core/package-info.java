/**
 * Core primitives of the <strong>legacy ECS engine</strong> — entity/component
 * graphs and shared traversal state for the original {@code Entity}-based
 * layout / pagination / render pipeline.
 *
 * <p>This is <em>not</em> the engine behind the public API. The canonical
 * pipeline ({@code GraphCompose.document() -> DocumentSession -> LayoutCompiler
 * -> LayoutGraph -> PdfFixedLayoutBackend}) in {@code com.demcha.compose.document.*}
 * imports nothing from this package directly, and the former
 * {@code GraphCompose.pdf(...)} surface that drove the ECS has been removed. The
 * ECS <em>execution</em> engine — the {@code EntityManager.processSystems()} loop
 * and the layout / pagination / render systems it drives — is dead: it runs only
 * under the legacy engine regression tests.</p>
 *
 * <p>The genuinely shared engine packages are elsewhere and are <em>not</em>
 * deprecated: {@code engine.components} (value types), {@code engine.measurement}
 * (text-measurement contracts), {@code engine.font}, and
 * {@code engine.render} (backend-neutral render-pass contracts) are all used by
 * the canonical pipeline.</p>
 *
 * @deprecated Legacy ECS engine, superseded by the canonical
 *     {@code com.demcha.compose.document.layout} pipeline. No public entry point
 *     runs it and it is not on the canonical hot path; it is retained only for the
 *     legacy engine regression tests — a candidate for removal. Do not extend it or
 *     spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.core;
