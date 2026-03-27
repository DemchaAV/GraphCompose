package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.TableBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.layout.coordinator.Placement;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.PdfComposer;
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
        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            TableBuilder builder = composer.componentBuilder()
                    .table()
                    .entityName("PaginationTable")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10))
                    .columns(TableColumnSpec.fixed(72), TableColumnSpec.auto(), TableColumnSpec.auto())
                    .width(540)
                    .row("ID", "Category", "Description");

            for (int i = 0; i < 95; i++) {
                builder.row(
                        String.valueOf(i + 1),
                        "Category " + (i % 7),
                        "Single line value " + i + " for negotiated table pagination"
                );
            }

            table = builder.build();
            composer.build();

            for (var rowId : table.getChildren()) {
                Placement placement = composer.entityManager().getEntity(rowId)
                        .orElseThrow()
                        .getComponent(Placement.class)
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
