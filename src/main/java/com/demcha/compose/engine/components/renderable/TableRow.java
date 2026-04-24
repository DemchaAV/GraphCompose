package com.demcha.compose.engine.components.renderable;

import com.demcha.compose.engine.render.Render;

/**
 * Render marker for atomic table rows.
 * <p>
 * Backend-specific row drawing lives in renderer-side handlers so table layout
 * stays reusable across output formats.
 * </p>
 */
public class TableRow implements Render {
}
