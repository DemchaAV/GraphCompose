package com.demcha.compose.layout_core.system.rendering;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.Render;
import com.demcha.compose.layout_core.system.interfaces.RenderingSystemECS;

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
