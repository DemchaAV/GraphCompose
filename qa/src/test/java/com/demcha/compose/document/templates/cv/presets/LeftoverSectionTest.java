package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A section these presets have no slot for reaches the page anyway.
 *
 * <p>A preset with a designed layout places sections into fixed slots, and it
 * renders the categories it thought to ask about. Give it an "Awards", a
 * "Publications", or a runtime module the catalogue has no role for, and the
 * section was never looked at: the page came out, it looked finished, and the
 * reader had no way to tell. Removing that last silent loss is what the leftover
 * tail is for — under the title the author wrote, since that is the only label
 * that can be right for a category the preset does not know about.</p>
 *
 * <p>All three presets are two-column layouts whose body used to be an atomic
 * row, which is why they could not do this before: there was nowhere to put an
 * extra section. They flow now.</p>
 */
class LeftoverSectionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void aSectionWithNoSlotStillReachesThePage(String id,
                                               double margin,
                                               Supplier<DocumentTemplate<CvDocument>> factory)
            throws Exception {
        String text = renderText(factory.get(), margin, documentWithUnknownSections());

        // Upper-cased because every one of these presets spaces and capitalises
        // its headings — the point is that the words are the author's.
        assertThat(text)
                .describedAs("%s: the author's own heading, not the preset's vocabulary", id)
                .contains("AWARDS")
                .contains("PUBLICATIONS")
                .contains("VOLUNTEERING")
                .contains("RESEARCH");

        assertThat(text)
                .describedAs("%s: prose leftover", id)
                .contains("Chairedthelayoutworkinggroup");
        // Case-insensitive for this one: Mint Editorial has an awards block of
        // its own, so "Awards" is a claimed section there and an unclaimed one in
        // the other two — either way the row has to reach the page.
        assertThat(text)
                .describedAs("%s: row content, claimed or leftover", id)
                .containsIgnoringCase("BestPaper")
                .contains("DocumentEngineering2024");
        assertThat(text)
                .describedAs("%s: entries leftover, drawn as entries", id)
                .contains("COMPOSABLELAYOUTPRIMITIVES");
        // The module's title. Its subtitle is not asserted: Monogram Sidebar
        // draws no entry subtitle at all, which is a separate fix in flight.
        assertThat(text)
                .describedAs("%s: a runtime module the catalogue has no role for", id)
                .contains("MENTOR");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void theSectionsTheSlotsDoKnowAreStillDrawnOnce(String id,
                                                    double margin,
                                                    Supplier<DocumentTemplate<CvDocument>> factory)
            throws Exception {
        // The tail must not double-draw what a slot already took: claiming is
        // what keeps a section out of remaining(), and a preset that rendered
        // Experience twice would be a louder bug than the one being fixed.
        String text = renderText(factory.get(), margin, documentWithUnknownSections());

        assertThat(occurrences(text, "Ownstherenderingpipeline"))
                .describedAs("%s: the experience slot drew its section exactly once", id)
                .isEqualTo(1);
        assertThat(occurrences(text, "Chairedthelayoutworkinggroup"))
                .describedAs("%s: and the leftover prose is drawn exactly once too", id)
                .isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void aSlotThatCannotDrawWhatItMatchedDoesNotSwallowIt(String id,
                                                          double margin,
                                                          Supplier<DocumentTemplate<CvDocument>> factory)
            throws Exception {
        // "Selected Projects" written as timeline entries matches a projects slot
        // that draws label/value rows. The slot's renderer returns on the type it
        // did not expect, so claiming it there would print the heading, print
        // nothing under it, and keep the section out of the leftover tail — the
        // loss this whole change exists to remove, one layer further in.
        CvDocument doc = CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan@example.com", "London, UK")
                        .build())
                .section(new ParagraphSection("Professional Summary", "Builds pipelines."))
                .section(EntriesSection.builder("Selected Projects")
                        .entry("GraphCompose", "Open source", "2024",
                                "Declarative PDF layout engine.")
                        .entry("LayoutLint", "Open source", "2023",
                                "Static analyser for fragile authoring patterns.")
                        .build())
                .build();

        String text = renderText(factory.get(), margin, doc);

        // Case-insensitive: an entry title is upper-cased by every one of these
        // presets, and which renderer draws it depends on where it lands.
        assertThat(text)
                .describedAs("%s: the entries reach the page, whichever route they take", id)
                .containsIgnoringCase("GraphCompose")
                .containsIgnoringCase("LayoutLint")
                .containsIgnoringCase("PDFlayoutengine")
                .containsIgnoringCase("analyserforfragile");
    }

    @Test
    void anEmptyModuleDoesNotShadowTheSectionThatHasTheContent() throws Exception {
        // A module may name a role and carry nothing. Claiming it for the slot
        // leaves the real, keyword-matching section unclaimed — it survives, but
        // in the leftover tail at the bottom of the CV rather than in the slot
        // its content belongs to.
        //
        // Both places head a section with its author's title, so the words alone
        // cannot say which one drew it. The order can: "Awards" matches no slot
        // in this preset, so it can only be in the tail, and it is declared
        // first. Drawn in its slot, the experience section comes out above it;
        // swept into the tail, it comes out below — the tail keeps document
        // order.
        CvDocument doc = CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan@example.com", "London, UK")
                        .build())
                .section(new ParagraphSection("Professional Summary", "Builds pipelines."))
                .section(ModuleSection.of("Experience", SectionRole.EXPERIENCE,
                        CvKind.ENTRIES_DATED))
                .section(RowsSection.builder("Awards", RowStyle.PLAIN)
                        .row("Duke's Choice", "2024")
                        .build())
                .section(EntriesSection.builder("Professional Experience")
                        .entry("Principal Engineer", "Acme Rendering", "2022-present",
                                "Owns the rendering pipeline.")
                        .build())
                .build();

        String text = renderText(SidebarPortrait.create(),
                SidebarPortrait.RECOMMENDED_MARGIN, doc);

        assertThat(text)
                .describedAs("the job survives the empty module that named its role")
                .contains("Ownstherenderingpipeline");
        assertThat(text.indexOf("AWARDS"))
                .describedAs("the tail drew the section no slot claimed — it is what "
                        + "the experience section's position is read against")
                .isGreaterThanOrEqualTo(0);
        assertThat(text.indexOf("PROFESSIONALEXPERIENCE"))
                .describedAs("and the experience section is drawn in its slot, above "
                        + "the leftover tail rather than inside it")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(text.indexOf("AWARDS"));
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("sidebar_portrait", SidebarPortrait.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) SidebarPortrait::create),
                Arguments.of("monogram_sidebar", MonogramSidebar.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) MonogramSidebar::create),
                Arguments.of("mint_editorial", MintEditorial.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<CvDocument>>) MintEditorial::create));
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            count++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return count;
    }

    private static String renderText(DocumentTemplate<CvDocument> template,
                                     double margin, CvDocument doc) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(margin))
                .create()) {
            template.compose(session, doc);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Squeezed: these presets letter-space their headings, so the text
            // layer carries them one character at a time. (PDFBox's Latin GSUB
            // also eats "ti", which is why no assertion here spans that pair.)
            return new PDFTextStripper().getText(document).replaceAll("\\s+", "");
        }
    }

    /**
     * A CV whose slots are all filled and which then carries four categories no
     * preset here has a place for — one of each shape, including a runtime
     * module whose role is {@code OTHER}.
     */
    private static CvDocument documentWithUnknownSections() {
        List<CvSection> sections = new ArrayList<>();
        sections.add(new ParagraphSection("Professional Summary",
                "Platform engineer building document pipelines."));
        sections.add(SkillsSection.builder("Technical Skills")
                .group("Languages", "Java 21", "Kotlin")
                .build());
        sections.add(EntriesSection.builder("Education")
                .entry("MSc Computer Science", "University of Manchester",
                        "2019-2021", "Distinction.")
                .build());
        sections.add(EntriesSection.builder("Professional Experience")
                .entry("Principal Engineer", "Acme Rendering", "2022-present",
                        "Owns the rendering pipeline.")
                .build());

        // None of the four below matches any slot in any of these presets.
        sections.add(new ParagraphSection("Publications",
                "Chaired the layout working group and wrote its report."));
        sections.add(RowsSection.builder("Awards", RowStyle.PLAIN)
                .row("Best Paper", "Document Engineering 2024")
                .build());
        sections.add(EntriesSection.builder("Research")
                .entry("Composable layout primitives", "University of Manchester",
                        "2020", "Thesis on deterministic document rendering.")
                .build());
        sections.add(ModuleSection.builder("Volunteering", SectionRole.OTHER,
                        CvKind.ENTRIES_DATED)
                .item(CvItem.of("Mentor").at("Rails Girls Berlin").period("2019-2021"))
                .build());

        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan@example.com", "London, UK")
                        .build())
                .sections(sections.toArray(new CvSection[0]))
                .build();
    }
}
