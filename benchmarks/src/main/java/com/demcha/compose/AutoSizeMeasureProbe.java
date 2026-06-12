package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.DocumentLayoutPassContext;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutCompiler;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.NodeRegistry;
import com.demcha.compose.document.node.DocumentNode;

import java.util.List;

/**
 * Finding 12 probe — counts the text-width measurements the layout makes to
 * resolve auto-size paragraphs.
 *
 * <p>Auto-size shrinks the font over a size grid until the line fits. This probe
 * compiles a page of auto-size headings through a {@link CountingTextMeasurementSystem}
 * and reports the {@code textWidth} request count, so a develop (linear scan) vs
 * branch (binary search) A/B shows the measurement reduction directly. No
 * {@code src/main} changes.</p>
 */
public final class AutoSizeMeasureProbe {

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();

        int autoSizeParagraphs = 8;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.of(220, 1200))
                .margin(10, 10, 10, 10)
                .create()) {
            session.pageFlow(flow -> {
                for (int i = 0; i < autoSizeParagraphs; i++) {
                    // 36pt heading that must shrink to ~16pt to fit the ~200pt inner
                    // width: the linear scan steps 36 -> 16 by 0.5 (~40 measures),
                    // the binary search resolves the same size in ~log2(n).
                    flow.addParagraph(p -> p
                            .text("Auto-size headline that should fit on a single line")
                            .autoSize(36, 6));
                }
            });

            List<DocumentNode> roots = session.roots();
            LayoutCanvas canvas = session.canvas();
            NodeRegistry registry = session.registry();

            try (PdfMeasurementResources resources = PdfMeasurementResources.open(List.of())) {
                CountingTextMeasurementSystem counter =
                        new CountingTextMeasurementSystem(resources.textMeasurementSystem());
                DocumentLayoutPassContext context = new DocumentLayoutPassContext(
                        registry, canvas, resources.fontLibrary(), counter, false);
                LayoutCompiler compiler = new LayoutCompiler(registry);
                LayoutGraph layout = compiler.compile(new DocumentGraph(roots), context, context);

                CountingTextMeasurementSystem.Counts c = counter.snapshot();
                System.out.println("GraphCompose Finding-12 Auto-Size Measurement Probe");
                System.out.printf("auto-size paragraphs: %d, pages: %d%n",
                        autoSizeParagraphs, layout.totalPages());
                System.out.printf("textWidth requests during compile (auto-size grid search + wrap): %d%n",
                        c.widthRequests());
                System.out.printf("per auto-size paragraph: %.1f%n",
                        c.widthRequests() / (double) autoSizeParagraphs);
            }
        }
    }
}
