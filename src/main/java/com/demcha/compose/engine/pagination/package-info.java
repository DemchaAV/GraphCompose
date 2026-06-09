/**
 * Pagination and page-placement helpers of the <strong>legacy ECS engine</strong>.
 *
 * <p>This package paginates the resolved {@code Entity} graph for the original
 * {@code Entity}-based pipeline ({@code PageBreaker} for the pagination walk,
 * {@code PageLayoutCalculator} for page-coordinate math, {@code TextBlockProcessor}
 * for block-text fragmentation, {@code ParentContainerUpdater} for upward
 * size/position propagation). It is renderer-neutral, but it is part of the
 * parallel legacy ECS engine: the canonical pipeline
 * ({@code com.demcha.compose.document.layout}) does its own pagination and
 * imports nothing here.</p>
 *
 * @deprecated Legacy ECS engine, superseded by the canonical
 *     {@code com.demcha.compose.document.layout} pipeline. Not reachable from
 *     any public API; retained only for legacy regression tests and a candidate
 *     for removal. Do not extend it or spend optimization effort here.
 */
@Deprecated
package com.demcha.compose.engine.pagination;
