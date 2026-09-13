package com.demcha.compose.document.templates;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.presets.ProfessionalSidebar;
import com.demcha.compose.document.templates.cv.presets.ProfessionalSidebarFixtures;
import com.demcha.compose.document.templates.invoice.presets.ConsultingInvoice;
import com.demcha.compose.document.templates.invoice.presets.ConsultingInvoiceFixtures;
import com.demcha.compose.document.templates.invoice.presets.LumaStudioInvoice;
import com.demcha.compose.document.templates.invoice.presets.LumaStudioInvoiceFixtures;
import com.demcha.compose.document.templates.receipt.presets.ModernReceipt;
import com.demcha.compose.document.templates.receipt.presets.ReceiptFixtures;
import com.demcha.compose.document.templates.rota.presets.CobaltRota;
import com.demcha.compose.document.templates.rota.presets.CobaltRotaFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The text layer of the promoted templates says the words they draw.
 *
 * <p>Ten shipped presets once made their tracking by writing something that is
 * not the text — a space between every letter, a hair space, a run of sized
 * spacers, an invisible rectangle — so a heading drawn as PROFILE came out of
 * the file as {@code P R O F I L E} and a name could not be searched for. The
 * engine tracks with the pen and states the run's own {@code ActualText}, so
 * the words survive. This gate holds them to that.</p>
 *
 * <p>The CV family is covered preset by preset in
 * {@code CvPresetTextLayerTest}; this one covers the other families the defect
 * reached, and pins the three labels by name.</p>
 *
 * <p>The detector is asserted against fixtures of its own below. That matters
 * more than it looks: the first sweep of this defect was run on a PDFBox that
 * ignores {@code ActualText}, and it called twenty-two correct templates
 * broken. A detector nobody has tried to fool is a detector nobody should
 * trust.</p>
 */
class TemplateTextLayerGateTest {

    /**
     * Three or more single letters, each separated by one space character.
     *
     * <p>Any space separator counts, not just {@code U+0020}: one of the
     * presets spelled its headings with hair and thin spaces, which a plain
     * space class reads straight past.</p>
     */
    private static final Pattern SPELLED_OUT =
            Pattern.compile("(?<!\\p{L})(?:\\p{L}[\\p{Zs}]){2,}\\p{L}(?!\\p{L})");

    // -- the shipped templates ---------------------------------------------

    @Test
    void theConsultingInvoiceSaysBilled() throws Exception {
        String extracted = extract(session ->
                ConsultingInvoice.create().compose(
                        session, ConsultingInvoiceFixtures.canonicalInvoice()));

        assertThat(extracted).contains("BILLED");
        assertThat(extracted).doesNotContain("B I L L E D");
        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    @Test
    void theLumaStudioInvoiceSaysInvoice() throws Exception {
        String extracted = extract(session ->
                LumaStudioInvoice.create().compose(
                        session, LumaStudioInvoiceFixtures.canonicalInvoice()));

        assertThat(extracted).contains("INVOICE");
        assertThat(extracted).doesNotContain("I N V O I C E");
        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    @Test
    void theProfessionalSidebarSaysProfile() throws Exception {
        String extracted = extract(session ->
                ProfessionalSidebar.create().compose(
                        session, ProfessionalSidebarFixtures.canonicalCv()));

        assertThat(extracted).contains("PROFILE");
        assertThat(extracted).doesNotContain("P R O F I L E");
        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    /**
     * The brand qualifier is tracked; the rules that flank it are not.
     *
     * <p>Reached only when the brand carries no logo, which no committed
     * preview does — so this is the one place the text lockup is drawn at all.
     * The tracking belongs to the word: applying it to the whole run would
     * spread the two rules away from the word they point at, which is a
     * styling change rather than the text-layer fix, and centre alignment
     * hides it from the layout snapshot.</p>
     */
    @Test
    void onlyTheBrandQualifierIsTracked() throws Exception {
        String extracted = extract(session ->
                ConsultingInvoice.create().compose(
                        session, ConsultingInvoiceFixtures.logolessInvoice()));

        assertThat(extracted).contains("NORTHPOINT");
        assertThat(extracted).contains("CONSULTING");
        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    /** The rota family's only preset, which no committed preview renders. */
    @Test
    void theRotaReadsBackAsWords() throws Exception {
        String extracted = extract(session ->
                CobaltRota.create().compose(session, CobaltRotaFixtures.canonicalRota()));

        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    /**
     * The control. This preset never faked its tracking — it has carried
     * native spaced caps from the day it shipped — so it is the case that must
     * keep passing. If a change to the detector ever reddens this one, the
     * detector is wrong, not the receipt.
     */
    @Test
    void theReceiptsNativeSpacedCapsStayClean() throws Exception {
        String extracted = extract(session ->
                ModernReceipt.create().compose(session, ReceiptFixtures.canonicalReceipt()));

        assertThat(extracted).contains("AMOUNT COLLECTED");
        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    // -- the detector itself -----------------------------------------------

    @Test
    void theDetectorCatchesLettersSpelledOutWithOrdinarySpaces() throws Exception {
        String extracted = extract(session -> session.pageFlow(page -> page
                .addParagraph("P R O F I L E")));

        assertThat(spelledOutRuns(extracted)).contains("P R O F I L E");
    }

    /**
     * The failure one preset actually shipped: the gaps were hair and thin
     * spaces, so a detector written against {@code " "} alone reads the line
     * as a single word and passes it.
     */
    @Test
    void theDetectorCatchesLettersSpelledOutWithHairAndThinSpaces() throws Exception {
        String hair = "P R O F I L E";
        String thin = "B I L L E D";

        assertThat(spelledOutRuns(hair)).isNotEmpty();
        assertThat(spelledOutRuns(thin)).isNotEmpty();
    }

    @Test
    void theDetectorPassesAWordTrackedWithThePen() throws Exception {
        String extracted = extract(session -> session.pageFlow(page -> page
                .addParagraph(p -> p
                        .text("PROFILE")
                        .textStyle(DocumentTextStyle.builder()
                                .size(18)
                                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.3))
                                .build()))));

        assertThat(extracted).contains("PROFILE");
        assertThat(spelledOutRuns(extracted)).isEmpty();
    }

    @Test
    void theDetectorLeavesOrdinaryProseAlone() {
        // Nothing spelled out: headings, prose, and initials short enough to be
        // words rather than a spelled-out run.
        assertThat(spelledOutRuns("PROFILE EXPERIENCE EDUCATION")).isEmpty();
        assertThat(spelledOutRuns("Nothing here is spelled out letter by letter")).isEmpty();
        assertThat(spelledOutRuns("Ordinary prose with short words a b and c")).isEmpty();

        // And it still fires on a genuine run buried in prose.
        assertThat(spelledOutRuns("Delivery lead, a s p e c i a l case aside"))
                .containsExactly("a s p e c i a l");
    }

    // -- helpers -----------------------------------------------------------

    private static List<String> spelledOutRuns(String text) {
        Matcher matcher = SPELLED_OUT.matcher(text);
        List<String> runs = new ArrayList<>();
        while (matcher.find()) {
            runs.add(matcher.group());
        }
        return runs;
    }

    private interface Composition {
        void compose(DocumentSession session);
    }

    private static String extract(Composition composition) throws IOException {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().create()) {
            composition.compose(session);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // One line break is a wrapping decision, not a text-layer defect.
            return new PDFTextStripper().getText(document).replaceAll("\\s*\\R\\s*", " ");
        }
    }
}
