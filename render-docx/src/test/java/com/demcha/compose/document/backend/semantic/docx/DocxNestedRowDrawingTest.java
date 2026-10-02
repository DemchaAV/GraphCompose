package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A drawing in a row nested through a layer stack of one layer takes its room in the flow.
 *
 * <p>A row cannot sit in a row cell, so templates wrap one in a layer stack of one layer to
 * nest it; nothing is laid over anything there, and the row is written in the flow. A disc
 * drawn over a label in one of its cells is drawn where the page puts it, and its room was not
 * kept: {@code IndigoProposal}'s meta tiles and feature tiles set each label a disc's or a
 * tile's height, 24pt to 28pt, higher than the page.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxNestedRowDrawingTest {

    private static final double DISC = 24;
    private static final double LABEL_GAP = 6;

    @Test
    void aDiscOverALabelHoldsItsRoomAboveTheLabel() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, nested(row -> row.weights(1, 1)
                .addSection("Tile", tile -> tile.spacing(0)
                        .add(disc())
                        .addParagraph(p -> p.text("DATE").margin(DocumentInsets.top(LABEL_GAP))))
                .addSection("Other", tile -> tile.spacing(0).addParagraph("27 May 2026"))))) {
            XWPFParagraph label = paragraphHolding(document, "DATE");

            assertThat(before(label)).as("the disc and the label's own gap above it")
                    .isEqualTo(Math.round((DISC + LABEL_GAP) * 20));
        }
    }

    @Test
    void anIconAloneInItsCellLeavesItToTheRow() throws Exception {
        // PaymentsInvoice's card head: the disc alone in a section of its own, beside the heading.
        // Its row is held as tall as the page makes it; space held in its cell as well would
        // make the row taller.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, nested(row -> row.weights(1, 3)
                .addSection("Icon", icon -> icon.spacing(0).add(disc()))
                .addParagraph("Bank details")))) {
            XWPFTableCell icon = paragraphHolding(document, "Bank details").getBody() instanceof XWPFTableCell cell
                    ? cell.getTableRow().getCell(0)
                    : null;

            assertThat(icon).isNotNull();
            for (XWPFParagraph paragraph : icon.getParagraphs()) {
                assertThat(before(paragraph)).as("no disc's room in its own cell").isLessThan(Math.round(DISC * 20));
            }
        }
    }

    private static DocumentNode disc() {
        return new ShapeContainerBuilder().name("Disc").circle(DISC)
                .fillColor(DocumentColor.rgb(230, 228, 250))
                .center(new com.demcha.compose.document.dsl.ShapeBuilder().name("Mark").size(10, 10)
                        .fillColor(DocumentColor.rgb(61, 37, 173)).build())
                .build();
    }

    /** A row wrapped in a layer stack of one layer, the way a template nests one. */
    private static Consumer<PageFlowBuilder> nested(Consumer<RowBuilder> row) {
        SectionBuilder holder = new SectionBuilder().name("Holder");
        holder.addRow("Tiles", row);
        DocumentNode node = holder.build();
        return page -> page
                .addParagraph("Above")
                .add(new LayerStackBuilder().name("TilesLayer").layer(node, LayerAlign.TOP_LEFT, 0).build());
    }

    private static XWPFParagraph paragraphHolding(XWPFDocument document, String text) {
        for (var table : document.getTables()) {
            for (var row : table.getRows()) {
                for (var cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        if (paragraph.getText().equals(text)) {
                            return paragraph;
                        }
                    }
                }
            }
        }
        throw new AssertionError("no paragraph holding " + text);
    }

    private static long before(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
        return spacing == null || !spacing.isSetBefore() ? 0 : DocxTwips.of(spacing.getBefore());
    }
}
