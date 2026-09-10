package com.demcha.compose.document.layout;

import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.layout.payloads.ListItemSpec;
import com.demcha.compose.document.layout.payloads.MarkerContentItem;
import com.demcha.compose.document.layout.payloads.PreparedListLayout;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.layout.DocumentNodeAdapters.toTextStyle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * The measured marker/content geometry: where a marker starts, how wide it
 * actually is, what gap follows it, and the one content origin every visual line
 * of the item shares.
 *
 * <p>Nothing here is rendered yet — wrapping and emit still run the legacy
 * pipeline, so these are the numbers the next change draws with, proved on their
 * own while the legacy output is still frozen beside them.</p>
 */
class ListMarkerGeometryTest {

    private static final double AVAILABLE = 296.0;
    private static final double EPS = 1e-9;

    // --- the marker is measured, not counted ------------------------------

    @Test
    void theMarkerWidthIsTheMeasuredGlyphWidthOfTheMarkerAlone() throws Exception {
        try (Measurer measurer = new Measurer()) {
            for (ListMarker marker : List.of(
                    ListMarker.bullet(), ListMarker.dash(),
                    ListMarker.custom(">"), ListMarker.custom("=>"), ListMarker.custom("MMM"))) {

                MarkerContentItem item = measurer.one(l -> l.marker(marker).items("Item"));
                double expected = measurer.width(marker.value().strip());

                assertThat(item.measuredMarkerWidth())
                        .as("marker %s", marker.value().strip())
                        .isEqualTo(expected, within(EPS));
            }
        }
    }

    @Test
    void theSyntheticSeparatorIsNotPartOfTheMeasuredMarker() throws Exception {
        // The separator ListMarker appends exists so "•" + text does not render
        // as "•text". Here the space between marker and content is markerGap, so
        // measuring the separator too would charge for the gap twice.
        try (Measurer measurer = new Measurer()) {
            MarkerContentItem item = measurer.one(l -> l.bullet().markerGap(0).items("Item"));

            assertThat(item.measuredMarkerWidth()).isEqualTo(measurer.width("•"), within(EPS));
            assertThat(item.measuredMarkerWidth())
                    .as("and is strictly narrower than the marker plus its separator")
                    .isLessThan(measurer.width("• "));
            assertThat(item.contentX())
                    .as("at gap 0 the content starts exactly where the marker ends")
                    .isEqualTo(item.measuredMarkerWidth(), within(EPS));
        }
    }

    @Test
    void theBulletsResolvedGeometryAtTheDefaultStyle() throws Exception {
        // The numbers themselves, so a change to any of them is a change someone
        // has to look at. Beside the legacy layout at the same style, where the
        // first line's text starts 8.792pt in and its wrapped lines start
        // 11.676pt in — the gap this replaces was never one number.
        try (Measurer measurer = new Measurer()) {
            MarkerContentItem item = measurer.one(l -> l.bullet().items("Item"));

            assertThat(item.markerX()).isZero();
            assertThat(item.measuredMarkerWidth()).isEqualTo(4.900, within(0.001));
            assertThat(item.markerGap()).isEqualTo(4.0, within(EPS));
            assertThat(item.contentX()).isEqualTo(8.900, within(0.001));
            assertThat(item.contentWidth()).isEqualTo(AVAILABLE - 8.900, within(0.001));
        }
    }

    @Test
    void aWiderMarkerPushesContentFurtherRightByExactlyItsExtraWidth() throws Exception {
        try (Measurer measurer = new Measurer()) {
            MarkerContentItem bullet = measurer.one(l -> l.bullet().items("Item"));
            MarkerContentItem wide = measurer.one(l -> l.marker("MMM").items("Item"));

            double extra = wide.measuredMarkerWidth() - bullet.measuredMarkerWidth();
            assertThat(extra).as("a constant marker width would make this vacuous").isPositive();
            assertThat(wide.contentX() - bullet.contentX()).isEqualTo(extra, within(EPS));
            assertThat(bullet.contentWidth() - wide.contentWidth()).isEqualTo(extra, within(EPS));
        }
    }

    // --- markerGap is geometry --------------------------------------------

