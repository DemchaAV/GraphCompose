package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.system.interfaces.Render;

/**
 * Render marker for atomic table rows.
 * <p>
 * Backend-specific row drawing lives in renderer-side handlers so table layout
 * stays reusable across output formats.
 * </p>
 */
public class TableRow implements Render {
}
