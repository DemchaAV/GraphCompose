package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code hangingIndent(true)}: the marker sits in its own column and every
 * visual line of an item — the first, the ones it wraps onto, and the ones that
 * continue on later pages — starts at one shared content origin.
 *
 * <p>The legacy layout approximates that origin separately for each line with a
 * run of spaces rounded up to clear the marker, which is why its wrapped lines
 * land a fraction of a space width off its first line. Here the origin is one
 * number, so there is nothing to drift.</p>
 *
 * <p>All numbers below are measured at the default 14pt style on a page with a
 * 12pt margin, so an item starts at x=12: bullet 4.900 wide, gap 4.0, content
 * at 20.900.</p>
 */
class ListHangingIndentTest {

    private static final double EPS = 0.001;

    // --- the shared content origin -----------------------------------------

    @Test
    void everyLineOfAWrappedItemStartsAtTheSameContentOrigin() throws Exception {
        Rendered rendered = render(165, 240, l -> l.bullet().hangingIndent(true).items(
                "Long item text should wrap across several visual lines while keeping one bullet."));

        List<PlacedFragment> fragments = rendered.fragments();
        assertThat(fragments).as("one marker, one content block").hasSize(2);

        PlacedFragment marker = fragments.get(0);
        PlacedFragment content = fragments.get(1);
        assertThat(texts(marker)).containsExactly("•");
        assertThat(marker.x()).isEqualTo(12.000, within(EPS));
        assertThat(marker.width()).as("the measured bullet, not a guess").isEqualTo(4.900, within(EPS));
        assertThat(content.x()).isEqualTo(20.900, within(EPS));
        assertThat(content.width()).isEqualTo(132.100, within(EPS));

        assertThat(texts(content)).hasSizeGreaterThanOrEqualTo(3);
        assertThat(texts(content)).as("no marker leaked into the flow").noneMatch(t -> t.contains("•"));
        assertThat(texts(content)).as("and no space indent either")
                .allMatch(t -> t.isEmpty() || !Character.isWhitespace(t.charAt(0)));

        // The text was wrapped inside the content column, not inside the row and
        // then merely drawn there — every line fits the narrower width.
        assertThat(lineWidths(content))
                .allSatisfy(w -> assertThat(w).isLessThanOrEqualTo(content.width() + EPS));
        assertThat(lineWidths(content).stream().mapToDouble(Double::doubleValue).max().orElseThrow())
                .as("and at least one line uses most of it, so the bound is real")
                .isGreaterThan(content.width() * 0.75);

        // The claim, at the glyphs: every content line drawn from one x.
        List<Glyph> glyphs = rendered.glyphs();
        assertThat(glyphs.stream().filter(g -> !g.text().equals("•")))
                .allSatisfy(g -> assertThat(g.x()).isEqualTo(20.900, within(EPS)));
    }

    @Test
    void theMarkerSharesTheFirstContentLinesBaseline() throws Exception {
        Rendered rendered = render(165, 240, l -> l.bullet().hangingIndent(true).items(
                "Long item text should wrap across several visual lines here."));

        List<Glyph> glyphs = rendered.glyphs();
        Glyph marker = glyphs.get(0);
        Glyph firstContentLine = glyphs.get(1);

        assertThat(marker.text()).isEqualTo("•");
        assertThat(marker.y())
                .as("the marker is on the first line, not a block of its own beside it")
                .isEqualTo(firstContentLine.y(), within(EPS));

        // ...and it does not make the row any taller. Compared against the same
        // text with no marker at all, at a length that wraps to one line either
        // way, so the two are the same content and only the marker differs.
        double withMarker = listNode(render(200, 240,
                l -> l.bullet().hangingIndent(true).items("Item")).graph()).placementHeight();
        double withoutMarker = listNode(render(200, 240,
                l -> l.noMarker().hangingIndent(true).items("Item")).graph()).placementHeight();
        assertThat(withMarker)
                .as("the marker rides the content's line rather than adding one")
                .isEqualTo(withoutMarker, within(EPS));

        // The two fragments of a row are one box: same top, same height, which
        // is what makes the shared baseline structural rather than lucky.
        PlacedFragment markerBox = rendered.fragments().get(0);
        PlacedFragment contentBox = rendered.fragments().get(1);
        assertThat(markerBox.y()).isEqualTo(contentBox.y(), within(EPS));
        assertThat(markerBox.height()).isEqualTo(contentBox.height(), within(EPS));
    }

