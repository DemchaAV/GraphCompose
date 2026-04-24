package com.demcha.compose.engine.render.pdf;

import com.demcha.compose.engine.render.guides.GuidesRenderer;
import com.demcha.compose.engine.render.guides.impl.BoxRenderImpl;
import com.demcha.compose.engine.render.guides.impl.MarginRenderImpl;
import com.demcha.compose.engine.render.guides.impl.PaddingRenderImpl;
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
