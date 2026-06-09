/**
 * Layout systems of the <strong>legacy ECS engine</strong> — they turn
 * low-level {@code Entity} graphs into resolved geometry for the original
 * {@code Entity}-based pipeline.
 *
 * <p>Despite the former "shared" framing, the canonical pipeline
 * ({@code com.demcha.compose.document.layout.LayoutCompiler}) imports nothing
 * from this package. It is part of the parallel legacy ECS engine reachable
 * only from the legacy engine regression tests.</p>
 *
 * @deprecated Legacy ECS engine, superseded by the canonical
 *     {@code com.demcha.compose.document.layout} pipeline. Not reachable from
 *     any public API; retained only for legacy regression tests and a candidate
 *     for removal. Do not extend it or spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.layout;