    // --- markers -----------------------------------------------------------

    @Test
    void eachMarkerGetsItsOwnMeasuredColumn() throws Exception {
        assertMarkerAndContent(l -> l.bullet().hangingIndent(true).items("Item"), "•", 4.900, 20.900);
        assertMarkerAndContent(l -> l.dash().hangingIndent(true).items("Item"), "-", 4.662, 20.662);
        assertMarkerAndContent(l -> l.marker(">").hangingIndent(true).items("Item"), ">", 8.176, 24.176);
        assertMarkerAndContent(l -> l.marker("=>").hangingIndent(true).items("Item"), "=>", 16.352, 32.352);
        assertMarkerAndContent(l -> l.marker("MMM").hangingIndent(true).items("Item"), "MMM", 34.986, 50.986);
    }

    @Test
    void theGapIsPointsOfRealSpaceBetweenMarkerAndContent() throws Exception {
        for (double gap : List.of(0.0, 4.0, 8.0, 16.0)) {
            Rendered rendered = render(200, 240,
                    l -> l.bullet().hangingIndent(true).markerGap(gap).items("Item"));
            List<PlacedFragment> fragments = rendered.fragments();

            assertThat(fragments.get(0).x()).as("gap %s: the marker never moves", gap)
                    .isEqualTo(12.000, within(EPS));
            assertThat(fragments.get(1).x()).as("gap %s: content", gap)
                    .isEqualTo(12.000 + 4.900 + gap, within(EPS));
            assertThat(fragments.get(1).width()).as("gap %s: content width", gap)
                    .isEqualTo(176.000 - 4.900 - gap, within(EPS));
        }
    }

    @Test
    void aMarkerlessItemGetsNoMarkerFragmentAndNoGap() throws Exception {
        Rendered rendered = render(165, 240,
                l -> l.noMarker().hangingIndent(true).markerGap(16).items("Item"));

        assertThat(rendered.fragments()).as("nothing is drawn for a marker that is not there").hasSize(1);
        PlacedFragment content = rendered.fragments().get(0);
        assertThat(content.x()).as("flush at the item start, not inset by a reserved gap")
                .isEqualTo(12.000, within(EPS));
        assertThat(content.width()).isEqualTo(141.000, within(EPS));
        assertThat(rendered.glyphs().get(0).x()).isEqualTo(12.000, within(EPS));
    }

    @Test
    void aMarkerOnlyRowKeepsItsPlaceItsMarkerAndItsHeight() throws Exception {
        Rendered rendered = render(165, 240, l -> l.bullet().hangingIndent(true).items("Alpha", "   ", "Beta"));

        List<PlacedFragment> fragments = rendered.fragments();
        assertThat(fragments).as("three rows, each a marker and a content block").hasSize(6);
        assertThat(markerCount(rendered)).isEqualTo(3);

        PlacedFragment emptyRowMarker = fragments.get(2);
        PlacedFragment emptyRowContent = fragments.get(3);
        assertThat(texts(emptyRowMarker)).containsExactly("•");
        assertThat(texts(emptyRowContent)).as("no invented filler text").containsExactly("");
        assertThat(emptyRowMarker.height()).as("a real row with a real height").isPositive();

        // The rows are evenly spaced, so the empty one occupies a full row.
        double firstToSecond = fragments.get(0).y() - fragments.get(2).y();
        double secondToThird = fragments.get(2).y() - fragments.get(4).y();
        assertThat(firstToSecond).isEqualTo(secondToThird, within(EPS));
    }

    // --- nesting -----------------------------------------------------------

