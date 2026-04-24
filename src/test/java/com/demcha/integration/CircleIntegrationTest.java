package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.components_builders.ComponentBuilder;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CircleIntegrationTest {

    @Test
    void shouldRenderSingleCircleWithoutGuides() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("circle_single_clean", "clean", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(false)
                .create()) {

            composer.componentBuilder()
                    .circle()
                    .size(180, 180)
                    .fillColor(ComponentColor.ROYAL_BLUE)
                    .padding(Padding.of(8))
                    .margin(Margin.of(15))
                    .anchor(Anchor.topCenter())
                    .build();

            composer.build();
        }

        assertPdfExists(outputFile, 1);
    }

    @Test
    void shouldRenderSingleCircleWithGuides() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("circle_single_guides", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            composer.componentBuilder()
                    .circle()
                    .size(150, 150)
                    .fillColor(ComponentColor.LIGHT_GRAY)
                    .stroke(new Stroke(ComponentColor.ROYAL_BLUE, 2))
                    .padding(Padding.of(10))
                    .margin(Margin.of(15))
                    .anchor(Anchor.topCenter())
                    .build();

            composer.build();
        }

        assertPdfExists(outputFile, 1);
    }

    @Test
    void shouldPaginateCircleColumnAcrossPages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("circle_pagination_test", "guides", "integration");
        List<Entity> circles;

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
//                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();
            circles = createColoredCircles(cb);

            var containerBuilder = cb.vContainer(Align.top(12))
                    .entityName("CirclePaginationColumn")
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(20));

            for (Entity circle : circles) {
                containerBuilder.addChild(circle);
            }
            containerBuilder.build();

            composer.build();
        }

        assertPdfExists(outputFile, 2);

        int pageCount;
        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            pageCount = document.getNumberOfPages();
            assertThat(pageCount).isGreaterThan(1);
        }

        Set<Integer> occupiedPages = new HashSet<>();
        for (Entity circle : circles) {
            Placement placement = circle.getComponent(Placement.class).orElseThrow();
            assertThat(placement.startPage()).isEqualTo(placement.endPage());
            assertThat(placement.startPage()).isGreaterThanOrEqualTo(0);
            assertThat(placement.endPage()).isLessThan(pageCount);
            occupiedPages.add(placement.startPage());
        }

        assertThat(occupiedPages)
                .as("Circles should be distributed across more than one page")
                .hasSizeGreaterThan(1);
    }

    private void assertPdfExists(Path outputFile, int minPages) throws Exception {
        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument document = Loader.loadPDF(outputFile.toFile())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);
        }
    }

    private List<Entity> createColoredCircles(ComponentBuilder cb) {
        List<Entity> circles = new ArrayList<>();
        List<Color> colors = List.of(
                ComponentColor.ROYAL_BLUE,
                ComponentColor.LIGHT_BLUE,
                ComponentColor.ORANGE,
                ComponentColor.DARK_GREEN,
                ComponentColor.PURPLE);

        for (int i = 0; i < colors.size(); i++) {
            Entity circle = cb.circle()
                    .size(150, 150)
                    .fillColor(colors.get(i))
                    .stroke(new Stroke(ComponentColor.DARK_BLUE, 2))
                    .padding(Padding.of(6))
                    .margin(Margin.of(10))
                    .anchor(Anchor.center())
                    .entityName("Circle" + i)
                    .build();
            circles.add(circle);
        }
        return circles;
    }
}
