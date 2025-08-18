package com.demcha.legacy;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;

public class PdfCreateExample {
    public static void main(String[] args) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            System.out.println("Meta Data is: "+ page.getMediaBox().getHeight() + " " +  page.getMediaBox().getWidth());
            doc.addPage(page);

            PDFont font = new PDType1Font(Standard14Fonts.FontName.COURIER);


            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset(50, 800);
                cs.showText("Hello from PDFBox 3.0.5");
                cs.showText(" Artem Demchyshyn");
                cs.endText();
            }

            doc.save("Test.pdf");
        }
    }
}
