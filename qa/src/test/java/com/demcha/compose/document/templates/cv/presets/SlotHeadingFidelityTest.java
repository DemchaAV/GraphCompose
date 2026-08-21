package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.CvComposedText;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import com.demcha.compose.document.templates.cv.data.Slot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.demcha.compose.document.templates.cv.CvComposedText.squash;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The three column-flow presets print the heading each section's author
 * wrote, in every slot.
 *
 * <p>A slot used to carry a label chosen when the preset was written:
 * "Key Skills" over whatever the skills section was called, "Experience"
 * over "Employment History". That is a rename the author never asked for,
 * and it is the difference between a preset that can draw a CV assembled at
 * runtime and one that can only draw the categories it thought of — a module
 * titled "Certifications &amp; Awards" routed to the education slot would
 * reach the page as a word nobody wrote.</p>
 *
 * <p>Each fixture ends with a "Publications" section, which no slot in any of
 * these presets claims. It is the marker for the leftover tail, and every slot
 * heading has to appear <em>before</em> it: the tail prints titles verbatim
 * too, so without that ordering a title found anywhere in the document would
 * prove nothing about the slot. A keyword-list edit that quietly stopped a
 * fixture section from being claimed fails here rather than passing as a
 * tautology.</p>
 *
 * <p>Titles are picked so that no retired label is a substring of one, which
 * is what lets the second half of each case assert the label is gone —
 * catching a slot that prints both.</p>
 */
class SlotHeadingFidelityTest {

