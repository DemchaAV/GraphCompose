package com.demcha.components.renderable;

import com.demcha.components.containers.abstract_builders.GuidesRenderer;
import com.demcha.components.core.Entity;
import com.demcha.system.PdfRender;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.io.IOException;

public class Link implements PdfRender, GuidesRenderer {


    @Override
    public boolean pdfRender(Entity e, PDPageContentStream cs, PDDocument doc, boolean guideLine) throws IOException {

//        // прямоугольник поверх текста
//        PDRectangle position = new PDRectangle();
//        position.setLowerLeftX(x);
//        position.setLowerLeftY(y - 2); // немного ниже baseline
//        position.setUpperRightX(x + textWidth);
//        position.setUpperRightY(y + textHeight);
//
//        PDAnnotationLink link = new PDAnnotationLink();
//        link.setRectangle(position);
//
//        PDActionURI action = new PDActionURI();
//        action.setURI("https://www.google.com");
//



        return false;
    }
}