    @Test
    void aChildHangsUnderItsParentsTextAndAGrandchildUnderTheChilds() throws Exception {
        Rendered rendered = render(220, 240, l -> l.hangingIndent(true)
                .addItem("Top", c -> c.addItem("Child", g -> g.addItem("Grandchild"))));

        List<PlacedFragment> fragments = rendered.fragments();
        assertThat(fragments).hasSize(6);

        double topMarker = fragments.get(0).x();
        double topContent = fragments.get(1).x();
        double childMarker = fragments.get(2).x();
        double childContent = fragments.get(3).x();
        double grandchildMarker = fragments.get(4).x();
        double grandchildContent = fragments.get(5).x();

        assertThat(topMarker).isEqualTo(12.000, within(EPS));
        assertThat(topContent).isEqualTo(20.900, within(EPS));
        assertThat(childMarker).as("the child's marker starts at the parent's text")
                .isEqualTo(topContent, within(EPS));
        assertThat(childContent).isEqualTo(32.684, within(EPS));
        assertThat(grandchildMarker).as("and the grandchild's at the child's")
                .isEqualTo(childContent, within(EPS));
        assertThat(grandchildContent).isEqualTo(44.468, within(EPS));

        // Each depth keeps its own width, narrowing by exactly what it indented.
        assertThat(fragments.get(1).width()).isEqualTo(187.100, within(EPS));
        assertThat(fragments.get(3).width()).isEqualTo(175.316, within(EPS));
        assertThat(fragments.get(5).width()).isEqualTo(163.532, within(EPS));

        // Depth is geometry here, so nothing in the text carries it.
        assertThat(fragments).allSatisfy(f -> assertThat(texts(f))
                .allMatch(t -> !t.contains(" ") && !t.startsWith(" ")));
    }

    // --- page splits --------------------------------------------------------

    @Test
    void anItemCrossingThreePageBoundariesDrawsItsMarkerOnceAndNeverMovesItsContent() throws Exception {
        Rendered rendered = render(150, 70, l -> l.bullet().hangingIndent(true).items(
                "Long item text should wrap across many visual lines so that it has to cross two page "
                + "boundaries and continue on the following pages with its indent intact and unchanged."));

        assertThat(rendered.graph().totalPages()).isEqualTo(4);
        assertThat(markerCount(rendered))
                .as("the marker belongs to the item, not to each page of it")
                .isEqualTo(1);

        // Page 0 opens with the marker; every later page is content alone — and
        // is fragment index 0 on its own page, which is exactly why marker
        // emission cannot be inferred from that index.
        assertThat(rendered.fragments().stream().filter(f -> f.pageIndex() == 0)).hasSize(2);
        for (int page = 1; page <= 3; page++) {
            List<PlacedFragment> onPage = onPage(rendered, page);
            assertThat(onPage).as("page %d carries content only", page).hasSize(1);
            assertThat(texts(onPage.get(0))).noneMatch(t -> t.contains("•"));
            assertThat(onPage.get(0).fragmentIndex())
                    .as("page %d restarts fragment numbering at zero", page)
                    .isZero();
        }

        // One content origin and one content width, on all four pages.
        for (int page = 0; page <= 3; page++) {
            PlacedFragment content = onPage(rendered, page).stream()
                    .filter(f -> !texts(f).equals(List.of("•")))
                    .findFirst()
                    .orElseThrow();
            assertThat(content.x()).as("page %d contentX", page).isEqualTo(20.900, within(EPS));
            assertThat(content.width()).as("page %d contentWidth", page).isEqualTo(117.100, within(EPS));
            assertThat(lineWidths(content)).as("page %d wraps inside the content column", page)
                    .allSatisfy(w -> assertThat(w).isLessThanOrEqualTo(117.100 + EPS));
        }

        // ...and at the glyphs, on every page.
        assertThat(rendered.glyphs().stream().filter(g -> !g.text().equals("•")))
                .allSatisfy(g -> assertThat(g.x()).isEqualTo(20.900, within(EPS)));
    }

    // --- alignment ----------------------------------------------------------

