package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TableLayoutIntegrationTest {

    @Test
    void shouldRenderTableWithNegotiatedColumns() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("table_layout_test", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            composer.componentBuilder()
                    .table()
                    .entityName("RoadmapTable")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(16))
                    .columns(
                            TableColumnSpec.fixed(90),
                            TableColumnSpec.auto(),
                            TableColumnSpec.auto()
                    )
                    .defaultCellStyle(TableCellStyle.builder()
                            .fillColor(ComponentColor.WHITE)
                            .build())
                    .columnStyle(0, TableCellStyle.builder()
                            .fillColor(ComponentColor.LIGHT_GRAY)
                            .build())
                    .row("Q1", "Foundations", "Layout engine baseline")
                    .row("Q2", "Tables", "Negotiated widths and row pagination")
                    .row("Q3", "Templates", "Resume and letter refinements")
                    .row("Q4", "Exports", "Renderer stabilization")
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }
}
