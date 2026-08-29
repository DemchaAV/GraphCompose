package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * End-to-end coverage for the inline highlight "chip": the layout reserves the
 * chip's horizontal padding, and the PDF render paints a rounded fill behind the
 * glyphs without dropping the text.
 */
class InlineHighlightRenderTest {

    private static final DocumentInsets PAD = DocumentInsets.symmetric(1.0, 4.0);
    private static final DocumentTextStyle MONO =
            DocumentTextStyle.builder().fontName(FontName.COURIER).size(11).build();
    /** Vivid yellow — distinct from black text on a white page. */
    private static final DocumentColor FILL = DocumentColor.rgb(255, 235, 59);

    @Test
    void chipSpanReservesHorizontalPaddingInItsWidth() throws Exception {
        List<ParagraphTextSpan> plain = textSpans(p -> p.inlineText("ABCDEF", MONO));
        List<ParagraphTextSpan> chip = textSpans(p -> p.inlineHighlight("ABCDEF", MONO, DocumentColor.GRAY, 3.0, PAD));
        assertThat(plain).hasSize(1);
        assertThat(chip).hasSize(1);
        assertThat(plain.get(0).background()).isNull();
        assertThat(chip.get(0).background()).isNotNull();
        assertThat(chip.get(0).width())
                .as("chip width = glyph width + horizontal padding")
                .isCloseTo(plain.get(0).width() + PAD.horizontal(), within(0.5));
    }

