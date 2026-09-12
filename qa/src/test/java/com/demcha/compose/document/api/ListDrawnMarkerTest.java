package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphShapeSpan;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphSvgSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * A list marker that is drawn rather than typed — a coloured disc, an icon, a
 * glyph in a colour of its own — on the same measured marker column a text
 * marker gets.
 *
 * <p>What made this a gap rather than a preference: the marker's one span was
 * built in the list's own text style, so a design with an accent mark beside
 * near-black copy could not be written as a list at all. The templates that
 * wanted it built a paragraph per item instead — a drawn mark, then a run of
 * spaces standing in for the gap, then the text — which buys the mark and loses
 * the column. Measured on that construction at a width that wraps: the first
 * line's text starts at 25.246 and every line after it at 12.000, because a
 * paragraph has no marker column to hang under. The spaces are a second cost;
 * rounded to a whole count and then measured at each item's own type size, one
 * declared 9.175pt gap came out as 9.308pt in one column of a real CV and
 * 8.356pt in another.</p>
 *
 * <p>Numbers below are at the default face, 9pt, on a page with a 12pt margin,
 * so an item starts at x=12.</p>
 */
class ListDrawnMarkerTest {

    private static final double EPS = 0.001;
    private static final DocumentColor ACCENT = DocumentColor.rgb(0x00, 0x88, 0x88);
    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().size(9.0).build();

    private static final String LONG =
            "Coordinate care plans with doctors, therapists and support staff to improve "
            + "outcomes across the whole ward and its discharge pathway.";

    // --- the marker is measured as what it draws ----------------------------

    @Test
    void aDiscMarkerTakesItsDeclaredDiameterAndTheGapAfterItIsPoints() throws Exception {
        Rendered rendered = render(200, 240, l -> l
                .textStyle(BODY)
                .marker(m -> m.dot(4.0, ACCENT))
                .hangingIndent(true)
                .markerGap(9.175)
                .items("Patient assessment"));

        List<PlacedFragment> fragments = rendered.fragments();
        assertThat(fragments).as("one marker, one content block").hasSize(2);
        PlacedFragment marker = fragments.get(0);
        PlacedFragment content = fragments.get(1);

        assertThat(marker.x()).isEqualTo(12.000, within(EPS));
        assertThat(marker.width())
                .as("the column is the disc's own diameter, not the width of any text")
                .isEqualTo(4.000, within(EPS));
        assertThat(content.x())
                .as("markerX + diameter + the gap, exactly as asked for")
                .isEqualTo(12.000 + 4.000 + 9.175, within(EPS));

        // Drawn, not typed: the marker's fragment carries a shape.
        assertThat(spans(marker)).hasSize(1);
        assertThat(spans(marker).get(0)).isInstanceOf(ParagraphShapeSpan.class);
        assertThat(((ParagraphShapeSpan) spans(marker).get(0)).width()).isEqualTo(4.000, within(EPS));
        assertThat(texts(marker))
                .as("and nothing typed leaks into the text layer in its place")
                .containsExactly("");
    }

    @Test
    void aTextMarkerCanCarryAColourTheItemsTextDoesNot() throws Exception {
        Rendered rendered = render(200, 240, l -> l
                .textStyle(BODY)
                .marker(m -> m.color("•", ACCENT))
                .hangingIndent(true)
                .items("Patient assessment"));

        ParagraphTextSpan markerSpan = (ParagraphTextSpan) spans(rendered.fragments().get(0)).get(0);
        ParagraphTextSpan contentSpan = (ParagraphTextSpan) spans(rendered.fragments().get(1)).get(0);

        assertThat(markerSpan.text()).isEqualTo("•");
        assertThat(markerSpan.textStyle().color()).isEqualTo(ACCENT.color());
        assertThat(contentSpan.textStyle().color())
                .as("the item keeps the list's colour, which is the whole point")
                .isNotEqualTo(markerSpan.textStyle().color());

        // And it is still a measured marker column, not a coloured prefix inside
        // the text: the two are separate fragments at separate x.
        assertThat(rendered.fragments()).hasSize(2);
        assertThat(rendered.fragments().get(1).x()).isGreaterThan(rendered.fragments().get(0).x());
    }

    @Test
    void anIconMarkerTakesItsIconsBox() throws Exception {
        Rendered rendered = render(200, 240, l -> l
                .textStyle(BODY)
                .marker(m -> m.svgIcon(TICK, 8.0))
                .hangingIndent(true)
                .markerGap(4.0)
                .items("Patient assessment"));

        PlacedFragment marker = rendered.fragments().get(0);
        assertThat(marker.width()).isEqualTo(8.000, within(EPS));
        assertThat(spans(marker).get(0)).isInstanceOf(ParagraphSvgSpan.class);
        assertThat(rendered.fragments().get(1).x()).isEqualTo(12.000 + 8.000 + 4.000, within(EPS));
    }