    @Test
    void theGapIsAppliedInPointsAndTheDefaultIsFour() throws Exception {
        try (Measurer measurer = new Measurer()) {
            double markerWidth = measurer.one(l -> l.bullet().items("Item")).measuredMarkerWidth();

            for (double gap : List.of(0.0, 4.0, 8.0, 16.0)) {
                MarkerContentItem item = measurer.one(l -> l.bullet().markerGap(gap).items("Item"));
                assertThat(item.markerGap()).as("gap %s", gap).isEqualTo(gap, within(EPS));
                assertThat(item.contentX()).isEqualTo(markerWidth + gap, within(EPS));
                assertThat(item.contentWidth()).isEqualTo(AVAILABLE - markerWidth - gap, within(EPS));
            }

            assertThat(measurer.one(l -> l.bullet().items("Item")).markerGap())
                    .as("the default gap, unstated")
                    .isEqualTo(ListNode.DEFAULT_MARKER_GAP, within(EPS));
        }
    }

    @Test
    void aMarkerlessItemTakesNoWidthAndNoGap() throws Exception {
        try (Measurer measurer = new Measurer()) {
            MarkerContentItem item = measurer.one(l -> l.noMarker().markerGap(16).items("Item"));

            assertThat(item.measuredMarkerWidth()).isZero();
            assertThat(item.markerGap()).as("no marker, so no gap to leave after it").isZero();
            assertThat(item.markerX()).isZero();
            assertThat(item.contentX()).as("flush, not inset by an unexplained gap").isZero();
            assertThat(item.contentWidth()).isEqualTo(AVAILABLE, within(EPS));
        }
    }

    @Test
    void aMarkerOnlyRowStillReservesItsMarkerAndGap() throws Exception {
        try (Measurer measurer = new Measurer()) {
            List<MarkerContentItem> items = measurer.all(l -> l.bullet().items("Java", "   ", "SQL"));

            assertThat(items).hasSize(3);
            assertThat(items.get(1).content()).isEmpty();
            assertThat(items.get(1).measuredMarkerWidth()).isPositive();
            assertThat(items.get(1).contentX()).isEqualTo(items.get(0).contentX(), within(EPS));
        }
    }

    // --- depth is an outline, not a fixed step ----------------------------

    @Test
    void aChildsMarkerStartsWhereItsParentsTextStarts() throws Exception {
        try (Measurer measurer = new Measurer()) {
            List<MarkerContentItem> items = measurer.all(l -> l
                    .addItem("Top", c -> c
                            .addItem("Child", g -> g.addItem("Grandchild"))));

            assertThat(items).hasSize(3);
            MarkerContentItem top = items.get(0);
            MarkerContentItem child = items.get(1);
            MarkerContentItem grandchild = items.get(2);

            assertThat(top.markerX()).isZero();
            assertThat(child.markerX()).isEqualTo(top.contentX(), within(EPS));
            assertThat(grandchild.markerX()).isEqualTo(child.contentX(), within(EPS));

            // ...and each level's own content still clears its own marker.
            assertThat(child.contentX())
                    .isEqualTo(child.markerX() + child.measuredMarkerWidth() + child.markerGap(), within(EPS));
            assertThat(grandchild.contentWidth())
                    .isEqualTo(AVAILABLE - grandchild.contentX(), within(EPS));
        }
    }

    @Test
    void siblingsAtOneDepthShareAMarkerColumnEvenWithDifferentMarkerWidths() throws Exception {
        try (Measurer measurer = new Measurer()) {
            List<MarkerContentItem> items = measurer.all(l -> l
                    .addItem("Parent", c -> c
                            .addItem("Narrow")
                            .addItem("Wide")));

            assertThat(items).hasSize(3);
            assertThat(items.get(2).markerX())
                    .as("the second child hangs under the parent, not under its sibling")
                    .isEqualTo(items.get(1).markerX(), within(EPS));
        }
    }

    @Test
    void returningToAShallowerDepthReturnsToThatDepthsColumn() throws Exception {
        try (Measurer measurer = new Measurer()) {
            List<MarkerContentItem> items = measurer.all(l -> l
                    .addItem("First", c -> c.addItem("Child"))
                    .addItem("Second"));

            assertThat(items).extracting(MarkerContentItem::depth).containsExactly(0, 1, 0);
            assertThat(items.get(2).markerX())
                    .as("back at the top level, back at its origin")
                    .isEqualTo(items.get(0).markerX(), within(EPS));
        }
    }