    @Test
    void centreAndRightAlignTextInsideTheContentColumnWithoutMovingTheMarker() throws Exception {
        for (TextAlign align : List.of(TextAlign.CENTER, TextAlign.RIGHT)) {
            Rendered rendered = render(200, 240, l -> l.bullet().hangingIndent(true).align(align)
                    .items("Short"));

            List<PlacedFragment> fragments = rendered.fragments();
            assertThat(fragments.get(0).x()).as("%s: the marker column is geometry", align)
                    .isEqualTo(12.000, within(EPS));
            assertThat(fragments.get(1).x()).as("%s: so is the content column", align)
                    .isEqualTo(20.900, within(EPS));
            assertThat(fragments.get(1).width()).isEqualTo(167.100, within(EPS));

            List<Glyph> glyphs = rendered.glyphs();
            assertThat(glyphs.get(0).x()).as("%s: marker unmoved", align).isEqualTo(12.000, within(EPS));
            assertThat(glyphs.get(1).x())
                    .as("%s: text placed inside the content column, not at its start", align)
                    .isGreaterThan(20.900);
            assertThat(glyphs.get(1).x() + 33.460)
                    .as("%s: and inside its right edge", align)
                    .isLessThanOrEqualTo(20.900 + 167.100 + EPS);
        }
    }

    // --- containers ---------------------------------------------------------

    @Test
    void paddingMovesTheWholeGeometryInAndMarginMovesTheBox() throws Exception {
        Rendered padded = render(200, 240, l -> l.bullet().hangingIndent(true)
                .padding(3, 5, 7, 11).items("Item"));
        Rendered margined = render(200, 240, l -> l.bullet().hangingIndent(true)
                .margin(3, 5, 7, 11).items("Item"));

        // Both put the marker 11pt in from the page margin, by different routes.
        assertThat(padded.fragments().get(0).x()).isEqualTo(23.000, within(EPS));
        assertThat(margined.fragments().get(0).x()).isEqualTo(23.000, within(EPS));
        assertThat(padded.fragments().get(1).x()).isEqualTo(31.900, within(EPS));
        assertThat(margined.fragments().get(1).x()).isEqualTo(31.900, within(EPS));

        assertThat(padded.glyphs().get(0).x()).isEqualTo(23.000, within(EPS));
        assertThat(margined.glyphs().get(0).x()).isEqualTo(23.000, within(EPS));
    }

    /**
     * The narrow-container contract, stated rather than inherited.
     *
     * <p>When the marker column alone is wider than the row, the marker is still
     * drawn at its measured width and overflows; the content is floored at
     * {@code ParagraphWrapping.MIN_TEXT_WIDTH} — 1pt, the same floor the text
     * pipeline applies whenever a prefix eats a line — and is broken as far as it
     * will go, overflowing too. Nothing throws, and nothing is silently dropped.
     * The alternative, letting the width reach zero, hits the engine's
     * zero-width behaviour, where a line becomes empty and its text disappears;
     * that is the outcome this floor exists to avoid.</p>
     */
    @Test
    void aMarkerWiderThanTheRowOverflowsAndTheTextSurvives() throws Exception {
        double page = 60.0;
        Rendered rendered = render(page, 240, l -> l.marker("MMMMMMMMMM").hangingIndent(true)
                .markerGap(16).items("Content"));

        List<PlacedFragment> fragments = rendered.fragments();
        PlacedFragment marker = fragments.get(0);
        PlacedFragment content = fragments.get(1);
        double available = page - 24.0;

        assertThat(marker.width()).as("drawn at its measured width, unclipped")
                .isGreaterThan(available);
        assertThat(content.width()).as("floored at the engine's minimum text width")
                .isEqualTo(1.0, within(EPS));
        assertThat(content.x()).as("still placed past the marker and its gap")
                .isGreaterThan(12.000 + available);

        // The text is broken up and overflows, but every character of it is
        // still there — no empty line, no exception.
        String rendered1 = String.join("", texts(content));
        assertThat(rendered1).isEqualTo("Content");
        assertThat(texts(content)).as("one character per line at this width").hasSize(7);
    }

    // --- the legacy layout is a different layout ---------------------------

