package com.demcha.compose.document.dsl;

import com.demcha.compose.document.layout.LayoutAnchorId;
import com.demcha.compose.document.layout.LayoutAnchorNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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
    void aPerEntryStyleBeatsTheTimelineDefaultForThatSlotAlone() {
        // Precedence, and its scope: the entry that overrides gets its own style, the slots
        // it did not override keep the timeline's, and the entry beside it is untouched.
        // Three separate things one `override != null` decides, so they are asserted
        // together — and the resolution moved when the model was normalized.
        DocumentTextStyle timelineTitle = DocumentTextStyle.builder().size(19).build();
        DocumentTextStyle timelineMeta = DocumentTextStyle.builder().size(7).build();
        DocumentTextStyle timelineBody = DocumentTextStyle.builder().size(11).build();
        DocumentTextStyle ownTitle = DocumentTextStyle.builder().size(23).build();
        DocumentTextStyle ownBody = DocumentTextStyle.builder().size(5).build();

        SectionNode timeline = timelineOf(t -> t
                .titleStyle(timelineTitle).metaStyle(timelineMeta).bodyStyle(timelineBody)
                .entry(TimelineMarker.dot(8, NAVY), e -> e
                        .title("Overridden", ownTitle).meta("M").body("B", ownBody))
                .entry(TimelineMarker.dot(8, NAVY), e -> e
                        .title("Plain").meta("M").body("B")));

        List<ParagraphNode> overridden = paragraphsOf(header(entry(timeline, 0)).children().get(1));
        assertThat(overridden.get(0).textStyle().size()).as("its own title style")
                .isEqualTo(23.0, within(1e-9));
        assertThat(overridden.get(1).textStyle().size()).as("the meta it did not override")
                .isEqualTo(7.0, within(1e-9));
        assertThat(lastParagraph(entry(timeline, 0)).textStyle().size()).isEqualTo(5.0, within(1e-9));

        List<ParagraphNode> plain = paragraphsOf(header(entry(timeline, 1)).children().get(1));
        assertThat(plain.get(0).textStyle().size()).as("the next entry is unaffected")
                .isEqualTo(19.0, within(1e-9));
        assertThat(lastParagraph(entry(timeline, 1)).textStyle().size()).isEqualTo(11.0, within(1e-9));
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

    // --- the two ways to describe an entry -----------------------------------

    @Test
    void theShorthandAndTheLongFormBuildTheSameEntry() {
        // entry(marker, ...) is meant to be sugar, not a second path. If it ever grows one,
        // every guarantee proven through one form stops covering the other.
        SectionNode shorthand = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e
                        .title("Senior Engineer").meta("2023 - now").body("What I did.")));
        SectionNode longForm = timelineOf(t -> t
                .entry(e -> e
                        .marker(TimelineMarker.dot(8, NAVY))
                        .title("Senior Engineer").meta("2023 - now").body("What I did.")));

        assertThat(outline(longForm))
                .as("same structure, same text, same order")
                .isEqualTo(outline(shorthand));
    }

    @Test
    void contentFillsTheEntrysColumnInsteadOfATitleAndABody() {
        SectionNode timeline = timelineOf(t -> t
                .entry(e -> e
                        .marker(TimelineMarker.dot(8, NAVY))
                        .content(column -> column
                                .addParagraph("Mine, first")
                                .addParagraph("Mine, second"))));

        SectionNode entry = entry(timeline, 0);
        assertThat(paragraphTexts(header(entry).children().get(1)))
                .as("the caller's blocks land in the content column of the header row")
                .containsExactly("Mine, first", "Mine, second");
        assertThat(paragraphsOf(entry))
                .as("and nothing hangs below the header row, because no body was described")
                .isEmpty();
    }

    @Test
    void theSemanticApiAndContentCannotBeMixed() {
        String message = "Cannot combine title/meta/body entry content with custom content(). "
                         + "Use either the semantic entry API or content().";

        assertThatIllegalStateException()
                .as("semantic first")
                .isThrownBy(() -> timelineOf(t -> t.entry(e -> e
                        .marker(TimelineMarker.dot(8, NAVY))
                        .title("T")
                        .content(column -> column.addParagraph("also this")))))
                .withMessage(message);

        assertThatIllegalStateException()
                .as("and custom first — the rule is not about which came last")
                .isThrownBy(() -> timelineOf(t -> t.entry(e -> e
                        .marker(TimelineMarker.dot(8, NAVY))
                        .content(column -> column.addParagraph("mine"))
                        .title("T"))))
                .withMessage(message);
    }

    @Test
    void aStyleOverrideOnItsOwnAlreadyCommitsTheEntryToTheSemanticApi() {
        // Easy to miss when the rule is read as "title, meta or body": a style override
        // names a slot that content() does not have, so the two are just as incompatible.
        assertThatIllegalStateException()
                .isThrownBy(() -> timelineOf(t -> t.entry(e -> e
                        .marker(TimelineMarker.dot(8, NAVY))
                        .titleStyle(DocumentTextStyle.builder().size(12).build())
                        .content(column -> column.addParagraph("mine")))))
                .withMessageContaining("either the semantic entry API or content()");
    }

    @Test
    void anEntryWithoutAMarkerSaysWhichCallIsMissing() {
        assertThatIllegalStateException()
                .isThrownBy(() -> timelineOf(t -> t.entry(e -> e.title("no marker"))))
                .withMessageContaining("marker(...)");
    }

    @Test
    void theShorthandsMarkerCannotBeReplacedFromInsideTheEntry() {
        // Two markers declared for one entry. Letting either win silently is the kind of
        // order-dependence that only shows up in the rendered document.
        assertThatIllegalStateException()
                .isThrownBy(() -> timelineOf(t -> t
                        .entry(TimelineMarker.dot(8, NAVY), e -> e
                                .marker(TimelineMarker.dot(12, NAVY))
                                .title("T"))))
                .withMessageContaining("exactly one marker")
                .withMessageContaining("entry(marker, ...)");
    }

    @Test
    void aMarkerCannotBeDeclaredTwiceInTheAdvancedFormEither() {
        // The same invariant, reached without the shorthand. A guard keyed on "the marker
        // came from entry(marker, ...)" would let this one through and silently keep the
        // second marker — one entry, two markers declared, no error.
        assertThatIllegalStateException()
                .isThrownBy(() -> timelineOf(t -> t
                        .entry(e -> e
                                .marker(TimelineMarker.dot(8, NAVY))
                                .marker(TimelineMarker.square(12, NAVY))
                                .title("T"))))
                .withMessageContaining("exactly one marker");
    }

    // --- the axis column -----------------------------------------------------

    @Test
    void axisWidthSizesTheMarkerColumnInPointsInsteadOfShares() {
        SectionNode timeline = timelineOf(t -> t
                .axisWidth(18)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(header(entry(timeline, 0)).columns())
                .as("a fixed axis, then the content column taking the rest")
                .containsExactly(DocumentRowColumn.fixed(18), DocumentRowColumn.weight(1.0));
    }

    @Test
    void aWeightAxisStillReachesTheRowAsWeightsAsItAlwaysHas() {
        // columns(weight, weight) resolves identically — the snapshots say so. But
        // RowNode.weights() is public, and spelling it the other way would empty that list
        // for every timeline already written.
        SectionNode timeline = timelineOf(t -> t
                .markerColumnWeight(0.4)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(header(entry(timeline, 0)).weights()).containsExactly(0.4, 1.0);
        assertThat(header(entry(timeline, 0)).columns()).isEmpty();
    }

    @Test
    void theAxisWidthIsDeclaredOnceInEitherOrder() {
        // A weight is a share of the row and a fixed width is points. There is no
        // conversion between them without a row width, so a timeline that asks for both
        // has not said what it wants.
        assertThatIllegalStateException()
                .as("weight then points")
                .isThrownBy(() -> timelineOf(t -> t.markerColumnWeight(0.4).axisWidth(18)))
                .withMessageContaining("one width, declared once");
        assertThatIllegalStateException()
                .as("points then weight")
                .isThrownBy(() -> timelineOf(t -> t.axisWidth(18).markerColumnWeight(0.4)))
                .withMessageContaining("one width, declared once");
    }

    @Test
    void anIgnoredMarkerColumnWeightDoesNotCountAsDeclaringTheAxis() {
        // markerColumnWeight has always ignored a non-positive value. A call that changed
        // nothing must not then block axisWidth — that would be a new failure in code that
        // used to work.
        SectionNode timeline = timelineOf(t -> t
                .markerColumnWeight(0)
                .axisWidth(18)
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x")));

        assertThat(header(entry(timeline, 0)).columns())
                .containsExactly(DocumentRowColumn.fixed(18), DocumentRowColumn.weight(1.0));
    }

    @Test
    void axisWidthTakesOnlyAPositiveFiniteNumberOfPoints() {
        assertThatIllegalArgumentException().isThrownBy(() -> timelineOf(t -> t.axisWidth(0)))
                .withMessageContaining("positive finite");
        assertThatIllegalArgumentException().isThrownBy(() -> timelineOf(t -> t.axisWidth(-4)))
                .withMessageContaining("positive finite");
        assertThatIllegalArgumentException().isThrownBy(() -> timelineOf(t -> t.axisWidth(Double.NaN)))
                .withMessageContaining("positive finite");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> timelineOf(t -> t.axisWidth(Double.POSITIVE_INFINITY)))
                .withMessageContaining("positive finite");
    }

    // --- the leading column --------------------------------------------------

    @Test
    void aLeadingColumnPutsAThirdColumnBeforeTheMarker() {
        SectionNode timeline = timelineOf(t -> t
                .leadingColumn(DocumentRowColumn.fixed(48))
                .entry(e -> e
                        .marker(TimelineMarker.dot(8, NAVY))
                        .leading(date -> date.addParagraph("2023"))
                        .title("Senior Engineer")));

        RowNode header = header(entry(timeline, 0));
        assertThat(header.children()).hasSize(3);
        assertThat(paragraphTexts(header.children().get(0)))
                .as("leading first, before the marker")
                .containsExactly("2023");
        assertThat(markerContent(header.children().get(1)))
                .as("then the marker")
                .isInstanceOf(EllipseNode.class);
        assertThat(paragraphTexts(header.children().get(2))).containsExactly("Senior Engineer");
    }

    @Test
    void anEntryWithNoLeadingContentStillGetsTheColumn() {
        // The column belongs to the timeline, not to the entry. An entry that skips it has
        // to keep the empty space, or its marker starts where another entry's date starts.
        SectionNode timeline = timelineOf(t -> t
                .leadingColumn(DocumentRowColumn.fixed(48))
                .entry(e -> e.marker(TimelineMarker.dot(8, NAVY))
                        .leading(date -> date.addParagraph("2023")).title("With"))
                .entry(e -> e.marker(TimelineMarker.dot(8, NAVY)).title("Without")));

        RowNode withoutLeading = header(entry(timeline, 1));
        assertThat(withoutLeading.children())
                .as("three columns either way")
                .hasSize(3);
        assertThat(paragraphTexts(withoutLeading.children().get(0)))
                .as("the first is simply empty")
                .isEmpty();
        assertThat(markerContent(withoutLeading.children().get(1)))
                .as("so the marker is still the second column, as in the entry above")
                .isInstanceOf(EllipseNode.class);
    }

    @Test
    void withNoLeadingColumnTheHeaderRowIsTheTwoColumnOneItAlwaysWas() {
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("T")));

        assertThat(header(entry(timeline, 0)).children())
                .as("declaring no leading column adds no column")
                .hasSize(2);
    }

    @Test
    void anAutoLeadingColumnIsRejectedForTheReasonItWouldFail() {
        // Measured, not assumed: with auto(), a row whose leading text is "2023" and one
        // whose leading text is "September 2024 - present" put their markers 131pt apart.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> timelineOf(t -> t.leadingColumn(DocumentRowColumn.auto())))
                .withMessageContaining("measured from its own row's content")
                .withMessageContaining("fixed(points) or weight(share)");
    }

    @Test
    void leadingContentWithoutALeadingColumnNamesTheCallToAdd() {
        assertThatIllegalStateException()
                .isThrownBy(() -> timelineOf(t -> t
                        .entry(e -> e.marker(TimelineMarker.dot(8, NAVY))
                                .leading(date -> date.addParagraph("2023")))))
                .withMessageContaining("leadingColumn(...)");
    }

    @Test
    void leadingWorksWithEitherWayOfDescribingTheContent() {
        // It describes a different column, so it is not part of the choice between them.
        SectionNode semantic = timelineOf(t -> t
                .leadingColumn(DocumentRowColumn.weight(0.3))
                .entry(e -> e.marker(TimelineMarker.dot(8, NAVY))
                        .leading(d -> d.addParagraph("2023")).title("T")));
        SectionNode custom = timelineOf(t -> t
                .leadingColumn(DocumentRowColumn.weight(0.3))
                .entry(e -> e.marker(TimelineMarker.dot(8, NAVY))
                        .leading(d -> d.addParagraph("2023")).content(c -> c.addParagraph("T"))));

        assertThat(paragraphTexts(header(entry(semantic, 0)).children().get(0))).containsExactly("2023");
        assertThat(paragraphTexts(header(entry(custom, 0)).children().get(0))).containsExactly("2023");
    }

    @Test
    void aColumnSlotIsDeclaredOnceNotAssigned() {
        // marker, leading and content each take a whole column. Two of them is a mistake,
        // and which one survived should not depend on the order the calls were written in.
        assertThatIllegalStateException()
                .as("leading twice")
                .isThrownBy(() -> timelineOf(t -> t
                        .leadingColumn(DocumentRowColumn.fixed(48))
                        .entry(e -> e.marker(TimelineMarker.dot(8, NAVY))
                                .leading(d -> d.addParagraph("a"))
                                .leading(d -> d.addParagraph("b")))))
                .withMessageContaining("declares leading(...) once");

        assertThatIllegalStateException()
                .as("content twice")
                .isThrownBy(() -> timelineOf(t -> t
                        .entry(e -> e.marker(TimelineMarker.dot(8, NAVY))
                                .content(c -> c.addParagraph("a"))
                                .content(c -> c.addParagraph("b")))))
                .withMessageContaining("declares content(...) once");
    }

    // --- markers -------------------------------------------------------------

    @Test
    void aCustomMarkerRendersWithoutTheBuilderKnowingWhatItIs() {
        // The point of the factory: three shapes and a caller's own arrangement reach the
        // marker column, and nothing in TimelineBuilder was told about any of it.
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.custom(16, 16, column -> column.addLayerStack(stack -> stack
                        .back(new EllipseNode("ring", 16, 16, NAVY, null, null, null, null, null))
                        .center(new EllipseNode("disc", 10, 10, DocumentColor.WHITE,
                                null, null, null, null, null))
                        .center(new EllipseNode("pip", 4, 4, NAVY, null, null, null, null, null)))),
                        e -> e.title("Custom")));

        DocumentNode marker = markerContent(header(entry(timeline, 0)).children().get(0));
        assertThat(marker).isInstanceOf(LayerStackNode.class);
        assertThat(((LayerStackNode) marker).layers()).hasSize(3);
    }

    @Test
    void aMarkerIsWrappedInAnAnchorSoTheLayoutCanReportWhereItLanded() {
        // The wrapper every other test in this file reads through. It is what turns "the
        // marker drew three shapes" into one box the finished layout can report, and it
        // sits inside the row's column rather than being it — a row hosts a fixed set of
        // child types and this is not one of them.
        SectionNode timeline = timelineOf(t -> t
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("x"))
                .entry(TimelineMarker.dot(8, NAVY), e -> e.title("y")));

        DocumentNode first = header(entry(timeline, 0)).children().get(0).children().get(0);
        DocumentNode second = header(entry(timeline, 1)).children().get(0).children().get(0);
        assertThat(first).isInstanceOf(LayoutAnchorNode.class);

        LayoutAnchorId firstId = ((LayoutAnchorNode) first).id();
        LayoutAnchorId secondId = ((LayoutAnchorNode) second).id();
        assertThat(firstId.index()).as("the entry's position").isZero();
        assertThat(secondId.index()).isEqualTo(1);
        assertThat(firstId.kind()).isSameAs(secondId.kind());
        assertThat(firstId.groupKey())
                .as("one owner for the whole timeline, so its markers find each other")
                .isSameAs(secondId.groupKey());
    }

    @Test
    void twoTimelinesOnAPageAnchorOnDifferentOwners() {
        // The property that lets a pass ask for its own markers and get nobody else's.
        SectionNode page = new SectionBuilder()
                .addTimeline(t -> t.entry(TimelineMarker.dot(8, NAVY), e -> e.title("first timeline")))
                .addTimeline(t -> t.entry(TimelineMarker.dot(8, NAVY), e -> e.title("second timeline")))
                .build();

        Object first = ownerOf((SectionNode) page.children().get(0));
        Object second = ownerOf((SectionNode) page.children().get(1));
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void aCustomMarkerDeclaresABoxTheTimelineTakesAtItsWord() {
        // Declared rather than measured, and not required to be square: the box is what a
        // rail anchors on, and it must not depend on what the recipe happened to draw.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TimelineMarker.custom(0, 16, column -> { }))
                .withMessageContaining("width must be a positive finite");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TimelineMarker.custom(16, Double.NaN, column -> { }))
                .withMessageContaining("height must be a positive finite");
        assertThatNullPointerException()
                .isThrownBy(() -> TimelineMarker.custom(16, 16, null));
    }

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
        return markerContent(header(entry(timeline, 0)).children().get(0));
    }

    /**
     * What the marker's recipe drew, reached through the anchor that wraps it.
     *
     * <p>The column holds a {@code LayoutAnchorNode} holding the drawn marker, so that the
     * finished layout reports one box per marker however many shapes it took to draw. The
     * unwrapping lives here rather than in each test, and
     * {@link #aMarkerIsWrappedInAnAnchorSoTheLayoutCanReportWhereItLanded()} is where the
     * wrapper itself is asserted.</p>
     */
    private static DocumentNode markerContent(DocumentNode markerColumn) {
        DocumentNode anchor = markerColumn.children().get(0);
        assertThat(anchor).isInstanceOf(LayoutAnchorNode.class);
        return anchor.children().get(0).children().get(0);
    }

    /** The owner every marker in one timeline anchors on. */
    private static Object ownerOf(SectionNode timeline) {
        DocumentNode anchor = header(entry(timeline, 0)).children().get(0).children().get(0);
        return ((LayoutAnchorNode) anchor).id().groupKey();
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

    /**
     * The node tree as indented {@code Kind[text]} lines. Structure, order and text — not
     * styles, which the record types do not compare reliably anyway.
     */
    private static String outline(DocumentNode node) {
        StringBuilder out = new StringBuilder();
        outline(node, 0, out);
        return out.toString();
    }

    private static void outline(DocumentNode node, int depth, StringBuilder out) {
        out.append("  ".repeat(depth)).append(node.getClass().getSimpleName());
        if (node instanceof ParagraphNode paragraph) {
            out.append('[').append(paragraph.text()).append(']');
        }
        out.append('\n');
        for (DocumentNode child : node.children()) {
            outline(child, depth + 1, out);
        }
    }
}
