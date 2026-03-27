package com.demcha.compose.layout_core.components.renderable;

import com.demcha.compose.layout_core.components.content.link.Email;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRender;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.IOException;

public class Link implements PdfRender {


    private void addLink(PdfRenderingSystemECS renderingSystemECS, int indexPage, PDRectangle position, LinkUrl url) throws IOException {
        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(position);
        link.setDestination(null);

        PDActionURI action = new PDActionURI();
        action.setURI(url.getUrl());
        link.setAction(action);

// УБИРАЕМ РАМКУ
        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(0);
        link.setBorderStyle(border);
        link.setHighlightMode(PDAnnotationLink.HIGHLIGHT_MODE_NONE);

//  обнуляем /Border
        COSArray borderArray = new COSArray();
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        borderArray.add(COSInteger.ZERO);
        link.getCOSObject().setItem(COSName.BORDER, borderArray);


// добавляем на страницу
        PDDocument doc = renderingSystemECS.doc();
        PDPage page = doc.getPage(indexPage);
        page.getAnnotations().add(link);
    }

    @Override
    public boolean pdf(EntityManager entityManager, Entity e, PdfRenderingSystemECS renderingSystemECS, boolean guideLines) throws IOException {

        // прямоугольник поверх текста
        var renderingPosition = e.getComponent(Placement.class).orElseThrow();
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        ContentSize size = e.getComponent(ContentSize.class).orElseThrow();
        var url = e.getComponent(LinkUrl.class)
                .or(() -> e.getComponent(Email.class))
                .orElseThrow();
        PDRectangle position = new PDRectangle();

        float x = (float) renderingPosition.x();
        float y = (float) renderingPosition.y();

        position.setLowerLeftX(x);
        position.setLowerLeftY(y);
        position.setUpperRightX(x + (float) (size.width() + padding.horizontal()));
        position.setUpperRightY(y + (float) (size.height() + padding.vertical()));

        int indexPage = e.getComponent(Placement.class).orElseThrow().startPage();
        addLink(renderingSystemECS, indexPage, position, url);

        return true;
    }
}