    @Test
    void optingInMovesTextAndLeavingItAloneDoesNot() throws Exception {
        Consumer<ListBuilder> shape = l -> l.bullet().items(
                "Long item text should wrap across several visual lines here.");

        Rendered legacy = render(165, 240, shape);
        Rendered hanging = render(165, 240, shape.andThen(l -> l.hangingIndent(true)));

        // Legacy: the marker is inside the text and the wrapped lines carry
        // spaces measured to clear it, landing them past the first line's text.
        assertThat(legacy.fragments()).hasSize(1);
        assertThat(texts(legacy.fragments().get(0)).get(0)).startsWith("• ");
        assertThat(texts(legacy.fragments().get(0)).get(1)).startsWith("   ");

        // Hanging: a marker column, a content column, one origin.
        assertThat(hanging.fragments()).hasSize(2);
        assertThat(texts(hanging.fragments().get(1))).noneMatch(t -> t.startsWith(" "));

        double legacyFirstLineText = legacy.glyphs().get(0).textX();
        double legacyWrappedText = legacy.glyphs().get(1).textX();
        assertThat(legacyWrappedText - legacyFirstLineText)
                .as("the legacy drift this replaces")
                .isEqualTo(2.884, within(0.05));

        assertThat(hanging.glyphs().get(1).x() - hanging.glyphs().get(2).x())
                .as("and its absence")
                .isEqualTo(0.0, within(EPS));
    }

    // ------------------------------------------------------------------

    private static void assertMarkerAndContent(Consumer<ListBuilder> spec,
                                               String markerText,
                                               double markerWidth,
                                               double contentX) throws Exception {
        Rendered rendered = render(200, 240, spec);
        List<PlacedFragment> fragments = rendered.fragments();
        assertThat(texts(fragments.get(0))).as("%s marker", markerText).containsExactly(markerText);
        assertThat(fragments.get(0).width()).as("%s width", markerText).isEqualTo(markerWidth, within(EPS));
        assertThat(fragments.get(1).x()).as("%s contentX", markerText).isEqualTo(contentX, within(EPS));
    }

    private record Rendered(LayoutGraph graph, List<Glyph> glyphs) {
        List<PlacedFragment> fragments() {
            return graph.fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .toList();
        }
    }

    private record Glyph(int page, double x, double textX, double y, String text) {
    }

    private static Rendered render(double width, double height, Consumer<ListBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Root").addList(spec).build();
            return new Rendered(session.layoutGraph(), glyphs(session.toPdfBytes()));
        }
    }

    private static List<PlacedFragment> onPage(Rendered rendered, int page) {
        return rendered.fragments().stream().filter(f -> f.pageIndex() == page).toList();
    }

    private static long markerCount(Rendered rendered) {
        return rendered.fragments().stream()
                .filter(f -> texts(f).equals(List.of("•")))
                .count();
    }

    private static List<Double> lineWidths(PlacedFragment fragment) {
        return ((ParagraphFragmentPayload) fragment.payload()).lines().stream()
                .map(ParagraphLine::width)
                .toList();
    }

    private static List<String> texts(PlacedFragment fragment) {
        return ((ParagraphFragmentPayload) fragment.payload()).lines().stream()
                .map(ParagraphLine::text)
                .toList();
    }

    private static PlacedNode listNode(LayoutGraph graph) {
        return graph.nodes().stream()
                .filter(n -> "ListNode".equals(n.nodeKind()))
                .findFirst()
                .orElseThrow();
    }

    private static List<Glyph> glyphs(byte[] pdf) throws IOException {
        List<Glyph> rows = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    if (positions.isEmpty()) {
                        return;
                    }
                    double textX = positions.stream()
                            .filter(p -> !p.getUnicode().isEmpty()
                                         && Character.isLetterOrDigit(p.getUnicode().charAt(0)))
                            .mapToDouble(TextPosition::getXDirAdj)
                            .findFirst()
                            .orElse(positions.get(0).getXDirAdj());
                    rows.add(new Glyph(getCurrentPageNo() - 1, positions.get(0).getXDirAdj(),
                            textX, positions.get(0).getYDirAdj(), text));
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
        }
        return rows;
    }
}
