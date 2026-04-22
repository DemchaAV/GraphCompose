package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TableStylingIntegrationTest {

    @Test
    void shouldRenderScopedTableStyles() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("table_styling_test", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(24, 24, 24, 24)
                .guideLines(false)
                .create()) {

            composer.componentBuilder()
                    .table()
                    .entityName("StylingTable")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(12))
                    .columns(TableColumnSpec.fixed(110), TableColumnSpec.auto(), TableColumnSpec.auto())
                    .defaultCellStyle(TableCellStyle.builder()
                            .fillColor(ComponentColor.WHITE)
                            .padding(Padding.of(6))
                            .build())
                    .columnStyle(0, TableCellStyle.builder()
                            .fillColor(ComponentColor.LIGHT_GRAY)
                            .build())
                    .rowStyle(0, TableCellStyle.builder()
                            .fillColor(ComponentColor.LIGHT_BLUE)
                            .padding(Padding.of(10))
                            .build())
                    .row("Role", "Owner", "Status")
                    .row("Engine", "GraphCompose", "Stable")
                    .row("Feature", "Table Builder", "In progress")
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();
    }
}
