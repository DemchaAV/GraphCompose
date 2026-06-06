package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers {@link AbstractFlowBuilder#addTimeline} / {@link TimelineBuilder}: each
 * entry becomes a section carrying the connector rail (a left border), a marker
 * row, and the entry's content.
 */
class TimelineBuilderTest {

    private static final DocumentColor NAVY = DocumentColor.rgb(20, 40, 70);

    @Test
    void addTimelineProducesOneSectionPerEntryWithRail() {
        SectionNode root = new SectionBuilder().addTimeline(t -> t
                        .entry(TimelineMarker.dot(8, NAVY), e -> e.title("First").body("body one"))
                        .entry(TimelineMarker.numbered(2, 14, NAVY, DocumentColor.WHITE),
                                e -> e.title("Second").meta("2020 - 2021").body("body two")))
                .build();

        assertThat(root.children()).hasSize(1);
        SectionNode timeline = (SectionNode) root.children().get(0);
        assertThat(timeline.children()).hasSize(2);

        SectionNode entry = (SectionNode) timeline.children().get(0);
        assertThat(entry.borders().hasAny())
                .as("each entry carries the connector rail as a left border")
                .isTrue();
        assertThat(entry.children()).anySatisfy(child -> assertThat(child).isInstanceOf(RowNode.class));
        assertThat(lastParagraph(entry).text()).isEqualTo("body one");
    }

    @Test
    void entryWithoutContentStillRendersAMarkerRow() {
        SectionNode root = new SectionBuilder()
                .addTimeline(t -> t.entry(TimelineMarker.square(8, NAVY), null))
                .build();
        SectionNode timeline = (SectionNode) root.children().get(0);
        assertThat(timeline.children()).hasSize(1);
        SectionNode entry = (SectionNode) timeline.children().get(0);
        assertThat(entry.children()).anySatisfy(child -> assertThat(child).isInstanceOf(RowNode.class));
    }

    @Test
    void entryRejectsNullMarker() {
        assertThatNullPointerException().isThrownBy(() ->
                new SectionBuilder().addTimeline(t -> t.entry(null, e -> e.title("x"))));
    }

    private static ParagraphNode lastParagraph(SectionNode entry) {
        return entry.children().stream()
                .filter(ParagraphNode.class::isInstance)
                .map(ParagraphNode.class::cast)
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
