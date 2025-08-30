package com.demcha;

import com.demcha.components.components_builders.*;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.layout.Anchor;
import com.demcha.core.EntityManager;
import com.demcha.system.LayoutSystem;
import com.demcha.system.PdfFileManagerSystem;
import com.demcha.system.PdfRenderingSystem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Path target = Paths.get("output.pdf");

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(false);
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));


        entityManager.addSystem(new LayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystem(doc));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));



        var link = new LinkBuilder(entityManager).create()
                .linkUrl(new LinkUrl("https://www.google.com/"))
                .anchor(Anchor.center())
                .displayText(new DisplayUrlTextBuilder(entityManager).create()
                        .textWithAutoSize("Email")
                )
                .build();


        entityManager.processSystems();


    }


}


