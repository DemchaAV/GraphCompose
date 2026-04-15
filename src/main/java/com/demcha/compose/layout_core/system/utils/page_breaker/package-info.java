/**
 * Pagination and page-placement helpers for resolved GraphCompose entities.
 *
 * <p>This package owns the logic that runs after layout has produced
 * {@code ComputedPosition} and size metadata but before the renderer writes the
 * final document. Its responsibilities include:</p>
 *
 * <ul>
 *   <li>choosing a child-first pagination order for the resolved hierarchy</li>
 *   <li>mapping resolved Y coordinates into page-relative {@code Placement}</li>
 *   <li>splitting breakable content such as block text across pages</li>
 *   <li>propagating child-driven size and page-shift updates into parent containers</li>
 * </ul>
 *
 * <p>The package is intentionally renderer-neutral. It does not open PDF
 * streams, create pages directly, or decide backend drawing policy. Instead it
 * mutates the entity graph so later renderers can consume one deterministic,
 * page-aware layout result.</p>
 *
 * <p>Key classes in this package are {@code PageBreaker} for the pass-level
 * pagination walk, {@code PageLayoutCalculator} for page-coordinate math,
 * {@code TextBlockProcessor} for block-text fragmentation, and
 * {@code ParentContainerUpdater} for upward size/position propagation.</p>
 */
package com.demcha.compose.layout_core.system.utils.page_breaker;
