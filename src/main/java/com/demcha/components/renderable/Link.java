package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.components.style.Padding;
import com.demcha.system.PdfRender;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.IOException;

public class Link implements PdfRender, GuidesRenderer {


    private void addLink(PDDocument doc, int indexPage, PDRectangle position, LinkUrl url) throws IOException {
        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(position);
        link.setDestination(null);

        PDActionURI action = new PDActionURI();
        action.setURI(url.url());
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
        PDPage page = doc.getPage(indexPage);
        page.getAnnotations().add(link);
    }

    @Override
    public boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc, int indexPage, boolean guideLine) throws IOException {

        // прямоугольник поверх текста
        System.out.println(e);
        var renderingPosition = RenderingPosition.from(e).orElseThrow();
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        ContentSize size = e.getComponent(ContentSize.class).orElseThrow();
        LinkUrl url = e.getComponent(LinkUrl.class).orElseThrow();
        PDRectangle position = new PDRectangle();

        float x = (float) renderingPosition.x();
        float y = (float) renderingPosition.y();

        position.setLowerLeftX(x);
        position.setLowerLeftY(y);
        position.setUpperRightX(x + (float) (size.width() + padding.horizontal()));
        position.setUpperRightY(y + (float) (size.height() + padding.vertical()));

        addLink(doc, indexPage, position, url);


        return true;
    }
}

