package com.demcha.compose.engine.render;

import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.Render;
import com.demcha.compose.engine.render.RenderingSystemECS;

import java.io.IOException;

/**
 * Backend-owned renderer for one render marker type.
 */
public interface RenderHandler<R extends Render, RS extends RenderingSystemECS<?>> {

    Class<R> renderType();

    boolean render(EntityManager manager,
                   Entity entity,
                   R renderComponent,
                   RS renderingSystem,
                   boolean guideLines) throws IOException;
}
