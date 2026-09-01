package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Pins where a running header / footer actually lands on the page.
 *
 * <p>The rest of the suite proves chrome text is <em>present</em> — that
 * {@code {page}} expands, that the date token resolves, that a numbered zone is
 * suppressed on an uncounted page. Nothing proves <em>where</em> it is drawn,
 * and no committed baseline covers chrome either: the visual baselines are all
 * template presets, and no template registers a header or footer. So a change
 * to how the three slots are positioned moves text in every consumer's document
 * and passes every gate in this repository.</p>
 *
 * <p>The assertions are written as the placement rules themselves — left slot
 * flush to the left margin, right slot flush to the right margin, centre slot
 * centred in the space between them, one shared baseline per zone, repeated
 * identically on every page — rather than as measured constants. A rule states
 * what the renderer promises, so a failure names the broken promise instead of
 * a number that drifted; and unlike a pixel baseline it neither re-encodes on a
 * different platform's rasteriser nor hides a small shift under a tolerance
 * budget.</p>
 *
 * @author Artem Demchyshyn
 */
class ChromeGeometryGuardTest {

    private static final float PAGE_WIDTH = 300f;
    private static final float PAGE_HEIGHT = 240f;
    private static final float MARGIN = 24f;

    /** Half a point: below what a reader can see, above float noise. */
    private static final float EPSILON = 0.5f;

    /**
     * Deep enough that reserving it cannot be absorbed by rounding: it takes more
     * than half the content area of a {@value #PAGE_HEIGHT}pt page, so the page
     * count has to move rather than landing on the same number by luck.
     */
    private static final float TALL_FOOTER_HEIGHT = 100f;

    /** {@code DocumentHeaderFooter} defaults, restated so the maths below reads. */
    private static final float ZONE_HEIGHT = 30f;
    private static final float FONT_SIZE = 9f;

    private static final String HEADER_LEFT = "Engine";
    private static final String HEADER_RIGHT = "Chrome";
    private static final String FOOTER_LEFT = "Confidential";
    private static final String FOOTER_RIGHT = "v2";

    @Test
    void chromeSlotsHoldTheirPlacementOnEveryPage() throws Exception {
        byte[] pdf = renderTwoPageDocumentWithChrome();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages())
                    .as("the fixture must paginate, so the footer is exercised on more than one page")
                    .isGreaterThanOrEqualTo(2);

            Glyphs glyphs = new Glyphs();
            glyphs.getText(document);

            for (int page = 1; page <= 2; page++) {
                List<TextPosition> onPage = glyphs.page(page);

                Run headerLeft = Run.locate(onPage, HEADER_LEFT);
                Run headerRight = Run.locate(onPage, HEADER_RIGHT);
                Run footerLeft = Run.locate(onPage, FOOTER_LEFT);
                Run footerRight = Run.locate(onPage, FOOTER_RIGHT);
                Run footerCentre = Run.locate(onPage, "Page " + page + " of " + document.getNumberOfPages());

                assertThat(headerLeft.startX)
                        .as("page %d: the header left slot starts at the left margin", page)
                        .isCloseTo(MARGIN, offset(EPSILON));
                assertThat(headerRight.endX)
                        .as("page %d: the header right slot ends at the right margin", page)
                        .isCloseTo(PAGE_WIDTH - MARGIN, offset(EPSILON));
                assertThat(headerLeft.baselineY)
                        .as("page %d: both header slots share one baseline", page)
                        .isCloseTo(headerRight.baselineY, offset(EPSILON));

                assertThat(footerLeft.startX)
                        .as("page %d: the footer left slot starts at the left margin", page)
                        .isCloseTo(MARGIN, offset(EPSILON));
                assertThat(footerRight.endX)
                        .as("page %d: the footer right slot ends at the right margin", page)
                        .isCloseTo(PAGE_WIDTH - MARGIN, offset(EPSILON));
                assertThat(footerCentre.centreX())
                        .as("page %d: the footer centre slot is centred between the margins", page)
                        .isCloseTo(PAGE_WIDTH / 2f, offset(EPSILON));
                assertThat(footerLeft.baselineY)
                        .as("page %d: all three footer slots share one baseline", page)
                        .isCloseTo(footerCentre.baselineY, offset(EPSILON))
                        .isCloseTo(footerRight.baselineY, offset(EPSILON));

                assertThat(headerLeft.baselineY)
                        .as("page %d: the header sits above the footer", page)
                        .isLessThan(footerLeft.baselineY);

                // Absolute, not just relative: a uniform shift of a whole zone
                // keeps every rule above satisfied while moving the text in every
                // document that has one. These two are the renderer's own
                // formulae, and their asymmetry — the footer offsets by a full
                // font size, the header by half of one — is inherited rather than
                // designed. Pinned so that a rewrite has to decide about it out
                // loud instead of quietly picking something else.
                assertThat(footerLeft.baselineY)
                        .as("page %d: the footer baseline sits one font size above the zone's"
                                + " bottom edge", page)
                        .isCloseTo(PAGE_HEIGHT - (ZONE_HEIGHT - FONT_SIZE),
                                offset(EPSILON));
                assertThat(headerLeft.baselineY)
                        .as("page %d: the header baseline sits half a font size below the zone's"
                                + " top edge", page)
                        .isCloseTo(ZONE_HEIGHT - FONT_SIZE / 2f,
                                offset(EPSILON));
            }

