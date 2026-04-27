package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
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

    private boolean isPageBackgroundFragment(PlacedFragment fragment) {
        return fragment.payload() instanceof BuiltInNodeDefinitions.ShapeFragmentPayload payload
                && payload.fillColor() != null
                && payload.stroke() == null
                && payload.sideBorders() == null
                && fragment.path() != null
                && fragment.path().startsWith("@page-background");
    }
}
