package com.demcha.loyaut_core.system.implemented_systems.word_sustems;

import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.system.interfaces.Render;
import com.demcha.loyaut_core.system.interfaces.RenderingSystemECS;

import java.io.IOException;

public interface WordRender extends Render {
    boolean word(Entity e, RenderingSystemECS renderingSystem, boolean guideLines) throws IOException;
}
