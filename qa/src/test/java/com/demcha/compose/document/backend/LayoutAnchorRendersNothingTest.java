package com.demcha.compose.document.backend;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutAnchorId;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * What each fixed backend does with an anchor fragment, asked of a real render.
 *
 * <p>The layout tests prove the anchor reports the right box. They cannot prove the thing
 * the two no-op handlers exist for: a fixed backend refuses a fragment payload it has no
 * handler for, so a document containing an anchor would fail at <em>export</em> — long
 * after the layout everything else asserts. Registration is a line in a list, and a line
 * in a list can be deleted without breaking a compile.</p>
 *
 * <p>So each backend is asked twice, once with the marker wrapped and once bare, and the
 * two are compared. That is stronger than "does not throw": it says the anchor reached the
 * handler <em>and</em> the handler drew nothing, which is the whole of its contract. The
 * PDF is compared as pixels because it is painted, and the deck by its shape count because
 * a slide is a shape tree — an anchor that produced any ink or any shape moves one of the
 * two numbers.</p>
 *
 * <p>Proven fail-closed: removing either handler from its backend's {@code defaultHandlers}
 * turns the matching test red with {@code UnsupportedNodeCapabilityException}.</p>
 */
class LayoutAnchorRendersNothingTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 160);

    private enum Kind { MARKER }

    @Test
    void aDocumentCarryingAnAnchorRendersToPdfExactlyAsItWouldWithout() throws Exception {
        BufferedImage anchored = pdfPage(LayoutAnchorRendersNothingTest::anchor);
        BufferedImage bare = pdfPage(UnaryOperator.identity());

        assertThat(anchored.getWidth()).isEqualTo(bare.getWidth());
        assertThat(anchored.getHeight()).isEqualTo(bare.getHeight());
        assertThat(differingPixels(anchored, bare))
                .as("an anchor is metadata; the page it is on must be the same page")
                .isZero();
    }

    @Test
    void aDocumentCarryingAnAnchorRendersToPptxExactlyAsItWouldWithout() throws Exception {
        assertThat(slideShapeCount(LayoutAnchorRendersNothingTest::anchor))
                .as("no shape stands for the anchor, and the deck is written")
                .isEqualTo(slideShapeCount(UnaryOperator.identity()));
    }

    @Test
    void bothBackendsAcceptTheAnchorRatherThanRefusingItsPayload() {
        // The failure this guards is an export-time throw, not a wrong picture: each
        // backend's handlerFor raises UnsupportedNodeCapabilityException on a payload class
        // it does not know. Stated separately from the comparisons above so the message
        // says which of the two things broke.
        assertThatCode(() -> {
            pdfPage(LayoutAnchorRendersNothingTest::anchor);
            slideShapeCount(LayoutAnchorRendersNothingTest::anchor);
        }).doesNotThrowAnyException();
    }

    private static DocumentNode anchor(DocumentNode marker) {
        return new LayoutAnchorNode("", new LayoutAnchorId(new Object(), Kind.MARKER, 0), marker);
    }

    private static BufferedImage pdfPage(UnaryOperator<DocumentNode> wrap) throws Exception {
        try (DocumentSession session = document(wrap)) {
            List<BufferedImage> pages = session.toImages(72);
            assertThat(pages).hasSize(1);
            return pages.get(0);
        }
    }

    private static int slideShapeCount(UnaryOperator<DocumentNode> wrap) throws Exception {
        try (DocumentSession session = document(wrap)) {
            try (XMLSlideShow deck = new XMLSlideShow(new ByteArrayInputStream(session.toPptxBytes()))) {
                return deck.getSlides().get(0).getShapes().size();
            }
        }
    }

    /** One page, one paragraph and one marker — wrapped or not, by the caller's choice. */
    private static DocumentSession document(UnaryOperator<DocumentNode> wrap) throws Exception {
        DocumentSession session = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(20))
                .create();
        session.pageFlow()
                .addParagraph("Beside the marker")
                .add(wrap.apply(new EllipseNode("dot", 8, 8, INK, null, null, null, null, null)))
                .build();
        return session;
    }

    private static int differingPixels(BufferedImage left, BufferedImage right) {
        int differing = 0;
        for (int y = 0; y < left.getHeight(); y++) {
            for (int x = 0; x < left.getWidth(); x++) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        return differing;
    }
}
