package com.demcha.compose.document.backend;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * What each backend does with a timeline whose rail is post-layout geometry.
 *
 * <p>The rail stopped being a border on a semantic node and became a fragment contributed
 * after layout. That is fine for a backend that consumes a {@code LayoutGraph} and
 * invisible to one that walks the semantic tree, so the three are asked separately rather
 * than assumed to agree.</p>
 *
 * <ul>
 *   <li><b>PDF</b> and <b>PPTX</b> are fixed-layout: they see the rail and draw it. The
 *       payload is the one a section's own decoration already uses, so no handler had to
 *       be invented — which is exactly what this checks, since a payload no backend knows
 *       fails at export rather than at layout.</li>
 *   <li><b>DOCX</b> is semantic: {@code DocxSemanticBackend} never sees a layout graph, so
 *       it cannot see the rail. The contract is that the timeline's <em>content</em> still
 *       exports, that the export does not throw, and that the omission is written down —
 *       not that a warning is raised, which this architecture cannot produce.</li>
 * </ul>
 */
class TimelineRailAcrossBackendsTest {

    private static final DocumentColor RAIL = DocumentColor.rgb(150, 158, 172);
    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 70);

    @Test
    void pdfDrawsTheRailAndTheTimelineRendersUnchangedByTheBackendChoice() throws Exception {
        try (DocumentSession session = timeline()) {
            assertThat(session.toPdfBytes()).isNotEmpty();
            assertThat(session.layoutGraph().fragments())
                    .as("the rail reached the graph the pdf backend consumes")
                    .anySatisfy(f -> assertThat(f.path()).isEqualTo("@timeline-rail"));
        }
    }

    @Test
    void pptxRendersARailBearingTimelineWithoutInventingAHandler() throws Exception {
        // The rail's payload is a shape, which every fixed backend already handles. If it
        // were not, this would throw at export — the failure mode a layout test cannot see.
        try (DocumentSession session = timeline()) {
            byte[] deck = session.toPptxBytes();
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
                assertThat(show.getSlides()).isNotEmpty();
                assertThat(show.getSlides().get(0).getShapes())
                        .as("the slide carries the timeline")
                        .isNotEmpty();
            }
        }
    }

    @Test
    void docxExportsTheTimelinesContentWithoutTheRailAndWithoutThrowing() throws Exception {
        // The documented contract, asserted rather than described. DOCX walks the semantic
        // tree; the rail is not in it, so the rail is absent by construction — and the
        // entries' text has to be there all the same.
        try (DocumentSession session = timeline()) {
            assertThat(docxText(session))
                    .as("every entry's content survives the export")
                    .contains("Senior Engineer")
                    .contains("Engineer")
                    .contains("Led the layout engine rewrite.");
        }
    }

    @Test
    void theTextExtractorItselfSeesARowsCells() throws Exception {
        // The control. A RowNode exports as a one-row table, so document.getParagraphs()
        // alone cannot see anything laid out in a row — a timeline's title and meta among
        // them. If this assertion fails, the extractor is the problem and not the backend.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(360, 260).margin(DocumentInsets.of(20)).create()) {
            PageFlowBuilder flow = session.pageFlow();
            flow.addRow(row -> row.addParagraph("left cell").addParagraph("right cell"));
            flow.build();

            assertThat(docxText(session)).contains("left cell").contains("right cell");
        }
    }

    /** Everything the export says, paragraphs and table cells alike. */
    private static String docxText(DocumentSession session) {
        byte[][] out = new byte[1][];
        assertThatCode(() -> out[0] = session.export(new DocxSemanticBackend()))
                .as("a rail the semantic backend cannot see must not fail the export")
                .doesNotThrowAnyException();
        assertThat(out[0]).isNotEmpty();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(out[0]));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception failure) {
            throw new IllegalStateException("could not read the exported document", failure);
        }
    }

    private static DocumentSession timeline() throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(360, 260)
                .margin(DocumentInsets.of(20))
                .create();
        PageFlowBuilder flow = session.pageFlow();
        content().accept(flow);
        flow.build();
        return session;
    }

    private static Consumer<PageFlowBuilder> content() {
        return flow -> flow.addTimeline(t -> t
                .connector(RAIL, 1.5)
                .entry(TimelineMarker.dot(8, INK), e -> e
                        .title("Senior Engineer").meta("2023 - Present")
                        .body("Led the layout engine rewrite."))
                .entry(TimelineMarker.numbered(2, 14, INK, DocumentColor.WHITE), e -> e
                        .title("Engineer").meta("2021 - 2023")));
    }
}
