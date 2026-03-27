package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import com.demcha.compose.font_library.FontName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for table layout rendering.
 * Tests a 2-column × 5-row table with centering and guide lines.
 */
class TableLayoutIntegrationTest {

    private static final Path VISUAL_DIR = Path.of("target", "visual-tests");

    @Test
    void shouldRenderTableWith2ColumnsAnd5Rows() throws Exception {
        Path outputFile = VISUAL_DIR .resolve("table_layout_test.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            Entity row1 = createTableRow(cb, "Row 1 Col 1", "Row 1 Col 2");
            Entity row2 = createTableRow(cb, "Row 2 Col 1", "Row 2 Col 2");
            Entity row3 = createTableRow(cb, "Row 3 Col 1", "Row 3 Col 2");
            Entity row4 = createTableRow(cb, "Row 4 Col 1", "Row 4 Col 2");
            Entity row5 = createTableRow(cb, "Row 5 Col 1", "Row 5 Col 2");

            cb.vContainer(Align.middle(5))
                    .entityName("Table")
                    .anchor(Anchor.center())
                    .padding(Padding.of(10))
                    .margin(Margin.of(10))
                    .addChild(row1)
                    .addChild(row2)
                    .addChild(row3)
                    .addChild(row4)
                    .addChild(row5)
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }

    private Entity createTableRow(ComponentBuilder cb, String col1Text, String col2Text) {
        Entity col1 = cb.text()
                .textWithAutoSize(col1Text)
                .textStyle(TextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(12)
                        .color(ComponentColor.BLACK)
                        .build())
                .anchor(Anchor.center())
                .margin(Margin.of(5))
                .build();

        Entity col2 = cb.text()
                .textWithAutoSize(col2Text)
                .textStyle(TextStyle.builder()
                        .fontName(FontName.HELVETICA)
                        .size(12)
                        .color(ComponentColor.BLACK)
                        .build())
                .anchor(Anchor.center())
                .margin(Margin.of(5))
                .build();

        return cb.hContainer(Align.middle(10))
                .entityName("Row")
                .anchor(Anchor.center())
                .addChild(col1)
                .addChild(col2)
                .build();
    }
}

