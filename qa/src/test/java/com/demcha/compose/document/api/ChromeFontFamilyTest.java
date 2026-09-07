package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A header or footer draws in the family the author names.
 *
 * <p>Until the zone could name one it drew in standard-14 Helvetica and nothing
 * else, so any footer outside WinAnsi — a Cyrillic page counter, a Greek imprint,
 * a Hebrew notice — came out as a row of {@code ?}. The body never had that
 * problem, not because it falls back (the engine has no fallback chain anywhere)
 * but because the body could always be told which family to use.</p>
 *
 * <p>The second case pins the other half of that sentence: the default is still
 * Helvetica and still substitutes, because a zone that says nothing must render
 * exactly as it did before it could speak.</p>
 *
 * @author Artem Demchyshyn
 */
class ChromeFontFamilyTest {

    private static final String CYRILLIC_FOOTER = "Стр. {page} из {pages}";
    private static final String CYRILLIC_RENDERED = "Стр. 1 из 1";

    @Test
    void aZoneNamingACyrillicCapableFamilyDrawsCyrillic() throws Exception {
        byte[] pdf = render(FontName.PT_SANS);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text)
                    .as("the footer keeps its letters when the zone names a family that has them")
                    .contains(CYRILLIC_RENDERED);
            assertThat(footerLine(text))
                    .as("and nothing in the footer was substituted")
                    .doesNotContain("?");
        }
    }

    @Test
    void theDefaultFamilyStillSubstitutesWhatItCannotEncode() throws Exception {
        byte[] pdf = render(null);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertThat(text)
                    .as("standard-14 Helvetica has no Cyrillic, and the engine does not"
                            + " silently pick another family on the author's behalf")
                    .doesNotContain(CYRILLIC_RENDERED)
                    .contains("?");
        }
    }

    /**
     * Writes both renders side by side for human review, in the same place the
     * visual demos put theirs — a page counter is the kind of thing that is easier
     * to believe having seen it.
     */
    @Test
    void writesAComparisonSheetForReview() throws Exception {
        Path out = Path.of("target/visual-tests/chrome-font");
        Files.createDirectories(out);
        Path helvetica = Files.write(out.resolve("footer-default-helvetica.pdf"), render(null));
        Path ptSans = Files.write(out.resolve("footer-pt-sans.pdf"), render(FontName.PT_SANS));

        assertThat(ptSans).isNotEmptyFile();
        assertThat(Files.size(ptSans))
                .as("the named family is embedded as a subset, so its render carries the font"
                        + " program the standard-14 one does not")
                .isGreaterThan(Files.size(helvetica));
    }

    @Test
    void eachSectionOfACombinedDocumentKeepsItsOwnZoneFamily() throws Exception {
        Path output = Files.createTempFile("chrome-sections", ".pdf");
        try {
            try (DocumentSession latin = section("Page {page}", null);
                 DocumentSession cyrillic = section(CYRILLIC_FOOTER, FontName.PT_SANS);
                 MultiSectionDocument combined = GraphCompose.documents(output)
                         .section(latin).section(cyrillic).create()) {
                combined.buildPdf();
            }

            try (PDDocument document = Loader.loadPDF(output.toFile())) {
                String text = new PDFTextStripper().getText(document);
                assertThat(text)
                        .as("the section chrome path resolves the family per section, not once"
                                + " for the combined document")
                        .contains("Стр.");
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static DocumentSession section(String centerText, FontName fontName) {
        DocumentSession document = GraphCompose.document()
                .pageSize(320, 200)
                .margin(DocumentInsets.of(24))
                .create();
        DocumentHeaderFooter.DocumentHeaderFooterBuilder footer =
                DocumentHeaderFooter.builder().centerText(centerText);
        if (fontName != null) {
            footer.fontName(fontName);
        }
        document.chrome().footer(footer.build());
        document.dsl().pageFlow()
                .name("Section")
                .addParagraph(paragraph -> paragraph.name("Body").text("Body"))
                .build();
        return document;
    }

    /** The line the footer was drawn on, so an assertion cannot be satisfied by body text. */
    private static String footerLine(String pageText) {
        return pageText.lines()
                .filter(line -> line.contains("1"))
                .reduce((first, second) -> second)
                .orElse("");
    }

    private static byte[] render(FontName fontName) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(320, 200)
                .margin(DocumentInsets.of(24))
                .create()) {

            DocumentHeaderFooter.DocumentHeaderFooterBuilder footer = DocumentHeaderFooter.builder()
                    .centerText(CYRILLIC_FOOTER);
            if (fontName != null) {
                footer.fontName(fontName);
            }
            document.chrome().footer(footer.build());

            document.dsl().pageFlow()
                    .name("CyrillicChrome")
                    .addParagraph(paragraph -> paragraph.name("Body").text("Body"))
                    .build();

            return document.toPdfBytes();
        }
    }
}
