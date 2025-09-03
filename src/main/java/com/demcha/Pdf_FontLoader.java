package com.demcha;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

@Slf4j
public class Pdf_FontLoader {
    public static PDType0Font loadFont(String path) {
        try (PDDocument doc = new PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);

            // Подключаем шрифт из файла
            var font = PDType0Font.load(doc, new File(path));

            return font;
        } catch (IOException e) {
            log.error(e.getMessage(), path);
            throw new RuntimeException(e);
        }
    }
}
