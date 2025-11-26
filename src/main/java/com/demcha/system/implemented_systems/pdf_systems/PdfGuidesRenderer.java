package com.demcha.system.implemented_systems.pdf_systems;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.RenderCoordinate;
import com.demcha.components.layout.coordinator.Placement;
import com.demcha.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.exceptions.RenderGuideLinesException;
import com.demcha.system.GuidLineSettings;
import com.demcha.system.interfaces.guides.BoxRender;
import com.demcha.system.interfaces.guides.GuidesRenderer;
import com.demcha.system.interfaces.guides.MarginRender;
import com.demcha.system.interfaces.guides.PaddingRender;
import com.demcha.system.interfaces.guides.impl.BoxRenderImpl;
import com.demcha.system.interfaces.guides.impl.MarginRenderImpl;
import com.demcha.system.interfaces.guides.impl.PaddingRenderImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Getter
@Accessors(fluent = true)
public class PdfGuidesRenderer extends GuidesRenderer<PDPageContentStream> {


    PdfGuidesRenderer(PdfRenderingSystemECS renderingSystem) {
        super(renderingSystem, new BoxRenderImpl<>(renderingSystem), new MarginRenderImpl<>(renderingSystem), new PaddingRenderImpl<>(renderingSystem));
    }

    public static RenderGuideLinesException rethrowAsGuideLinesException(IOException io, String message) throws RenderGuideLinesException {
        return new RenderGuideLinesException(message, io);
    }

    public <T extends Component> boolean guidesRender(Entity entity, EnumSet<Guide> defaultGuides, T component) throws RenderGuideLinesException {
        var placement = entity.getComponent(Placement.class).orElseThrow();
        try (PDPageContentStream pdPageContentStream = renderingSystem.stream().openContentStream(entity)) {
            return guidesRender(entity, pdPageContentStream, defaultGuides);

        } catch (IOException e) {
            throw rethrowAsGuideLinesException(e, "Error opening content stream for Guides render component :" + component.getClass().getSimpleName());
        }

    }




    public boolean guidesRender(Entity e, PDPageContentStream cs, EnumSet<Guide> guides) throws RenderGuideLinesException {
        boolean any = false;

        if (guides.contains(Guide.MARGIN)) any |= margin().fromStream(e, cs);
        if (guides.contains(Guide.PADDING)) any |= padding().fromStream(e, cs);
        if (guides.contains(Guide.BOX)) any |= box().fromStream(e, cs);
        return any;
    }



}