    @Test
    void chipFillPaintsBehindTheGlyphsAndKeepsTheText() throws Exception {
        byte[] pdf = render(p -> p.inlineText("Status ").inlineHighlight("OK", MONO, FILL, 3.0, PAD));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Status").contains("OK").doesNotContain("?");
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 255, 235, 59, 40))
                    .as("the chip fill must paint behind the glyphs")
                    .isTrue();
        }
    }

    @Test
    void hugeCornerRadiusClampsInsteadOfThrowing() throws Exception {
        byte[] pdf = render(p -> p.inlineHighlight("X", MONO, FILL, 999.0, PAD));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 255, 235, 59, 40)).isTrue();
        }
    }

    @Test
    void linkedChipEmitsAClickableAnnotationAndKeepsText() throws Exception {
        byte[] pdf = render(p -> p.inlineText("See ").inlineHighlight(
                "docs", MONO, FILL, 3.0, PAD, new DocumentLinkOptions("https://example.com")));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            boolean hasLink = document.getPage(0).getAnnotations().stream()
                    .anyMatch(PDAnnotationLink.class::isInstance);
            assertThat(hasLink).as("a linked chip emits a clickable annotation").isTrue();
            assertThat(new PDFTextStripper().getText(document)).contains("docs").doesNotContain("?");
        }
    }

    @Test
    void inlineCodeRendersOnAChipWithoutGlyphSubstitution() throws Exception {
        List<ParagraphTextSpan> spans = textSpans(p -> p.inlineText("Pkg ").inlineCode("io.github.demchaav"));
        assertThat(spans.stream().filter(s -> s.background() != null).count())
                .as("inline code is exactly one chip span")
                .isEqualTo(1);
        byte[] pdf = render(p -> p.inlineText("Pkg ").inlineCode("io.github.demchaav"));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("io.github.demchaav").doesNotContain("?");
        }
    }

    @Test
    void chipFirstOnLineWithLeadingSpaceKeepsItsBackground() throws Exception {
        // Regression: a leading-space chip as the FIRST run on a line must not be
        // rebuilt by the whitespace trimmer (which would null its background and
        // render it as plain text). The `chip(" Paid ", ...)` badge idiom hits this.
        List<ParagraphTextSpan> spans = textSpans(p -> p.inlineChip(" Paid ", DocumentColor.rgb(22, 101, 52), FILL));
        ParagraphTextSpan chip = spans.stream().filter(s -> s.background() != null).findFirst()
                .orElseThrow(() -> new AssertionError("leading-space chip lost its background: " + spans));
        assertThat(chip.text()).contains("Paid");
        byte[] pdf = render(p -> p.inlineChip(" Paid ", DocumentColor.rgb(22, 101, 52), FILL));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 255, 235, 59, 40))
                    .as("a first-on-line leading-space chip must still paint its fill")
                    .isTrue();
        }
    }

    @Test
    void chipMidLineKeepsTheTextOnBothSides() throws Exception {
        // Text before AND after the chip: the chip's isolated text-block / cursorX
        // advance must let the trailing plain span resume correctly.
        byte[] pdf = render(p -> p.inlineText("a ").inlineHighlight("CHIP", MONO, FILL, 3.0, PAD).inlineText(" b"));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
            assertThat(text).isEqualTo("a CHIP b");
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            assertThat(containsColorNear(image, 255, 235, 59, 40)).isTrue();
        }
    }

    @Test
    void multiWordChipStaysOneSpanAndCollapsesNewlines() throws Exception {
        // Invariant: a chip is one atomic token — a multi-word chip is a
        // single span (not split per word), and an embedded newline collapses to
        // a space (unlike a plain text run, which would split into lines).
        List<ParagraphTextSpan> badge = textSpans(p -> p.inlineChip(" On hold ", DocumentColor.rgb(146, 64, 14), FILL));
        assertThat(badge.stream().filter(s -> s.background() != null).count())
                .as("a multi-word chip is one span, not split per word")
                .isEqualTo(1);
        assertThat(badge.stream().filter(s -> s.background() != null).findFirst().orElseThrow().text())
                .contains("On hold");

        List<ParagraphTextSpan> code = textSpans(p -> p.inlineCode("a\nb"));
        assertThat(code.stream().filter(s -> s.background() != null).findFirst().orElseThrow().text())
                .isEqualTo("a b")
                .doesNotContain("\n");
    }

    @Test
    void overWideChipBreaksWithinColumnWithoutThrowing() throws Exception {
        // A single-word chip wider than the column breaks within it (at soft seams,
        // char-split as a last resort): every visual fragment stays inside the inner
        // width, keeps its rounded fill, and the coordinate survives end-to-end.
        double innerWidth = 90 - 20;
        List<ParagraphLine> lines;
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(90, 140).margin(10, 10, 10, 10).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineCode("io.github.demchaav.a.very.long.package.name")).build();
            lines = paragraphLines(session.layoutGraph());
            pdf = session.toPdfBytes();
        }
        List<ParagraphLine> chipLines = lines.stream()
                .filter(l -> l.spans().stream().anyMatch(s -> s instanceof ParagraphTextSpan ts && ts.background() != null))
                .toList();
        assertThat(chipLines).as("the over-wide chip breaks to >= 2 fragments").hasSizeGreaterThanOrEqualTo(2);
        for (ParagraphLine line : chipLines) {
            ParagraphTextSpan fragment = chipSpan(line);
            assertThat(fragment.width()).as("each fragment stays inside the inner width")
                    .isLessThanOrEqualTo(innerWidth + 0.5);
            assertThat(fragment.background()).as("each fragment keeps its fill").isNotNull();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", "");
            assertThat(text).contains("io.github.demchaav.a.very.long.package.name").doesNotContain("?");
        }
    }

    @Test
    void multiWordChipWrapsToOneCoalescedFillPerLineFragment() throws Exception {
        // A multi-word chip too wide for the column wraps; each visual line carries
        // exactly ONE coalesced chip span (one rounded fill per fragment, not per
        // word), and a continuation fragment opens on its inner edge (no lead pad).
        List<ParagraphLine> lines;
        try (DocumentSession session = GraphCompose.document().pageSize(130, 220).margin(12, 12, 12, 12).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineHighlight("alpha beta gamma delta epsilon", MONO, FILL, 3.0, PAD))
                    .build();
            lines = paragraphLines(session.layoutGraph());
        }
        List<ParagraphLine> chipLines = lines.stream()
                .filter(l -> l.spans().stream().anyMatch(s -> s instanceof ParagraphTextSpan ts && ts.background() != null))
                .toList();
        assertThat(chipLines).as("the multi-word chip wraps to >=2 line fragments").hasSizeGreaterThanOrEqualTo(2);
        for (ParagraphLine line : chipLines) {
            long chipSpans = line.spans().stream()
                    .filter(s -> s instanceof ParagraphTextSpan ts && ts.background() != null).count();
            assertThat(chipSpans).as("each fragment is one coalesced span, not split per word").isEqualTo(1);
        }
        ParagraphTextSpan first = chipSpan(chipLines.get(0));
        ParagraphTextSpan continuation = chipSpan(chipLines.get(1));
        ParagraphTextSpan last = chipSpan(chipLines.get(chipLines.size() - 1));
        assertThat(first.background().padding().left()).as("first fragment keeps the lead pad").isGreaterThan(0.0);
        assertThat(continuation.background().padding().left())
                .as("continuation fragment is open on the inner edge (slice)").isEqualTo(0.0);
        assertThat(last.background().padding().right()).as("final fragment keeps the trail pad (closed-right)")
                .isGreaterThan(0.0);
    }

    @Test
    void wrappedChipPaintsAcrossBothFragments() throws Exception {
        // The same chip on a wide column (one line) vs a narrow one (wrapped): the
        // wrapped fill spans more vertical extent — proof the fill paints on every
        // fragment, not just the first. (The fragments' fills overlap vertically via
        // the vertical padding, like a browser highlight, so they read as one band.)
        String text = "alpha beta gamma delta epsilon";
        int single = fillYExtent(renderHighlightAt(420, text), 255, 235, 59, 40);
        int wrapped = fillYExtent(renderHighlightAt(130, text), 255, 235, 59, 40);
        assertThat(single).as("the single-line chip paints").isGreaterThan(0);
        assertThat(wrapped).as("the wrapped chip fill spans an extra line vs the single-line chip")
                .isGreaterThan(single + 15);
    }

    private static ParagraphTextSpan chipSpan(ParagraphLine line) {
        return (ParagraphTextSpan) line.spans().stream()
                .filter(s -> s instanceof ParagraphTextSpan ts && ts.background() != null)
                .findFirst().orElseThrow();
    }

    private static List<ParagraphLine> paragraphLines(LayoutGraph graph) {
        return graph.fragments().stream()
                .map(PlacedFragment::payload)
                .filter(ParagraphFragmentPayload.class::isInstance)
                .map(ParagraphFragmentPayload.class::cast)
                .flatMap(payload -> payload.lines().stream())
                .toList();
    }

    private static BufferedImage renderHighlightAt(double pageWidth, String text) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(pageWidth, 220).margin(12, 12, 12, 12).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineHighlight(text, MONO, FILL, 3.0, PAD))
                    .build();
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(0, 144);
        }
    }

    /** Vertical extent (maxY - minY, px) of pixels matching the colour; 0 if none present. */
    private static int fillYExtent(BufferedImage image, int r, int g, int b, int tolerance) {
        int minY = Integer.MAX_VALUE;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (Math.abs(((rgb >> 16) & 0xFF) - r) <= tolerance
                        && Math.abs(((rgb >> 8) & 0xFF) - g) <= tolerance
                        && Math.abs((rgb & 0xFF) - b) <= tolerance) {
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    break;
                }
            }
        }
        return maxY < 0 ? 0 : maxY - minY;
    }

    @Test
    void wrappedChipHasNoSoftWrapSeamSpaceAtAnyWidth() throws Exception {
        // Across a sweep of column widths (so the break lands at many different
        // points), no chip fragment may begin or end with a soft-wrap space: a
        // fragment leads/trails with a space only where it carries the authored
        // outer pad (leftPad/rightPad > 0). Guards both the leading and trailing
        // seam-collapse.
        String text = "alpha beta gamma delta epsilon zeta eta theta";
        for (int width = 78; width <= 220; width += 6) {
            int w = width;
            List<ParagraphLine> lines;
            try (DocumentSession session = GraphCompose.document().pageSize(w, 300).margin(10, 10, 10, 10).create()) {
                session.dsl().pageFlow().name("Flow")
                        .addParagraph(p -> p.inlineHighlight(text, MONO, FILL, 3.0, PAD)).build();
                lines = paragraphLines(session.layoutGraph());
            }
            lines.stream()
                    .flatMap(l -> l.spans().stream())
                    .filter(s -> s instanceof ParagraphTextSpan ts && ts.background() != null)
                    .map(ParagraphTextSpan.class::cast)
                    .forEach(f -> {
                        if (f.background().padding().left() == 0.0) {
                            assertThat(f.text()).as("no leading soft-wrap space at width " + w).doesNotStartWith(" ");
                        }
                        if (f.background().padding().right() == 0.0) {
                            assertThat(f.text()).as("no trailing soft-wrap space at width " + w).doesNotEndWith(" ");
                        }
                    });
        }
    }

    @Test
    void wrappedChipMidParagraphKeepsItsFullTextAcrossTheBreak() throws Exception {
        // A chip with plain text on BOTH sides that wraps: every chip word survives
        // in order (not dropped or duplicated at the wrap seam), and the surrounding
        // text is intact on both sides.
        String chip = "alpha beta gamma delta epsilon";
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(130, 220).margin(12, 12, 12, 12).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineText("Tags: ").inlineHighlight(chip, MONO, FILL, 3.0, PAD).inlineText(" end"))
                    .build();
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String rendered = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
            assertThat(rendered).isEqualTo("Tags: alpha beta gamma delta epsilon end");
        }
    }

    @Test
    void chipFollowsTheParagraphSizeInsteadOfTheDefault() throws Exception {
        // A chip is glyphs on a fill, so it has to be sized like the glyphs around
        // it: in a 9 pt paragraph the badge measures 9 pt, not the 14 pt default.
        DocumentTextStyle small = DocumentTextStyle.builder().size(9).build();
        List<ParagraphTextSpan> spans = textSpans(p -> p
                .textStyle(small)
                .inlineText("Build ")
                .inlineChip(" Paid ", DocumentColor.rgb(22, 101, 52), FILL));

        ParagraphTextSpan plain = spans.stream().filter(s -> s.background() == null).findFirst().orElseThrow();
        ParagraphTextSpan chip = spans.stream().filter(s -> s.background() != null).findFirst().orElseThrow();
        assertThat(plain.textStyle().size()).isEqualTo(9.0, within(1e-9));
        assertThat(chip.textStyle().size())
                .as("the chip is drawn at the paragraph size, not the 14 pt default")
                .isEqualTo(9.0, within(1e-9));
        assertThat(chip.textStyle().color()).isEqualTo(new Color(22, 101, 52));

        // Measured, not just declared: the same chip in a default-styled paragraph
        // is wider, because its glyphs really are bigger.
        List<ParagraphTextSpan> defaultSized = textSpans(p -> p
                .inlineChip(" Paid ", DocumentColor.rgb(22, 101, 52), FILL));
        double wide = defaultSized.stream().filter(s -> s.background() != null).findFirst().orElseThrow().width();
        assertThat(chip.width()).as("a 9 pt chip measures narrower than a 14 pt one").isLessThan(wide);
    }

    @Test
    void explicitlyStyledChipKeepsItsOwnSize() throws Exception {
        List<ParagraphTextSpan> spans = textSpans(p -> p
                .textStyle(DocumentTextStyle.builder().size(9).build())
                .inlineStyledChip("BIG", DocumentTextStyle.builder().size(18).build(), FILL));
        ParagraphTextSpan chip = spans.stream().filter(s -> s.background() != null).findFirst().orElseThrow();
        assertThat(chip.textStyle().size())
                .as("an explicit chip style overrides the paragraph, both ways")
                .isEqualTo(18.0, within(1e-9));
    }

    @Test
    void twoAdjacentDifferentChipsStaySeparateSpans() throws Exception {
        List<ParagraphTextSpan> spans = textSpans(p -> p
                .inlineChip("A", DocumentColor.rgb(0, 100, 0), DocumentColor.rgb(220, 255, 220))
                .inlineChip("B", DocumentColor.rgb(100, 0, 0), DocumentColor.rgb(255, 220, 220)));
        assertThat(spans.stream().filter(s -> s.background() != null).count())
                .as("two distinct chips do not coalesce into one fill")
                .isEqualTo(2);
    }

    @Test
    void wrappedLinkedChipEmitsAClickableRectPerFragment() throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(130, 220).margin(12, 12, 12, 12).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineHighlight("alpha beta gamma delta epsilon", MONO, FILL, 3.0, PAD,
                            new DocumentLinkOptions("https://example.com")))
                    .build();
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            long links = document.getPage(0).getAnnotations().stream()
                    .filter(PDAnnotationLink.class::isInstance).count();
            assertThat(links).as("a wrapped linked chip is clickable on each visual line fragment")
                    .isGreaterThanOrEqualTo(2);
            assertThat(new PDFTextStripper().getText(document)).contains("alpha").contains("epsilon").doesNotContain("?");
        }
    }

    @Test
    void overWideCodeChipBreaksAtSoftSeams() throws Exception {
        // A coordinate breaks at its . : / - joiners, not mid-identifier: at least
        // one non-final fragment ends with a soft-break character.
        List<ParagraphLine> lines;
        try (DocumentSession session = GraphCompose.document().pageSize(120, 200).margin(10, 10, 10, 10).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineCode("org.junit.jupiter:junit-jupiter:5.10.2")).build();
            lines = paragraphLines(session.layoutGraph());
        }
        List<ParagraphLine> chipLines = lines.stream()
                .filter(l -> l.spans().stream().anyMatch(s -> s instanceof ParagraphTextSpan ts && ts.background() != null))
                .toList();
        assertThat(chipLines).hasSizeGreaterThanOrEqualTo(2);
        boolean seamBreak = chipLines.subList(0, chipLines.size() - 1).stream()
                .map(l -> chipSpan(l).text())
                .anyMatch(t -> !t.isEmpty() && ".:/-".indexOf(t.charAt(t.length() - 1)) >= 0);
        assertThat(seamBreak).as("the coordinate breaks at a . : / - seam, not mid-identifier").isTrue();
    }

    @Test
    void overWidePlainTokenBreaksWithinColumn() throws Exception {
        // The plain wrap path (no inline runs, no markdown): a lone over-wide first
        // token char-splits within the column instead of overflowing it.
        double innerWidth = 90 - 20;
        String token = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        List<ParagraphLine> lines;
        try (DocumentSession session = GraphCompose.document().pageSize(90, 160).margin(10, 10, 10, 10).create()) {
            session.dsl().pageFlow().name("Flow").addParagraph(p -> p.text(token)).build();
            lines = paragraphLines(session.layoutGraph());
        }
        assertThat(lines).as("the over-wide plain token breaks to >= 2 lines").hasSizeGreaterThanOrEqualTo(2);
        for (ParagraphLine line : lines) {
            assertThat(line.width()).as("each line stays inside the inner width").isLessThanOrEqualTo(innerWidth + 0.5);
        }
        String joined = lines.stream().flatMap(l -> l.spans().stream())
                .filter(ParagraphTextSpan.class::isInstance).map(s -> ((ParagraphTextSpan) s).text())
                .reduce("", String::concat).replaceAll("\\s+", "");
        assertThat(joined).isEqualTo(token);
    }

    @Test
    void overWideTokenInMarkdownBreaksWithinColumn() throws Exception {
        // The markdown wrap path (markdown enabled + markdown syntax present): a lone
        // over-wide token still breaks within the column.
        double innerWidth = 90 - 20;
        String token = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        List<ParagraphLine> lines;
        try (DocumentSession session = GraphCompose.document().pageSize(90, 160).margin(10, 10, 10, 10).create()) {
            session.markdown(true);
            session.dsl().pageFlow().name("Flow").addParagraph(p -> p.text("**b** " + token)).build();
            lines = paragraphLines(session.layoutGraph());
        }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
        for (ParagraphLine line : lines) {
            assertThat(line.width()).as("each line stays inside the inner width").isLessThanOrEqualTo(innerWidth + 0.5);
        }
        String joined = lines.stream().flatMap(l -> l.spans().stream())
                .filter(ParagraphTextSpan.class::isInstance).map(s -> ((ParagraphTextSpan) s).text())
                .reduce("", String::concat).replaceAll("\\s+", "");
        assertThat(joined).as("the long token survives the break").contains(token);
    }

    @Test
    void nonFirstOverWideChipBreaksWithinColumn() throws Exception {
        // The over-wide chip is NOT the first token: leading prose is flushed, then
        // the chip breaks on the following lines (it still fits none of them whole).
        double innerWidth = 120 - 20;
        List<ParagraphLine> lines;
        try (DocumentSession session = GraphCompose.document().pageSize(120, 200).margin(10, 10, 10, 10).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineText("Use ").inlineCode("io.github.demchaav.a.very.long.package.name"))
                    .build();
            lines = paragraphLines(session.layoutGraph());
        }
        List<ParagraphLine> chipLines = lines.stream()
                .filter(l -> l.spans().stream().anyMatch(s -> s instanceof ParagraphTextSpan ts && ts.background() != null))
                .toList();
        assertThat(chipLines).as("the trailing chip breaks to >= 2 fragments").hasSizeGreaterThanOrEqualTo(2);
        for (ParagraphLine line : chipLines) {
            assertThat(chipSpan(line).width()).isLessThanOrEqualTo(innerWidth + 0.5);
        }
    }

    @Test
    void overWideLinkedChipEmitsAClickableRectPerFragment() throws Exception {
        // Char-split (single-word) linked chip: each visual fragment is independently
        // clickable, mirroring the multi-word wrapped-chip guarantee.
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(120, 200).margin(10, 10, 10, 10).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineHighlight(
                            "io.github.demchaav.a.very.long.package.name", MONO, FILL, 3.0, PAD,
                            new DocumentLinkOptions("https://example.com")))
                    .build();
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            long links = document.getPage(0).getAnnotations().stream()
                    .filter(PDAnnotationLink.class::isInstance).count();
            assertThat(links).as("a char-split linked chip is clickable on each fragment")
                    .isGreaterThanOrEqualTo(2);
        }
    }

    private static byte[] render(Consumer<ParagraphBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(320, 140).margin(16, 16, 16, 16).create()) {
            session.dsl().pageFlow().name("Flow").addParagraph(body::accept).build();
            return session.toPdfBytes();
        }
    }

    private static List<ParagraphTextSpan> textSpans(Consumer<ParagraphBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(320, 140).margin(16, 16, 16, 16).create()) {
            session.dsl().pageFlow().name("Flow").addParagraph(body::accept).build();
            LayoutGraph graph = session.layoutGraph();
            return graph.fragments().stream()
                    .map(PlacedFragment::payload)
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .flatMap(payload -> payload.lines().stream())
                    .flatMap(line -> line.spans().stream())
                    .filter(ParagraphTextSpan.class::isInstance)
                    .map(ParagraphTextSpan.class::cast)
                    .toList();
        }
    }

    private static boolean containsColorNear(BufferedImage image, int r, int g, int b, int tolerance) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (Math.abs(((rgb >> 16) & 0xFF) - r) <= tolerance
                        && Math.abs(((rgb >> 8) & 0xFF) - g) <= tolerance
                        && Math.abs((rgb & 0xFF) - b) <= tolerance) {
                    return true;
                }
            }
        }
        return false;
    }
}
