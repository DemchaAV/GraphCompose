package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TextPaginationIntegrationTest {

    private static final int TEXT_COMPONENTS_COUNT = 72;

    @Test
    void shouldFillThreePagesWithText() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("text_pagination_three_pages", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            var container = cb.vContainer(Align.middle(4))
                    .entityName("TextPaginationContainer")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10));

            for (int i = 0; i < TEXT_COMPONENTS_COUNT; i++) {
                container.addChild(cb.text()
                        .textWithAutoSize(repeatedText(i))
                        .textStyle(TextStyle.DEFAULT_STYLE)
                        .anchor(Anchor.center())
                        .margin(Margin.of(4))
                        .build());
            }

            container.build();
            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }
    }

    private String repeatedText(int index) {
        return "TextComponentTest TextComponentTest TextComponentTest #" + index;
    }
}

