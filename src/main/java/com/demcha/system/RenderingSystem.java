package com.demcha.system;

import com.demcha.components.content.Text;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.RectangleComponent;
import com.demcha.components.layout.Position;
import com.demcha.core.PdfDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.*;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class RenderingSystem implements System {
    public static void main(String[] args) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 100, y = 600, w = 200, h = 100; // origin is bottom-left
                cs.setStrokingColor(Color.BLACK);
                cs.setLineWidth(2f);

                cs.addRect(x, y, w, h); // define path
                cs.stroke();            // draw the border
            }

            doc.save("rectangle.pdf");
        }
    }

    @Override
    public void process(PdfDocument pdfDocument) {
        PDDocument pdf = new PDDocument();
        PDPage page = new PDPage();
        pdf.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {

            // Получаем все сущности, у которых есть PositionComponent
            // (Предполагаем, что в PdfDocument есть такой метод)
            for (UUID entityId : pdfDocument.getEntitiesWithComponent(Position.class)) {
                var entity = pdfDocument.getEntity(entityId);
                Optional<Position> position = Optional.empty();
                Optional<Text> text = Optional.empty();

                if (entity.isPresent()) {
                    Entity e = entity.get();
                    position = e.getComponent(Position.class);
                    text = e.getComponent(Text.class);
                }else {
                    log.error("entity {} not found", entityId);
                }


                // Проверяем, есть ли у сущности TextComponent
                if (text.isPresent()) {
                    contentStream.newLineAtOffset((float) position.get().x(), (float) position.get().y());
                    contentStream.showText(text.get().text());
                }else {
                    log.debug("Component {} not found", Text.class.getName());
                }

                // Здесь можно добавить логику для отрисовки других компонентов (Image, Box и т.д.)
            }

            contentStream.endText();
            pdf.save("output.pdf");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                pdf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void textRendering(Text textComponent) {
        //TODO Logic for renderint text class Text.class
    }

    private void addRect(RectangleComponent rectangle) {
        //TODO

    }
}