            Run footerOnPageOne = Run.locate(glyphs.page(1), FOOTER_LEFT);
            Run footerOnPageTwo = Run.locate(glyphs.page(2), FOOTER_LEFT);
            assertThat(footerOnPageTwo.baselineY)
                    .as("a repeating zone lands on the same baseline on every page")
                    .isCloseTo(footerOnPageOne.baselineY, offset(EPSILON));
            assertThat(footerOnPageTwo.startX)
                    .as("a repeating zone lands at the same x on every page")
                    .isCloseTo(footerOnPageOne.startX, offset(EPSILON));
        }
    }

    /**
     * The zone's {@code height} is a drawing offset, not a layout inset: it is
     * never subtracted from the content area, so the body lays out exactly as if
     * no footer had been registered. Here a footer tall enough to claim a quarter
     * of the page changes neither the page count nor where a given line lands —
     * whether that band then collides with body text is left to the author's
     * margin.
     *
     * <p>Pinned deliberately. The change that finally reserves the space has to
     * come here and say so, rather than reflowing every existing document
     * silently.</p>
     */
    @Test
    void aTallFooterDoesNotPushTheBodyAtAll() throws Exception {
        byte[] withoutFooter = renderBody(false);
        byte[] withTallFooter = renderBody(true);

        try (PDDocument bare = Loader.loadPDF(withoutFooter);
             PDDocument stamped = Loader.loadPDF(withTallFooter)) {

            assertThat(stamped.getNumberOfPages())
                    .as("a 60pt footer on a %.0fpt page reserves nothing, so the body still"
                            + " fills the same number of pages", PAGE_HEIGHT)
                    .isEqualTo(bare.getNumberOfPages());

            Glyphs bareGlyphs = new Glyphs();
            bareGlyphs.getText(bare);
            Glyphs stampedGlyphs = new Glyphs();
            stampedGlyphs.getText(stamped);

            Run bareLine = Run.locate(bareGlyphs.page(1), "Body line 0");
            Run stampedLine = Run.locate(stampedGlyphs.page(1), "Body line 0");
            assertThat(stampedLine.baselineY)
                    .as("the body does not move when a footer is registered")
                    .isCloseTo(bareLine.baselineY, offset(EPSILON));

            float lowestBareBaseline = bareGlyphs.page(1).stream()
                    .map(TextPosition::getYDirAdj)
                    .max(Float::compare)
                    .orElseThrow();
            assertThat(lowestBareBaseline)
                    .as("and it reaches into the band the footer paints over (top edge %.0f)",
                            PAGE_HEIGHT - TALL_FOOTER_HEIGHT)
                    .isGreaterThan(PAGE_HEIGHT - TALL_FOOTER_HEIGHT);
        }
    }

    /**
     * The same body, optionally under a footer tall enough that reserving its
     * height has to reflow the document.
     */
    private static byte[] renderBody(boolean withTallFooter) throws Exception {
        return renderBody(withTallFooter, false, TALL_FOOTER_HEIGHT);
    }

    private static byte[] renderBody(boolean withFooter, boolean reserveSpace, float footerHeight)
            throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {

            if (withFooter) {
                document.chrome().footer(DocumentHeaderFooter.builder()
                        .leftText(FOOTER_LEFT)
                        .height(footerHeight)
                        .reserveSpace(reserveSpace)
                        .build());
            }
            fillBody(document);
            return document.toPdfBytes();
        }
    }

    /**
     * The opposite pin to {@link #aTallFooterDoesNotPushTheBodyAtAll}: asked to
     * reserve, the same zone takes its band out of the content area and the body
     * has to go somewhere else.
     */
    @Test
    void aReservingFooterTakesItsBandOutOfTheContentArea() throws Exception {
        try (PDDocument unreserved = Loader.loadPDF(renderBody(true, false, TALL_FOOTER_HEIGHT));
             PDDocument reserved = Loader.loadPDF(renderBody(true, true, TALL_FOOTER_HEIGHT))) {

            assertThat(reserved.getNumberOfPages())
                    .as("a %.0fpt band out of a %.0fpt page has to cost the body pages",
                            TALL_FOOTER_HEIGHT, PAGE_HEIGHT)
                    .isGreaterThan(unreserved.getNumberOfPages());

            Glyphs glyphs = new Glyphs();
            glyphs.getText(reserved);
            // The zone's own text sits inside the band by construction, so the scan
            // has to stop above it — otherwise it measures the footer and always fails.
            float footerBaseline = Run.locate(glyphs.page(1), FOOTER_LEFT).baselineY;
            float lowestBodyBaseline = glyphs.page(1).stream()
                    .map(TextPosition::getYDirAdj)
                    .filter(baseline -> baseline < footerBaseline - EPSILON)
                    .max(Float::compare)
                    .orElseThrow();
            assertThat(lowestBodyBaseline)
                    .as("and no body line is laid out into the band the footer paints")
                    .isLessThanOrEqualTo(PAGE_HEIGHT - TALL_FOOTER_HEIGHT);
        }
    }

    /**
     * Reserving takes the larger of margin and zone height, not their sum, so the
     * common case — a zone that already fits inside the margin — moves nothing.
     */
    @Test
    void aZoneThatFitsInsideTheMarginReservesNothing() throws Exception {
        float shorterThanMargin = MARGIN - 4f;

        try (PDDocument off = Loader.loadPDF(renderBody(true, false, shorterThanMargin));
             PDDocument on = Loader.loadPDF(renderBody(true, true, shorterThanMargin))) {

            assertThat(on.getNumberOfPages()).isEqualTo(off.getNumberOfPages());

            Glyphs offGlyphs = new Glyphs();
            offGlyphs.getText(off);
            Glyphs onGlyphs = new Glyphs();
            onGlyphs.getText(on);
            assertThat(Run.locate(onGlyphs.page(1), "Body line 0").baselineY)
                    .as("asking to reserve a band the margin already clears changes nothing")
                    .isCloseTo(Run.locate(offGlyphs.page(1), "Body line 0").baselineY, offset(EPSILON));
        }
    }

    private static byte[] renderTwoPageDocumentWithChrome() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(MARGIN))
                .create()) {

            document.chrome()
                    .header(DocumentHeaderFooter.builder()
                            .leftText(HEADER_LEFT)
                            .rightText(HEADER_RIGHT)
                            .showSeparator(true)
                            .build())
                    .footer(DocumentHeaderFooter.builder()
                            .leftText(FOOTER_LEFT)
                            .centerText("Page {page} of {pages}")
                            .rightText(FOOTER_RIGHT)
                            .showSeparator(true)
                            .build());

            fillBody(document);
            return document.toPdfBytes();
        }
    }

    private static void fillBody(DocumentSession document) {
        var flow = document.dsl().pageFlow().name("ChromeGeometry");
        for (int i = 0; i < 24; i++) {
            int index = i;
            flow.addParagraph(paragraph -> paragraph
                    .name("Body" + index)
                    .text("Body line " + index + " exists to push the flow onto a second page."));
        }
        flow.build();
    }

    /** One extracted string: where it starts, where it ends, and its baseline. */
    private record Run(float startX, float endX, float baselineY) {

        float centreX() {
            return (startX + endX) / 2f;
        }

        /**
         * Finds {@code needle} among the page's glyphs and reports its geometry.
         * Matching runs over the concatenated glyph text rather than over the
         * strings the stripper emits, because a footer's three slots share a
         * baseline and can be handed over as one merged line.
         */
        static Run locate(List<TextPosition> glyphs, String needle) {
            StringBuilder text = new StringBuilder();
            List<Integer> glyphIndexPerChar = new ArrayList<>();
            for (int i = 0; i < glyphs.size(); i++) {
                String unicode = glyphs.get(i).getUnicode();
                if (unicode == null) {
                    continue;
                }
                text.append(unicode);
                for (int c = 0; c < unicode.length(); c++) {
                    glyphIndexPerChar.add(i);
                }
            }

            int at = text.indexOf(needle);
            assertThat(at)
                    .as("expected to find %s in the extracted page text <%s>", needle, text)
                    .isGreaterThanOrEqualTo(0);

            TextPosition first = glyphs.get(glyphIndexPerChar.get(at));
            TextPosition last = glyphs.get(glyphIndexPerChar.get(at + needle.length() - 1));
            return new Run(
                    first.getXDirAdj(),
                    last.getXDirAdj() + last.getWidthDirAdj(),
                    first.getYDirAdj());
        }
    }

    /** Collects every glyph of every page, keeping the page it came from. */
    private static final class Glyphs extends PDFTextStripper {

        private final Map<Integer, List<TextPosition>> byPage = new HashMap<>();

        Glyphs() throws IOException {
            setSortByPosition(true);
        }

        List<TextPosition> page(int pageNumber) {
            List<TextPosition> positions = byPage.get(pageNumber);
            assertThat(positions).as("page %d carries extractable text", pageNumber).isNotNull();
            return positions;
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) {
            byPage.computeIfAbsent(getCurrentPageNo(), page -> new ArrayList<>()).addAll(positions);
        }
    }
}
