package com.demcha.compose.engine.components.geometry;


/**
 * Marker for parent-like entities whose own {@code ContentSize} may grow to fit children.
 *
 * <p>This marker is consumed by the container expansion phase, not by the page breaker.
 * It should be used for entities that own child entities and are expected to expand their
 * box when children require more space.</p>
 *
 * <p>It does <strong>not</strong> mean that the entity itself can flow across pages.
 * Pagination behavior is controlled separately by {@code Breakable}.</p>
 *
 * <p>The name is kept as-is for compatibility with the current codebase, but conceptually
 * it means {@code expandable-by-children}.</p>
 */
public interface Expendable {
}
