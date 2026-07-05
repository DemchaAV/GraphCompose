package com.demcha.compose.document.table;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An AUTO column sizes to fit a composed cell's intrinsic content (e.g. an
 * inline-code chip) instead of collapsing to zero because the composed cell
 * contributed no natural width. A FIXED column keeps its declared width, so an
 * over-wide chip breaks inside it (handled by the paragraph wrap engine).
 */
class TableAutoColumnComposedWidthTest {

    private static final String CODE = "io.github.demchaav";

    /** Number of visual chip fragments (rounded-fill spans) rendered by the composed cell. */
    private static long chipFragmentCount(DocumentSession session) {
        return session.layoutGraph().fragments().stream()
                .map(PlacedFragment::payload)
                .filter(ParagraphFragmentPayload.class::isInstance)
                .map(ParagraphFragmentPayload.class::cast)
                .flatMap(payload -> payload.lines().stream())
                .filter(line -> line.spans().stream()
                        .anyMatch(s -> s instanceof ParagraphTextSpan ts && ts.background() != null))
                .count();
    }

    private static TableNode chipTable(DocumentSession session, DocumentTableColumn column) {
        return new TableNode(
                "ChipTable",
                List.of(column),
                List.of(List.of(DocumentTableCell.node(session.dsl().paragraph().inlineCode(CODE).build()))),
                DocumentTableStyle.empty(),
                null,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    @Test
    void autoColumnGrowsToFitComposedChipOnOneFragment() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 160)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.add(chipTable(session, DocumentTableColumn.auto()));
            // The AUTO column sizes to the chip's natural width (the page is wide
            // enough), so the chip renders on ONE fragment instead of collapsing to
            // zero width and breaking character by character.
            assertThat(chipFragmentCount(session)).isEqualTo(1);
        }
    }

    @Test
    void fixedNarrowColumnStillBreaksTheComposedChip() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 160)
                .margin(DocumentInsets.of(10))
                .create()) {
            session.add(chipTable(session, DocumentTableColumn.fixed(40)));
            // A FIXED column is never grown; the over-wide chip breaks inside it.
            assertThat(chipFragmentCount(session)).isGreaterThanOrEqualTo(2);
        }
    }
}
