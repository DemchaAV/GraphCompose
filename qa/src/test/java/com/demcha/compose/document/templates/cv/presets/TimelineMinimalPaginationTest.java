package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every page Timeline Minimal computes becomes a page of its own in the PDF.
 *
 * <p>The preset slices its own content and emits one atomic row per slice,
 * because a row cannot be broken by the paginator. Each slice carries its own
 * timeline axis, so two of them sharing a sheet would not be a tighter layout
 * — it would be two axes stacked under each other.</p>
 *
 * <p>Nothing in the preset forces them apart: the rows are added back to back
 * and reach separate pages by overflowing, which happens only once a row no
 * longer fits. The separation instead falls out of how the slices are cut. A
 * page closes early only when the next block does not fit what is left of it,
 * and that block opens the following page — so any two neighbouring slices
 * together already exceed one page by construction, and no pair of them can
 * settle onto one sheet.</p>
 *
 * <p>That is an argument about the splitting code, not about this preset, and
 * it is the kind that stops holding quietly. This pins the consequence where
 * it is visible: an uneven document, none of whose pages is packed to the
 * margin, still gets one sheet per slice and not one blank sheet more. An
 * explicit page break between the rows is what the assertions below rule out
 * as much as a shared page — a break landing on a page the flow has already
 * left costs an empty sheet.</p>
 */
class TimelineMinimalPaginationTest {

    @Test
    void eachComputedPageGetsAPdfPageOfItsOwn() throws Exception {
        LayoutSnapshot snapshot = layoutOf(longCvWithAThinTail());

        List<LayoutNodeSnapshot> bodyRows = snapshot.nodes().stream()
                .filter(node -> node.entityName() != null
                        && node.entityName().startsWith("CvV2TimelineMinimalBody")
                        && !node.entityName().contains("Break"))
                .toList();

        assertThat(bodyRows)
                .describedAs("the fixture has to spill past two pages or the "
                        + "assertion below proves nothing")
                .hasSizeGreaterThanOrEqualTo(3);

        // Body row n belongs on page n: the first shares page 0 with the
        // masthead, and every later one opens a page. Comparing the whole
        // sequence at once reports which slice slipped rather than just that
        // one did.
        List<Integer> pages = bodyRows.stream()
                .map(LayoutNodeSnapshot::startPage)
                .toList();
        List<Integer> expected = java.util.stream.IntStream.range(0, bodyRows.size())
                .boxed()
                .toList();

        assertThat(pages)
                .describedAs("two slices sharing a page draw two timeline axes "
                        + "on one sheet")
                .isEqualTo(expected);

        assertThat(snapshot.totalPages())
                .describedAs("a page break before the first row, or after the "
                        + "last, would add a blank page")
                .isEqualTo(bodyRows.size());
    }

    private static LayoutSnapshot layoutOf(CvDocument doc) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(TimelineMinimal.RECOMMENDED_MARGIN))
                .create()) {
            TimelineMinimal.create().compose(session, doc);
            return session.layoutSnapshot();
        }
    }

    /**
     * A CV that needs three pages and fills none of them.
     *
     * <p>The main column is a run of mid-sized blocks rather than one long
     * one. A block that does not fit what is left of a page moves to the next
     * one whole instead of being orphaned, so each page closes well short of
     * its capacity — which is the shape that lets two slices share a sheet
     * when nothing forces them apart. A fixture packed to the margins would
     * pass whether or not anything forces them.</p>
     */
    private static CvDocument longCvWithAThinTail() {
        List<CvSection> sections = new ArrayList<>();
        sections.add(new ParagraphSection("Professional Summary",
                "Builds reliable document pipelines across the JVM."));
        sections.add(SkillsSection.builder("Technical Skills")
                .group("Languages", "Java 21", "Kotlin")
                .group("Frameworks", "Spring Boot", "Quarkus")
                .build());

        EntriesSection.Builder education =
                EntriesSection.builder("Education & Certifications");
        for (int i = 1; i <= 6; i++) {
            education.entry("Degree " + i, "Institution " + i,
                    (2000 + i) + "-" + (2004 + i), "");
        }
        sections.add(education.build());

        // Six blocks matching no module keyword, so each lands in the main
        // column under its own title. Sized so two fit a page and a third
        // does not.
        for (int block = 1; block <= 20; block++) {
            RowsSection.Builder advisory =
                    RowsSection.builder("Advisory Board " + block, RowStyle.PLAIN);
            for (int row = 1; row <= 4; row++) {
                advisory.row("Programme " + row, "Reviewed delivery plans.");
            }
            sections.add(advisory.build());
        }

        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jane", "Doe")
                        .jobTitle("Backend Engineer")
                        .contact("+44 0", "j@d.com", "London")
                        .link("GitHub", "https://github.com/jane")
                        .build())
                .sections(sections.toArray(new CvSection[0]))
                .build();
    }
}
