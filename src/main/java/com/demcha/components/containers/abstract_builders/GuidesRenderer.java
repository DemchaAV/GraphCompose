package com.demcha.components.containers.abstract_builders;

import com.demcha.components.core.Entity;
import com.demcha.exeptions.RenderGuideLinesException;
import com.demcha.system.RenderingSystemECS;

import java.io.IOException;
import java.util.EnumSet;

public interface GuidesRenderer {


    default <T extends RenderingSystemECS> boolean renderGuides(Entity entity, EnumSet<Guide> guides) throws IOException {
        boolean any = false;

        if (guides.contains(Guide.MARGIN)) any |= renderMargin(entity);
        if (guides.contains(Guide.PADDING)) any |= renderPadding(entity);
        if (guides.contains(Guide.BOX)) any |= boxRender(entity);
        return any;
    }



    <T extends RenderingSystemECS> boolean boxRender(Entity e) throws RenderGuideLinesException;

    <T extends RenderingSystemECS> boolean renderPadding(Entity e) throws RenderGuideLinesException;

    <T extends RenderingSystemECS> boolean renderMargin(Entity e) throws RenderGuideLinesException;


    enum Guide {MARGIN, PADDING, BOX}
}
