package com.demcha.Templatese;

import com.demcha.Pdf_FontLoader;
import com.demcha.components.components_builders.TextBuilder;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.Anchor;
import com.demcha.core.EntityManager;
import com.demcha.system.LayoutSystem;
import com.demcha.system.PdfFileManagerSystem;
import com.demcha.system.PdfRenderingSystem;
import lombok.AllArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
@AllArgsConstructor
public class CV_1 {
    private EntityManager entityManager;

    public Entity name(String name) {
        Entity nameEntity = new TextBuilder(entityManager).create()
                .textWithAutoSize(name)
                .anchor(Anchor.centerTop())
                .textStyle(TextStyle.builder()
                        .size(35)
                        .color(new Color(44, 62, 80))
                        .font(Pdf_FontLoader
                                .loadFont("C:\\Users\\Demch\\Downloads\\OpenPDF-2.0.1\\OpenPDF-2.0.1\\pdf-toolbox\\src" +
                                          "\\test\\resources\\com\\lowagie\\examples\\fonts\\noto\\NotoSansMono-Regular.ttf"))
//                        .font(TextStyle.HELVETICA)
                        .build())

                .build();
        return nameEntity;
    }
}
class test{
    public static void main(String[] args) {
        Path target = Paths.get("new_test_file.pdf");

        EntityManager entityManager = new EntityManager();
        entityManager.setGuideLines(true);
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage(PDRectangle.A4));


        entityManager.addSystem(new LayoutSystem(doc.getPage(0)));
        entityManager.addSystem(new PdfRenderingSystem(doc));
        entityManager.addSystem(new PdfFileManagerSystem(target, doc));

        CV_1 cv = new CV_1(entityManager);
        cv.name("Artem Demchyshyn");
        entityManager.processSystems();
    }
}
