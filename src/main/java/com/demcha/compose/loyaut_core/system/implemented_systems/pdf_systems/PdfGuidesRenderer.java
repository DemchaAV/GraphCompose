package com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems;

import com.demcha.compose.loyaut_core.system.interfaces.guides.GuidesRenderer;
import com.demcha.compose.loyaut_core.system.interfaces.guides.impl.BoxRenderImpl;
import com.demcha.compose.loyaut_core.system.interfaces.guides.impl.MarginRenderImpl;
import com.demcha.compose.loyaut_core.system.interfaces.guides.impl.PaddingRenderImpl;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

@Slf4j
@Getter
@Accessors(fluent = true)
public class PdfGuidesRenderer extends GuidesRenderer<PDPageContentStream> {

    public PdfGuidesRenderer(PdfRenderingSystemECS renderingSystem) {
        super(renderingSystem, new BoxRenderImpl<>(renderingSystem), new MarginRenderImpl<>(renderingSystem), new PaddingRenderImpl<>(renderingSystem));
    }

}