    /** Claimed by no slot in any of the three presets, so it can only be in the tail. */
    private static final String TAIL_MARKER = "Publications";

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void everySlotPrintsTheHeadingItsAuthorWrote(String id,
                                                 DocumentTemplate<CvDocument> preset,
                                                 CvDocument doc,
                                                 List<String> retiredLabels) {
        String text = squash(CvComposedText.of(preset, doc));
        int tail = text.indexOf(squash(TAIL_MARKER));

        assertThat(tail)
                .as("%s must draw the leftover tail — it is the marker the rest is measured against", id)
                .isGreaterThanOrEqualTo(0);
        for (CvSection section : doc.sectionsIn(Slot.MAIN)) {
            if (TAIL_MARKER.equals(section.title())) {
                continue;
            }
            assertThat(text.indexOf(squash(section.title())))
                    .as("%s must head this section with the author's words, in its slot "
                        + "above the tail (%s)", id, section.title())
                    .isGreaterThanOrEqualTo(0)
                    .isLessThan(tail);
        }
        for (String retired : retiredLabels) {
            assertThat(text)
                    .as("%s must not still print its own \"%s\"", id, retired)
                    .doesNotContain(squash(retired));
        }
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("sidebar-portrait", SidebarPortrait.create(),
                        sidebarPortraitCv(),
                        // "Languages" is absent from this list on purpose: that
                        // block keeps its label when it shows part of a section,
                        // which the two tests below pin from both sides.
                        List.of("Education", "Key Skills", "Professional Profile",
                                "Experience", "Projects")),
                Arguments.of("monogram-sidebar", MonogramSidebar.create(),
                        monogramSidebarCv(),
                        // Only the sidebar's word was hardcoded. This preset's
                        // main slots already read the title, through a blank
                        // check that could not fire; asserting their fallbacks
                        // are gone would assert something that never printed.
                        List.of("Expertise")),
                Arguments.of("mint-editorial", MintEditorial.create(),
                        mintEditorialCv(),
                        // "Skills" is absent here: the skill bars still carry
                        // that word, because the index above them took the
                        // author's title — pinned by the two tests below.
                        List.of("Profile", "Experience")));
    }

    @Test
    void theLanguageBlockKeepsItsLabelWhenItShowsPartOfASection() {
        // Sidebar Portrait's language block also accepts an "Additional
        // Information" section and picks the language rows out of it. Three of
        // the four rows below never reach the page, so the section's own title
        // would head content the reader cannot see.
        CvDocument doc = cv(RowsSection.builder("Additional Information", RowStyle.PLAIN)
                .row("Languages", "English (Fluent), German (Intermediate)")
                .row("Work Eligibility", "Eligible to work in the UK")
                .row("Open Source", "Maintainer of GraphCompose")
                .row("Speaking", "JVM Summit 2024")
                .build());

        String text = squash(CvComposedText.of(SidebarPortrait.create(), doc));

        assertThat(text)
                .as("a block showing three rows of four keeps its own label, over "
                    + "the languages it did find")
                .contains(squash("Languages"))
                .contains(squash("English"))
                .contains(squash("German"))
                .doesNotContain(squash("Additional Information"));
    }

    @Test
    void theLanguageBlockTakesTheAuthorsTitleWhenItShowsTheWholeSection() {
        // Every row is a language, so nothing is picked over and the heading
        // has nothing to disown.
        CvDocument doc = cv(RowsSection.builder("Spoken Languages", RowStyle.PLAIN)
                .row("English", "(Fluent)")
                .row("German", "(Intermediate)")
                .build());

        String text = squash(CvComposedText.of(SidebarPortrait.create(), doc));

        assertThat(text)
                .as("a block showing the whole section carries its title")
                .contains(squash("Spoken Languages"));
    }

    @Test
    void mintEditorialHeadsTheSkillIndexWithTheAuthorsTitleAndTheBarsWithItsOwnWord() {
        // One section, two adjacent blocks. The author's words go over the one
        // the reader reaches first, so the section opens in their language even
        // if the column flow puts the bars on the next page.
        String text = squash(CvComposedText.of(MintEditorial.create(), mintEditorialCv()));

        assertThat(CvComposedText.occurrences(text, squash("Technical Expertise")))
                .as("the author's title heads the index, and heads it once")
                .isEqualTo(1);
        assertThat(text)
                .as("and the bars below carry this preset's own word")
                .contains(squash("Skills"));
    }

    @Test
    void mintEditorialDropsTheBarsHeadingWhenItWouldRepeatTheOneAboveIt() {
        // A section this preset claims may be titled with the very word the bars
        // would otherwise print. The same heading twice, over different content,
        // reads as a rendering fault.
        CvDocument doc = cv(SkillsSection.builder("Skills")
                .group("Platform", "Java 21", "Kotlin")
                .build());

        String text = squash(CvComposedText.of(MintEditorial.create(), doc));

        assertThat(CvComposedText.occurrences(text, squash("Skills")))
                .as("one heading, not the same word twice in a row")
                .isEqualTo(1);
    }

    @Test
    void aHeadingTooWideForItsColumnBreaksBetweenWordsAndNotInsideOne() {
        // Letter-spacing puts a space between every pair of letters, and
        // wrapping breaks on whitespace — so before the gap became U+00A0 this
        // heading came out as "EDUCATION & CERTI / FICATIONS". Reverting the
        // gap character turns this red.
        String title = "Education & Certifications";
        List<String> lines = headingLines(SidebarPortrait.create(), cv(
                EntriesSection.builder(title)
                        .entry("MSc Computer Science", "University of Manchester",
                                "2018 - 2020", "Distinction.")
                        .build()), title);

        assertThat(lines)
                .as("the heading has to wrap for this to be measuring anything")
                .hasSizeGreaterThan(1);
        List<String> words = new ArrayList<>();
        for (String word : title.split(" ")) {
            String squashed = squash(word);
            if (!squashed.isEmpty()) {
                words.add(squashed);
            }
        }
        int next = 0;
        for (String line : lines) {
            StringBuilder whole = new StringBuilder();
            while (next < words.size() && whole.length() < line.length()) {
                whole.append(words.get(next++));
            }
            assertThat(whole.toString())
                    .as("a wrapped heading line is whole words, not a word cut in half")
                    .isEqualTo(line);
        }
        assertThat(next)
                .as("and every word of the heading reached a line")
                .isEqualTo(words.size());
    }

    // -- fixtures --------------------------------------------------------

    /** The six sections Sidebar Portrait's slots claim, plus the tail marker. */
    private static CvDocument sidebarPortraitCv() {
        return CvDocument.builder()
                .identity(identity())
                .sections(List.of(
                        summary(),
                        experience(),
                        education(),
                        coreSkills(),
                        RowsSection.builder("Spoken Languages", RowStyle.PLAIN)
                                .row("English", "(Fluent)")
                                .row("German", "(Intermediate)")
                                .build(),
                        projects(),
                        tailMarker()))
                .build();
    }

    /** The six sections Monogram Sidebar's slots claim, plus the tail marker. */
    private static CvDocument monogramSidebarCv() {
        return CvDocument.builder()
                .identity(identity())
                .sections(List.of(
                        summary(),
                        experience(),
                        education(),
                        coreSkills(),
                        projects(),
                        RowsSection.builder("Additional Notes", RowStyle.PLAIN)
                                .row("Eligibility", "UK and EU")
                                .build(),
                        tailMarker()))
                .build();
    }

    /** The seven sections Mint Editorial's blocks claim, plus the tail marker. */
    private static CvDocument mintEditorialCv() {
        return CvDocument.builder()
                .identity(identity())
                .sections(List.of(
                        summary(),
                        experience(),
                        education(),
                        // Claimed through Mint's "expertise" keyword, so the
                        // title stays clear of the word "Skills" — which is what
                        // this preset prints over the bars.
                        SkillsSection.builder("Technical Expertise")
                                .group("Platform", "Java 21", "Kotlin")
                                .group("Rendering", "PDFBox")
                                .build(),
                        RowsSection.builder("Outside Interests", RowStyle.PLAIN)
                                .row("Cycling", "Audax randonneuring")
                                .build(),
                        RowsSection.builder("Awards & Honours", RowStyle.PLAIN)
                                .row("Duke's Choice", "2024")
                                .build(),
                        RowsSection.builder("Professional References", RowStyle.PLAIN)
                                .row("Dana Okafor", "Head of Platform")
                                .build(),
                        tailMarker()))
                .build();
    }

    private static RowsSection tailMarker() {
        return RowsSection.builder(TAIL_MARKER, RowStyle.PLAIN)
                .row("Deterministic document layout", "JVM Summit 2024")
                .build();
    }

    private static ParagraphSection summary() {
        return new ParagraphSection("Career Summary",
                "Ten years on document pipelines and layout engines.");
    }

    private static EntriesSection experience() {
        return EntriesSection.builder("Employment History")
                .entry("Principal Engineer", "Acme Rendering", "2022 - 2025",
                        "Owns the rendering pipeline.")
                .build();
    }

    private static EntriesSection education() {
        return EntriesSection.builder("Degrees & Certifications")
                .entry("MSc Computer Science", "University of Manchester",
                        "2018 - 2020", "Distinction.")
                .build();
    }

    private static SkillsSection coreSkills() {
        return SkillsSection.builder("Core Skills")
                .group("Platform", "Java 21", "Kotlin")
                .group("Rendering", "PDFBox")
                .build();
    }

    private static RowsSection projects() {
        return RowsSection.builder("Project Work", RowStyle.BULLETED_STACKED)
                .row("GraphCompose", "Declarative Java PDF layout engine")
                .build();
    }

    private static CvDocument cv(CvSection section) {
        return CvDocument.builder().identity(identity()).section(section).build();
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jordan", "Rivera")
                .jobTitle("Platform Engineer")
                .contact("+44 20 5555 1000", "jordan@example.com", "London, UK")
                .build();
    }

    /**
     * The wrapped lines of one heading, squashed, in layout order.
     *
     * <p>Selected by content rather than by node name: a line of the heading is
     * a run of the title's own letters, and no other text in the fixture is a
     * substring of it.</p>
     */
    private static List<String> headingLines(DocumentTemplate<CvDocument> preset,
                                             CvDocument doc, String title) {
        String whole = squash(title);
        List<String> lines = new ArrayList<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            preset.compose(session, doc);
            session.layoutGraph().fragments().stream()
                    .filter(fragment -> fragment.payload() instanceof ParagraphFragmentPayload)
                    .map(fragment -> (ParagraphFragmentPayload) fragment.payload())
                    .forEach(paragraph -> paragraph.lines().forEach(line -> {
                        String squashed = squash(line.text());
                        if (!squashed.isEmpty() && whole.contains(squashed)) {
                            lines.add(squashed);
                        }
                    }));
        }
        return lines;
    }
}
