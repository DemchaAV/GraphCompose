package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A composed table cell renders composite content, not just leaf content.
 *
 * <p>The matrix covers every <em>composite</em> node kind a cell can hold —
 * section, container, row, layer stack, align — plus the leaf and nested-table
 * shapes that already worked, as the control group. A composite owns its
 * children through {@code NodeDefinition.children(...)} rather than emitting
 * them from {@code emitFragments}, so a composed cell has to walk the whole
 * sub-tree; dispatching to the child's own {@code emitFragments} alone yields
 * the composite's decoration and silently drops everything inside it.
 *
 * <p>The cell reserves the child's measured height either way, so a dropped
 * sub-tree is invisible in the page geometry: it renders as a correctly-sized
 * blank hole. These cases assert on the rendered PDF text so a regression
 * cannot pass by producing well-shaped emptiness.</p>
 */
class TableCellComposedNodeTypeTest {

    private static ParagraphNode paragraph(String name, String text) {
        return new ParagraphNode(name, text, DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                DocumentInsets.zero(), DocumentInsets.zero());
    }

    static Stream<Arguments> composedCellContent() {
        return Stream.of(
                Arguments.of("ParagraphNode",
                        paragraph("Leaf", "MARK-PARAGRAPH"),
                        List.of("MARK-PARAGRAPH")),
                Arguments.of("ListNode",
                        new ListNode("Bullets", List.of("MARK-LIST-ONE", "MARK-LIST-TWO"),
                                ListMarker.bullet(), DocumentTextStyle.DEFAULT, TextAlign.LEFT,
                                0.0, 2.0, null, true, DocumentInsets.zero(), DocumentInsets.zero()),
                        List.of("MARK-LIST-ONE", "MARK-LIST-TWO")),
                Arguments.of("SectionNode",
                        new SectionNode("Stacked",
                                List.of(paragraph("S1", "MARK-SECTION-ONE"),
                                        paragraph("S2", "MARK-SECTION-TWO")),
                                2.0, DocumentInsets.zero(), DocumentInsets.zero(), null, null),
                        List.of("MARK-SECTION-ONE", "MARK-SECTION-TWO")),
                Arguments.of("ContainerNode",
                        new ContainerNode("Boxed",
                                List.of(paragraph("C1", "MARK-CONTAINER")),
                                2.0, DocumentInsets.zero(), DocumentInsets.zero(), null, null),
                        List.of("MARK-CONTAINER")),
                // A row splits the cell width between its children, so its
                // markers are short enough to survive in half a column.
                Arguments.of("RowNode",
                        new RowNode("Side",
                                List.of(paragraph("R1", "ROW-A"),
                                        paragraph("R2", "ROW-B")),
                                List.of(1.0, 1.0), 4.0,
                                DocumentInsets.zero(), DocumentInsets.zero(), null, null,
                                DocumentCornerRadius.ZERO),
                        List.of("ROW-A", "ROW-B")),
                Arguments.of("LayerStackNode",
                        new LayerStackNode("Stack",
                                List.of(new LayerStackNode.Layer(paragraph("L1", "MARK-LAYERSTACK"))),
                                DocumentInsets.zero(), DocumentInsets.zero()),
                        List.of("MARK-LAYERSTACK")),
                Arguments.of("AlignNode",
                        new AlignNode(paragraph("A1", "MARK-ALIGN"), HorizontalAlign.CENTER),
                        List.of("MARK-ALIGN")),
                Arguments.of("nested TableNode",
                        new TableNode("Inner",
                                List.of(DocumentTableColumn.fixed(170)),
                                List.of(List.of(DocumentTableCell.text("MARK-NESTED-TABLE"))),
                                DocumentTableStyle.empty(), 170.0,
                                DocumentInsets.zero(), DocumentInsets.zero()),
                        List.of("MARK-NESTED-TABLE")));
    }

    @ParameterizedTest(name = "{0} renders inside a composed table cell")
    @MethodSource("composedCellContent")
    void composedCellRendersCompositeAndLeafContent(String label,
                                                    DocumentNode content,
                                                    List<String> expectedText) throws Exception {
        // The composed column is wide enough that a row child still gets a
        // legible half-slot: the assertions below read the rendered text, and a
        // marker broken across a wrap would fail for the wrong reason.
        TableNode table = new TableNode(
                "ComposedMatrix",
                List.of(DocumentTableColumn.fixed(260), DocumentTableColumn.fixed(80)),
                List.of(List.of(DocumentTableCell.node(content), DocumentTableCell.text("Neighbour"))),
                DocumentTableStyle.empty(),
                340.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        byte[] pdfBytes;
        List<PlacedFragment> fragments;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);

            fragments = session.layoutGraph().fragments();
            pdfBytes = session.toPdfBytes();
        }

        // The outer table is a single row, so it contributes exactly one
        // TableRowFragmentPayload of its own. Anything beyond that came from
        // the composed child's sub-tree.
        assertThat(fragments)
                .describedAs("%s composed into a cell must contribute fragments beyond the "
                             + "single row fragment the outer table emits for itself", label)
                .hasSizeGreaterThan(1);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String extracted = new PDFTextStripper().getText(document);
            assertThat(extracted)
                    .describedAs("%s composed into a cell must reach the rendered page", label)
                    .contains(expectedText);
            assertThat(extracted).contains("Neighbour");
        }
    }
}
