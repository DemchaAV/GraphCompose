package com.demcha.compose.document.backend.fixed.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds what a reordered run states about itself in the file.
 *
 * <p>A right-to-left run reaches the content stream backwards, because a page is painted
 * left to right. Every reader used to be left to work the letters back out by running the
 * bidirectional algorithm over what it extracted; the run now carries its own text as
 * {@code ActualText}, so a reader that honours the section gets the letters as written with
 * no algorithm at all, and one that ignores it loses nothing it ever had.</p>
 *
 * <p>The section is one run, deliberately not the line. A reader takes {@code ActualText}
 * <em>instead of</em> the glyphs it covers — so a section spanning the line would swallow
 * the left-to-right words inside it, hand their letters back as part of one substituted
 * string, and leave their order to however the reader reassembles it. Measured before this
 * shape was chosen: wrapping the line made PDFBox return the embedded Latin word reversed
 * and dropped the chip's glyph positions entirely. Wrapped run by run, both extract exactly
 * as they did before the sections existed. That cost is why the sections are narrow, and the
 * mixed-line case below is the regression guard for it.</p>
 */
class RtlActualTextTest {

    private static final DocumentTextStyle HEBREW_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.DAVID_LIBRE).size(18).build();
    private static final DocumentTextStyle ARABIC_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.AMIRI).size(18).build();

    private static final String HEBREW = "שלום עולם";
    private static final String ARABIC = "مرحبا بالعالم";

    @Test
    void aReversedRunStatesItsTextAsWritten() throws Exception {
        // The section's whole point: the file says what the backwards glyphs mean, in the
        // order the author wrote them, without a reader having to know any algorithm.
        List<String> sections = actualTextSections(render(p -> p.text(HEBREW)
                .direction(TextDirection.RTL).textStyle(HEBREW_STYLE)));

        assertThat(sections).containsExactly(HEBREW);
    }

    @Test
    void anArabicSectionCarriesLettersNotTheShapesItWasDrawnAs() throws Exception {
        // The glyphs are the joined forms — that is what drawing Arabic means here — and
        // the section is about meaning, so the base letters go in.
        List<String> sections = actualTextSections(render(p -> p.text(ARABIC)
                .direction(TextDirection.RTL).textStyle(ARABIC_STYLE)));

        assertThat(sections).containsExactly(ARABIC);
    }

    @Test
    void aLeftToRightRunInsideTheLineStaysOutsideAnySection() throws Exception {
        // The narrowness itself. A section covering the Latin would swallow it — its
        // letters handed back inside a substituted string rather than glyph by glyph — so
        // the sections must contain only what was actually drawn backwards.
        List<String> sections = actualTextSections(render(p -> p.text("GRAPH " + HEBREW)
                .direction(TextDirection.RTL).textStyle(HEBREW_STYLE)));

        assertThat(String.join("", sections))
                .describedAs("the sections carry the reordered runs, and no Latin letter "
                        + "was reordered")
                .contains(HEBREW)
                .doesNotContain("GRAPH")
                .doesNotContain("G");
    }

    @Test
    void aDocumentWithNoReorderedRunCarriesNoSection() throws Exception {
        // The other half of narrowness: the path every existing document takes emits
        // nothing new at all.
        byte[] pdf = render(p -> p.text("An ordinary Latin paragraph.")
                .textStyle(DocumentTextStyle.builder().size(14).build()));

        assertThat(actualTextSections(pdf)).isEmpty();
        assertThat(operatorsOf(pdf))
                .describedAs("no marked-content operator at all — parsed as operators, "
                        + "because a glyph string is free to contain the letters B, D, C")
                .doesNotContain("BDC");
    }

    @Test
    void aMixedLineStillExtractsExactlyAsItDidWithoutTheSections() throws Exception {
        // The regression guard for the shape of the feature. PDFBox reassembles a line by
        // running the bidirectional algorithm over what it extracted; a section that
        // covered more than the reversed run fed that algorithm text which was already
        // logical, and the embedded Latin came back reversed. Chip and plain run both,
        // because the chip draws in its own text object and fails independently.
        byte[] plain = render(p -> p.text("GRAPH " + HEBREW)
                .direction(TextDirection.RTL).textStyle(HEBREW_STYLE));
        byte[] chip = render(p -> p.rich(rich -> rich
                        .highlight("GRAPH", HEBREW_STYLE, DocumentColor.rgb(220, 0, 0), 4,
                                DocumentInsets.of(4))
                        .style(" " + HEBREW, HEBREW_STYLE))
                .direction(TextDirection.RTL).textStyle(HEBREW_STYLE));

        assertThat(extractedText(plain)).isEqualTo("GRAPH " + HEBREW);
        assertThat(extractedText(chip)).isEqualTo("GRAPH " + HEBREW);

        // And nothing about the sections may cost a glyph its position: every drawn glyph
        // is still individually accounted for.
        assertThat(glyphPositionCount(chip))
                .describedAs("the chip's own glyphs stay extractable one by one")
                .isEqualTo(("GRAPH " + HEBREW).length());
    }

    @Test
    void adjacentSectionsExtractAsTheSentenceThatWasWritten() throws Exception {
        // A styled word inside a right-to-left sentence is the most common real document
        // this feature meets, and it makes two runs — two sections, side by side. Each
        // collapses to one extractor position, which is structurally the situation that
        // broke the wide-section shape; measured here so it stays working at chunk size
        // two rather than assumed from the single-run cases.
        DocumentTextStyle large = DocumentTextStyle.builder()
                .fontName(FontName.DAVID_LIBRE).size(24).build();
        byte[] pdf = render(p -> p
                .rich(rich -> rich
                        .style("שלום", large)
                        .style(" עולם", HEBREW_STYLE))
                .direction(TextDirection.RTL).textStyle(HEBREW_STYLE));

        // How the engine chunks a line into spans is its own business — the contract is
        // that the sections, read in logical order, are the sentence that was written.
        List<String> sections = actualTextSections(pdf);
        assertThat(sections).hasSizeGreaterThanOrEqualTo(2);
        StringBuilder logical = new StringBuilder();
        for (int index = sections.size() - 1; index >= 0; index--) {
            logical.append(sections.get(index)); // sections sit in visual order, right to left
        }
        assertThat(logical.toString())
                .describedAs("the sections together state the written sentence")
                .isEqualTo("שלום עולם");
        assertThat(extractedText(pdf)).isEqualTo("שלום עולם");
    }

    /**
     * Every ActualText value the page carries, in content-stream order.
     *
     * <p>The section is spelled {@code /Span /PropN BDC} in the stream, with the property
     * list holding the text living in the page's resources — so the stream is walked for
     * the names, and each name resolved through the resources. Read that way rather than
     * through an extractor, so what is measured is what the file says.</p>
     */
    private static List<String> actualTextSections(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            org.apache.pdfbox.pdmodel.PDPage page = document.getPage(0);
            String stream = contentStreamOf(pdf);
            List<String> sections = new ArrayList<>();
            java.util.regex.Matcher bdc = java.util.regex.Pattern
                    .compile("/Span\\s+/(\\S+)\\s+BDC").matcher(stream);
            while (bdc.find()) {
                org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList
                        properties = page.getResources().getProperties(
                                org.apache.pdfbox.cos.COSName.getPDFName(bdc.group(1)));
                assertThat(properties)
                        .describedAs("the section's property list %s resolves", bdc.group(1))
                        .isNotNull();
                String text = properties.getCOSObject().getString(
                        org.apache.pdfbox.cos.COSName.ACTUAL_TEXT);
                if (text != null) {
                    sections.add(text);
                }
            }
            return sections;
        }
    }

    /** Every operator in the page's content stream, in order. */
    private static List<String> operatorsOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> operators = new ArrayList<>();
            org.apache.pdfbox.pdfparser.PDFStreamParser parser =
                    new org.apache.pdfbox.pdfparser.PDFStreamParser(document.getPage(0));
            Object token;
            while ((token = parser.parseNextToken()) != null) {
                if (token instanceof org.apache.pdfbox.contentstream.operator.Operator operator) {
                    operators.add(operator.getName());
                }
            }
            return operators;
        }
    }

    private static String contentStreamOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf);
             InputStream in = document.getPage(0).getContents()) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static String extractedText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).trim();
        }
    }

    /** How many drawn glyphs the extractor can still point at individually. */
    private static int glyphPositionCount(byte[] pdf) throws IOException {
        List<TextPosition> positions = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> all) throws IOException {
                    positions.addAll(all);
                    super.writeString(text, all);
                }
            }.getText(document);
        }
        return positions.size();
    }

    private static byte[] render(Consumer<com.demcha.compose.document.dsl.ParagraphBuilder> body) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 110)
                .margin(DocumentInsets.of(20))
                .create()) {

            document.pageFlow(page -> page.addParagraph(body::accept));
            return document.toPdfBytes();
        }
    }
}
