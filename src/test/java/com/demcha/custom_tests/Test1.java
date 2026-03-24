package com.demcha.custom_tests;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class Test1 {
    private final String pathOut = "C:\\Users\\Demch\\OneDrive\\Java\\PDF_CV_CREATOR\\target\\visual-tests";
    @Test
    void testComposeBuilder() {

        var path = Path.of(pathOut, "test1.pdf");


        try (var composer = GraphCompose.pdf(path)
                .pageSize(PDRectangle.A4)
                .guideLines(true)
                .create()) {

            composer.componentBuilder()
                    .text()
                    .textWithAutoSize("In-memory PDF")
                    .margin(Margin.of(5))
                    .padding(Padding.of(5))
                    .textStyle(TextStyle.DEFAULT_STYLE)
                    .anchor(Anchor.topLeft())
                    .build();

            composer.build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
