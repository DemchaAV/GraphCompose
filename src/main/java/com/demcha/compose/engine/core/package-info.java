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
 * and the dispatch contracts it drives — is dead code: nothing invokes it, in
 * production or in tests.</p>
 *
 * <p>The genuinely shared engine packages are elsewhere and are <em>not</em>
 * deprecated: {@code engine.components} (value types), {@code engine.measurement}
 * (text-measurement contracts), {@code engine.font}, and
 * {@code engine.render} (backend-neutral render-pass contracts) are all used by
 * the canonical pipeline.</p>
 *
 * @deprecated Legacy ECS engine, superseded by the canonical
 *     {@code com.demcha.compose.document.layout} pipeline. No public entry point
 *     runs it and it is not on the canonical hot path; it survives only because
 *     the live {@code Entity} object model and the shared render / pagination /
 *     debug helpers still compile against these primitives. It goes away when the
 *     internal {@code Entity} model is retired (see
 *     {@code docs/roadmaps/post-2.0-engineering.md}). Do not extend it or spend
 *     optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.core;
