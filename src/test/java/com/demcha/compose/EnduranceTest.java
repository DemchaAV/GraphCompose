package com.demcha.compose;

import com.demcha.Templatese.CvTheme;
import com.demcha.Templatese.TemplateBuilder;
import com.demcha.compose.loyaut_core.core.PdfComposer;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Endurance / Soak Test
 * Proves that the library can generate 100,000 documents 
 * within a limited heap (e.g., -Xmx128m) without memory leaks.
 */
public class EnduranceTest {

    private static final int TOTAL_DOCUMENTS = 100_000;
    private static final int LOG_INTERVAL = 1_000;

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Starting Endurance Test: 100,000 documents");
        System.out.println("Heap limit should be set (e.g., -Xmx128m)");
        System.out.println("------------------------------------------------------------");

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= TOTAL_DOCUMENTS; i++) {
            generateOne(i);
            
            if (i % LOG_INTERVAL == 0) {
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                System.out.printf("Progress: %6d / %d | Elapsed: %4ds | Heap: %3d MB%n", 
                    i, TOTAL_DOCUMENTS, elapsedSeconds, usedMem);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("------------------------------------------------------------");
        System.out.printf("✅ Successfully generated %d documents in %d ms%n", TOTAL_DOCUMENTS, totalTime);
    }

    private static void generateOne(int id) throws Exception {
        try (PdfComposer composer = GraphCompose.pdf().pageSize(PDRectangle.A4).create()) {
            TemplateBuilder template = TemplateBuilder.from(composer.componentBuilder(), CvTheme.defaultTheme());
            template.moduleBuilder("Endurance", composer.canvas())
                    .addChild(template.blockText("Document ID: " + id + ". This is a soak test message to check for memory leaks.", 
                        composer.canvas().innerWidth()))
                    .build();
            composer.toBytes();
        }
    }
}
