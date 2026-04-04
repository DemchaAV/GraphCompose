package com.demcha.compose.layout_core.system.implemented_systems.word_systems;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;

import java.io.IOException;

/**
 * Legacy Word render contract kept as a migration fallback.
 * <p>
 * New backend implementations should prefer renderer-side handlers rather than
 * embedding format-specific drawing logic inside engine render markers.
 * </p>
 */
public interface WordRender extends Render {
    boolean word(Entity e, RenderingSystemECS renderingSystem, boolean guideLines) throws IOException;
}
