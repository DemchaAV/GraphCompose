package com.demcha.compose.document.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.PreparedListLayout;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the marker/content layout costs, stated as counts rather than timings.
 *
 * <p>Two things are worth holding: the marker is measured once for the list, not
 * once per row, and resolving a list of N items stays linear in N. Both are
 * structural, so they can be asserted exactly instead of benchmarked and
 * argued about.</p>
 */
class ListMarkerContentCostTest {

    @Test
    void theMarkerIsMeasuredOncePerDistinctMarkerNotOncePerItem() throws Exception {
        try (Probe probe = new Probe()) {
            probe.prepare(l -> l.bullet().hangingIndent(true).items(items(200)));

            assertThat(probe.measuredTexts("•"))
                    .as("200 rows, one bullet measurement")
                    .isEqualTo(1);
        }
    }

    @Test
    void aNestedListMeasuresEachDepthsMarkerOnceNotOncePerRowAtThatDepth() throws Exception {
        try (Probe probe = new Probe()) {
            probe.prepare(l -> l.hangingIndent(true)
                    .addItem("Top", c -> {
                        for (int i = 0; i < 50; i++) {
                            c.addItem("Child " + i);
                        }
                    }));

            assertThat(probe.measuredTexts("•")).as("one top-level marker").isEqualTo(1);
            assertThat(probe.measuredTexts("◦")).as("fifty children, one measurement").isEqualTo(1);
        }
    }

    @Test
    void resolvingStaysLinearInTheNumberOfItems() throws Exception {
        // A quadratic resolver — one that searched back for each item's parent,
        // say — would show up here as a super-linear growth in measured work.
        try (Probe small = new Probe(); Probe large = new Probe()) {
            small.prepare(l -> l.bullet().hangingIndent(true).items(items(100)));
            large.prepare(l -> l.bullet().hangingIndent(true).items(items(400)));

            double ratio = (double) large.totalMeasurements() / small.totalMeasurements();
            assertThat(ratio)
                    .as("4x the items should be about 4x the work, not 16x (was %s → %s)",
                            small.totalMeasurements(), large.totalMeasurements())
                    .isLessThan(6.0);
        }
    }

    @Test
    void aMarkedRowEmitsTwoFragmentsAndAMarkerlessRowEmitsOne() throws Exception {
        // The cost of the layout, stated plainly: a marker column is a second
        // fragment per row. It is not a second pagination unit and not a second
        // measurement — but it is a fragment, and that is worth knowing.
        assertThat(fragmentCount(l -> l.bullet().hangingIndent(true).items("A", "B", "C")))
                .isEqualTo(6);
        assertThat(fragmentCount(l -> l.noMarker().hangingIndent(true).items("A", "B", "C")))
                .isEqualTo(3);
        assertThat(fragmentCount(l -> l.bullet().items("A", "B", "C")))
                .as("the legacy layout is unchanged at one per row")
                .isEqualTo(3);
    }

    @Test
    void theLegacyLayoutResolvesNoGeometryAtAll() throws Exception {
        try (Probe probe = new Probe()) {
            PreparedListLayout layout = probe.prepare(l -> l.bullet().items(items(50)));
            assertThat(layout.markerContentItems())
                    .as("nothing is normalized or measured for a list that did not ask")
                    .isEmpty();
        }
    }

    // ------------------------------------------------------------------

    private static List<String> items(int count) {
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add("Item " + i);
        }
        return out;
    }

    private static int fragmentCount(Consumer<ListBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 800)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Root").addList(spec).build();
            return (int) session.layoutGraph().fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .count();
        }
    }

    /** A prepare pass whose text measurements are counted. */
    private static final class Probe implements AutoCloseable {
        private final PDDocument document = new PDDocument();
        private final FontLibrary fonts;
        private final Counting measurement;

        private Probe() throws Exception {
            this.fonts = PdfFontLibraryFactory.library(document);
            this.measurement = new Counting(new FontLibraryTextMeasurementSystem(fonts, PdfFont.class));
        }

        private PreparedListLayout prepare(Consumer<ListBuilder> spec) {
            ListBuilder builder = new ListBuilder().name("L");
            spec.accept(builder);
            ListNode node = builder.build();
            return TextFlowSupport.prepareList(node, context(), new BoxConstraints(376.0, 776.0))
                    .requirePreparedLayout(PreparedListLayout.class);
        }

        private int measuredTexts(String text) {
            return (int) measurement.seen.stream().filter(text::equals).count();
        }

        private int totalMeasurements() {
            return measurement.seen.size();
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
                    return LayoutCanvas.from(400, 800, new Margin(12, 12, 12, 12));
                }
            };
        }

        @Override
        public void close() throws Exception {
            document.close();
        }
    }

    /** Records every text handed to the measurement system. */
    private static final class Counting implements TextMeasurementSystem {
        private final TextMeasurementSystem delegate;
        private final List<String> seen = new ArrayList<>();

        private Counting(TextMeasurementSystem delegate) {
            this.delegate = delegate;
        }

        @Override
        public ContentSize measure(TextStyle style, String text) {
            seen.add(text);
            return delegate.measure(style, text);
        }

        @Override
        public double textWidth(TextStyle style, String text) {
            seen.add(text);
            return delegate.textWidth(style, text);
        }

        @Override
        public LineMetrics lineMetrics(TextStyle style) {
            return delegate.lineMetrics(style);
        }
    }
}
