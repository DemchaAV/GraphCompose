package com.demcha.system.pdf_systems;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.core.Entity;
import com.demcha.system.Render;
import com.demcha.system.RenderingSystemECS;

import java.util.EnumSet;

/**
 * This is abstract class for rendering  component
 */

public interface SystemRender extends  GuidesRenderer {

    /**
     * Rendering current Entity with flag guideLines witch will run method renderGuides
     * @param entity Owner Entity of Component who implement a interface @Render.class
     * @param renderingSystem
     * @param guideLines
     * @param <T>
     */
    <T extends RenderingSystemECS>  void draw(Entity entity, T renderingSystem, boolean guideLines);

    <T extends RenderingSystemECS>  boolean renderGuides(Entity entity, T renderingSystem, EnumSet<GuidesRenderer.Guide> defaultGuides);


}
       