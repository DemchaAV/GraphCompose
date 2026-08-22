package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.cv.CvComposedText;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.demcha.compose.document.templates.cv.CvComposedText.squash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Timeline Minimal draws its projects in the column the caller asked for.
 *
 * <p>The narrow sidebar suits a list of short labels. A CV whose projects
 * carry a paragraph each fills it long before the wide column fills, so the
 * projects run onto a page of their own while the main column ends half
 * empty. Which column they belong in is a property of the CV rather than of
 * the design, so it is the caller's to choose — and the default stays what it
 * has always been.</p>
 *
 * <p>Placement is read off the <em>node names</em> the preset gives its
 * blocks, not off reading order. Order would only be a proxy: the preset emits
 * one body row per page, so once the document paginates a later page's sidebar
 * follows an earlier page's main column and the proxy inverts. The names say
 * which column drew the block whatever the pagination does.</p>
 */
class TimelineMinimalProjectsColumnTest {

    /** How the preset names a block: the column, then its normalised title. */
    private static final String SIDEBAR_PROJECTS = "CvV2TimelineMinimalSidebarprojects";
    private static final String MAIN_PROJECTS = "CvV2TimelineMinimalMainprojects";

    @Test
    void byDefaultTheProjectsAreDrawnInTheSidebar() {
        List<String> names = blockNames(TimelineMinimal.create());

        assertThat(names)
                .as("the sidebar draws them, and only the sidebar")
                .contains(SIDEBAR_PROJECTS)
                .doesNotContain(MAIN_PROJECTS);
    }

    @Test
    void theProjectsColumnOptionMovesThemToTheMainColumn() {
        List<String> names = blockNames(TimelineMinimal.create(movedToMain()));

        assertThat(names)
                .as("moved, not copied — the sidebar no longer draws them")
                .contains(MAIN_PROJECTS)
                .doesNotContain(SIDEBAR_PROJECTS);
    }

    @Test
    void theMovedProjectsAreDrawnAfterTheWorkHistory() {
        List<String> names = blockNames(TimelineMinimal.create(movedToMain()));

        assertThat(names)
                .as("a reader of the wide column meets the career first and the "
                    + "projects that came out of it second")
                .containsSubsequence("CvV2TimelineMinimalMainprofessionalsummary",
                        "CvV2TimelineMinimalMainprofessionalexperience",
                        MAIN_PROJECTS);
    }

    @Test
    void theOptionMovesEveryProjectExactlyOnceAndLosesNoOtherSection() {
        String moved = compose(TimelineMinimal.create(movedToMain()));

        // Counted, not sampled: a block drawn in both columns and a block
        // dropped from the middle of the list are the two failures column
        // pagination exists to prevent, and presence alone sees neither.
        assertThat(CvComposedText.occurrences(moved, squash(PROJECT_DESCRIPTION)))
                .as("every project drawn, and each of them once")
                .isEqualTo(PROJECT_NAMES.length);
        for (String name : PROJECT_NAMES) {
            assertThat(moved).as("%s reaches the page", name).contains(squash(name));
        }
        assertThat(moved)
                .as("and the work history, the sidebar blocks and a section no "
                    + "slot claimed are all still there")
                .contains(squash("Principal Engineer"), squash("MSc Computer Science"),
                        squash("Java 21"), squash("Publications"),
                        squash("Deterministic layout"));
    }

