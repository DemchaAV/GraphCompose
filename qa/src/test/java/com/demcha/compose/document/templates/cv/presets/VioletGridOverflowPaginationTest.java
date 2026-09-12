package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * What {@link VioletGrid} promises past its one page.
 *
 * <p>The preset is drawn for a single sheet, and says so: a CV with more content than the
 * design holds "runs onto a second page rather than losing anything — each experience entry
 * is held together, so a role is never cut in half". The canonical fixture fits one page, so
 * nothing exercised that promise and nothing would notice it breaking. This does, on
 * {@link VioletGridFixtures#overflowCv()}, which is the canonical document with six earlier
 * roles appended and no other change.</p>
 *
 * <p>A role is identified here by the parts that carry its index — its dates, its disc, its
 * title line and its bullets — and never by the box that happens to hold them. The promise
 * is about the role, and a test that named the container would be pinning one way of
 * drawing it: it would have to be rewritten to say the same thing the moment the container
 * changed, which is exactly when it is supposed to be watching.</p>
 *
 * <p>Three things are pinned. The sheet grows a page rather than dropping content, and every
 * part of a role lands on one page. Which role sits on which page is recorded too, because a
 * change there is a change to the design's pagination and should have to be argued for rather
 * than discovered later. And the three columns hold their x on <em>both</em> pages: the dates
 * hang in the left margin and the disc rides the rail by construction on a sheet that never
 * breaks, but whether they still do on a page the design was not drawn for is a separate
 * question.</p>
 */
class VioletGridOverflowPaginationTest {

    /**
     * The parts a role is made of, each named {@code <part>_<role>} or deeper.
     *
     * <p>None of these is a prefix of another: {@code Highlights_0} and {@code
     * HighlightDot_0_1} both survive the {@code Highlight_} test, because what follows the
     * prefix has to be digits and then either nothing or an underscore.</p>
     */
    private static final List<String> ROLE_PARTS = List.of(
            "Period_", "Marker_", "TitleTable_", "Role_", "Location_",
            "Highlights_", "HighlightDot_", "Highlight_");

    @Test
    void theSheetGrowsAPageAndNoRoleIsCutInHalf() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            VioletGrid.create().compose(session, VioletGridFixtures.overflowCv());
            var snapshot = session.layoutSnapshot();

            assertThat(snapshot.totalPages())
                    .as("the overflow document runs onto a second page rather than losing content")
                    .isEqualTo(2);

            // Every page any part of a role was placed on. A role held together contributes
            // exactly one; a role cut in half contributes two, and so does one whose bullets
            // were left behind on the page above.
            Map<Integer, TreeSet<Integer>> pagesPerRole = new TreeMap<>();
            for (LayoutNodeSnapshot node : snapshot.nodes()) {
                int role = roleOf(node.entityName());
                if (role < 0) {
                    continue;
                }
                TreeSet<Integer> pages =
                        pagesPerRole.computeIfAbsent(role, key -> new TreeSet<>());
                pages.add(node.startPage());
                pages.add(node.endPage());
            }

            assertThat(pagesPerRole.keySet())
                    .as("nine roles, three canonical and six earlier")
                    .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8);
            assertThat(pagesPerRole)
                    .as("every role is held together: a role is never cut in half")
                    .allSatisfy((role, pages) -> assertThat(pages)
                            .as("role %d spans pages %s", role, pages).hasSize(1));

            Map<Integer, Integer> ownership = new TreeMap<>();
            pagesPerRole.forEach((role, pages) -> ownership.put(role, pages.first()));
            assertThat(ownership)
                    .as("which role sits on which page is part of the design's pagination: the"
                        + " break falls between the sixth and the seventh")
                    .containsExactlyInAnyOrderEntriesOf(Map.of(
                            0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0,
                            6, 1, 7, 1, 8, 1));
        }
    }

    @Test
    void theSectionsAfterTheRolesFollowThemOntoTheSecondPage() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            VioletGrid.create().compose(session, VioletGridFixtures.overflowCv());
            Map<String, Integer> pages = new TreeMap<>();
            for (LayoutNodeSnapshot node : session.layoutSnapshot().nodes()) {
                String name = node.entityName();
                if (name != null && (name.equals("ProjectsHeading") || name.equals("Project_0")
                                     || name.equals("CredentialHeadings") || name.equals("QuoteBand"))) {
                    pages.put(name, node.startPage());
                }
            }
            assertThat(pages)
                    .as("downstream pagination: everything after the roles is on the second page")
                    .containsExactlyInAnyOrderEntriesOf(Map.of(
                            "ProjectsHeading", 1,
                            "Project_0", 1,
                            "CredentialHeadings", 1,
                            "QuoteBand", 1));
        }
    }

    @Test
    void theThreeColumnsKeepTheirXOnBothPages() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            VioletGrid.create().compose(session, VioletGridFixtures.overflowCv());

            Map<String, List<Double>> columns = new TreeMap<>();
            for (LayoutNodeSnapshot node : session.layoutSnapshot().nodes()) {
                String name = node.entityName();
                if (name == null) {
                    continue;
                }
                if (name.startsWith("Period_")) {
                    column(columns, "date").add(node.placementX());
                } else if (name.startsWith("Marker_")) {
                    // The disc's centre, which is the line the rail is drawn on.
                    column(columns, "marker")
                            .add(node.placementX() + node.placementWidth() / 2.0);
                } else if (name.startsWith("TitleTable_")) {
                    column(columns, "content").add(node.placementX());
                    column(columns, "contentWidth").add(node.placementWidth());
                }
            }

            // All nine roles, not one sample per column: a single sample would pass on a
            // document whose second page had drifted, and a second page is the whole point
            // of this fixture.
            assertColumn(columns, "date", 29.905,
                    "the dates hang in the left margin, flush with the page margin");
            assertColumn(columns, "marker", 105.796, "the disc's centre is the rail");
            assertColumn(columns, "content", 125.826,
                    "the copy starts one entry indent right of the rail");
            assertColumn(columns, "contentWidth", 439.535,
                    "and runs the full entry width on both pages");
        }
    }

    /**
     * Which role a node belongs to, or -1 for a node that is not part of one.
     *
     * @param name the node's entity name, possibly {@code null}
     * @return the role's index, or -1
     */
    private static int roleOf(String name) {
        if (name == null) {
            return -1;
        }
        for (String part : ROLE_PARTS) {
            if (!name.startsWith(part)) {
                continue;
            }
            String rest = name.substring(part.length());
            int digits = 0;
            while (digits < rest.length() && Character.isDigit(rest.charAt(digits))) {
                digits++;
            }
            if (digits == 0 || (digits < rest.length() && rest.charAt(digits) != '_')) {
                continue;
            }
            return Integer.parseInt(rest.substring(0, digits));
        }
        return -1;
    }

    private static List<Double> column(Map<String, List<Double>> columns, String key) {
        return columns.computeIfAbsent(key, name -> new ArrayList<>());
    }

    private static void assertColumn(Map<String, List<Double>> columns, String key,
                                     double expected, String because) {
        assertThat(columns.get(key)).as(because).hasSize(9)
                .allSatisfy(value -> assertThat(value).isCloseTo(expected, within(0.01)));
    }
}
