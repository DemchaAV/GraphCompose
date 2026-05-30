package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PageBackgroundTest {

    private static final double EPS = 1e-3;

    @Test
    void documentWithoutPageBackgroundEmitsNoBackgroundFragments() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80, DocumentInsets.zero(), DocumentInsets.zero()));
            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.fragments()).noneMatch(this::isPageBackgroundFragment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void singlePageDocumentGetsOneBackgroundFragmentBeforeContent() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .pageBackground(DocumentColor.of(Color.LIGHT_GRAY))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80, DocumentInsets.zero(), DocumentInsets.zero()));
            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.totalPages()).isEqualTo(1);

            List<PlacedFragment> backgrounds = graph.fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();

            assertThat(backgrounds).hasSize(1);
            PlacedFragment bg = backgrounds.get(0);
            assertThat(bg.pageIndex()).isEqualTo(0);
            assertThat(bg.x()).isEqualTo(0.0, within(EPS));
            assertThat(bg.y()).isEqualTo(0.0, within(EPS));
            assertThat(bg.width()).isEqualTo(400.0, within(EPS));
            assertThat(bg.height()).isEqualTo(300.0, within(EPS));

            assertThat(graph.fragments().get(0)).isSameAs(bg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void multiPageDocumentGetsOneBackgroundFragmentPerPage() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 240)
                .margin(DocumentInsets.of(20))
                .pageBackground(DocumentColor.of(new Color(250, 245, 235)))
                .create()) {

            for (int i = 0; i < 6; i++) {
                session.add(new SpacerNode("Block" + i, 200, 90,
                        DocumentInsets.zero(), DocumentInsets.zero()));
            }
            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.totalPages()).isGreaterThan(1);

            List<PlacedFragment> backgrounds = graph.fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();

            assertThat(backgrounds).hasSize(graph.totalPages());

            for (int i = 0; i < backgrounds.size(); i++) {
                PlacedFragment bg = backgrounds.get(i);
                assertThat(bg.pageIndex()).isEqualTo(i);
                assertThat(bg.width()).isEqualTo(400.0, within(EPS));
                assertThat(bg.height()).isEqualTo(240.0, within(EPS));
            }

            for (int i = 0; i < backgrounds.size(); i++) {
                assertThat(graph.fragments().get(i)).isSameAs(backgrounds.get(i));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void clearingPageBackgroundRemovesBackgroundFragments() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .pageBackground(Color.PINK)
                .create()) {

            session.add(new SpacerNode("Block", 200, 80, DocumentInsets.zero(), DocumentInsets.zero()));
            assertThat(session.layoutGraph().fragments()).anyMatch(this::isPageBackgroundFragment);

            session.pageBackground((DocumentColor) null);
            assertThat(session.layoutGraph().fragments()).noneMatch(this::isPageBackgroundFragment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void emptyDocumentEmitsNoBackgroundEvenWhenColorIsSet() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .pageBackground(DocumentColor.of(Color.RED))
                .create()) {

            LayoutGraph graph = session.layoutGraph();
            assertThat(graph.totalPages()).isEqualTo(0);
            assertThat(graph.fragments()).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -- New multi-rect pageBackgrounds(List) API ------------------------

    @Test
    void pageBackgroundsListEmitsOneFragmentPerFillPerPage() {
        DocumentColor sidebar = DocumentColor.of(Color.LIGHT_GRAY);
        DocumentColor main = DocumentColor.WHITE;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackgrounds(List.of(
                        PageBackgroundFill.leftColumn(0.33, sidebar),
                        PageBackgroundFill.rightColumn(0.67, main)))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.totalPages()).isEqualTo(1);
            List<PlacedFragment> bg = graph.fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();
            assertThat(bg).hasSize(2);
            // Painted in list order at z=0
            assertThat(bg.get(0).x()).isCloseTo(0.0, within(EPS));
            assertThat(bg.get(0).width()).isCloseTo(400.0 * 0.33, within(EPS));
            assertThat(bg.get(1).x()).isCloseTo(400.0 * 0.33, within(EPS));
            assertThat(bg.get(1).width()).isCloseTo(400.0 * 0.67, within(EPS));
            for (PlacedFragment f : bg) {
                assertThat(f.y()).isCloseTo(0.0, within(EPS));
                assertThat(f.height()).isCloseTo(300.0, within(EPS));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void pageBackgroundFullPageMatchesLegacySingleColor() {
        DocumentColor cream = DocumentColor.of(new Color(250, 245, 235));
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackgrounds(List.of(PageBackgroundFill.fullPage(cream)))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            LayoutGraph graph = session.layoutGraph();

            List<PlacedFragment> bg = graph.fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();
            assertThat(bg).hasSize(1);
            assertThat(bg.get(0).x()).isCloseTo(0.0, within(EPS));
            assertThat(bg.get(0).y()).isCloseTo(0.0, within(EPS));
            assertThat(bg.get(0).width()).isCloseTo(400.0, within(EPS));
            assertThat(bg.get(0).height()).isCloseTo(300.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void pageBackgroundsEmptyListClearsBackground() {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackground(DocumentColor.of(Color.RED))
                .pageBackgrounds(List.of())
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            LayoutGraph graph = session.layoutGraph();

            assertThat(graph.fragments())
                    .noneMatch(this::isPageBackgroundFragment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void pageBackgroundFillRejectsOutOfRangeRatios() {
        DocumentColor c = DocumentColor.WHITE;
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PageBackgroundFill(-0.1, 0, 0.5, 1, c));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PageBackgroundFill(0, 1.1, 0.5, 1, c));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PageBackgroundFill(0, 0, 0, 1, c));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PageBackgroundFill(0, 0, 1.1, 1, c));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PageBackgroundFill(0, 0, 0.5, 0, c));
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new PageBackgroundFill(0, 0, 0.5, 1, null));
    }

    @Test
    void pageBackgroundFillFactoryHelpersComputeRectsCorrectly() {
        DocumentColor c = DocumentColor.WHITE;
        assertThat(PageBackgroundFill.fullPage(c))
                .isEqualTo(new PageBackgroundFill(0.0, 0.0, 1.0, 1.0, c));
        assertThat(PageBackgroundFill.leftColumn(0.3, c))
                .isEqualTo(new PageBackgroundFill(0.0, 0.0, 0.3, 1.0, c));
        assertThat(PageBackgroundFill.rightColumn(0.4, c))
                .isEqualTo(new PageBackgroundFill(0.6, 0.0, 0.4, 1.0, c));
        assertThat(PageBackgroundFill.column(0.25, 0.5, c))
                .isEqualTo(new PageBackgroundFill(0.25, 0.0, 0.5, 1.0, c));
    }

    // -- Partial-height band placement (y-coordinate regression) ---------
    // yRatio is top-down (0.0 = page top) but PlacedFragment.y is PDF
    // bottom-up, so a partial band must convert via
    // (1 - yRatio - heightRatio) * pageHeight. Pre-fix these collapsed to
    // y == 0 because every factory used heightRatio == 1; these assert real
    // partial bands land at the correct vertical position.

    @Test
    void topBandAppearsAtTopOfPage() {
        DocumentColor band = DocumentColor.of(Color.DARK_GRAY);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackgrounds(List.of(
                        new PageBackgroundFill(0.0, 0.0, 1.0, 0.16, band)))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            List<PlacedFragment> bg = session.layoutGraph().fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();

            assertThat(bg).hasSize(1);
            // yRatio 0 = page top → bottom-left at (1 - 0 - 0.16) * 300 = 252,
            // NOT 0 (the bottom, which was the pre-fix behaviour).
            assertThat(bg.get(0).y()).isCloseTo(252.0, within(EPS));
            assertThat(bg.get(0).height()).isCloseTo(48.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void bottomBandAppearsAtBottomOfPage() {
        DocumentColor band = DocumentColor.of(Color.DARK_GRAY);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackgrounds(List.of(
                        new PageBackgroundFill(0.0, 0.84, 1.0, 0.16, band)))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            List<PlacedFragment> bg = session.layoutGraph().fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();

            assertThat(bg).hasSize(1);
            // yRatio 0.84 + heightRatio 0.16 = 1.0 → bottom edge flush with
            // the page bottom: (1 - 0.84 - 0.16) * 300 = 0.
            assertThat(bg.get(0).y()).isCloseTo(0.0, within(EPS));
            assertThat(bg.get(0).height()).isCloseTo(48.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void midPageBandLandsAtCorrectVerticalPosition() {
        DocumentColor band = DocumentColor.of(Color.DARK_GRAY);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackgrounds(List.of(
                        new PageBackgroundFill(0.0, 0.4, 1.0, 0.2, band)))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            List<PlacedFragment> bg = session.layoutGraph().fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();

            assertThat(bg).hasSize(1);
            // Band spanning 40%..60% from the top → bottom-left at
            // (1 - 0.4 - 0.2) * 300 = 120, height 0.2 * 300 = 60.
            assertThat(bg.get(0).y()).isCloseTo(120.0, within(EPS));
            assertThat(bg.get(0).height()).isCloseTo(60.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -- Band factory helpers (build on the corrected coordinate model) --

    @Test
    void bandFactoryHelpersComputeRatiosCorrectly() {
        DocumentColor c = DocumentColor.WHITE;
        assertThat(PageBackgroundFill.topBand(0.16, c))
                .isEqualTo(new PageBackgroundFill(0.0, 0.0, 1.0, 0.16, c));
        assertThat(PageBackgroundFill.bottomBand(0.16, c))
                .isEqualTo(new PageBackgroundFill(0.0, 0.84, 1.0, 0.16, c));
        assertThat(PageBackgroundFill.band(0.4, 0.2, c))
                .isEqualTo(new PageBackgroundFill(0.0, 0.4, 1.0, 0.2, c));
        assertThat(PageBackgroundFill.topBandPoints(48, 300, c))
                .isEqualTo(new PageBackgroundFill(0.0, 0.0, 1.0, 48.0 / 300.0, c));
        assertThat(PageBackgroundFill.bandPoints(120, 60, 300, c))
                .isEqualTo(new PageBackgroundFill(0.0, 120.0 / 300.0, 1.0, 60.0 / 300.0, c));
    }

    @Test
    void topBandAndBottomBandHelpersRenderAtCorrectEdges() {
        DocumentColor fill = DocumentColor.of(Color.DARK_GRAY);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.zero())
                .pageBackgrounds(List.of(
                        PageBackgroundFill.topBand(0.16, fill),
                        PageBackgroundFill.bottomBand(0.16, fill)))
                .create()) {

            session.add(new SpacerNode("Block", 200, 80,
                    DocumentInsets.zero(), DocumentInsets.zero()));
            List<PlacedFragment> bg = session.layoutGraph().fragments().stream()
                    .filter(this::isPageBackgroundFragment)
                    .toList();

            assertThat(bg).hasSize(2);
            // List order: topBand first, bottomBand second.
            assertThat(bg.get(0).y()).isCloseTo(252.0, within(EPS));   // top band
            assertThat(bg.get(0).height()).isCloseTo(48.0, within(EPS));
            assertThat(bg.get(1).y()).isCloseTo(0.0, within(EPS));     // bottom band
            assertThat(bg.get(1).height()).isCloseTo(48.0, within(EPS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isPageBackgroundFragment(PlacedFragment fragment) {
        return fragment.payload() instanceof ShapeFragmentPayload payload
                && payload.fillColor() != null
                && payload.stroke() == null
                && payload.sideBorders() == null
                && fragment.path() != null
                && fragment.path().startsWith("@page-background");
    }
}
