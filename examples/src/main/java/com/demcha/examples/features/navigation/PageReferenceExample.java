package com.demcha.examples.features.navigation;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 native page references: {@code addPageReference(anchor)}
 * prints the page a declared {@code anchor(...)} lands on — the "see page N"
 * cross-reference — in a single authoring pass. The engine resolves the number
 * from the laid-out document automatically (a second layout pass), so there is no
 * manual probe-then-render; {@code pageIndex()} remains for programmatic access.
 *
 * <pre>{@code
 * flow.addRow(r -> r.columns(auto(), auto())
 *     .addParagraph("Configuration is in the Appendix on page")
 *     .addPageReference("appendix", style, TextAlign.LEFT));   // resolves to the real page
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class PageReferenceExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(96, 102, 112);

    private PageReferenceExample() {
    }

    /**
     * Renders a two-page note whose first page cross-references the appendix by
     * its resolved page number, in one authoring pass.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/navigation", "page-reference.pdf");

        DocumentTextStyle title = DocumentTextStyle.DEFAULT.withSize(18).withColor(INK);
        DocumentTextStyle body = DocumentTextStyle.DEFAULT.withSize(11).withColor(INK);
        DocumentTextStyle ref = DocumentTextStyle.DEFAULT.withSize(11).withColor(MUTED);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(320, 240)
                .margin(DocumentInsets.of(30))
                .create()) {
            DocumentStroke dots = DocumentStroke.of(MUTED, 1.2);
            document.pageFlow(page -> {
                page.addParagraph(p -> p.text("Release notes").textStyle(title));
                page.addParagraph(p -> p.text("Where to read more — the page resolves itself:")
                        .textStyle(ref).padding(DocumentInsets.top(10)));

                // One authoring pass: the reference resolves to the appendix's real page.
                page.addRow(r -> r.gap(6).columns(DocumentRowColumn.auto(), DocumentRowColumn.weight(1), DocumentRowColumn.auto())
                        .padding(DocumentInsets.top(8))
                        .addParagraph(p -> p.text("Appendix").textStyle(body))
                        .addLine(l -> l.fill().height(11).stroke(dots).dashed(0.1, 4).lineCap(DocumentLineCap.ROUND))
                        .addPageReference("appendix", body, TextAlign.RIGHT));

                page.addPageBreak(b -> b.name("toAppendix"));

                page.addSection(s -> s.anchor("appendix")
                        .addParagraph(p -> p.text("Appendix").textStyle(title)));
                page.addParagraph(p -> p.text("Configuration keys, defaults, and units.")
                        .textStyle(body).padding(DocumentInsets.top(6)));
            });
            document.buildPdf();
        }
        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
