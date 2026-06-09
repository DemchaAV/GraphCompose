/**
 * Core primitives of the <strong>legacy ECS engine</strong> — entity/component
 * graphs and shared traversal state for the original {@code Entity}-based
 * layout / pagination / render pipeline.
 *
 * <p>This is <em>not</em> the engine behind the public API. The canonical
 * pipeline is {@code GraphCompose.document() -> DocumentSession ->
 * LayoutCompiler -> LayoutGraph -> PdfFixedLayoutBackend} in
 * {@code com.demcha.compose.document.*}, and it imports nothing from this
 * package. The ECS stack ({@code engine.core}, {@code engine.layout},
 * {@code engine.pagination}) is a parallel second engine that no public entry
 * point reaches — the former {@code GraphCompose.pdf(...)} surface has been
 * removed — and it survives only to back the legacy engine regression tests.</p>
 *
 * <p>The genuinely shared engine packages are elsewhere and are <em>not</em>
 * deprecated: {@code engine.components} (value types), {@code engine.measurement}
 * (text-measurement contracts), {@code engine.font}, and
 * {@code engine.render} (backend-neutral render-pass contracts) are all used by
 * the canonical pipeline.</p>
 *
 * @deprecated Legacy ECS engine, superseded by the canonical
 *     {@code com.demcha.compose.document.layout} pipeline. No public API reaches
 *     it, it is not on the canonical hot path, and it is retained only for the
 *     legacy engine regression tests — a candidate for removal once those are
 *     retired. Do not extend it or spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.core;