    // --- container interaction --------------------------------------------

    @Test
    void geometryIsRelativeToTheItemStartSoPaddingDoesNotEnterIt() throws Exception {
        try (Measurer measurer = new Measurer()) {
            MarkerContentItem plain = measurer.one(l -> l.bullet().items("Item"));
            MarkerContentItem padded = measurer.one(l -> l.bullet().padding(3, 5, 7, 11).items("Item"));

            assertThat(padded.markerX()).as("padding moves the row, not the marker within it").isZero();
            assertThat(padded.contentX()).isEqualTo(plain.contentX(), within(EPS));
            // Only the room left for text shrinks, by the horizontal padding.
            assertThat(padded.contentWidth()).isEqualTo(plain.contentWidth() - 16.0, within(EPS));
        }
    }

    @Test
    void aContainerTooNarrowForTheMarkerLetsContentOverflowRatherThanVanish() throws Exception {
        try (Measurer measurer = new Measurer(12.0)) {
            MarkerContentItem item = measurer.one(l -> l.marker("MMMMMMMMMM").markerGap(16).items("Item"));

            assertThat(item.contentX()).isGreaterThan(12.0);
            assertThat(item.contentWidth())
                    .as("clamped to something drawable, matching the legacy wrap clamp")
                    .isEqualTo(1.0, within(EPS));
        }
    }

    // --- the record defends its own invariant ------------------------------

    @Test
    void aResolvedItemMustAgreeWithItsOwnParts() {
        ListItemSpec spec = new ListItemSpec(0, ListMarker.bullet(), "Item");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MarkerContentItem(spec, 0, 5, 4, 99, 100))
                .withMessageContaining("contentX must be markerX + measuredMarkerWidth + markerGap");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MarkerContentItem(spec, 0, -1, 4, 3, 100))
                .withMessageContaining("measuredMarkerWidth");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MarkerContentItem(spec, 0, Double.NaN, 4, 4, 100))
                .withMessageContaining("measuredMarkerWidth");
    }

    // ------------------------------------------------------------------

    /** Resolves list geometry through the real prepare pass and measures text the way it does. */
    private static final class Measurer implements AutoCloseable {
        private final PDDocument document = new PDDocument();
        private final FontLibrary fonts;
        private final TextMeasurementSystem measurement;
        private final double available;

        private Measurer() throws Exception {
            this(AVAILABLE);
        }

        private Measurer(double available) throws Exception {
            this.fonts = PdfFontLibraryFactory.library(document);
            this.measurement = new FontLibraryTextMeasurementSystem(fonts, PdfFont.class);
            this.available = available;
        }

        private List<MarkerContentItem> all(Consumer<ListBuilder> spec) {
            ListBuilder builder = new ListBuilder().name("L").hangingIndent(true);
            spec.accept(builder);
            ListNode node = builder.build();
            return TextFlowSupport.prepareList(node, context(), new BoxConstraints(available, 216.0))
                    .requirePreparedLayout(PreparedListLayout.class)
                    .markerContentItems();
        }

        private MarkerContentItem one(Consumer<ListBuilder> spec) {
            List<MarkerContentItem> items = all(spec);
            assertThat(items).hasSize(1);
            return items.get(0);
        }

        private double width(String text) {
            return measurement.textWidth(toTextStyle(DocumentTextStyle.DEFAULT), text);
        }

        private PrepareContext context() {
            return new PrepareContext() {
                @Override
                public <E extends com.demcha.compose.document.node.DocumentNode> PreparedNode<E> prepare(
                        E node, BoxConstraints constraints) {
                    throw new UnsupportedOperationException("a list leaf prepares no children");
                }

                @Override
                public FontLibrary fonts() {
                    return fonts;
                }

                @Override
                public TextMeasurementSystem textMeasurement() {
                    return measurement;
                }

                @Override
                public LayoutCanvas canvas() {
                    return LayoutCanvas.from(320, 240, new Margin(12, 12, 12, 12));
                }
            };
        }

        @Override
        public void close() throws Exception {
            document.close();
        }
    }
}
