package com.demcha;

import com.demcha.compose.loyaut_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.loyaut_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.Canvas;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.components.layout.Anchor;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.components.style.Padding;
import com.demcha.compose.loyaut_core.core.EntityManager;
import com.demcha.compose.loyaut_core.system.LayoutSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfFileManagerSystem;
import com.demcha.compose.loyaut_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BlockTextTest {

    private static final String OUTPUT_FILE = "testBlockText.pdf";

    public static void main(String[] args) {

        EntityManager entityManager = new EntityManager(true);
        entityManager.setGuideLines(true);

        PDDocument doc = new PDDocument();
        Canvas canvasPdf = new PdfCanvas(PDRectangle.A4, 0.0f, 0.0f);
        canvasPdf.addMargin(new Margin(15, 10, 15, 15));

        setupSystems(entityManager, doc, canvasPdf);
        String whitespace = "***";

        BlockTextBuilder text = new BlockTextBuilder(entityManager, Align.left(5), TextStyle.DEFAULT_STYLE)
                .strategy(BlockIndentStrategy.ALL_LINES)
                .size(400, 2)
                .anchor(Anchor.center())
                .text(
                        List.of("**CVRewriter (AI-Powered API Service)** – *Portfolio Project* Developed a full-stack application centred around a **Spring Boot REST API** to generate"),
                        TextStyle.DEFAULT_STYLE,
                        Padding.of(5),
                        Margin.of(5),
                        whitespace// Буллит. Первая строка получит "- ", остальные — пробелы той же ширины.
                );
        text.padding( Padding.of(5));
        text.margin(Margin.of(5));

        text.build();
        entityManager.processSystems();
    }

    private static void setupSystems(EntityManager entityManager, PDDocument doc, Canvas canvasPdf) {
        Path target = Paths.get(OUTPUT_FILE);
        PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvasPdf);
        entityManager.getSystems().addSystem(new LayoutSystem<>(canvasPdf, renderingSystemECS));
        entityManager.getSystems().addSystem(renderingSystemECS);
        entityManager.getSystems().addSystem(new PdfFileManagerSystem(target, doc));
    }
}
