package com.demcha.compose.document;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.nio.file.Path;

public class Test {

    @org.junit.jupiter.api.Test
    void test() throws Exception {
        try (DocumentSession document = GraphCompose.document(Path.of("output.pdf"))
                .margin(24, 24, 24, 24)
                .create()) {

            DocumentTextStyle body = DocumentTextStyle.builder()
                    .fontName(FontName.LATO)
                    .size(12)
                    .color(DocumentColor.DARK_GRAY)
                    .build();

            document.pageFlow(page -> page
                    .spacing(12)
                    .addSection("InfoCard", card -> card
                            .fillColor(DocumentColor.rgb(245, 248, 255))
//                            .stroke(DocumentStroke.of(DocumentColor.ROYAL_BLUE, 0.8))
                            .cornerRadius(12)
                            .padding(DocumentInsets.of(12))
                            .margin(DocumentInsets.bottom(10))
                            .addParagraph(paragraph -> paragraph
                                    .text("Это блочный текст внутри контейнера с заливкой.")
                                    .textStyle(body)
                                    .lineSpacing(2))));

            document.buildPdf();
        }
    }

}
