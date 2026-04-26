package com.demcha.compose.document.style;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextAutoSizeTest {

    @Test
    void autoSizeShrinksFontWhenTextWouldOtherwiseWrap() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(220, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .name("Title")
                            .text("Auto-size headline that should fit on a single line")
                            .textStyle(DocumentTextStyle.builder().size(36).build())
                            .autoSize(36, 6))
                    .build();

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(1);
            PlacedNode title = findBySemantic(graph.nodes(), "Title");
            assertThat(title).isNotNull();
            // Page inner width is ~200 (220 - 2*10). The 36pt headline would normally
            // wrap; auto-size should shrink the font enough that the placed node's
            // width measures within page inner width and total height matches a
            // single line (no wrap).
            assertThat(title.placementWidth()).isLessThanOrEqualTo(200.0 + 0.5);
            // Single-line height for the resolved font; far smaller than the
            // wrapped-version height at 36pt would be.
            assertThat(title.placementHeight()).isLessThan(36.0 * 1.6);
        }
    }

    @Test
    void autoSizeRespectsMaxSizeWhenTextAlreadyFits() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .name("Short")
                            .text("Hi")
                            .textStyle(DocumentTextStyle.builder().size(24).build())
                            .autoSize(24, 6))
                    .build();

            PlacedNode short_ = findBySemantic(session.layoutGraph().nodes(), "Short");
            assertThat(short_).isNotNull();
            // Two-character text fits comfortably; the resolved height should
            // match a single 24pt line (around ~28pt) rather than something tiny.
            assertThat(short_.placementHeight()).isGreaterThan(20.0);
        }
    }

    @Test
    void autoSizeFallsBackToMinSizeWhenTextNeverFits() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(140, 200)
                .margin(DocumentInsets.of(10))
                .create()) {

            // Even at 6pt the text may not fit on a single line; the layout should
            // still produce a paginated paragraph instead of throwing.
            session.dsl()
                    .pageFlow()
                    .name("Flow")
                    .addParagraph(paragraph -> paragraph
                            .name("Long")
                            .text("This is an intentionally long sentence that will never fit on a single 120pt line of text.")
                            .textStyle(DocumentTextStyle.builder().size(36).build())
                            .autoSize(36, 6))
                    .build();

            assertThat(session.layoutGraph().totalPages()).isGreaterThanOrEqualTo(1);
        }
    }

    private static PlacedNode findBySemantic(List<PlacedNode> nodes, String name) {
        for (PlacedNode node : nodes) {
            if (name.equals(node.semanticName())) {
                return node;
            }
        }
        return null;
    }
}
