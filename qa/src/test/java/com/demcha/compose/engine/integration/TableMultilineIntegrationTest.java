package com.demcha.compose.engine.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TableMultilineIntegrationTest {

    @Test
    void shouldRenderMultilineCellsWithCellOverridesAndVerticalAnchors() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("table_multiline_cells", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            composer.componentBuilder()
                    .table()
                    .entityName("ShiftTable")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(16))
                    .columns(
                            TableColumnLayout.fixed(120),
                            TableColumnLayout.fixed(180),
                            TableColumnLayout.fixed(180)
                    )
                    .width(480)
                    .defaultCellStyle(TableCellLayoutStyle.builder()
                            .padding(new Padding(6, 8, 6, 8))
                            .fillColor(ComponentColor.WHITE)
                            .stroke(new Stroke(ComponentColor.BLACK, 1.0))
                            .build())
                    .row(
                            TableCellContent.text("Mark"),
                            TableCellContent.lines("09:00 17:00", "Clean crushed ice").withStyle(TableCellLayoutStyle.builder()
                                    .fillColor(ComponentColor.LIGHT_GRAY)
                                    .textAnchor(Anchor.topLeft())
                                    .build()),
                            TableCellContent.lines("12:00 20:00", "Training").withStyle(TableCellLayoutStyle.builder()
                                    .fillColor(ComponentColor.LIGHT_BLUE)
                                    .textAnchor(Anchor.centerLeft())
                                    .build()))
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }
}
