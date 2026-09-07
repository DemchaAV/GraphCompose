package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.node.TextAlign;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A contract probe: a right-aligned paragraph cannot live in an `auto()` column.
 *
 * Alignment needs somewhere to happen, so a right-aligned paragraph claims the
 * full row width. An `auto()` column sizes to its content and cannot grant that,
 * and the two demands are not reconcilable — so the row fails to lay out rather
 * than quietly placing the text somewhere approximate.
 *
 * The failure surfaces at **render** time, not at compile time, which is what
 * makes it worth a test rather than a sentence: nothing in the builder's types
 * says these two calls are incompatible, and the combination reads perfectly
 * sensible right up until a document is produced.
 *
 * Both halves are asserted deliberately. A test that only proved the throw would
 * pass just as well if `auto()` columns stopped working altogether; pairing it
 * with the working form pins the boundary rather than one side of it.
 *
 * The escape is not to widen anything: in a column sized to its own text, the
 * text already ends at the column's right edge, so the alignment is what to
 * drop.
 */
class AutoColumnRightAlignContractTest {

    private static final String TEXT = "0.00";

    @Test
    void rightAlignInsideAnAutoColumnFailsToLayOut() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow
                    .addRow(row -> row
                            .columns(DocumentRowColumn.auto(), DocumentRowColumn.auto())
                            .addSection(left -> left.addParagraph("Total"))
                            .addSection(right -> right.addParagraph(
                                    p -> p.text(TEXT).align(TextAlign.RIGHT))))));

            assertThatThrownBy(session::layoutGraph)
                    .describedAs(
                            "a right-aligned paragraph claims the full row width, which an auto column "
                                    + "cannot grant — the row must fail rather than place the text approximately")
                    .isInstanceOf(RuntimeException.class)
                    // The message, not just the throw. "Something failed" would
                    // pass for any unrelated breakage; the contract is that the
                    // engine says the columns do not fit and what to do.
                    .hasMessageContaining("fixed and auto columns need")
                    .hasMessageContaining("is available");
        }
    }

    @Test
    void theSameColumnLaysOutOnceTheAlignmentIsDropped() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow
                    .addRow(row -> row
                            .columns(DocumentRowColumn.auto(), DocumentRowColumn.auto())
                            .addSection(left -> left.addParagraph("Total"))
                            .addSection(right -> right.addParagraph(TEXT)))));

            assertThatCode(session::layoutGraph)
                    .describedAs("without the alignment the auto column sizes to its text and lays out")
                    .doesNotThrowAnyException();
            assertThat(session.layoutGraph().fragments())
                    .describedAs("and it actually produces something")
                    .isNotEmpty();
        }
    }
}
