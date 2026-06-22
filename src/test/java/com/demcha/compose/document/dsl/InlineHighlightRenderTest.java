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
        // PR-1 invariant: a chip is one atomic token — a multi-word chip is a
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
    void overWideAtomicChipRendersWithoutThrowing() throws Exception {
        // A chip wider than the column is emitted on its own line (PR-1 atomic);
        // it must still render the text on one page without throwing.
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(90, 140).margin(10, 10, 10, 10).create()) {
            session.dsl().pageFlow().name("Flow")
                    .addParagraph(p -> p.inlineCode("io.github.demchaav.a.very.long.package.name")).build();
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(document)).doesNotContain("?");
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
        assertThat(first.background().padding().left()).as("first fragment keeps the lead pad").isGreaterThan(0.0);
        assertThat(continuation.background().padding().left())
                .as("continuation fragment is open on the inner edge (slice)").isEqualTo(0.0);
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
