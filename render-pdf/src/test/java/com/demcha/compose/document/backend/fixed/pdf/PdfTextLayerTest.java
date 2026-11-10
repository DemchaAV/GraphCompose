package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.emoji.EmojiLibrary;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * An inline icon that states text leaves that text in the page's text layer: copying the line
 * carries the emoji, and nothing extra is painted.
 */
class PdfTextLayerTest {

    private static final EmojiLibrary EMOJI = EmojiLibrary.getDefault();

    private static final String ROCKET = "🚀";
    private static final String WOMAN_TECHNOLOGIST = "👩‍💻";
    private static final String RED_HEART = "❤️";

    @Test
    void copiedLineCarriesEachEmojiWholeAndInPlace() throws Exception {
        byte[] pdf = render(p -> p
                .inlineText("Launch ").inlineEmoji(":rocket:", 14)
                .inlineText(" by ").inlineEmoji(":woman_technologist:", 14)
                .inlineText(" with ").inlineEmoji(":heart:", 14)
                .inlineText(" done."));
        Path output = VisualTestOutputs.preparePdf("emoji-copy-text", "text-layer");
        Files.write(output, pdf);

        assertThat(text(pdf)).contains(
                "Launch " + ROCKET + " by " + WOMAN_TECHNOLOGIST + " with " + RED_HEART + " done.");
    }

    @Test
    void textLayerPaintsNothing() throws Exception {
        // Same drawing twice, once stating its text and once not. The text layer must add no
        // pixel, and must hand the graphics state back: rendering mode 3 outlives ET, so a
        // missing q/Q would leave " done." invisible and the pages would differ.
        SvgIcon rocket = EMOJI.require(":rocket:");
        byte[] stating = render(p -> p.inlineText("Launch ").inlineSvgIcon(rocket, 14).inlineText(" done."));
        byte[] silent = render(p -> p.inlineText("Launch ").inlineSvgIcon(rocket.withText(null), 14)
                .inlineText(" done."));

        assertThat(text(stating)).contains("Launch " + ROCKET + " done.");
        assertThat(text(silent)).doesNotContain(ROCKET);
        assertThat(pixels(stating)).isEqualTo(pixels(silent));
    }

    @Test
    void iconStatingItsOwnTextCopiesThatText() throws Exception {
        SvgIcon check = SvgIcon.parse("<svg viewBox='0 0 10 10'><path d='M1 5 L4 8 L9 1' "
                + "stroke='#2E7D32' fill='none'/></svg>");

        byte[] stating = render(p -> p.inlineText("Tested ").inlineSvgIcon(check.withText("✓"), 10));
        byte[] silent = render(p -> p.inlineText("Tested ").inlineSvgIcon(check, 10));

        assertThat(text(stating)).contains("Tested ✓");
        assertThat(text(silent).strip()).isEqualTo("Tested");
        assertThat(type3Fonts(silent)).as("an icon without text adds no font").isEmpty();
    }

