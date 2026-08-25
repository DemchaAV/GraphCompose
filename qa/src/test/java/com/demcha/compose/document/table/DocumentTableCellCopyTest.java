package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DocumentTableCell} copy factories must carry every component across.
 *
 * <p>{@code withStyle} / {@code colSpan} / {@code rowSpan} each rebuild the
 * record rather than mutating it, so a component dropped from one of those
 * constructor calls would not fail to compile — the cell would just quietly
 * lose its composed child, its span, or both, and the table would render a
 * blank or mis-sized cell. These cases pin all five components through each
 * copy, and then prove through the rendered page that a styled composed cell
 * still draws the child it was built with.</p>
 */
class DocumentTableCellCopyTest {

    private static ParagraphNode paragraph(String name, String text) {
        return new ParagraphNode(name, text, DocumentTextStyle.DEFAULT, TextAlign.LEFT, 0.0,
                DocumentInsets.zero(), DocumentInsets.zero());
    }

    private static DocumentTableStyle tintedStyle() {
        return DocumentTableStyle.builder()
                .fillColor(DocumentColor.rgb(238, 242, 250))
                .padding(DocumentInsets.of(6))
                .build();
    }

    static Stream<Arguments> composedContent() {
        return Stream.of(
                Arguments.of("ParagraphNode", paragraph("Leaf", "STYLED-PARAGRAPH"), "STYLED-PARAGRAPH"),
                Arguments.of("SectionNode",
                        new SectionNode("Stacked",
                                List.of(paragraph("S1", "STYLED-SECTION-ONE"),
                                        paragraph("S2", "STYLED-SECTION-TWO")),
                                2.0, DocumentInsets.zero(), DocumentInsets.zero(), null, null),
                        "STYLED-SECTION-ONE"));
    }

    @ParameterizedTest(name = "withStyle keeps {0} content and spans")
    @MethodSource("composedContent")
    void withStyleKeepsComposedContentAndSpans(String label, DocumentNode content, String ignored) {
        DocumentTableCell base = DocumentTableCell.node(content).colSpan(2).rowSpan(3);
        DocumentTableStyle style = tintedStyle();

        DocumentTableCell styled = base.withStyle(style);

        assertThat(styled.content())
                .describedAs("withStyle must carry the composed %s across the copy", label)
                .isSameAs(content);
        assertThat(styled.hasComposedContent()).isTrue();
        assertThat(styled.colSpan()).isEqualTo(2);
        assertThat(styled.rowSpan()).isEqualTo(3);
        assertThat(styled.style()).isSameAs(style);
        assertThat(styled.lines()).isEqualTo(base.lines());
    }

    @ParameterizedTest(name = "colSpan/rowSpan keep {0} content and style")
    @MethodSource("composedContent")
    void spanCopiesKeepComposedContentAndStyle(String label, DocumentNode content, String ignored) {
        DocumentTableStyle style = tintedStyle();
        DocumentTableCell base = DocumentTableCell.node(content).withStyle(style);

        DocumentTableCell wider = base.colSpan(2);
        DocumentTableCell taller = base.rowSpan(4);

        assertThat(wider.content())
                .describedAs("colSpan must carry the composed %s across the copy", label)
                .isSameAs(content);
        assertThat(wider.style()).isSameAs(style);
        assertThat(wider.colSpan()).isEqualTo(2);
        assertThat(wider.rowSpan()).isEqualTo(1);

        assertThat(taller.content())
                .describedAs("rowSpan must carry the composed %s across the copy", label)
                .isSameAs(content);
        assertThat(taller.style()).isSameAs(style);
        assertThat(taller.colSpan()).isEqualTo(1);
        assertThat(taller.rowSpan()).isEqualTo(4);
    }

    @ParameterizedTest(name = "a styled {0} cell still renders its child")
    @MethodSource("composedContent")
    void styledComposedCellStillRendersItsChild(String label,
                                                DocumentNode content,
                                                String expectedText) throws Exception {
        TableNode table = new TableNode(
                "StyledComposed",
                List.of(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(140)),
                List.of(List.of(
                        DocumentTableCell.node(content).withStyle(tintedStyle()),
                        DocumentTableCell.text("Neighbour"))),
                DocumentTableStyle.empty(),
                340.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);
            pdfBytes = session.toPdfBytes();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String extracted = new PDFTextStripper().getText(document);
            assertThat(extracted)
                    .describedAs("a styled composed %s cell must still render its child", label)
                    .contains(expectedText);
            assertThat(extracted).contains("Neighbour");
        }
    }

    @Test
    void styleOverrideDoesNotChangeWhichTextTheCellRenders() throws Exception {
        assertThat(renderedText(null)).isEqualTo(renderedText(tintedStyle()));
    }

    private static String renderedText(DocumentTableStyle style) throws Exception {
        DocumentTableCell cell = DocumentTableCell.node(paragraph("Body", "SAME-TEXT-EITHER-WAY"));
        TableNode table = new TableNode(
                "StyleParity",
                List.of(DocumentTableColumn.fixed(200)),
                List.of(List.of(style == null ? cell : cell.withStyle(style))),
                DocumentTableStyle.empty(),
                200.0,
                DocumentInsets.zero(),
                DocumentInsets.zero());

        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.add(table);
            pdfBytes = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