    @Test
    void aDrawnMarkerIsMeasuredOnceHoweverManyItemsShowIt() throws Exception {
        // Same marker on every row means the same column on every row — the
        // property that makes a list a list, and the one the per-item paragraph
        // construction cannot promise.
        Rendered rendered = render(200, 240, l -> l
                .textStyle(BODY)
                .marker(m -> m.dot(4.0, ACCENT))
                .hangingIndent(true)
                .markerGap(9.175)
                .items("Alpha", "Beta", "Gamma"));

        assertThat(rendered.fragments()).hasSize(6);
        for (int row = 0; row < 3; row++) {
            assertThat(rendered.fragments().get(row * 2).x())
                    .as("row %d markerX", row).isEqualTo(12.000, within(EPS));
            assertThat(rendered.fragments().get(row * 2 + 1).x())
                    .as("row %d contentX", row).isEqualTo(25.175, within(EPS));
        }
    }

    // --- and it is the marker column, so everything else holds --------------

    @Test
    void everyLineOfAWrappedItemHangsUnderTheTextAndNotUnderTheDisc() throws Exception {
        Rendered rendered = render(220, 300, l -> l
                .textStyle(BODY)
                .marker(m -> m.dot(3.238, ACCENT))
                .hangingIndent(true)
                .markerGap(9.175)
                .items(LONG));

        PlacedFragment content = rendered.fragments().get(1);
        assertThat(content.x()).isEqualTo(12.000 + 3.238 + 9.175, within(EPS));
        assertThat(texts(content)).as("it wraps").hasSizeGreaterThanOrEqualTo(3);
        assertThat(lineWidths(content))
                .allSatisfy(w -> assertThat(w).isLessThanOrEqualTo(content.width() + EPS));

        // The claim the workaround cannot make: one origin for every line.
        assertThat(rendered.glyphs())
                .allSatisfy(g -> assertThat(g.x()).isEqualTo(24.413, within(EPS)));
    }

    @Test
    void aDiscIsDrawnOnceWhenItsItemCrossesPages() throws Exception {
        Rendered rendered = render(150, 70, l -> l
                .textStyle(BODY)
                .marker(m -> m.dot(4.0, ACCENT))
                .hangingIndent(true)
                .markerGap(4.0)
                .items(LONG + " " + LONG));

        assertThat(rendered.graph().totalPages()).isGreaterThanOrEqualTo(2);
        assertThat(rendered.fragments().stream().filter(f -> !spans(f).isEmpty()
                && spans(f).get(0) instanceof ParagraphShapeSpan))
                .as("the disc belongs to the item, not to each page of it")
                .hasSize(1);

        for (int page = 0; page < rendered.graph().totalPages(); page++) {
            int onPage = page;
            PlacedFragment content = rendered.fragments().stream()
                    .filter(f -> f.pageIndex() == onPage)
                    .filter(f -> !texts(f).equals(List.of("")))
                    .findFirst()
                    .orElseThrow();
            assertThat(content.x()).as("page %d contentX", page).isEqualTo(20.000, within(EPS));
        }
    }

    @Test
    void aDrawnMarkerNestsTheWayATypedOneDoes() throws Exception {
        Rendered rendered = render(220, 240, l -> l
                .textStyle(BODY)
                .hangingIndent(true)
                .markerGap(4.0)
                .marker(m -> m.dot(4.0, ACCENT))
                .markerFor(0, ListMarker.ofRuns(
                        com.demcha.compose.document.dsl.RichText.empty().dot(4.0, ACCENT).runs()))
                .markerFor(1, ListMarker.custom("-"))
                .addItem("Top", c -> c.addItem("Child")));

        List<PlacedFragment> fragments = rendered.fragments();
        assertThat(fragments).hasSize(4);
        assertThat(fragments.get(0).x()).isEqualTo(12.000, within(EPS));
        assertThat(fragments.get(1).x()).isEqualTo(20.000, within(EPS));
        assertThat(fragments.get(2).x())
                .as("the child's marker starts at the parent's text, disc or no disc")
                .isEqualTo(fragments.get(1).x(), within(EPS));
        assertThat(spans(fragments.get(0)).get(0)).isInstanceOf(ParagraphShapeSpan.class);
        assertThat(texts(fragments.get(2))).containsExactly("-");
    }

    @Test
    void aDrawnMarkerDoesNotMakeItsRowTaller() throws Exception {
        // The marker rides the item's first baseline, which is what keeps a
        // list's rows evenly pitched whatever their markers are.
        double withDisc = listNode(render(200, 240, l -> l.textStyle(BODY)
                .marker(m -> m.dot(4.0, ACCENT)).hangingIndent(true).items("Item")).graph())
                .placementHeight();
        double withBullet = listNode(render(200, 240, l -> l.textStyle(BODY)
                .bullet().hangingIndent(true).items("Item")).graph())
                .placementHeight();
        assertThat(withDisc).isEqualTo(withBullet, within(EPS));
    }

