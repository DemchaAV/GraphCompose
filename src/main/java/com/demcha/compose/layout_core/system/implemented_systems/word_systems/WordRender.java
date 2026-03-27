package com.demcha.compose.layout_core.system.implemented_systems.word_systems;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;

import java.io.IOException;

public interface WordRender extends Render {
    boolean word(Entity e, RenderingSystemECS renderingSystem, boolean guideLines) throws IOException;
}
