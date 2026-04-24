package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.engine.components.content.link.Email;
import com.demcha.compose.engine.components.content.link.LinkUrl;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.renderable.Link;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import com.demcha.compose.engine.render.RenderHandler;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.IOException;

public final class PdfLinkRenderHandler implements RenderHandler<Link, PdfRenderingSystemECS> {

    @Override
    public Class<Link> renderType() {
        return Link.class;
    }

    @Override
    public boolean render(EntityManager manager,
                          Entity entity,
                          Link renderComponent,
                          PdfRenderingSystemECS renderingSystem,
                          boolean guideLines) throws IOException {
        Placement placement = entity.getComponent(Placement.class).orElseThrow();
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        ContentSize size = entity.getComponent(ContentSize.class).orElseThrow();
        LinkUrl url = entity.getComponent(LinkUrl.class)
                .or(() -> entity.getComponent(Email.class))
                .orElseThrow();

        PDRectangle position = new PDRectangle();
        float x = (float) placement.x();
        float y = (float) placement.y();
        position.setLowerLeftX(x);
        position.setLowerLeftY(y);
        position.setUpperRightX(x + (float) (size.width() + padding.horizontal()));
        position.setUpperRightY(y + (float) (size.height() + padding.vertical()));

        int pageIndex = placement.startPage();
        renderingSystem.ensurePage(pageIndex);
        addLink(renderingSystem.doc().getPage(pageIndex), position, url);
        return true;
    }

    private void addLink(PDPage page, PDRectangle position, LinkUrl url) throws IOException {
        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(position);
        link.setDestination(null);

        PDActionURI action = new PDActionURI();
        action.setURI(url.getUrl());
        link.setAction(action);

        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(0);
        link.setBorderStyle(border);
        link.setHighlightMode(PDAnnotationLink.HIGHLIGHT_MODE_NONE);

        COSArray borderArray = new COSArray();
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        link.getCOSObject().setItem(COSName.BORDER, borderArray);

        page.getAnnotations().add(link);
    }
}
