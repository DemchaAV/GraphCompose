package com.demcha.compose.document.templates.core.page;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * The safe area a full-bleed template reserves on the pages its body continues onto.
 *
 * <p>A template sets the page margin to zero so a page background can reach the
 * paper edge, and gives up the safe area on all four sides to buy the two it needed.
 * A column's own padding does not cover the difference: it is an edge of the column,
 * spent on the page the column opens on.</p>
 */
class ContinuationSafeAreaTest {

    @Test
    void itInsetsTheContinuationPagesAndLeavesTheFirstAlone() {
        try (DocumentSession session = fullBleed()) {
            ContinuationSafeArea.applyTo(session, 2, 36);
            fillPastOnePage(session);

            assertThat(session.layoutGraph().totalPages()).isGreaterThan(1);
            assertThat(firstLineTopGap(session, 1))
                    .as("page 2 clears the safe area")
                    .isGreaterThanOrEqualTo(36.0);
            assertThat(firstLineTopGap(session, 0))
                    .as("page 1 keeps the top edge its own design gave it")
                    .isCloseTo(30.0, within(0.5));
        }
    }

    @Test
    void itKeepsTheHorizontalEdgesItWasGiven() {
        // The bug being prevented: reserving a top inset by rebuilding all four
        // edges would silently un-bleed the sides, and the page backgrounds would
        // no longer line up with the columns.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(new DocumentInsets(10, 20, 30, 40))
                .create()) {
            ContinuationSafeArea.applyTo(session, 2, 36);
            fillPastOnePage(session);

            assertThat(leftEdgeOfFirstLine(session, 1))
                    .as("page 2 keeps the left margin the caller chose, plus the column padding")
                    .isCloseTo(leftEdgeOfFirstLine(session, 0), within(0.5));
        }
    }

    @Test
    void aMarginThatAlreadyClearsTheSafeAreaIsLeftAlone() {
        // The empty case has to be a no-op, not an empty rule list: pageMargins()
        // replaces, and an empty list clears. A template calling this
        // unconditionally must not be able to delete rules the caller set.
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(48))
                .create()) {
            session.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.zero())));

            ContinuationSafeArea.applyTo(session, 2, 36);

            fillPastOnePage(session);
            assertThat(firstLineTopGap(session, 0))
                    .as("the caller's own full-bleed first page survives the call")
                    .isCloseTo(30.0, within(0.5));
        }
    }

    @Test
    void aSafeAreaMustBeAUsableLengthOnAPageThatCanHaveOne() {
        try (DocumentSession session = fullBleed()) {
            assertThatThrownBy(() -> ContinuationSafeArea.applyTo(session, 1, 36))
                    .as("page 1 is where a body starts; its own design owns its top edge")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("firstContinuationPage");
            assertThatThrownBy(() -> ContinuationSafeArea.applyTo(session, 2, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("topSafeArea");
            assertThatThrownBy(() -> ContinuationSafeArea.applyTo(session, 2, Double.NaN))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ContinuationSafeArea.applyTo(null, 2, 36))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void aBodyThatStartsLaterInsetsFromThePageItSays() {
        // A cover page and a body starting on page 2: the inset belongs from page 3,
        // and page 2 — the body's own first page — must keep its design.
        try (DocumentSession session = fullBleed()) {
            ContinuationSafeArea.applyTo(session, 3, 36);
            session.dsl().pageFlow()
                    .name("Root")
                    .padding(DocumentInsets.zero())
                    .addParagraph(p -> p.name("cover").text("Cover"))
                    .addPageBreak(b -> b.name("break"))
                    .addColumnFlow("Body", body -> body.addColumn("Only", column -> {
                        column.padding(DocumentInsets.of(30)).spacing(6);
                        for (int index = 0; index < 60; index++) {
                            int current = index;
                            column.addParagraph(p -> p
                                    .name("line-" + current)
                                    .text("line " + current + " with enough words to occupy a line"));
                        }
                    }))
                    .build();

            assertThat(session.layoutGraph().totalPages()).isGreaterThan(2);
            assertThat(firstLineTopGap(session, 1))
                    .as("page 2 opens the body and keeps its own top edge")
                    .isLessThan(36.0);
            assertThat(firstLineTopGap(session, 2))
                    .as("page 3 is the first continuation and clears the safe area")
                    .isGreaterThanOrEqualTo(36.0);
        }
    }

    // -- helpers -----------------------------------------------------------

    private static DocumentSession fullBleed() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.zero())
                .create();
    }

    private static void fillPastOnePage(DocumentSession session) {
        session.dsl().pageFlow()
                .name("Root")
                .padding(DocumentInsets.zero())
                .addColumnFlow("Body", body -> body.addColumn("Only", column -> {
                    column.padding(DocumentInsets.of(30)).spacing(6);
                    for (int index = 0; index < 60; index++) {
                        int current = index;
                        column.addParagraph(p -> p
                                .name("line-" + current)
                                .text("line " + current + " with enough words to occupy a line"));
                    }
                }))
                .build();
    }

    private static double firstLineTopGap(DocumentSession session, int pageIndex) {
        double pageHeight = session.canvas().height();
        return session.layoutGraph().fragments().stream()
                .filter(fragment -> fragment.pageIndex() == pageIndex)
                .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                .mapToDouble(fragment -> pageHeight - (fragment.y() + fragment.height()))
                .min()
                .orElseThrow(() -> new AssertionError("no text on page " + (pageIndex + 1)));
    }

    private static double leftEdgeOfFirstLine(DocumentSession session, int pageIndex) {
        return session.layoutGraph().fragments().stream()
                .filter(fragment -> fragment.pageIndex() == pageIndex)
                .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                .mapToDouble(fragment -> fragment.x())
                .min()
                .orElseThrow(() -> new AssertionError("no text on page " + (pageIndex + 1)));
    }
}
