package com.demcha.examples.templates.cv;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.presets.NavySidebar;
import com.demcha.compose.document.templates.cv.presets.ProfessionalSidebar;
import com.demcha.examples.support.NavySidebarSampleData;
import com.demcha.examples.support.ProfessionalSidebarSampleData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the sidebar CV samples to the width of the column they are drawn in.
 *
 * <p>A contact channel in both of these presets is one paragraph — the mark
 * and the value share a line — so a value wider than the sidebar's text
 * column wraps, and the mark is left alone on the first line. Nothing in the
 * promotion gates catches it: they measure the published template's own
 * fixture, whose addresses fit, while the sample data here is written for
 * the repository and can outgrow the column without a single test going
 * red. That is exactly how it shipped once, spotted in the rendered preview
 * rather than by a build.</p>
 *
 * <p>So the guard is on the samples, and it is the shape of the row rather
 * than the length of the string: a single-line channel is as tall as its
 * mark — 8.8 to 11pt across the two designs — and a wrapped one is close to
 * twice that. The bound sits between.</p>
 */
class SidebarContactRowsFitTest {

    /** Comfortably above the tallest single-line row, well under a wrapped one. */
    private static final double SINGLE_LINE_LIMIT = 14.0;

    @Test
    void professionalSidebarChannelsEachFitOneLine() {
        assertChannelsFitOneLine("Professional Sidebar",
                ProfessionalSidebar::create, ProfessionalSidebarSampleData.sample());
    }

    @Test
    void navySidebarChannelsEachFitOneLine() {
        assertChannelsFitOneLine("Navy Sidebar",
                NavySidebar::create, NavySidebarSampleData.sample());
    }

    private static void assertChannelsFitOneLine(String preset,
                                                 Supplier<DocumentTemplate<CvDocument>> factory,
                                                 CvDocument doc) {
        List<LayoutNodeSnapshot> channels;
        try (DocumentSession session = GraphCompose.document().create()) {
            factory.get().compose(session, doc);
            channels = session.layoutSnapshot().nodes().stream()
                    .filter(node -> node.entityName() != null
                            && node.entityName().startsWith("Contact_"))
                    .toList();
        }

        assertThat(channels)
                .describedAs("%s draws its contact channels as Contact_* nodes; finding none "
                        + "means the guard is looking at the wrong name, not that the sample "
                        + "is clean", preset)
                .isNotEmpty();

        List<String> wrapped = channels.stream()
                .filter(node -> node.placementHeight() > SINGLE_LINE_LIMIT)
                .map(node -> node.entityName() + " (" + node.placementHeight() + "pt)")
                .collect(Collectors.toList());

        assertThat(wrapped)
                .describedAs("%s: a contact value in the sample is wider than the sidebar "
                        + "column, so its row wrapped and left the mark alone on the first "
                        + "line. Shorten the value in the sample data — the column is not the "
                        + "thing to widen", preset)
                .isEmpty();
    }
}
