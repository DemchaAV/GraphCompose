package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A text band's slots split into text and page tokens, and where its line and separator stand.
 *
 * @author Artem Demchyshyn
 */
class DocxTextBandsTest {

    @Test
    void aSlotSplitsIntoItsTextAndItsTokensInOrder() {
        assertThat(DocxTextBands.segments("Page {page} of {pages}")).containsExactly(
                new DocxTextBands.Segment(DocxTextBands.Kind.TEXT, "Page "),
                new DocxTextBands.Segment(DocxTextBands.Kind.PAGE, ""),
                new DocxTextBands.Segment(DocxTextBands.Kind.TEXT, " of "),
                new DocxTextBands.Segment(DocxTextBands.Kind.PAGES, ""));
    }

    @Test
    void tokensSideBySideAndAtTheEdgesLeaveNoEmptyText() {
        assertThat(DocxTextBands.segments("{page}{pages} {date}")).containsExactly(
                new DocxTextBands.Segment(DocxTextBands.Kind.PAGE, ""),
                new DocxTextBands.Segment(DocxTextBands.Kind.PAGES, ""),
                new DocxTextBands.Segment(DocxTextBands.Kind.TEXT, " "),
                new DocxTextBands.Segment(DocxTextBands.Kind.DATE, ""));
    }

    @Test
    void anEmptySlotHasNothingAndAnUnknownTokenIsText() {
        assertThat(DocxTextBands.segments(null)).isEmpty();
        assertThat(DocxTextBands.segments("")).isEmpty();
        assertThat(DocxTextBands.segments("{chapter}")).containsExactly(
                new DocxTextBands.Segment(DocxTextBands.Kind.TEXT, "{chapter}"));
    }

    @Test
    void aHeadersBaselineStandsWhereThePageSetsIt() {
        DocumentHeaderFooter header = DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(10).build();
        double line = DocxTextBands.lineHeight(header);

        // The page sets the baseline height - fontSize / 2 below the top of the page.
        assertThat(DocxTextBands.distanceFromEdge(header) + line * DocxTextBands.BASELINE_SHARE)
                .isCloseTo(25, within(1e-9));
        assertThat(DocxTextBands.separatorSpace(header) + DocxTextBands.distanceFromEdge(header) + line)
                .as("the separator %s below the top of the page", 30).isCloseTo(30, within(1e-9));
    }

    @Test
    void aFootersBaselineStandsWhereThePageSetsIt() {
        DocumentHeaderFooter footer = DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.FOOTER).height(28).fontSize(9).build();
        double line = DocxTextBands.lineHeight(footer);

        // The page sets the baseline height - fontSize above the foot of the page.
        assertThat(DocxTextBands.distanceFromEdge(footer) + line * (1 - DocxTextBands.BASELINE_SHARE))
                .isCloseTo(19, within(1e-9));
    }

    @Test
    void anExactLineStandsItsBaselineWhereThePageSetsItAndStopsAtTheEdge() {
        // A header's 10pt line puts its baseline 8pt below its top, a footer's 2pt above its foot.
        assertThat(DocxTextBands.distanceFromEdge(true, 20, 10)).isCloseTo(12, within(1e-9));
        assertThat(DocxTextBands.distanceFromEdge(false, 6, 10)).isCloseTo(4, within(1e-9));
        assertThat(DocxTextBands.distanceFromEdge(true, 5, 10)).as("past the top edge").isZero();
        assertThat(DocxTextBands.distanceFromEdge(false, 1, 10)).as("past the foot").isZero();
    }

    @Test
    void aBandTooLowForItsLineStartsAtTheEdge() {
        DocumentHeaderFooter header = DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(4).fontSize(12).build();

        assertThat(DocxTextBands.distanceFromEdge(header)).isZero();
        assertThat(DocxTextBands.separatorSpace(header)).isZero();
    }

    @Test
    void eachNumberStyleHasWordsSwitchAndDecimalNone() {
        assertThat(DocxTextBands.numberFormat(DocumentPageNumberStyle.DECIMAL)).isEmpty();
        assertThat(DocxTextBands.numberFormat(null)).isEmpty();
        assertThat(DocxTextBands.numberFormat(DocumentPageNumberStyle.LOWER_ROMAN)).isEqualTo(" \\* roman");
        assertThat(DocxTextBands.numberFormat(DocumentPageNumberStyle.UPPER_ROMAN)).isEqualTo(" \\* ROMAN");
        assertThat(DocxTextBands.numberFormat(DocumentPageNumberStyle.LOWER_ALPHA)).isEqualTo(" \\* alphabetic");
        assertThat(DocxTextBands.numberFormat(DocumentPageNumberStyle.UPPER_ALPHA)).isEqualTo(" \\* ALPHABETIC");
    }
}
