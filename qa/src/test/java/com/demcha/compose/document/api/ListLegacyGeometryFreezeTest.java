package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Freezes the geometry of the legacy (prefix-based) semantic list before any
 * hanging-indent or marker-gap work exists, so that "unchanged" can later be
 * shown as a diff instead of asserted as a claim.
 *
 * <p>Recorded on the exact tip of {@code origin/develop} this branch was cut
 * from, so the golden dump <em>is</em> develop's behaviour: a later branch that
 * re-runs {@link #legacyGeometryStillMatchesTheRecordedDump()} is doing a
 * cross-branch comparison without needing a second checkout.</p>
 *
 * <h2>Why these instruments</h2>
 * <ul>
 *   <li><b>Fragment/line geometry</b> proves marker position, wrap width,
 *       continuation indent, nesting, pagination, padding and margin. It is the
 *       primary instrument because every one of those is observable in the
 *       layout graph.</li>
 *   <li><b>PDF first-visible-glyph x</b> proves where text actually lands on the
 *       page. It is needed because legacy indentation is <em>space glyphs inside
 *       the line string</em>, not an x offset: every line of a LEFT-aligned list
 *       is drawn from the same {@code lineX}, so fragment x alone cannot show
 *       that continuation lines are inset.</li>
 *   <li><b>Layout snapshots</b> are deliberately <em>not</em> extended here.
 *       A snapshot records nodes only — no fragments and no line text — so it
 *       cannot see marker spacing, continuation indent or wrapped-line
 *       behaviour. The two that exist ({@code document/list_markers},
 *       {@code document/nested_list_three_levels}) already freeze node-box shape
 *       and stay as they are.</li>
 *   <li><b>Pixel baselines</b> are deliberately not created. Nothing frozen here
 *       is a visual-only property; geometry and glyph x prove all of it.</li>
 * </ul>
 */
class ListLegacyGeometryFreezeTest {

    private static final String DUMP_RESOURCE = "list-legacy/legacy-list-geometry.txt";
    private static final String UPDATE_PROPERTY = "graphcompose.updateSnapshots";

    /** Every frozen fixture, in dump order. */
    private static Map<String, Fixture> fixtures() {
        Map<String, Fixture> all = new LinkedHashMap<>();
        all.put("bullet-short", new Fixture(320, 240, 12,
                l -> l.name("L").bullet().items("Java", "SQL")));
        all.put("dash-short", new Fixture(320, 240, 12,
                l -> l.name("L").dash().items("Java", "SQL")));
        all.put("custom-arrow", new Fixture(320, 240, 12,
                l -> l.name("L").marker(">").items("Java")));
        all.put("custom-wide", new Fixture(320, 240, 12,
                l -> l.name("L").marker("=>").items("Java")));
        all.put("custom-mmm", new Fixture(320, 240, 12,
                l -> l.name("L").marker("MMM").items("Java")));
        all.put("markerless", new Fixture(320, 240, 12,
                l -> l.name("L").noMarker().items("Java")));
        all.put("markerless-continuation", new Fixture(165, 240, 12,
                l -> l.name("L").noMarker().continuationIndent("    ")
                        .items("Long item text should wrap across several visual lines here.")));
        all.put("bullet-wrapped", new Fixture(165, 240, 12,
                l -> l.name("L").bullet().items(
                        "Long item text should wrap across several visual lines while keeping one bullet.")));
        all.put("dash-wrapped", new Fixture(165, 240, 12,
                l -> l.name("L").dash().items(
                        "Long item text should wrap across several visual lines while keeping one dash.")));
        all.put("wide-marker-wrapped", new Fixture(165, 240, 12,
                l -> l.name("L").marker("MMM").items(
                        "Long item text should wrap across several visual lines while keeping one marker.")));
        all.put("padding", new Fixture(320, 240, 12,
                l -> l.name("L").bullet().padding(3, 5, 7, 11).items("Java", "SQL")));
        all.put("margin", new Fixture(320, 240, 12,
                l -> l.name("L").bullet().margin(3, 5, 7, 11).items("Java", "SQL")));
        all.put("narrow", new Fixture(90, 240, 6,
                l -> l.name("L").bullet().items("Wrapping in a very narrow container indeed")));
        all.put("item-spacing", new Fixture(320, 240, 12,
                l -> l.name("L").bullet().itemSpacing(6).items("Java", "SQL")));
        all.put("nested", new Fixture(320, 240, 12,
                l -> l.name("L").bullet()
                        .addItem("Top one", c -> c
                                .addItem("Child one")
                                .addItem("Child two", g -> g.addItem("Grandchild")))
                        .addItem("Top two")));
        all.put("nested-wrapped", new Fixture(165, 240, 12,
                l -> l.name("L").bullet()
                        .addItem("Top", c -> c
                                .addItem("Child item text that should wrap across several visual lines here."))));
        all.put("pagination-split-item", new Fixture(165, 90, 12,
                l -> l.name("L").bullet().items(
                        "Long item text should wrap across many visual lines so that it has to cross a page "
                        + "boundary and continue on the following page with its continuation indent intact.")));
        all.put("pagination-whole-items", new Fixture(165, 90, 12,
                l -> l.name("L").bullet()
                        .items("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight")));
        return all;
    }

    // ------------------------------------------------------------------
    // The broad, diffable freeze
    // ------------------------------------------------------------------

    @Test
    void legacyGeometryStillMatchesTheRecordedDump() throws Exception {
        StringBuilder actual = new StringBuilder();
        for (Map.Entry<String, Fixture> entry : fixtures().entrySet()) {
            actual.append(dump(entry.getKey(), entry.getValue()));
        }

        Path resource = Path.of("src", "test", "resources", DUMP_RESOURCE);
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.createDirectories(resource.getParent());
            Files.writeString(resource, actual.toString(), StandardCharsets.UTF_8);
            return;
        }

        assertThat(resource)
                .as("legacy list geometry dump is recorded; re-record with -D" + UPDATE_PROPERTY + "=true")
                .exists();
        String expected = Files.readString(resource, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(actual.toString())
                .as("legacy list geometry changed; every line here is a compatibility contract")
                .isEqualTo(expected);
    }

    // ------------------------------------------------------------------
    // Named claims — one per class of behaviour the sabotage must break
    // ------------------------------------------------------------------

    @Test
    void markerLivesInTheFirstLineTextAndIsNeverRepeatedOnAContinuationLine() throws Exception {
        List<ParagraphFragmentPayload> payloads = payloads(fixtures().get("bullet-wrapped"));
        List<String> lines = lineTexts(payloads.get(0));

        assertThat(lines.get(0)).startsWith("• ");
        assertThat(lines).hasSizeGreaterThan(1);
        assertThat(lines.subList(1, lines.size()))
                .as("the marker is a first-line prefix, not a per-line decoration")
                .noneMatch(line -> line.contains("•"));
    }

    @Test
    void continuationIndentIsCeilOfTheMarkerWidthMeasuredInSpaces() throws Exception {
        // The legacy formula, stated as the equation it is:
        //   spaces = ceil(width(markerPrefix) / width(" "))
        // Helvetica/1000: bullet 350, dash 333, 'M' 833, space 278.
        assertLeadingSpaces("bullet-wrapped", 3);   // ceil((350+278)/278) = ceil(2.259)
        assertLeadingSpaces("dash-wrapped", 3);     // ceil((333+278)/278) = ceil(2.198)
        assertLeadingSpaces("wide-marker-wrapped", 10); // ceil((3*833+278)/278) = ceil(9.989)
    }

    @Test
    void continuationLinesSitRightOfTheFirstLineContentBecauseTheIndentIsRoundedUp() throws Exception {
        // This is the defect the opt-in geometry is meant to remove. Freezing it
        // keeps the "before" number honest instead of remembered.
        //
        // Compared like with like: where the *text* starts on line 0 (past the
        // marker) against where it starts on the wrapped lines (past the indent).
        // Rounding the indent up to a whole number of spaces overshoots.
        Rendered rendered = render(fixtures().get("bullet-wrapped"));
        List<Double> textX = rendered.textStartX();

        assertThat(textX).hasSizeGreaterThan(1);
        double firstLineText = textX.get(0);
        for (int i = 1; i < textX.size(); i++) {
            assertThat(textX.get(i))
                    .as("continuation line %d overshoots the first line's text start", i)
                    .isGreaterThan(firstLineText);
        }
        // All continuation lines agree with each other — the overshoot is constant.
        assertThat(textX.subList(1, textX.size()))
                .allMatch(x -> Math.abs(x - textX.get(1)) < 0.001);
        assertThat(textX.get(1) - firstLineText)
                .as("measured legacy overshoot for a bullet at the default style")
                .isCloseTo(2.884, within(0.05));
    }

    @Test
    void wrapWidthIsReducedByThePrefixSoAWiderMarkerWrapsSooner() throws Exception {
        List<String> bullet = lineTexts(payloads(fixtures().get("bullet-wrapped")).get(0));
        List<String> wide = lineTexts(payloads(fixtures().get("wide-marker-wrapped")).get(0));

        assertThat(wide.size())
                .as("a 10-space indent leaves less room per line than a 3-space one")
                .isGreaterThan(bullet.size());
    }

    @Test
    void nestedDepthIsTwoNonBreakingSpacesPerLevelAndNestedItemsLoseTheContinuationIndent() throws Exception {
        List<ParagraphFragmentPayload> nested = payloads(fixtures().get("nested"));
        assertThat(nested).as("depth-first flatten, one fragment per item").hasSize(5);

        assertThat(lineTexts(nested.get(0)).get(0)).isEqualTo("• Top one");
        assertThat(lineTexts(nested.get(1)).get(0)).isEqualTo("  ◦ Child one");
        assertThat(lineTexts(nested.get(2)).get(0)).isEqualTo("  ◦ Child two");
        assertThat(lineTexts(nested.get(3)).get(0)).isEqualTo("    ▪ Grandchild");
        assertThat(lineTexts(nested.get(4)).get(0)).isEqualTo("• Top two");

        // The flatten path sets marker = none(), so a nested item's wrapped lines
        // get no indent at all: they fall back to the container's left edge,
        // losing both the depth indent and the marker indent.
        List<String> wrapped = lineTexts(payloads(fixtures().get("nested-wrapped")).get(1));
        assertThat(wrapped).hasSizeGreaterThan(1);
        assertThat(wrapped.get(0)).startsWith("  ◦ ");
        assertThat(wrapped.subList(1, wrapped.size()))
                .as("nested continuation lines are flush left today")
                .allMatch(line -> !line.isEmpty() && !Character.isWhitespace(line.charAt(0)));
    }

    @Test
    void nestedMarkersBelowDepthZeroReachThePdfAsQuestionMarksWithTheDefaultFont() throws Exception {
        // Recorded because it is true today, not because it is right: the layout
        // line carries the real codepoints, but the default Helvetica/WinAnsi font
        // cannot encode U+25E6 or U+25AA, so the render substitutes '?'. The NBSP
        // depth indent degrades to a plain space the same way. Any change here is
        // a deliberate fix, and the freeze should make someone say so.
        Rendered rendered = render(fixtures().get("nested"));

        List<String> layoutLines = payloads(fixtures().get("nested")).stream()
                .map(p -> p.lines().get(0).text())
                .toList();
        assertThat(layoutLines.get(1)).isEqualTo("  ◦ Child one");
        assertThat(layoutLines.get(3)).isEqualTo("    ▪ Grandchild");

        List<String> pdfLines = rendered.rows().stream().map(GlyphRow::text).toList();
        assertThat(pdfLines.get(0)).as("U+2022 is in WinAnsi and survives").startsWith("• ");
        assertThat(pdfLines.get(1)).isEqualTo("  ? Child one");
        assertThat(pdfLines.get(3)).isEqualTo("    ? Grandchild");
    }

    @Test
    void aLongItemSplitsAcrossPagesKeepingTheIndentAndNotRepeatingTheMarker() throws Exception {
        Rendered rendered = render(fixtures().get("pagination-split-item"));
        assertThat(rendered.graph().totalPages()).isEqualTo(2);

        List<ParagraphFragmentPayload> page0 = payloadsOnPage(rendered.graph(), 0);
        List<ParagraphFragmentPayload> page1 = payloadsOnPage(rendered.graph(), 1);
        assertThat(page0).hasSize(1);
        assertThat(page1).hasSize(1);

        assertThat(lineTexts(page0.get(0)).get(0)).startsWith("• ");
        assertThat(lineTexts(page1.get(0)))
                .as("the continuation page keeps the indent and gets no second marker")
                .allMatch(line -> line.startsWith("   ") && !line.contains("•"));
    }

    @Test
    void wholeItemsPaginateOnItemBoundaries() throws Exception {
        Rendered rendered = render(fixtures().get("pagination-whole-items"));
        assertThat(rendered.graph().totalPages()).isEqualTo(2);
        assertThat(payloadsOnPage(rendered.graph(), 0)).hasSize(5);
        assertThat(payloadsOnPage(rendered.graph(), 1)).hasSize(3);
    }

    @Test
    void paddingMovesContentInsideTheBoxAndMarginMovesTheBox() throws Exception {
        Rendered padded = render(fixtures().get("padding"));
        Rendered margined = render(fixtures().get("margin"));

        PlacedFragment paddedFragment = paragraphFragments(padded.graph()).get(0);
        PlacedFragment marginedFragment = paragraphFragments(margined.graph()).get(0);

        assertThat(paddedFragment.x()).as("padding leaves the fragment box where it was").isEqualTo(12.0);
        assertThat(payload(paddedFragment).padding().left()).isEqualTo(11.0);
        assertThat(marginedFragment.x()).as("margin moves the fragment box").isEqualTo(23.0);
        assertThat(payload(marginedFragment).padding().left()).isEqualTo(0.0);

        // Both land the glyphs in the same place — by different routes.
        assertThat(padded.firstVisibleGlyphX().get(0)).isEqualTo(23.0, within(0.001));
        assertThat(margined.firstVisibleGlyphX().get(0)).isEqualTo(23.0, within(0.001));
    }

    @Test
    void aNarrowContainerWrapsWithoutOverflowingItsInnerWidth() throws Exception {
        Rendered rendered = render(fixtures().get("narrow"));
        PlacedNode list = listNode(rendered.graph());
        double innerWidth = 90.0 - 2 * 6.0;

        assertThat(list.placementWidth()).isLessThanOrEqualTo(innerWidth);
        for (ParagraphLine line : payloads(fixtures().get("narrow")).get(0).lines()) {
            assertThat(line.width())
                    .as("no measured line overflows the container")
                    .isLessThanOrEqualTo(innerWidth + 0.001);
        }
    }

    @Test
    void aMarkerlessListIsFlushLeftUnlessContinuationIndentIsSet() throws Exception {
        assertThat(lineTexts(payloads(fixtures().get("markerless")).get(0)).get(0)).isEqualTo("Java");

        List<String> withIndent = lineTexts(payloads(fixtures().get("markerless-continuation")).get(0));
        assertThat(withIndent.get(0)).doesNotStartWith(" ");
        assertThat(withIndent.subList(1, withIndent.size()))
                .as("continuationIndent is passed through verbatim, from the second line")
                .allMatch(line -> line.startsWith("    ") && !line.startsWith("     "));
    }

    @Test
    void markerGlyphWidthAloneMovesTheContentStart() throws Exception {
        // Marker width is measured, so a wider glyph pushes content further right
        // even before any explicit gap exists.
        Rendered arrow = render(fixtures().get("custom-arrow"));
        Rendered wide = render(fixtures().get("custom-wide"));
        Rendered mmm = render(fixtures().get("custom-mmm"));

        // Every marker is drawn from the same left edge, because the marker is
        // content, not geometry — there is no marker column to sit in.
        assertThat(arrow.firstVisibleGlyphX().get(0)).isEqualTo(12.0, within(0.001));
        assertThat(wide.firstVisibleGlyphX().get(0)).isEqualTo(12.0, within(0.001));
        assertThat(mmm.firstVisibleGlyphX().get(0)).isEqualTo(12.0, within(0.001));

        // ...and the text start is pushed right by exactly the marker's own width.
        // Measured on the two non-alphanumeric markers, where "first letter" is
        // unambiguously the item text: ">" 24.068, "=>" 32.244.
        assertThat(arrow.textStartX().get(0)).isEqualTo(24.068, within(0.001));
        assertThat(wide.textStartX().get(0)).isEqualTo(32.244, within(0.001));

        // The item's measured width is what grows with the marker.
        assertThat(listNode(render(fixtures().get("custom-wide")).graph()).placementWidth())
                .isGreaterThan(listNode(render(fixtures().get("custom-arrow")).graph()).placementWidth());
        assertThat(listNode(render(fixtures().get("custom-mmm")).graph()).placementWidth())
                .isGreaterThan(listNode(render(fixtures().get("custom-wide")).graph()).placementWidth());
    }

    // ------------------------------------------------------------------
    // Fixture plumbing
    // ------------------------------------------------------------------

    private record Fixture(double pageWidth, double pageHeight, double margin, Consumer<ListBuilder> spec) {
    }

    private record Rendered(LayoutGraph graph, List<GlyphRow> rows) {
        /** x of the first non-blank glyph on each line — the marker on a marked line. */
        List<Double> firstVisibleGlyphX() {
            return rows.stream().map(GlyphRow::visibleX).toList();
        }

        /**
         * x of the first letter-or-digit glyph on each line — where the item's
         * <em>text</em> actually starts, past any marker and any indent. This is
         * the number the opt-in geometry is meant to make equal across the lines
         * of one item, so it is the one the freeze has to pin.
         *
         * <p><b>Limit:</b> the rule cannot tell a marker from text when the marker
         * is itself alphanumeric (the {@code MMM} fixtures), where it returns the
         * marker's own x. The dump records it anyway — with {@code markerX} and the
         * line text beside it, so the reading is reconstructible — but claims that
         * depend on the text start use a non-alphanumeric marker.</p>
         */
        List<Double> textStartX() {
            return rows.stream().map(GlyphRow::textX).toList();
        }
    }

    private record GlyphRow(int page, double x, double visibleX, double textX, String text) {
    }

    private static Rendered render(Fixture fixture) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(fixture.pageWidth(), fixture.pageHeight())
                .margin(DocumentInsets.of(fixture.margin()))
                .create()) {
            session.pageFlow().name("Root").addList(fixture.spec()).build();
            return new Rendered(session.layoutGraph(), glyphRows(session.toPdfBytes()));
        }
    }

    private static List<ParagraphFragmentPayload> payloads(Fixture fixture) throws Exception {
        return paragraphFragments(render(fixture).graph()).stream()
                .map(ListLegacyGeometryFreezeTest::payload)
                .toList();
    }

    private static void assertLeadingSpaces(String fixtureName, int expected) throws Exception {
        List<String> lines = lineTexts(payloads(fixtures().get(fixtureName)).get(0));
        assertThat(lines).hasSizeGreaterThan(1);
        for (int i = 1; i < lines.size(); i++) {
            assertThat(leadingSpaces(lines.get(i)))
                    .as("%s continuation line %d", fixtureName, i)
                    .isEqualTo(expected);
        }
    }

    private static List<PlacedFragment> paragraphFragments(LayoutGraph graph) {
        return graph.fragments().stream()
                .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                .toList();
    }

    private static List<ParagraphFragmentPayload> payloadsOnPage(LayoutGraph graph, int page) {
        return paragraphFragments(graph).stream()
                .filter(f -> f.pageIndex() == page)
                .map(ListLegacyGeometryFreezeTest::payload)
                .toList();
    }

    private static ParagraphFragmentPayload payload(PlacedFragment fragment) {
        return (ParagraphFragmentPayload) fragment.payload();
    }

    private static PlacedNode listNode(LayoutGraph graph) {
        return graph.nodes().stream()
                .filter(n -> "ListNode".equals(n.nodeKind()))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> lineTexts(ParagraphFragmentPayload payload) {
        return payload.lines().stream().map(ParagraphLine::text).toList();
    }

    private static int leadingSpaces(String text) {
        int n = 0;
        while (n < text.length() && text.charAt(n) == ' ') {
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------
    // Dump rendering
    // ------------------------------------------------------------------

    private static String dump(String label, Fixture fixture) throws Exception {
        Rendered rendered = render(fixture);
        LayoutGraph graph = rendered.graph();
        StringBuilder out = new StringBuilder();
        out.append("### ").append(label)
                .append(" page=").append(fmt(fixture.pageWidth())).append('x').append(fmt(fixture.pageHeight()))
                .append(" margin=").append(fmt(fixture.margin()))
                .append(" pages=").append(graph.totalPages()).append('\n');

        for (PlacedNode node : graph.nodes()) {
            out.append("  node ").append(node.path())
                    .append(" kind=").append(node.nodeKind())
                    .append(" box=").append(box(node.placementX(), node.placementY(),
                            node.placementWidth(), node.placementHeight()))
                    .append(" pages=").append(node.startPage()).append("..").append(node.endPage())
                    .append(" pad=").append(insets(node.padding().top(), node.padding().right(),
                            node.padding().bottom(), node.padding().left()))
                    .append(" margin=").append(insets(node.margin().top(), node.margin().right(),
                            node.margin().bottom(), node.margin().left()))
                    .append('\n');
        }

        for (PlacedFragment fragment : paragraphFragments(graph)) {
            ParagraphFragmentPayload payload = payload(fragment);
            out.append("  frag ").append(fragment.path())
                    .append(" idx=").append(fragment.fragmentIndex())
                    .append(" page=").append(fragment.pageIndex())
                    .append(" box=").append(box(fragment.x(), fragment.y(), fragment.width(), fragment.height()))
                    .append(" pad=").append(insets(payload.padding().top(), payload.padding().right(),
                            payload.padding().bottom(), payload.padding().left()))
                    .append('\n');
            List<ParagraphLine> lines = payload.lines();
            for (int i = 0; i < lines.size(); i++) {
                ParagraphLine line = lines.get(i);
                out.append("    line[").append(i).append(']')
                        .append(" w=").append(fmt(line.width()))
                        .append(" lead=").append(leadingWhitespace(line.text()))
                        .append(" text=").append(escape(line.text()))
                        .append('\n');
            }
        }

        for (GlyphRow row : rendered.rows()) {
            out.append("    pdf page=").append(row.page())
                    .append(" x=").append(fmt(row.x()))
                    .append(" markerX=").append(fmt(row.visibleX()))
                    .append(" textX=").append(fmt(row.textX()))
                    .append(" text=").append(escape(row.text()))
                    .append('\n');
        }
        return out.append('\n').toString();
    }

    private static String box(double x, double y, double w, double h) {
        return "[" + fmt(x) + " " + fmt(y) + " " + fmt(w) + " " + fmt(h) + "]";
    }

    private static String insets(double top, double right, double bottom, double left) {
        return "[" + fmt(top) + " " + fmt(right) + " " + fmt(bottom) + " " + fmt(left) + "]";
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static int leadingWhitespace(String text) {
        int n = 0;
        while (n < text.length() && Character.isWhitespace(text.charAt(n))) {
            n++;
        }
        return n;
    }

    /** ASCII-only rendering so the dump survives any console or file encoding. */
    private static String escape(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                sb.append('_');
            } else if (c >= 0x20 && c < 0x7F && c != '"' && c != '\\') {
                sb.append(c);
            } else {
                sb.append(String.format(Locale.ROOT, "\\u%04X", (int) c));
            }
        }
        return sb.append('"').toString();
    }

    private static List<GlyphRow> glyphRows(byte[] pdf) throws IOException {
        List<GlyphRow> rows = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    if (positions.isEmpty()) {
                        return;
                    }
                    double visibleX = positions.stream()
                            .filter(p -> !p.getUnicode().isBlank())
                            .mapToDouble(TextPosition::getXDirAdj)
                            .findFirst()
                            .orElse(positions.get(0).getXDirAdj());
                    double textX = positions.stream()
                            .filter(p -> !p.getUnicode().isEmpty()
                                         && Character.isLetterOrDigit(p.getUnicode().charAt(0)))
                            .mapToDouble(TextPosition::getXDirAdj)
                            .findFirst()
                            .orElse(visibleX);
                    rows.add(new GlyphRow(getCurrentPageNo() - 1,
                            positions.get(0).getXDirAdj(), visibleX, textX, text));
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
        }
        return rows;
    }
}