    @Test
    void textsShareOneFontPerDocumentAndStartAnotherAfter255() throws Exception {
        SvgIcon dot = SvgIcon.parse("<svg viewBox='0 0 10 10'><circle cx='5' cy='5' r='4'/></svg>");
        List<String> texts = IntStream.range(0, 300).mapToObj(i -> "<" + i + ">").toList();

        // Every text twice, on a flow long enough to run over several pages.
        byte[] pdf = render(p -> {
            for (int round = 0; round < 2; round++) {
                for (String text : texts) {
                    p.inlineSvgIcon(dot.withText(text), 8).inlineText(" ");
                }
            }
        });

        String extracted = text(pdf);
        assertThat(texts).allSatisfy(text -> assertThat(extracted).contains(text));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
        // Repeated text reuses its code: 300 distinct texts fill one font and start a second.
        assertThat(lastCodes(pdf)).containsExactlyInAnyOrder(255, 45);
        // A CMap block holds at most 100 mappings, so the full font states its 255 in three.
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> blocks = new ArrayList<>();
            for (PDType3Font font : type3Fonts(document)) {
                if (font.getCOSObject().getInt(COSName.LAST_CHAR) == 255) {
                    COSStream map = font.getCOSObject().getCOSStream(COSName.TO_UNICODE);
                    try (InputStream in = map.createInputStream()) {
                        Matcher block = Pattern.compile("(\\d+) beginbfchar")
                                .matcher(new String(in.readAllBytes(), StandardCharsets.US_ASCII));
                        while (block.find()) {
                            blocks.add(block.group(1));
                        }
                    }
                }
            }
            assertThat(blocks).containsExactly("100", "100", "55");
        }
    }

    @Test
    void glyphSpansTheIconWhateverTextStateTheRunBeforeLeft() throws Exception {
        // A 2:1 icon after a word tracked in Tc (a Standard 14 face keeps all its tracking
        // there): the glyph's advance must be the icon's width, not its height plus the
        // tracking the word before it left behind, and it must sit on the baseline.
        SvgIcon wide = SvgIcon.parse("<svg viewBox='0 0 20 10'><rect width='20' height='10'/></svg>")
                .withText("[wide]");
        DocumentTextStyle tracked = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10)
                .letterSpacing(DocumentLetterSpacing.points(4))
                .build();

        byte[] pdf = render(p -> p.inlineText("SPACED", tracked).inlineSvgIcon(wide, 10).inlineText(" end"));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            TextLayerProbe probe = new TextLayerProbe();
            probe.getText(document);
            assertThat(probe.advances).singleElement()
                    .satisfies(advance -> assertThat(advance).isCloseTo(20.0, within(0.01)));
            assertThat(probe.rises).containsOnly(0.0);
        }
    }

    @Test
    void deterministicOutputStaysByteIdenticalWithATextLayer() throws Exception {
        assertThat(renderDeterministic()).isEqualTo(renderDeterministic());
    }

    @Test
    void sectionsOfOneDocumentShareOneFont() throws Exception {
        byte[] pdf;
        try (MultiSectionDocument document = GraphCompose.documents()
                .section(emojiSection("Cover "))
                .section(emojiSection("Body "))
                .create()) {
            pdf = document.toPdfBytes();
        }

        assertThat(text(pdf)).contains("Cover " + ROCKET).contains("Body " + ROCKET);
        assertThat(lastCodes(pdf)).containsExactly(1);
    }

    @Test
    void textLongerThanACMapCanStateIsNotWritten() throws Exception {
        SvgIcon dot = SvgIcon.parse("<svg viewBox='0 0 10 10'><circle cx='5' cy='5' r='4'/></svg>");
        String longest = "a".repeat(PdfTextLayer.MAX_TEXT_UNITS);
        String tooLong = "b".repeat(PdfTextLayer.MAX_TEXT_UNITS + 1);

        byte[] pdf = render(p -> p.inlineSvgIcon(dot.withText(longest), 8)
                .inlineText(" ").inlineSvgIcon(dot.withText(tooLong), 8));

        assertThat(text(pdf)).contains(longest).doesNotContain("b");
    }

    @Test
    void calledInsideATextObjectFailsWithNothingWritten() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PdfTextLayer layer = new PdfTextLayer(document);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                assertThatThrownBy(() -> layer.write(stream, ROCKET, 10, 10, 12, 12))
                        .isInstanceOf(IllegalStateException.class);
                // The caller's text object is still open, and closes as if nothing happened.
                stream.endText();
            }
            assertThat(operators(page)).containsExactly("BT", "ET");
        }
    }

    @Test
    void aFailureHalfWayStillClosesTheGlyphsTextObjectAndSavedState() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PdfTextLayer layer = new PdfTextLayer(document);
            PDPageContentStream stream = spy(new PDPageContentStream(document, page));
            doThrow(new IOException("stream refused the glyph")).when(stream).showText(anyString());

            assertThatThrownBy(() -> layer.write(stream, ROCKET, 10, 10, 12, 12))
                    .isInstanceOf(IOException.class)
                    .hasMessage("stream refused the glyph");
            stream.close();

            List<String> operators = operators(page);
            assertThat(operators).startsWith("q", "BT").endsWith("ET", "Q");
            assertThat(Collections.frequency(operators, "q")).isEqualTo(Collections.frequency(operators, "Q"));
            assertThat(Collections.frequency(operators, "BT")).isEqualTo(Collections.frequency(operators, "ET"));
        }
    }

    /**
     * Right-to-left lines. The emoji's glyph must sit between the words it was written
     * between and state its whole text; both are what the file controls.
     *
     * <p>Reading the line back is the reader's job, and readers disagree: PDFBox's
     * {@code PDFTextStripper.handleDirection} reverses a right-to-left run one UTF-16 unit at
     * a time, so any emoji inside one comes back with its surrogates swapped, and poppler
     * reverses the code points of a multi-code-point glyph (a ZWJ sequence, a U+FE0F);
     * pdf.js and MuPDF keep the glyph's text whole. So these hold the glyph, not a line.</p>
     */
    @Test
    void anEmojiInARightToLeftLineSitsBetweenItsWordsAndStatesItselfWhole() throws Exception {
        DocumentTextStyle hebrew = DocumentTextStyle.builder().fontName(FontName.DAVID_LIBRE).size(18).build();
        DocumentTextStyle arabic = DocumentTextStyle.builder().fontName(FontName.AMIRI).size(18).build();
        // Words with no letter in common, so every glyph belongs to exactly one of them.
        assertEmojiBetween("שלום", "חבר", hebrew);
        assertEmojiBetween("سلام", "كتب", arabic);
    }

    @Test
    void zwjSequenceAndSelectorInARightToLeftLineStayOnOneGlyphEach() throws Exception {
        DocumentTextStyle hebrew = DocumentTextStyle.builder().fontName(FontName.DAVID_LIBRE).size(18).build();
        byte[] pdf = render(p -> p.inlineEmoji(":woman_technologist:", 18).inlineText(" שלום ", hebrew)
                .inlineEmoji(":heart:", 18).direction(TextDirection.RTL));

        assertThat(glyphs(pdf).stream().filter(Glyph::textLayer).map(Glyph::unicode))
                .containsExactlyInAnyOrder(WOMAN_TECHNOLOGIST, RED_HEART);
    }

    @Test
    void aMixedLineWithARightToLeftWordReadsBackInWrittenOrder() throws Exception {
        // A left-to-right sentence with a Hebrew word: the emoji resolves left-to-right, and
        // PDFBox reads the whole line back exactly as written.
        DocumentTextStyle hebrew = DocumentTextStyle.builder().fontName(FontName.DAVID_LIBRE).size(18).build();
        byte[] pdf = render(p -> p.inlineText("Deploy ").inlineText("שלום", hebrew).inlineText(" ")
                .inlineEmoji(":rocket:", 18).inlineText(" done"));

        assertThat(text(pdf)).contains("Deploy שלום " + ROCKET + " done");
    }

    private static void assertEmojiBetween(String first, String second, DocumentTextStyle style)
            throws Exception {
        byte[] pdf = render(p -> p.inlineText(first + " ", style).inlineEmoji(":rocket:", 18)
                .inlineText(" " + second, style).direction(TextDirection.RTL));

        List<Glyph> glyphs = glyphs(pdf);
        List<Glyph> textLayer = glyphs.stream().filter(Glyph::textLayer).toList();
        assertThat(textLayer).hasSize(1);
        Glyph emoji = textLayer.get(0);
        double firstLeft = glyphs.stream().filter(g -> first.contains(g.unicode()))
                .mapToDouble(Glyph::x).min().orElseThrow();
        double secondRight = glyphs.stream().filter(g -> second.contains(g.unicode()))
                .mapToDouble(g -> g.x() + g.width()).max().orElseThrow();

        assertThat(emoji.unicode()).isEqualTo(ROCKET);
        // Drawn right to left: the second word is leftmost, the first rightmost.
        assertThat(emoji.x()).as("%s: emoji starts right of the second word", first)
                .isGreaterThanOrEqualTo(secondRight - 0.5);
        assertThat(emoji.x() + emoji.width()).as("%s: emoji ends left of the first word", first)
                .isLessThanOrEqualTo(firstLeft + 0.5);
    }

    private static DocumentSession emojiSection(String label) {
        DocumentSession section = GraphCompose.document()
                .pageSize(240, 200)
                .margin(DocumentInsets.of(16))
                .create();
        section.pageFlow(page -> page.addParagraph(p -> p.inlineText(label).inlineEmoji(":rocket:", 12)));
        return section;
    }

    private static byte[] render(Consumer<ParagraphBuilder> body) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 200)
                .margin(DocumentInsets.of(16))
                .create()) {
            session.pageFlow(page -> page.addParagraph(body::accept));
            return session.toPdfBytes();
        }
    }

    private static byte[] renderDeterministic() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 200)
                .margin(DocumentInsets.of(16))
                .create()) {
            session.pageFlow(page -> page.addParagraph(p -> p
                    .inlineText("Launch ").inlineEmoji(":rocket:", 14)
                    .inlineText(" with ").inlineEmoji(":heart:", 14)));
            return session.render(PdfFixedLayoutBackend.builder().deterministic(true).build());
        }
    }

    /** One glyph as PDFBox positions it, before any reordering of the line. */
    private record Glyph(String unicode, double x, double width, boolean textLayer) {
    }

    private static List<Glyph> glyphs(byte[] pdf) throws IOException {
        List<Glyph> glyphs = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    glyphs.add(new Glyph(text.getUnicode(), text.getXDirAdj(), text.getWidthDirAdj(),
                            text.getFont() instanceof PDType3Font));
                }
            }.getText(document);
        }
        return glyphs;
    }

    private static List<String> operators(PDPage page) throws IOException {
        List<String> names = new ArrayList<>();
        for (Object token : new PDFStreamParser(page).parse()) {
            if (token instanceof Operator operator) {
                names.add(operator.getName());
            }
        }
        return names;
    }

    /** Records, for every text-layer glyph shown, its advance and rise under the text state it met. */
    private static final class TextLayerProbe extends PDFTextStripper {
        private final List<Double> advances = new ArrayList<>();
        private final List<Double> rises = new ArrayList<>();

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
                throws IOException {
            if (font instanceof PDType3Font) {
                PDTextState state = getGraphicsState().getTextState();
                double wordSpacing = code == 32 ? state.getWordSpacing() : 0;
                advances.add((displacement.getX() * state.getFontSize() + state.getCharacterSpacing()
                        + wordSpacing) * state.getHorizontalScaling() / 100);
                rises.add((double) state.getRise());
            }
            super.showGlyph(textRenderingMatrix, font, code, displacement);
        }
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static int[] pixels(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 144);
            return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        }
    }

    private static List<PDType3Font> type3Fonts(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return type3Fonts(document);
        }
    }

    private static List<PDType3Font> type3Fonts(PDDocument document) throws IOException {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<PDType3Font> fonts = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            for (COSName name : resources.getFontNames()) {
                PDFont font = resources.getFont(name);
                if (font instanceof PDType3Font type3 && seen.add(type3.getCOSObject())) {
                    fonts.add(type3);
                }
            }
        }
        return fonts;
    }

    /** The last code of every text-layer font in the document: how many texts each holds. */
    private static List<Integer> lastCodes(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<Integer> codes = new ArrayList<>();
            for (PDType3Font font : type3Fonts(document)) {
                codes.add(font.getCOSObject().getInt(COSName.LAST_CHAR));
            }
            return codes;
        }
    }
}
