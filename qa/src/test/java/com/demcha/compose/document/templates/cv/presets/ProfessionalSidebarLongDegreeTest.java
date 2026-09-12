package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.Slot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A degree title too long for one line of the sidebar, on
 * {@link ProfessionalSidebar}.
 *
 * <p>The canonical fixture's degrees read {@code "DEGREE NAME"}, which fits,
 * and a title that fits is the only one the block was ever measured with. A
 * real degree — {@code "MSc Advanced Computer Science and Software
 * Engineering"} — wraps in a sidebar this narrow, and the row it wraps into has
 * to belong to it rather than to the line below.</p>
 *
 * <p>This is the block's own contract, not the timeline's: the rail, the dots
 * and the entry spacing are all the timeline's and are unaffected by how tall
 * one degree is. What is asserted here is that a degree which needs two lines
 * gets two lines of room.</p>
 */
class ProfessionalSidebarLongDegreeTest {

    private static final String LONG_DEGREE =
            "MSc Advanced Computer Science and Software Engineering";

    @Test
    void aDegreeThatWrapsDoesNotLandOnItsOwnInstitutionLine() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ProfessionalSidebar.create().compose(session, longDegreeCv());
            session.toPdfBytes();

            PlacedNode degree = node(session, "EducationDegree_0");
            PlacedNode institution = node(session, "EducationInstitution_0");

            assertThat(degree.placementHeight())
                    .as("the degree needs more than one line at this width")
                    .isGreaterThan(12.0);

            // y grows upwards, so the degree's foot must sit at or above the
            // institution's head. Overlap is the defect: the head band declared
            // one line of height for a title that needed two, and drew the
            // second over the line below rather than pushing it down.
            double degreeFoot = degree.placementY();
            double institutionHead = institution.placementY() + institution.placementHeight();
            assertThat(degreeFoot)
                    .as("degree foot %.3f must not fall below institution head %.3f",
                            degreeFoot, institutionHead)
                    .isGreaterThanOrEqualTo(institutionHead - 0.001);
        }
    }

    @Test
    void theSecondDegreeStillClearsTheFirstEntry() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ProfessionalSidebar.create().compose(session, longDegreeCv());
            session.toPdfBytes();

            PlacedNode firstDates = node(session, "EducationDates_0");
            PlacedNode secondDegree = node(session, "EducationDegree_1");

            assertThat(secondDegree.placementY() + secondDegree.placementHeight())
                    .as("a taller first entry pushes the second one down")
                    .isLessThanOrEqualTo(firstDates.placementY() + 0.001);
        }
    }

    private static PlacedNode node(DocumentSession session, String name) {
        return session.layoutGraph().nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no node named " + name));
    }

    /** The canonical fixture with one degree title that cannot fit one line. */
    private static CvDocument longDegreeCv() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("YOUR", "NAME")
                        .jobTitle("YOUR PROFESSIONAL TITLE")
                        .contact(new Contact("+1 234 567 8900",
                                "your.email@example.com",
                                "City, Country"))
                        .build())
                .section(Slot.SIDEBAR, EntriesSection.builder("EDUCATION")
                        .entry(LONG_DEGREE, "University Name", "2016 – 2020", "")
                        .entry("DEGREE NAME", "University Name", "2012 – 2016", "")
                        .build())
                .build();
    }
}
