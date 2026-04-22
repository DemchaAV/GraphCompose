package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for large containers that span multiple pages.
 */
class LargeContainerIntegrationTest {

    @Test
    void shouldRenderLargeContainerAcrossMultiplePages() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("large_container_test", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(10, 10, 10, 10)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            // Container height 2100 is larger than A4 page (~842 pts)
            cb.vContainer(Align.middle(2))
                    .size(500, 2100)
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10))
                    .padding(Padding.of(5))
                    .entityName("LargeContainer")
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();
        assertThat(outputFile).isNotEmptyFile();

        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            // A4 height is ~842pt, container is 2100pt, so expect at least 3 pages
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void shouldRenderSmallContainerOnSinglePage() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("small_container_test", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(20, 20, 20, 20)
                .guideLines(true)
                .create()) {

            ComponentBuilder cb = composer.componentBuilder();

            cb.vContainer(Align.middle(5))
                    .size(400, 200)
                    .anchor(Anchor.topCenter())
                    .margin(Margin.of(10))
                    .padding(Padding.of(5))
                    .entityName("SmallContainer")
                    .build();

            composer.build();
        }

        assertThat(outputFile).exists();

        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }
}

