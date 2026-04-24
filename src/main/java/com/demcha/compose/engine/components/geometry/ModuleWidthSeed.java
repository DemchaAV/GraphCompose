package com.demcha.compose.engine.components.geometry;

import com.demcha.compose.engine.components.core.Component;

/**
 * Root-level width hint for semantic modules.
 * <p>
 * Module layout resolves its final content width from the available width of the
 * parent container whenever a parent exists. For root modules there is no parent
 * inner box to inherit from, so builders may attach this seed to describe the
 * available width that the module should occupy before its own horizontal margin
 * is subtracted.
 * </p>
 */
public record ModuleWidthSeed(double width) implements Component {
}
