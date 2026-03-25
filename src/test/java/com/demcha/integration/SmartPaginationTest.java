package com.demcha.integration;

import com.demcha.Templatese.TemplateBuilder;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.style.ComponentColor;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smart Pagination Box Model Tests
 * Verifies that containers automatically measure content, 
 * wrap to new pages, and maintain margins/borders.
 */
public class SmartPaginationTest {

    @TempDir
    Path tempDir;

    @Test
    void testMassiveTextBlockPagination() throws Exception {
        Path outputFile = tempDir.resolve("massive_text.pdf");
        
        // Generate ~10,000 characters
        StringBuilder sb = new StringBuilder();
        String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ";
        for (int i = 0; i < 80; i++) {
            sb.append(lorem);
        }
        String massiveText = sb.toString();

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
                .pageSize(PDRectangle.A4)
                .margin(50, 50, 50, 50)
                .create()) {

            TemplateBuilder template = TemplateBuilder.from(composer.componentBuilder());
            template.moduleBuilder("MassiveText", composer.canvas())
                    .addChild(template.blockText(massiveText, composer.canvas().innerWidth()))
                    .build();

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
        Path outputFile = tempDir.resolve("container_split.pdf");

        try (PdfComposer composer = GraphCompose.pdf(outputFile)
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

            composer.build();
        }

        assertThat(outputFile).exists();
        try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            System.out.println("✅ Container correctly split across 2 pages.");
        }
    }
}
