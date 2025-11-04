package com.demcha.system.word_system;

import com.demcha.components.core.Entity;
import com.demcha.system.Render;
import com.demcha.system.RenderingSystemECS;

import java.io.IOException;

public interface WordRender extends Render {
    boolean word(Entity e, RenderingSystemECS renderingSystem, boolean guideLines) throws IOException;
}