    @Test
    void aDiscMarkerReachesThePageAndNotJustTheGeometry() throws Exception {
        // Everything else here measures where the marker is. This asks whether it
        // is there at all: a drawn marker contributes no glyphs, so no text-layer
        // or span assertion can tell a painted disc from a described one.
        long withDisc = inkedPixels(pdf(200, 120, l -> l
                .textStyle(BODY)
                .marker(m -> m.dot(6.0, ACCENT))
                .hangingIndent(true)
                .markerGap(4.0)
                .items("Alpha", "Beta", "Gamma")));
        long withoutMarker = inkedPixels(pdf(200, 120, l -> l
                .textStyle(BODY)
                .noMarker()
                .hangingIndent(true)
                .items("Alpha", "Beta", "Gamma")));

        assertThat(withDisc)
                .as("three 6pt discs of ink the markerless list does not have")
                .isGreaterThan(withoutMarker + 3 * 20);
    }

    // --- refusals -----------------------------------------------------------

    @Test
    void aDrawnMarkerWithoutTheOptInSaysSoInsteadOfVanishing() throws Exception {
        // Its plain reading is empty, so the legacy layout would render the list
        // with no marker at all and no signal.
        assertThatThrownBy(() -> render(200, 240, l -> l
                .textStyle(BODY)
                .marker(m -> m.dot(4.0, ACCENT))
                .items("Patient assessment")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hangingIndent(true)")
                .hasMessageContaining("not a drawing");
    }

    @Test
    void aPerItemDrawnMarkerWithoutTheOptInSaysSoToo() throws Exception {
        assertThatThrownBy(() -> render(200, 240, l -> l
                .textStyle(BODY)
                .addItem("Top", c -> c.addItem("Child"))
                .markerFor(1, ListMarker.ofRuns(
                        com.demcha.compose.document.dsl.RichText.empty().dot(4.0, ACCENT).runs()))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hangingIndent(true)");
    }

    // ------------------------------------------------------------------

    private record Rendered(LayoutGraph graph, List<Glyph> glyphs) {
        List<PlacedFragment> fragments() {
            return graph.fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .toList();
        }
    }

    private record Glyph(int page, double x, String text) {
    }

    private static Rendered render(double width, double height, Consumer<ListBuilder> spec)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Root").addList(spec).build();
            return new Rendered(session.layoutGraph(), glyphs(session.toPdfBytes()));
        }
    }

    private static byte[] pdf(double width, double height, Consumer<ListBuilder> spec)
            throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Root").addList(spec).build();
            return session.toPdfBytes();
        }
    }

    /** Pixels darker than paper on page 0, rendered at 72 dpi. */
    private static long inkedPixels(byte[] pdf) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            java.awt.image.BufferedImage image =
                    new org.apache.pdfbox.rendering.PDFRenderer(document).renderImage(0, 1.0f);
            long inked = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    int luma = ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3;
                    if (luma < 200) {
                        inked++;
                    }
                }
            }
            return inked;
        }
    }

    private static List<ParagraphSpan> spans(PlacedFragment fragment) {
        return ((ParagraphFragmentPayload) fragment.payload()).lines().stream()
                .map(ParagraphLine::spans)
                .flatMap(List::stream)
                .toList();
    }

    private static List<String> texts(PlacedFragment fragment) {
        return ((ParagraphFragmentPayload) fragment.payload()).lines().stream()
                .map(ParagraphLine::text)
                .toList();
    }

    private static List<Double> lineWidths(PlacedFragment fragment) {
        return ((ParagraphFragmentPayload) fragment.payload()).lines().stream()
                .map(ParagraphLine::width)
                .toList();
    }

    private static com.demcha.compose.document.layout.PlacedNode listNode(LayoutGraph graph) {
        return graph.nodes().stream()
                .filter(n -> "ListNode".equals(n.nodeKind()))
                .findFirst()
                .orElseThrow();
    }

    private static List<Glyph> glyphs(byte[] pdf) throws Exception {
        List<Glyph> rows = new java.util.ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper() {
                @Override
                protected void writeString(String text,
                                           List<org.apache.pdfbox.text.TextPosition> positions) {
                    if (positions.isEmpty()) {
                        return;
                    }
                    rows.add(new Glyph(getCurrentPageNo() - 1, positions.get(0).getXDirAdj(), text));
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
        }
        return rows;
    }

    private static final SvgIcon TICK = SvgIcon.parse(
            "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'>"
            + "<path d='M2 8 L6 12 L14 3' stroke='#008888' stroke-width='2' fill='none'/></svg>");
}
