package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A list with {@code hangingIndent(true)} through the PPTX backend.
 *
 * <p>The marker/content geometry is resolved in core and reaches every
 * fixed-layout backend as ordinary paragraph fragments, so PPTX needs no code of
 * its own for it. That is a claim about a mechanism, though, and the docs state
 * it as flatly as they state the PDF's — so it is asserted here against a real
 * rendered deck rather than left as an inference.</p>
 */
class PptxListHangingIndentTest {

    @Test
    void theMarkerAndContentColumnsSurviveIntoTheRenderedDeck() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 240)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Lists")
                    .addList(list -> list
                            .name("Hanging")
                            .bullet()
                            .hangingIndent(true)
                            .items("Long item text that wraps across more than one visual line here."))
                    .build();

            LayoutGraph graph = session.render(new GraphCapturingBackend());
            byte[] pptx = session.render(new PptxFixedLayoutBackend());

            // The resolved geometry: a marker at the item start, content one
            // measured marker width plus the gap further in.
            PptxGeometryAssertions.assertTextGeometryMatches(graph, pptx);

            try (XMLSlideShow deck = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFTextBox> boxes = deck.getSlides().get(0).getShapes().stream()
                        .filter(XSLFTextBox.class::isInstance)
                        .map(XSLFTextBox.class::cast)
                        .sorted(Comparator.comparingDouble(b -> b.getAnchor().getY()))
                        .toList();

                assertThat(boxes).as("a marker frame and the content lines").hasSizeGreaterThanOrEqualTo(2);

                XSLFTextBox marker = boxes.stream()
                        .filter(b -> "•".equals(b.getText().strip()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no marker frame in the deck"));

                List<XSLFTextBox> content = boxes.stream()
                        .filter(b -> !"•".equals(b.getText().strip()) && !b.getText().isBlank())
                        .toList();
                assertThat(content).as("the item wrapped").hasSizeGreaterThan(1);

                assertThat(marker.getAnchor().getX())
                        .as("the marker sits at the item start")
                        .isEqualTo(12.000, within(0.5));

                // The claim that matters: every content line, not just the first,
                // begins at the same x — and that x is past the marker.
                double contentX = content.get(0).getAnchor().getX();
                assertThat(contentX)
                        .as("content clears the marker column")
                        .isGreaterThan(marker.getAnchor().getX());
                assertThat(content)
                        .allSatisfy(box -> assertThat(box.getAnchor().getX())
                                .isEqualTo(contentX, within(0.5)));
            }
        }
    }
}
