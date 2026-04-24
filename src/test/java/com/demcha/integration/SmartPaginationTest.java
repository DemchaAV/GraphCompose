package com.demcha.integration;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import com.demcha.testing.VisualTestOutputs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smart Pagination Box Model Tests
 * Verifies that containers automatically measure content, 
 * wrap to new pages, and maintain margins/borders.
 */
public class SmartPaginationTest {

    @Test
    void testMassiveTextBlockPagination() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("massive_text", "clean", "integration");
        
        // Generate ~10,000 characters
        StringBuilder sb = new StringBuilder();
        String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ";
        for (int i = 0; i < 80; i++) {
            sb.append(lorem);
        }
        String massiveText = sb.toString();

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(50, 50, 50, 50)
                .create()) {

            var cb = composer.componentBuilder();
            var theme = CvTheme.courier();
            var title = cb.text()
                    .textWithAutoSize("MassiveText")
                    .entityName("Title_MassiveText")
                    .anchor(Anchor.topLeft())
                    .margin(Margin.of(5))
                    .textStyle(theme.sectionHeaderTextStyle())
                    .build();
            var massiveTextBlock = cb.blockText(Align.left(theme.spacing()), theme.bodyTextStyle())
                    .size(composer.canvas().innerWidth(), 2)
                    .padding(0, 5, 0, 25)
                    .text(cb.text()
                            .textWithAutoSize(massiveText)
                            .textStyle(theme.bodyTextStyle()))
                    .anchor(Anchor.center())
                    .build();
            var massiveTextModule = cb.moduleBuilder(Align.middle(theme.spacingModuleName()), composer.canvas())
                    .anchor(Anchor.topLeft())
                    .addChild(title)
                    .addChild(massiveTextBlock)
                    .build();

            cb.vContainer(Align.middle(theme.spacingModuleName()))
                    .size(composer.canvas().innerWidth(), 0)
                    .anchor(Anchor.topLeft())
                    .addChild(massiveTextModule)
                    .build();

            LayoutSnapshotAssertions.assertMatches(composer.layoutSnapshot(), "massive_text", "integration");
            composer.build();
        }

        assertThat(outputFile).exists();
        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(3);
            System.out.println("✅ Massive text block spans " + doc.getNumberOfPages() + " pages.");
        }
    }

    @Test
    void testContainerSplitWithGuides() throws Exception {
        Path outputFile = VisualTestOutputs.preparePdf("container_split", "guides", "integration");

        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(50, 50, 50, 50)
                .guideLines(true) // Use guides to verify "borders" (box boundaries)
                .create()) {

            var cb = composer.componentBuilder();
            
            // Create a container that is nearly the size of a page, plus some content
            // to force it to split.
            cb.vContainer(Align.middle(10))
                .entityName("SplitContainer")
                .addChild(cb.rectangle().size(500, 600).fillColor(ComponentColor.GRAY).build())
                .addChild(cb.rectangle().size(500, 300).fillColor(ComponentColor.BLUE).build())
                .build();

            LayoutSnapshotAssertions.assertMatches(composer.layoutSnapshot(), "container_split", "integration");
            composer.build();
        }

        assertThat(outputFile).exists();
        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            System.out.println("✅ Container correctly split across 2 pages.");
        }
    }
}
