package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

/**
 * Covers {@link AbstractFlowBuilder#addTimeline} / {@link TimelineBuilder}: each
 * entry becomes a section carrying the connector rail (a left border), a marker
 * row, and the entry's content.
 *
 * <p>Most of this file is a freeze. The rail is about to be reworked from a
 * per-entry left border into one logical axis anchored to markers, and nothing
 * pinned the current tree — three tests and a blank-page pixel smoke. These cases
 * exist so the rework has something to move <em>against</em>: every public method
 * is asserted at the node level, so a change that quietly drops one goes red here
 * rather than in a reader's document.</p>
 */
class TimelineBuilderTest {

    private static final DocumentColor NAVY = DocumentColor.rgb(20, 40, 70);

    // The defaults TimelineBuilder ships with, restated so a silent change to one
    // fails here by name instead of shifting every timeline already in the wild.
    private static final DocumentColor DEFAULT_RAIL = DocumentColor.rgb(150, 158, 172);
    private static final double DEFAULT_RAIL_WIDTH = 1.5;
    private static final double DEFAULT_GUTTER = 8.0;
    private static final double DEFAULT_MARKER_GAP = 8.0;
    private static final double DEFAULT_MARKER_COLUMN_WEIGHT = 0.10;
    private static final double DEFAULT_ENTRY_SPACING = 14.0;

