package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.layout.coordinator.PaddingCoordinate;
import com.demcha.components.style.Padding;
import com.demcha.system.PdfRender;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.io.IOException;

public class Link implements PdfRender, GuidesRenderer {


    @Override
    public boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc,int indexPage, boolean guideLine) throws IOException {

        // прямоугольник поверх текста
        PaddingCoordinate coordinate = PaddingCoordinate.from(e);
        Padding padding = e.getComponent(Padding.class).orElse(Padding.zero());
        ContentSize size = e.getComponent(ContentSize.class).orElseThrow();
        LinkUrl url = e.getComponent(LinkUrl.class).orElseThrow();
        PDRectangle position = new PDRectangle();

        float x = (float) coordinate.x();
        float y = (float) coordinate.y();

        position.setLowerLeftX(x);
        position.setLowerLeftY(y);
        position.setUpperRightX(x + (float) (size.width() + padding.horizontal()));
        position.setUpperRightY(y + (float) (size.height() + padding.vertical()));

        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(position);

        PDActionURI action = new PDActionURI();
        action.setURI(url.toString());

        PDPage page = doc.getPage(indexPage);

        page.getAnnotations().add(link);

        return true;
    }
}

