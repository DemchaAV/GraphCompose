package com.demcha.compose.engine.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.testsupport.engine.assembly.TableBuilder;
import com.demcha.compose.engine.components.content.table.TableCellContent;
import com.demcha.compose.engine.components.content.table.TableColumnLayout;
import com.demcha.compose.engine.components.content.table.TableLayoutData;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TablePaginationIntegrationTest {

    @Test
    void shouldKeepEachRowOnSinglePageWhenTableSpansMultiplePages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("table_pagination_test", "clean", "integration");

        Entity table;
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            TableBuilder builder = composer.componentBuilder()
                    .table()
                    .entityName("PaginationTable")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10))
                    .columns(TableColumnLayout.fixed(72), TableColumnLayout.auto(), TableColumnLayout.auto())
                    .width(540)
                    .row("ID", "Category", "Description");

            for (int i = 0; i < 95; i++) {
                builder.row(
                        TableCellContent.text(String.valueOf(i + 1)),
                        TableCellContent.text("Category " + (i % 7)),
                        TableCellContent.lines(
                                "Single line value " + i + " for negotiated table pagination",
                                "Backup slot " + i + " 18:00-22:00")
                );
            }

            table = builder.build();
            LayoutSnapshotAssertions.assertMatches(composer.layoutSnapshot(), "table_pagination_test", "integration");
            composer.build();

            for (Entity row : table.getComponent(TableLayoutData.class).orElseThrow().rowEntities()) {
                Placement placement = row.getComponent(Placement.class)
                        .orElseThrow();

                assertThat(placement.startPage()).isEqualTo(placement.endPage());
            }
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
    }
}