    @Test
    void addTimelineProducesOneSectionPerEntryWithRail() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("First").body("body one"))
                .entry(TimelineMarker.numbered(2, 14, NAVY, DocumentColor.WHITE),
                        e -> e.title("Second").meta("2020 - 2021").body("body two")));

        assertThat(timeline.children()).hasSize(2);
        SectionNode entry = entry(timeline, 0);
        assertThat(entry.borders().hasAny())
                .as("each entry carries the connector rail as a left border")
                .isTrue();
        assertThat(entry.children()).anySatisfy(child -> assertThat(child).isInstanceOf(RowNode.class));
        assertThat(lastParagraph(entry).text()).isEqualTo("body one");
    }

    @Test
    void entryWithoutContentStillRendersAMarkerRow() {
        SectionNode timeline = timelineOf(t -> t.entry(TimelineMarker.square(8, NAVY), null));
        assertThat(timeline.children()).hasSize(1);
        assertThat(entry(timeline, 0).children())
                .anySatisfy(child -> assertThat(child).isInstanceOf(RowNode.class));
    }

    @Test
    void entryRejectsNullMarker() {
        assertThatNullPointerException().isThrownBy(() ->
                new SectionBuilder().addTimeline(t -> t.entry(null, e -> e.title("x"))));
    }

    // --- the rail ------------------------------------------------------------

    @Test
    void theDefaultRailIsAMutedGreyHairline() {
        SectionNode timeline = timelineOf(t -> t.entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(entry(timeline, 0).borders().left().color().color())
                .isEqualTo(DEFAULT_RAIL.color());
        assertThat(entry(timeline, 0).borders().left().width())
                .isEqualTo(DEFAULT_RAIL_WIDTH, within(1e-9));
    }

    @Test
    void connectorSetsTheRailColourAndWidth() {
        SectionNode timeline = timelineOf(t -> t
                .connector(NAVY, 3.25)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(entry(timeline, 0).borders().left().color().color()).isEqualTo(NAVY.color());
        assertThat(entry(timeline, 0).borders().left().width()).isEqualTo(3.25, within(1e-9));
    }

    // --- spacing and the columns ---------------------------------------------

    @Test
    void gutterBecomesTheEntrysLeftPadding() {
        SectionNode timeline = timelineOf(t -> t
                .gutter(21)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(entry(timeline, 0).padding().left()).isEqualTo(21.0, within(1e-9));
    }

    @Test
    void spacingIsBottomPaddingOnEveryEntryButTheLast() {
        SectionNode timeline = timelineOf(t -> t
                .spacing(20)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("First"))
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("Second")));

        assertThat(entry(timeline, 0).padding().bottom())
                .as("the gap is padding inside the bordered entry, so the rail crosses it")
                .isEqualTo(20.0, within(1e-9));
        assertThat(entry(timeline, 1).padding().bottom())
                .as("no spacing after the last entry")
                .isEqualTo(0.0, within(1e-9));
    }

    @Test
    void markerGapBecomesTheHeaderRowGap() {
        SectionNode timeline = timelineOf(t -> t
                .markerGap(17)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(header(entry(timeline, 0)).gap()).isEqualTo(17.0, within(1e-9));
    }

    @Test
    void markerColumnWeightBecomesTheHeaderRowWeights() {
        SectionNode timeline = timelineOf(t -> t
                .markerColumnWeight(0.4)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(header(entry(timeline, 0)).weights())
                .as("marker column then content column")
                .containsExactly(0.4, 1.0);
    }

    @Test
    void theDefaultsAreTheOnesTimelinesInTheWildAlreadyRenderWith() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("First"))
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("Second")));

        SectionNode first = entry(timeline, 0);
        assertThat(first.padding().left()).isEqualTo(DEFAULT_GUTTER, within(1e-9));
        assertThat(first.padding().bottom()).isEqualTo(DEFAULT_ENTRY_SPACING, within(1e-9));
        assertThat(header(first).gap()).isEqualTo(DEFAULT_MARKER_GAP, within(1e-9));
        assertThat(header(first).weights()).containsExactly(DEFAULT_MARKER_COLUMN_WEIGHT, 1.0);
    }

    // --- pagination flags ----------------------------------------------------

    @Test
    void keepTogetherHoldsTheWholeTimelineNotTheEntries() {
        SectionNode timeline = timelineOf(t -> t
                .keepTogether()
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(timeline.keepTogether()).isTrue();
        assertThat(entry(timeline, 0).keepTogether())
                .as("keepTogether is about the timeline; the entries stay free")
                .isFalse();
    }

    @Test
    void keepEntriesTogetherHoldsEachEntryNotTheTimeline() {
        SectionNode timeline = timelineOf(t -> t
                .keepEntriesTogether()
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("First"))
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("Second")));

        assertThat(timeline.keepTogether()).isFalse();
        assertThat(entry(timeline, 0).keepTogether()).isTrue();
        assertThat(entry(timeline, 1).keepTogether()).isTrue();
    }

    // --- content and styles --------------------------------------------------

    @Test
    void titleAndMetaStackInTheHeaderWhileBodyHangsOffTheEntry() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e
                        .title("Senior Engineer").meta("2023 - now").body("What I did.")));

        SectionNode entry = entry(timeline, 0);
        assertThat(paragraphTexts(header(entry).children().get(1)))
                .as("title and meta stack in the content column of the header row")
                .containsExactly("Senior Engineer", "2023 - now");
        assertThat(lastParagraph(entry).text()).isEqualTo("What I did.");
    }

    @Test
    void perTimelineStylesReachTheParagraphsThatUseThem() {
        DocumentTextStyle title = DocumentTextStyle.builder().size(19).build();
        DocumentTextStyle meta = DocumentTextStyle.builder().size(7).build();
        DocumentTextStyle body = DocumentTextStyle.builder().size(11).build();

        SectionNode timeline = timelineOf(t -> t
                .titleStyle(title).metaStyle(meta).bodyStyle(body)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("T").meta("M").body("B")));

        SectionNode entry = entry(timeline, 0);
        List<ParagraphNode> head = paragraphsOf(header(entry).children().get(1));
        assertThat(head.get(0).textStyle().size()).isEqualTo(19.0, within(1e-9));
        assertThat(head.get(1).textStyle().size()).isEqualTo(7.0, within(1e-9));
        assertThat(lastParagraph(entry).textStyle().size()).isEqualTo(11.0, within(1e-9));
    }

    @Test
    void anEntryOmitsTheParagraphsItWasNotGiven() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("Only a title")));

        assertThat(paragraphTexts(header(entry(timeline, 0)).children().get(1)))
                .containsExactly("Only a title");
        assertThat(paragraphsOf(entry(timeline, 0)))
                .as("no body paragraph when no body was given")
                .isEmpty();
    }

    @Test
    void addHangsExtraContentOffTheEntryItself() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e
                        .title("T")
                        .add(extra -> extra.addParagraph("appended"))));

        assertThat(paragraphTexts(entry(timeline, 0)))
                .as("add() content is a sibling of the header row, not of the title")
                .contains("appended");
    }

    // --- markers -------------------------------------------------------------

    @Test
    void everyMarkerFactoryPutsItsOwnShapeInTheMarkerColumn() {
        assertThat(markerNode(TimelineMarker.dot(8, NAVY))).isInstanceOf(EllipseNode.class);
        assertThat(markerNode(TimelineMarker.circle(8, NAVY, null))).isInstanceOf(EllipseNode.class);
        assertThat(markerNode(TimelineMarker.numbered(3, 14, NAVY, DocumentColor.WHITE)))
                .isInstanceOf(ShapeContainerNode.class);
        assertThat(markerNode(TimelineMarker.square(8, NAVY))).isInstanceOf(ShapeNode.class);
    }

    // --- the shape of the whole tree ----------------------------------------

    @Test
    void anUntouchedTimelineBuildsTheSameTreeItAlwaysHas() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e
                        .title("First").meta("2021 - now").body("body one"))
                .entry(TimelineMarker.numbered(2, 14, NAVY, DocumentColor.WHITE),
                        e -> e.title("Second").body("body two")));

        assertThat(timeline.children()).hasSize(2);
        SectionNode first = entry(timeline, 0);

        assertThat(first.borders().hasAny()).isTrue();
        assertThat(first.padding().left()).as("the default gutter").isEqualTo(DEFAULT_GUTTER, within(1e-9));
        assertThat(first.padding().bottom())
                .as("the default entry spacing").isEqualTo(DEFAULT_ENTRY_SPACING, within(1e-9));
        assertThat(first.children().get(0)).isInstanceOf(RowNode.class);
        assertThat(first.children()).noneSatisfy(child ->
                assertThat(child).isInstanceOf(LayerStackNode.class));

        assertThat(header(first).children()).as("marker column then title column").hasSize(2);
        assertThat(lastParagraph(first).text()).isEqualTo("body one");
        assertThat(entry(timeline, 1).padding().bottom())
                .as("no spacing after the last entry").isEqualTo(0.0, within(1e-9));
    }

    // --- helpers -------------------------------------------------------------

    private static SectionNode timelineOf(Consumer<TimelineBuilder> spec) {
        SectionNode root = new SectionBuilder().addTimeline(spec).build();
        return (SectionNode) root.children().get(0);
    }

    private static SectionNode entry(SectionNode timeline, int index) {
        return (SectionNode) timeline.children().get(index);
    }

    private static RowNode header(SectionNode entry) {
        return entry.children().stream()
                .filter(RowNode.class::isInstance)
                .map(RowNode.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no header RowNode in the entry"));
    }

    private static DocumentNode markerNode(TimelineMarker marker) {
        SectionNode timeline = timelineOf(t -> t.entry(marker, e -> e.title("x")));
        SectionNode markerColumn = (SectionNode) header(entry(timeline, 0)).children().get(0);
        return markerColumn.children().get(0);
    }

    private static List<ParagraphNode> paragraphsOf(DocumentNode parent) {
        return parent.children().stream()
                .filter(ParagraphNode.class::isInstance)
                .map(ParagraphNode.class::cast)
                .toList();
    }

    private static List<String> paragraphTexts(DocumentNode parent) {
        return paragraphsOf(parent).stream().map(ParagraphNode::text).toList();
    }

    private static ParagraphNode lastParagraph(SectionNode entry) {
        return paragraphsOf(entry).stream()
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
