package com.demcha.legacy;

import com.demcha.components.content.link.Link;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.layout.VAnchor;
import com.demcha.components.content.text.block.TextBlock;
import com.demcha.legacy.components.data.text.TextData;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.content.Element;
import com.demcha.components.layout.Align;
import com.demcha.legacy.layout.MeasureCtx;
import com.demcha.legacy.layout.ArrangeCtx;
import com.demcha.legacy.layout.layouts.VerticalLayout;
import com.demcha.legacy.render.PdfRenderContext;
import com.demcha.legacy.render.RenderEngine;
import com.demcha.legacy.scene.Page;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage pdfPage = new PDPage(PDRectangle.A4);
            doc.addPage(pdfPage);

            try (PdfRenderContext pdfCtx = new PdfRenderContext(doc, pdfPage)) {
                // 1) Страница как корневой контейнер
                Page page = new Page(pdfPage);
                page.setMarginTop(50);
                page.setMarginLeft(50);
                page.setMarginRight(50);
                page.setMarginBottom(60);

                // 2) Вертикальный лейаут: элементы идут столбиком
                page.setLayout(new VerticalLayout(8, new Align(HAnchor.CENTER, VAnchor.MIDDLE)));

                // 3) Контент
                Element paragraph = new Element().add(
                        TextBlock.of(
                                "Java backend developer with hands-on experience in Spring Boot, REST APIs, and PDF generation using PDFBox. " +
                                "Building a flexible layout engine with measure/arrange passes and component-based rendering.",
                                new TextStyle(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11, TextDecoration.DEFAULT, java.awt.Color.DARK_GRAY),
                                360
                        )
                );
                page.add(paragraph);

                List<Element> elements = getElements();
                elements.forEach(page::add);

                // 4) Движок: measure → arrange → render (централизованно)
                RenderEngine engine = new RenderEngine();

                engine.measure(page, new MeasureCtx(page.contentWidth(), page.contentHeight()));
                engine.arrange(page, new ArrangeCtx(
                        page.startX(),
                        page.startY(),
                        page.contentWidth(),
                        page.contentHeight()
                ));
                engine.render(page, pdfCtx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            doc.save("CV_Generated.pdf");
        }
    }

    private static List<Element> getElements() {
        List<Element> elements = new ArrayList<>();

        Element name = new Element()
                .add(new TextData(
                        "Artem Demchyshyn",
                        new TextStyle(
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                                32,
                                TextDecoration.BOLD,
                                java.awt.Color.BLACK
                        )
                ));
        elements.add(name);

        Element github = new Element()
                .add(new TextData(
                        "GitHub",
                        new TextStyle(
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                                10,
                                TextDecoration.UNDERLINE,
                                java.awt.Color.BLUE
                        )
                ))
                .add(new Link("GitHub", "https://github.com/DemchaAV"));
        elements.add(github);

        Element address = new Element()
                .add(new TextData(
                        "TW8, London, UK",
                        new TextStyle(
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                                10,
                                TextDecoration.DEFAULT,
                                java.awt.Color.DARK_GRAY
                        )
                ));
        elements.add(address);

        Element email = new Element()
                .add(new TextData(
                        "Email",
                        new TextStyle(
                                new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                                10,
                                TextDecoration.UNDERLINE,
                                java.awt.Color.BLUE
                        )
                ))
                .add(new Link("Email", "mailto:DemchaAV@gmail.com"));
        elements.add(email);

        return elements;
    }
}
