/**
 * Pagination and page-placement helpers of the <strong>legacy ECS engine</strong>.
 *
 * <p>This package holds the {@code Entity}-based pagination helpers and markers
 * ({@code ParentContainerUpdater} for upward size/position propagation, the
 * {@code Breakable} marker, and {@code Offset}). It is renderer-neutral; the
 * canonical pipeline ({@code com.demcha.compose.document.layout}) does its own
 * pagination.</p>
 *
 * @deprecated Part of the legacy {@code Entity} engine, superseded by the
 *     canonical {@code com.demcha.compose.document.layout} pipeline; a candidate
 *     for removal once the {@code Entity} model is retired. Do not spend
 *     optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.pagination;
