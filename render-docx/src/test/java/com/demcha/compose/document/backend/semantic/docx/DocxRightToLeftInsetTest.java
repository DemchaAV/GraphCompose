package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A right-to-left paragraph is held in on the sides the page holds it in on.
 *
 * <p>Word and LibreOffice read {@code w:ind}'s {@code left} and {@code right} in a
 * {@code w:bidi} paragraph as the start and end of the flow, the way they read {@code w:jc}:
 * {@code left} is the right-hand side. A container's padding and a timeline body's column
 * were written by page side whatever the paragraph's direction, so a Hebrew paragraph in a
 * section padded on the left came out pushed in from the right, and a timeline body with its
 * marker on the rail stopped short of the right margin by the rail's whole column. Measured in
 * both editors before and after: the text now ends where the page ends it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxRightToLeftInsetTest {

    private static final String HEBREW = "שלום עולם";
    private static final DocumentColor ACCENT = DocumentColor.rgb(0x1A, 0x56, 0x94);

    @Test
    void aRightToLeftParagraphInASectionPaddedOnTheLeftIsHeldInAtItsEnd() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(new DocumentInsets(0, 0, 0, 90))
                        .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL))))) {
            CTInd indent = indentOf(paragraph(document, HEBREW));

            assertThat(indent.isSetLeft())
                    .as("left is the start of a right-to-left flow, the right-hand side, and the page holds nothing in there")
                    .isFalse();
            assertThat(DocxTwips.of(indent.getRight()))
                    .as("the padding on the page's left is the end of the flow")
                    .isEqualTo(90 * 20L);
        }
    }

    @Test
    void bothSidesOfAnUnevenPaddingChangePlaces() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(outer -> outer.padding(new DocumentInsets(0, 10, 0, 40))
                        .addSection(inner -> inner.padding(new DocumentInsets(0, 20, 0, 50))
                                .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL)))))) {
            CTInd indent = indentOf(paragraph(document, HEBREW));

            assertThat(DocxTwips.of(indent.getLeft())).as("the page's right: 10 + 20").isEqualTo(30 * 20L);
            assertThat(DocxTwips.of(indent.getRight())).as("the page's left: 40 + 50").isEqualTo(90 * 20L);
        }
    }

    @Test
    void aRightToLeftParagraphInASectionPaddedOnTheRightIsHeldInAtItsStart() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(new DocumentInsets(0, 40, 0, 0))
                        .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL))))) {
            CTInd indent = indentOf(paragraph(document, HEBREW));

            assertThat(DocxTwips.of(indent.getLeft()))
                    .as("the padding on the page's right is the start of the flow")
                    .isEqualTo(40 * 20L);
            assertThat(indent.isSetRight()).as("the page holds nothing in on its left").isFalse();
        }
    }

    @Test
    void anAutomaticParagraphThatResolvesRightToLeftIsTurnedToo() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(new DocumentInsets(0, 0, 0, 90))
                        .addParagraph(p -> p.text(HEBREW).direction(TextDirection.AUTO))))) {
            XWPFParagraph paragraph = paragraph(document, HEBREW);

            assertThat(paragraph.getCTP().getPPr().isSetBidi()).isTrue();
            assertThat(indentOf(paragraph).isSetLeft()).isFalse();
            assertThat(DocxTwips.of(indentOf(paragraph).getRight())).isEqualTo(90 * 20L);
        }
    }

    @Test
    void aLeftToRightParagraphBesideItKeepsThePagesSides() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(s -> s.padding(new DocumentInsets(0, 0, 0, 90))
                        .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL))
                        .addParagraph(p -> p.text("Latin"))))) {
            CTInd indent = indentOf(paragraph(document, "Latin"));

            assertThat(DocxTwips.of(indent.getLeft())).isEqualTo(90 * 20L);
            assertThat(indent.isSetRight()).isFalse();
        }
    }

    @Test
    void aRightToLeftParagraphInAPanelIsHeldInByItsSectionFromTheLeft() throws Exception {
        // The panel's own padding is its cell's margins, which are page sides in a table laid
        // out left to right. The section inside it is the paragraph's indent, and turns.
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addSection(panel -> panel.fillColor(DocumentColor.rgb(0xEE, 0xEE, 0xEE))
                        .padding(new DocumentInsets(4, 4, 4, 60))
                        .addSection(inner -> inner.padding(new DocumentInsets(0, 0, 0, 30))
                                .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL)))))) {
            XWPFParagraph paragraph = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(candidate -> candidate.getText().equals(HEBREW))
                    .findFirst().orElseThrow();
            var margins = document.getTables().get(0).getRow(0).getCell(0).getCTTc().getTcPr().getTcMar();

            assertThat(DocxTwips.of(margins.getLeft().getW())).isEqualTo(60 * 20L);
            assertThat(indentOf(paragraph).isSetLeft()).isFalse();
            assertThat(DocxTwips.of(indentOf(paragraph).getRight())).isEqualTo(30 * 20L);
        }
    }

    @Test
    void aRightToLeftBodyOnARailTimelineRunsToTheRightMarginAndStopsAtItsColumn() throws Exception {
        double margin = 20;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400)
                .margin(DocumentInsets.of(margin))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> t
                    .markerOnRail()
                    .entry(TimelineMarker.dot(8, ACCENT), e -> e
                            .title("Senior Engineer")
                            .add(body -> body.addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL))))));
            double columnX = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload paragraph
                            && paragraph.lines().stream().anyMatch(line -> line.text().contains("עולם")))
                    .findFirst().orElseThrow().x();
            byte[] docx = session.export(new DocxSemanticBackend());

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                CTInd indent = indentOf(paragraph(document, HEBREW));

                assertThat(columnX - margin).as("the page puts the body in the content column").isGreaterThan(10);
                assertThat(indent.isSetLeft()).as("nothing holds the body in from the right margin").isFalse();
                assertThat(DocxTwips.of(indent.getRight()) / 20.0)
                        .as("the rail's column is the end of the flow")
                        .isCloseTo(columnX - margin, within(0.1));
            }
        }
    }

    @Test
    void aRightToLeftParagraphOutsideAnyContainerHasNoIndent() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 30, page -> page
                .addParagraph(p -> p.text(HEBREW).direction(TextDirection.RTL)))) {
            assertThat(paragraph(document, HEBREW).getCTP().getPPr().isSetInd()).isFalse();
        }
    }

    private static CTInd indentOf(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getInd();
    }

    private static XWPFParagraph paragraph(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no paragraph reads " + text));
    }
}
