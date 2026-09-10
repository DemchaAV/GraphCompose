package com.demcha.compose.document.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.backend.fixed.pdf.PdfFontLibraryFactory;
import com.demcha.compose.document.layout.payloads.ListItemSpec;
import com.demcha.compose.document.layout.payloads.MarkerContentItem;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.PreparedListLayout;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.font.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * The internal list layout model: one strategy, resolved once, and the
 * normalized depth/marker/content view that the marker/content strategy keeps
 * instead of a concatenated label.
 *
 * <p>This change introduces the model and the seam only — an opted-in list still
 * renders through the legacy pipeline, and {@link #optingInChangesNothingYet()}
 * is what holds that true while the geometry is built on top.</p>
 */
class ListItemLayoutModelTest {

    // --- the one decision --------------------------------------------------

    @Test
    void theStrategyIsResolvedFromTheAuthoredFlagAndNothingElse() {
        assertThat(ListItemLayout.of(list(l -> l.bullet().items("a"))))
                .isEqualTo(ListItemLayout.LEGACY_PREFIX);
        assertThat(ListItemLayout.of(list(l -> l.bullet().hangingIndent(true).items("a"))))
                .isEqualTo(ListItemLayout.MARKER_CONTENT);

        // A gap on its own is not an opt-in: geometry is what hangingIndent buys.
        assertThat(ListItemLayout.of(list(l -> l.bullet().markerGap(12).items("a"))))
                .isEqualTo(ListItemLayout.LEGACY_PREFIX);
    }

    @Test
    void defaultsAreLegacyWithTheDocumentedGap() {
        ListNode node = list(l -> l.bullet().items("a"));
        assertThat(node.hangingIndent()).isFalse();
        assertThat(node.markerGap()).isEqualTo(ListNode.DEFAULT_MARKER_GAP);
        assertThat(ListNode.DEFAULT_MARKER_GAP).isEqualTo(4.0);
    }

    @Test
    void theBackCompatConstructorsStillProduceLegacyNodes() {
        ListNode eleven = new ListNode("L", List.of("a"), ListMarker.bullet(), null, null,
                0, 0, "", true, null, null);
        ListNode twelve = new ListNode("L", List.of("a"), List.of(), ListMarker.bullet(), null, null,
                0, 0, "", true, null, null);

        assertThat(eleven.hangingIndent()).isFalse();
        assertThat(twelve.hangingIndent()).isFalse();
        assertThat(eleven.markerGap()).isEqualTo(ListNode.DEFAULT_MARKER_GAP);
        assertThat(twelve.markerGap()).isEqualTo(ListNode.DEFAULT_MARKER_GAP);
    }

    @Test
    void markerGapIsValidatedEvenWhenTheLayoutWouldNotObserveIt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListBuilder().markerGap(-1))
                .withMessageContaining("markerGap");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListBuilder().markerGap(Double.NaN));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListBuilder().markerGap(Double.POSITIVE_INFINITY));

        // Zero is a legitimate choice, not a mistake.
        assertThat(list(l -> l.markerGap(0).items("a")).markerGap()).isEqualTo(0.0);
    }

    // --- the normalized model ----------------------------------------------

    @Test
    void aFlatListNormalizesToDepthZeroRowsCarryingTheListMarker() {
        List<ListItemSpec> specs = ListItemNormalizer.normalize(
                list(l -> l.dash().items("Java", "SQL")));

        assertThat(specs).hasSize(2);
        assertThat(specs).allSatisfy(spec -> {
            assertThat(spec.depth()).isZero();
            assertThat(spec.marker()).isEqualTo(ListMarker.dash());
            assertThat(spec.hasMarker()).isTrue();
        });
        assertThat(specs.stream().map(ListItemSpec::content)).containsExactly("Java", "SQL");
    }

    @Test
    void aNestedListKeepsDepthAndMarkerApartFromContentInsteadOfBakingThemIn() {
        List<ListItemSpec> specs = ListItemNormalizer.normalize(list(l -> l
                .addItem("Top one", c -> c
                        .addItem("Child one")
                        .addItem("Child two", g -> g.addItem("Grandchild")))
                .addItem("Top two")));

        assertThat(specs).hasSize(5);
        assertThat(specs.stream().map(ListItemSpec::depth)).containsExactly(0, 1, 1, 2, 0);
        assertThat(specs.stream().map(ListItemSpec::content)).containsExactly(
                "Top one", "Child one", "Child two", "Grandchild", "Top two");

        // Same glyph cascade as the legacy walk, so opting in never changes
        // which marker is shown — only where it sits.
        assertThat(specs.stream().map(spec -> spec.marker().value())).containsExactly(
                "• ", "◦ ", "◦ ", "▪ ", "• ");

        // ...and not one of them has the depth indent or the marker in its text.
        assertThat(specs).allSatisfy(spec -> {
            assertThat(spec.content()).doesNotContain(" ");
            assertThat(spec.content()).doesNotStartWith("•");
            assertThat(spec.content()).doesNotStartWith("◦");
            assertThat(spec.content()).doesNotStartWith("▪");
        });
    }

    @Test
    void aPerItemMarkerOverrideStillBeatsTheDepthCascade() {
        List<ListItemSpec> specs = ListItemNormalizer.normalize(list(l -> l
                .markerFor(1, ListMarker.custom("→"))
                .addItem("Top", c -> c.addItem("Child"))));

        assertThat(specs.get(0).marker()).isEqualTo(ListMarker.bullet());
        assertThat(specs.get(1).marker().value()).isEqualTo("→ ");
    }

    @Test
    void anEmptyItemWithAVisibleMarkerStaysAsAMarkerOnlyRow() {
        // Authored cardinality is preserved: three items in, three rows out. The
        // author asked for that bullet, and opting into marker geometry is not a
        // reason to lose it.
        List<ListItemSpec> specs = ListItemNormalizer.normalize(
                list(l -> l.bullet().items("Java", "   ", "SQL")));

        assertThat(specs).hasSize(3);
        assertThat(specs).extracting(ListItemSpec::content).containsExactly("Java", "", "SQL");
        assertThat(specs.get(1).hasMarker()).isTrue();
        assertThat(specs.get(1).markerText()).isEqualTo("•");
    }

    @Test
    void anEmptyParentKeepsItsOwnMarkerRowAndAllOfItsChildren() {
        List<ListItemSpec> specs = ListItemNormalizer.normalize(list(l -> l
                .addItem("", c -> c.addItem("Child survives"))));

        assertThat(specs).hasSize(2);
        assertThat(specs.get(0).depth()).isZero();
        assertThat(specs.get(0).content()).isEmpty();
        assertThat(specs.get(0).hasMarker()).as("the parent is a marker-only row").isTrue();
        assertThat(specs.get(1).depth()).isEqualTo(1);
        assertThat(specs.get(1).content()).isEqualTo("Child survives");
    }

    @Test
    void anEmptyItemWithNoMarkerDrawsNothingAndIsOmitted() {
        // The one case with neither text nor marker: nothing to draw, no marker,
        // no gap, and no row — which is what the normalized-content contract
        // already says.
        assertThat(ListItemNormalizer.normalize(list(l -> l.noMarker().items("Java", "   ", "SQL"))))
                .extracting(ListItemSpec::content)
                .containsExactly("Java", "SQL");

        assertThat(ListItemNormalizer.normalize(list(l -> l
                .markerFor(0, ListMarker.none())
                .addItem("", c -> c.addItem("Child survives")))))
                .extracting(ListItemSpec::content)
                .containsExactly("Child survives");
    }

    @Test
    void theSameEmptyItemShapesRenderUnchangedUnderTheLegacyLayout() throws Exception {
        // The cardinality rule is a marker/content decision. Legacy keeps the
        // behaviour it shipped with: a blank flat item is dropped whatever the
        // marker, and a blank nested parent still renders its baked marker.
        assertThat(legacyLineTexts(l -> l.bullet().items("Java", "   ", "SQL")))
                .containsExactly("• Java", "• SQL");
        assertThat(legacyLineTexts(l -> l.noMarker().items("Java", "   ", "SQL")))
                .containsExactly("Java", "SQL");
        assertThat(legacyLineTexts(l -> l.addItem("", c -> c.addItem("Child survives"))))
                .containsExactly("•", "  ◦ Child survives");
    }

    @Test
    void aMarkerlessItemIsMarkerlessRatherThanEmptyMarkered() {
        List<ListItemSpec> specs = ListItemNormalizer.normalize(
                list(l -> l.noMarker().items("Aligned row")));

        assertThat(specs).hasSize(1);
        assertThat(specs.get(0).hasMarker()).isFalse();
        assertThat(specs.get(0).markerText()).isEmpty();
    }

    // --- what gets measured -------------------------------------------------

    @Test
    void markerTextDropsTheSyntheticSeparatorAndKeepsAuthorTextIntact() {
        // ListMarker appends one trailing space so "•" + text does not render as
        // "•text". Under MARKER_CONTENT the space between marker and content is
        // markerGap, so measuring that separator too would count the gap twice.
        assertThat(spec(ListMarker.bullet()).markerText()).isEqualTo("•");
        assertThat(spec(ListMarker.dash()).markerText()).isEqualTo("-");
        assertThat(spec(ListMarker.custom("=>")).markerText()).isEqualTo("=>");
        assertThat(spec(ListMarker.none()).markerText()).isEmpty();

        // Only that one separator goes. A marker whose own text contains spaces
        // keeps them — trimming an author's marker is not ours to do.
        assertThat(spec(ListMarker.custom("[ x ]")).markerText()).isEqualTo("[ x ]");
        assertThat(spec(ListMarker.custom("a b")).markerText()).isEqualTo("a b");
    }

    @Test
    void aSpecRejectsANegativeDepthAndNormalizesNulls() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListItemSpec(-1, ListMarker.bullet(), "x"))
                .withMessageContaining("depth");

        ListItemSpec normalized = new ListItemSpec(0, null, null);
        assertThat(normalized.marker()).isEqualTo(ListMarker.none());
        assertThat(normalized.content()).isEmpty();
    }

    // --- the seam, and what it does not do yet ------------------------------

    @Test
    void thePreparedLayoutCarriesTheModelOnlyForTheMarkerContentStrategy() throws Exception {
        assertThat(preparedItems(l -> l.bullet().items("Java", "SQL")))
                .as("legacy has no marker left to keep apart from its text")
                .isEmpty();

        assertThat(preparedItems(l -> l.bullet().hangingIndent(true).items("Java", "SQL")))
                .extracting(MarkerContentItem::content)
                .containsExactly("Java", "SQL");
    }

    @Test
    void theModelSurvivesNestingThroughThePipeline() throws Exception {
        List<MarkerContentItem> items = preparedItems(l -> l
                .hangingIndent(true)
                .addItem("Top", c -> c.addItem("Child")));

        assertThat(items).hasSize(2);
        assertThat(items.stream().map(MarkerContentItem::depth)).containsExactly(0, 1);
        assertThat(items.get(1).content()).isEqualTo("Child");
    }

    @Test
    void notOptingInLeavesEveryShapeExactlyWhereItWas() throws Exception {
        // The compatibility half of the contract. The marker/content layout now
        // renders its own geometry, so the two are no longer interchangeable —
        // what has to stay true is that a list which never asked for it is
        // untouched, shape by shape. Where it lands when it does ask is
        // ListHangingIndentTest's subject.
        for (Consumer<ListBuilder> shape : List.<Consumer<ListBuilder>>of(
                l -> l.bullet().items("Java", "SQL"),
                l -> l.dash().items("Long item text that wraps across more than one visual line here."),
                l -> l.marker("=>").items("Custom"),
                l -> l.noMarker().items("Plain"),
                l -> l.addItem("Top", c -> c.addItem("Child", g -> g.addItem("Grandchild"))))) {

            List<String> legacy = renderedLines(shape);
            assertThat(legacy).as("a shape that renders nothing would prove nothing").isNotEmpty();
            assertThat(renderedLines(shape.andThen(l -> l.hangingIndent(false))))
                    .as("saying no explicitly is the same as not saying anything")
                    .isEqualTo(legacy);
        }

        // Where there is a marker, opting in is a different layout — the marker
        // leaves the text and the wrapped lines lose their space indent.
        for (Consumer<ListBuilder> marked : List.<Consumer<ListBuilder>>of(
                l -> l.bullet().items("Java", "SQL"),
                l -> l.dash().items("Long item text that wraps across more than one visual line here."),
                l -> l.marker("=>").items("Custom"),
                l -> l.addItem("Top", c -> c.addItem("Child", g -> g.addItem("Grandchild"))))) {

            assertThat(renderedLines(marked.andThen(l -> l.hangingIndent(true))))
                    .isNotEqualTo(renderedLines(marked));
        }

        // A list with no marker has nothing to hang, so the two layouts agree —
        // and that agreement is a property worth stating, not an oversight.
        Consumer<ListBuilder> markerless = l -> l.noMarker().items("Plain");
        assertThat(renderedLines(markerless.andThen(l -> l.hangingIndent(true))))
                .isEqualTo(renderedLines(markerless));
    }

    // ------------------------------------------------------------------

    private static ListItemSpec spec(ListMarker marker) {
        return new ListItemSpec(0, marker, "content");
    }

    private static ListNode list(Consumer<ListBuilder> spec) {
        ListBuilder builder = new ListBuilder().name("L");
        spec.accept(builder);
        return builder.build();
    }

    /**
     * Prepares a list the way the compiler does and returns the normalized model
     * the prepared layout came back carrying.
     */
    private static List<MarkerContentItem> preparedItems(Consumer<ListBuilder> spec) throws Exception {
        try (PDDocument measurementDocument = new PDDocument()) {
            FontLibrary fonts = PdfFontLibraryFactory.library(measurementDocument);
            PrepareContext ctx = new MeasuringPrepareContext(
                    fonts, new FontLibraryTextMeasurementSystem(fonts, PdfFont.class));
            return TextFlowSupport.prepareList(list(spec), ctx, new BoxConstraints(296.0, 216.0))
                    .requirePreparedLayout(PreparedListLayout.class)
                    .markerContentItems();
        }
    }

    /** Enough of a prepare pass to measure text; a list leaf needs nothing else. */
    private record MeasuringPrepareContext(FontLibrary fonts, TextMeasurementSystem textMeasurement)
            implements PrepareContext {

        @Override
        public <E extends com.demcha.compose.document.node.DocumentNode> PreparedNode<E> prepare(
                E node, BoxConstraints constraints) {
            throw new UnsupportedOperationException("a list leaf prepares no children");
        }

        @Override
        public LayoutCanvas canvas() {
            return LayoutCanvas.from(320, 240, new Margin(12, 12, 12, 12));
        }
    }

    /** Exact line texts a shape produces under the legacy layout. */
    private static List<String> legacyLineTexts(Consumer<ListBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 240)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Root").addList(spec).build();
            return session.layoutGraph().fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .flatMap(f -> ((ParagraphFragmentPayload) f.payload()).lines().stream())
                    .map(line -> line.text())
                    .toList();
        }
    }

    private static List<String> renderedLines(Consumer<ListBuilder> spec) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 240)
                .margin(DocumentInsets.of(12))
                .create()) {
            session.pageFlow().name("Root").addList(spec).build();
            return session.layoutGraph().fragments().stream()
                    .filter(f -> f.payload() instanceof ParagraphFragmentPayload)
                    .flatMap(f -> ((ParagraphFragmentPayload) f.payload()).lines().stream())
                    .map(line -> line.text() + "@" + String.format(Locale.ROOT, "%.3f", line.width()))
                    .toList();
        }
    }
}
