package com.demcha.compose.layout_core.system.utils.page_breaker;

/**
 * Marker for renderables whose content may continue across page boundaries.
 *
 * <p>This marker is used by the page breaker. A breakable entity may start on one page
 * and continue on the next page instead of being moved as a single indivisible block.</p>
 *
 * <p>It does <strong>not</strong> mean that the entity should resize to fit children.
 * Parent expansion is handled separately by {@code Expendable}.</p>
 */
public interface Breakable {
}