    @Test
    void nullOptionsAreRejectedRatherThanTakenAsTheDefault() {
        // Every preset in this package carrying an Options record rejects a null
        // one. A null arriving from a caller's configuration is a dropped value,
        // and drawing the stock layout for it hides that until somebody asks why
        // the projects never moved.
        assertThatThrownBy(() -> TimelineMinimal.create(BrandTheme.timelineMinimal(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("options");
        assertThatThrownBy(() -> TimelineMinimal.create((TimelineMinimal.Options) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("options");
    }

    @Test
    void aProjectHeavyCvNeedsFewerPagesWithTheProjectsInTheWideColumn() {
        // The reported symptom, measured. Twelve projects is the size used
        // because it is where the sidebar genuinely runs out of column: it takes
        // three slices where the wide column takes one. At nine the default also
        // reports two pages, but for a different reason — its single slice no
        // longer fits under the masthead and is displaced whole, leaving the
        // first page nearly blank. Asserting on that number would be measuring a
        // different mechanism than the one this option addresses.
        int sidebarPages = pages(TimelineMinimal.create());
        int mainPages = pages(TimelineMinimal.create(movedToMain()));

        assertThat(mainPages)
                .as("the wide column holds what the narrow one spills "
                    + "(sidebar %d pages, main %d)", sidebarPages, mainPages)
                .isLessThan(sidebarPages);
    }

    // -- fixtures --------------------------------------------------------

    private static final String[] PROJECT_NAMES = {
            "GraphCompose", "Ledger Sync", "Field Atlas", "Template Studio",
            "LayoutLint", "ChromeForge", "Signal Desk", "Paper Trail",
            "Quiet Hours", "Night Mail", "Slow Radio", "Wide Angle"};

    private static final String PROJECT_DESCRIPTION =
            "Declarative Java PDF layout engine with a semantic authoring DSL, "
            + "slot-based templates and snapshot testing, powering production CV, "
            + "invoice and proposal pipelines for hiring tools and billing systems.";

    private static TimelineMinimal.Options movedToMain() {
        return TimelineMinimal.Options.builder()
                .projectsColumn(TimelineMinimal.Column.MAIN)
                .build();
    }

    private static String compose(DocumentTemplate<CvDocument> preset) {
        return CvComposedText.squashedNodes(preset, projectHeavyCv());
    }

    /** Every composed block's name, in layout order. */
    private static List<String> blockNames(DocumentTemplate<CvDocument> preset) {
        try (DocumentSession session = newSession()) {
            preset.compose(session, projectHeavyCv());
            LayoutSnapshot snapshot = session.layoutSnapshot();
            return snapshot.nodes().stream()
                    .map(node -> node.entityName())
                    .filter(name -> name != null && name.startsWith("CvV2TimelineMinimal"))
                    .toList();
        }
    }

    private static int pages(DocumentTemplate<CvDocument> preset) {
        try (DocumentSession session = newSession()) {
            preset.compose(session, projectHeavyCv());
            return session.layoutGraph().totalPages();
        }
    }

    private static DocumentSession newSession() {
        float margin = (float) TimelineMinimal.RECOMMENDED_MARGIN;
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(margin, margin, margin, margin)
                .create();
    }

    /** A CV whose projects carry a paragraph each — the shape that spills. */
    private static CvDocument projectHeavyCv() {
        RowsSection.Builder projects =
                RowsSection.builder("Projects", RowStyle.BULLETED_STACKED);
        for (String name : PROJECT_NAMES) {
            projects.row(name, PROJECT_DESCRIPTION);
        }
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan@example.com", "London, UK")
                        .build())
                .sections(List.of(
                        new ParagraphSection("Professional Summary",
                                "Platform engineer with ten years on document pipelines, "
                                + "layout engines and the template systems other teams "
                                + "build on."),
                        EntriesSection.builder("Professional Experience")
                                .entry("Principal Engineer", "Acme Rendering", "2022 - 2025",
                                        "Owns the rendering pipeline and its release train.")
                                .build(),
                        EntriesSection.builder("Education")
                                .entry("MSc Computer Science", "University of Manchester",
                                        "2018 - 2020", "Distinction.")
                                .build(),
                        SkillsSection.builder("Technical Skills")
                                .group("Platform", "Java 21", "Kotlin")
                                .build(),
                        projects.build(),
                        RowsSection.builder("Publications", RowStyle.PLAIN)
                                .row("Deterministic layout", "JVM Summit 2024")
                                .build()))
                .build();
    }
}
