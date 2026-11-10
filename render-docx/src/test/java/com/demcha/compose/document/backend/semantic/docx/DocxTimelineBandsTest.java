package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A timeline whose markers sit on the rail keeps its entries in Word.
 *
 * <p>With the marker on the rail, the timeline wraps each entry's header row so the columns
 * it resolves are published, and lays the entry's body out in one of them — two engine
 * wrappers, a {@code HorizontalBandsNode} around the row and a {@code HorizontalBandContentNode}
 * around the body. The export knew neither and dropped both as drawing, so the entries'
 * titles, dates and text were missing from the document while the page showed them.</p>
 */
class DocxTimelineBandsTest {

    @Test
    void aTimelineWithItsMarkersOnTheRailKeepsEveryEntrysTitleDatesAndText() throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> t
                    .markerOnRail()
                    .connector(DocumentColor.rgb(0x99, 0x99, 0x99), 1.5)
                    .entry(TimelineMarker.dot(8, DocumentColor.rgb(0x1A, 0x56, 0x94)), e -> e
                            .title("Senior Engineer").meta("2023 - Present")
                            .body("Led the layout engine rewrite."))
                    .entry(TimelineMarker.dot(8, DocumentColor.rgb(0x1A, 0x56, 0x94)), e -> e
                            .title("Engineer").meta("2021 - 2023"))));
            docx = session.export(new DocxSemanticBackend(report::set));
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            assertThat(extractor.getText())
                    .contains("Senior Engineer").contains("2023 - Present")
                    .contains("Led the layout engine rewrite.")
                    .contains("Engineer").contains("2021 - 2023");
        }
        assertThat(report.get().bySubject())
                .as("nothing of the timeline's content is reported dropped")
                .doesNotContainKeys("HorizontalBandsNode", "HorizontalBandContentNode");
    }

    @Test
    void everyEntrysBodyIsIndentedAndWhatFollowsTheTimelineIsNot() throws Exception {
        // The indent belongs to the body alone: the second entry's body takes it too, and the
        // paragraph after the timeline is back at the page's edge.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(flow -> flow
                    .addTimeline(t -> t
                            .markerOnRail()
                            .entry(TimelineMarker.dot(8, DocumentColor.rgb(0x1A, 0x56, 0x94)), e -> e
                                    .title("First").body("First body."))
                            .entry(TimelineMarker.dot(8, DocumentColor.rgb(0x1A, 0x56, 0x94)), e -> e
                                    .title("Second").body("Second body.")))
                    .addParagraph("After the timeline."));
            byte[] docx = session.export(new DocxSemanticBackend());

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(leftIndent(document, "Second body."))
                        .isEqualTo(leftIndent(document, "First body.")).isPositive();
                assertThat(rightIndent(document, "First body.")).as("the column runs to the page's edge").isZero();
                assertThat(leftIndent(document, "After the timeline.")).isZero();
            }
        }
    }

    @Test
    void aTimelineInsideAPanelIndentsItsBodiesWithinThePanel() throws Exception {
        // In a panel the text is written in the panel's cell, whose own margin is the panel's
        // padding; the body is indented from there by the same column offset.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(flow -> flow.addSection(panel -> panel
                    .fillColor(DocumentColor.rgb(0xF4, 0xF4, 0xF4))
                    .padding(DocumentInsets.of(8))
                    .addTimeline(t -> t
                            .markerOnRail()
                            .entry(TimelineMarker.dot(8, DocumentColor.rgb(0x1A, 0x56, 0x94)), e -> e
                                    .title("Senior Engineer").body("Led the layout engine rewrite.")))));
            byte[] docx = session.export(new DocxSemanticBackend());

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                assertThat(extractor.getText()).contains("Senior Engineer").contains("Led the layout engine rewrite.");
                var cellBody = document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                        .filter(paragraph -> paragraph.getText().contains("Led the layout"))
                        .findFirst().orElseThrow();
                assertThat(DocxTwips.of(cellBody.getCTP().getPPr().getInd().getLeft())).isPositive();
            }
        }
    }

    private static long leftIndent(XWPFDocument document, String text) {
        var ind = paragraphWith(document, text).getCTP().getPPr() == null ? null
                : paragraphWith(document, text).getCTP().getPPr().getInd();
        return ind == null || !ind.isSetLeft() ? 0 : DocxTwips.of(ind.getLeft());
    }

    private static long rightIndent(XWPFDocument document, String text) {
        var ind = paragraphWith(document, text).getCTP().getPPr() == null ? null
                : paragraphWith(document, text).getCTP().getPPr().getInd();
        return ind == null || !ind.isSetRight() ? 0 : DocxTwips.of(ind.getRight());
    }

    private static org.apache.poi.xwpf.usermodel.XWPFParagraph paragraphWith(XWPFDocument document, String text) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getText().contains(text))
                .findFirst().orElseThrow();
    }

    @Test
    void anEntrysBodyIsIndentedToTheColumnThePageLaysItOutIn() throws Exception {
        double margin = 20;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 400)
                .margin(DocumentInsets.of(margin))
                .create()) {
            session.pageFlow(flow -> flow.addTimeline(t -> t
                    .markerOnRail()
                    .entry(TimelineMarker.dot(8, DocumentColor.rgb(0x1A, 0x56, 0x94)), e -> e
                            .title("Senior Engineer").body("Led the layout engine rewrite."))));
            double pageX = session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload()
                            instanceof com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload paragraph
                            && paragraph.lines().stream().anyMatch(line -> line.text().contains("Led the layout")))
                    .findFirst().orElseThrow().x();
            byte[] docx = session.export(new DocxSemanticBackend());

            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
                var body = document.getParagraphs().stream()
                        .filter(paragraph -> paragraph.getText().contains("Led the layout"))
                        .findFirst().orElseThrow();
                double indent = DocxTwips.of(body.getCTP().getPPr().getInd().getLeft()) / 20.0;

                assertThat(pageX - margin).as("the page puts the body in the content column").isGreaterThan(10);
                assertThat(indent).isCloseTo(pageX - margin, org.assertj.core.api.Assertions.within(0.1));
            }
        }
    }
}
