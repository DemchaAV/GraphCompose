package com.demcha.system.implemented_systems.word_sustems;

import com.demcha.components.core.Entity;
import com.demcha.system.intarfaces.Render;
import com.demcha.system.intarfaces.RenderingSystemECS;

import java.io.IOException;

public interface WordRender extends Render {
    boolean word(Entity e, RenderingSystemECS renderingSystem, boolean guideLines) throws IOException;
}
