package com.demcha.examples.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds each card's published font claim against the document that card publishes.
 *
 * <p>The claim decides the dependency block the site shows. A document drawn in a bundled
 * face cannot be reproduced from the engine and the templates alone — it compiles, then
 * throws {@code Bundled font resource not found} at the first glyph — and the companion
 * carrying those faces is versioned independently of the release, so such a card has to
 * send a reader to the published aggregate instead. A document drawn only in the
 * Standard-14 set needs none of that, and saying otherwise would hand every reader a
 * heavier dependency than their document requires.</p>
 *
 * <p>Read back from the PDF rather than from the register: the need follows from what was
 * rendered, it differs card by card inside one family — 25 of 27 CVs embed a face, 4 of 7
 * invoices do — and a hand-kept list would be wrong the first time a preset changed its
 * theme.</p>
 *
 * <p>The measurement here is deliberately the same one {@code ShowcaseSync} makes, and being
 * in its module and package this test could have called it. It does not, but the honest claim
 * is narrow: two copies of one algorithm agree by construction, so what this catches is a
 * manifest that no longer matches its documents — a sync that was never re-run, a document
 * regenerated on its own, a field edited by hand — and not a flaw in the algorithm itself.
 * The consumer scenario under {@code scripts/release-smoke} is what tests the conclusion, by
 * rendering from the coordinates the site publishes.</p>
 *
 * <p>This guard lives in the examples module because it needs both halves — Jackson to read
 * the manifest and PDFBox to read the documents — and core's test scope has neither.</p>
 */
class ShowcaseBundledFontClaimTest {

    /** The module directory is the working directory; the repository is its parent. */
    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    private static final Path WEB = REPO_ROOT.resolve("web");

    @Test
    void everyCardSaysWhetherItsDocumentNeedsTheBundledFonts() throws IOException {
        JsonNode manifest = new ObjectMapper().readTree(Files.readString(WEB.resolve("examples.json")));
        Set<String> wrong = new TreeSet<>();
        int embedding = 0;
        int standard14 = 0;

        for (JsonNode category : manifest.get("categories")) {
            for (JsonNode group : category.get("groups")) {
                for (JsonNode card : group.get("examples")) {
                    String id = card.path("id").asText();
                    JsonNode claim = card.get("needsBundledFonts");
                    if (claim == null || !claim.isBoolean()) {
                        wrong.add(id + " — says nothing about the fonts its document needs");
                        continue;
                    }
                    Path pdf = WEB.resolve(card.path("pdf").asText());
                    if (!Files.isRegularFile(pdf)) {
                        wrong.add(id + " — publishes no document at " + card.path("pdf").asText());
                        continue;
                    }
                    boolean embeds = embedsAFaceOfItsOwn(pdf);
                    if (embeds != claim.asBoolean()) {
                        wrong.add(id + " — says needsBundledFonts=" + claim.asBoolean()
                                + ", but its document embeds "
                                + (embeds ? "a face of its own" : "nothing but Standard-14"));
                    }
                    if (embeds) {
                        embedding++;
                    } else {
                        standard14++;
                    }
                }
            }
        }

        assertThat(embedding)
                .describedAs("no published document embeds a face of its own, so this guard could not "
                        + "tell a card that needs the aggregate from one that does not")
                .isGreaterThan(0);
        assertThat(standard14)
                .describedAs("every published document embeds a face, so a claim of 'always true' would "
                        + "pass here while telling every reader to take a heavier dependency than their "
                        + "document needs")
                .isGreaterThan(0);
        assertThat(wrong)
                .describedAs("the dependency block a reader pastes into their build follows from this "
                        + "flag, and both ways of being wrong reach them: too little and the render "
                        + "throws on a missing font, too much and they take the whole aggregate for a "
                        + "document drawn in Helvetica")
                .isEmpty();
    }

    /**
     * Whether the document embeds a font of its own, rather than drawing only in the
     * Standard-14 set every PDF reader already has.
     */
    private static boolean embedsAFaceOfItsOwn(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources == null) {
                    continue;
                }
                for (COSName name : resources.getFontNames()) {
                    PDFont font = resources.getFont(name);
                    if (font != null && font.isEmbedded()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
