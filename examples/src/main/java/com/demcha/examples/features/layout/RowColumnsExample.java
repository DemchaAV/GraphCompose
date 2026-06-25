package com.demcha.examples.features.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

import static com.demcha.compose.document.style.DocumentRowColumn.auto;
import static com.demcha.compose.document.style.DocumentRowColumn.weight;

/**
 * Runnable showcase for v1.9 row columns: {@code RowBuilder.columns(...)} sizes
 * each column as fixed points, intrinsic ({@code auto()}) content width, or a
 * {@code weight()} share of the remainder. Combined with {@code line().fill()}
 * it builds a table-of-contents row without measuring the gap — the label and
 * page number size to their content while the dotted leader fills between them.
 *
 * <pre>{@code
 * flow.addRow(r -> r.columns(auto(), weight(1), auto())
 *     .addParagraph(label)
 *     .addLine(l -> l.fill().dashed(0.1, 4).lineCap(ROUND))   // leader fills the gap
 *     .addParagraph(pageNumber));
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class RowColumnsExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);

    private static final String[][] ENTRIES = {
            {"Introduction", "1"},
            {"Getting started", "4"},
            {"A longer chapter title that runs on", "12"},
            {"Appendix", "28"},
    };

    private RowColumnsExample() {
    }

    /**
     * Renders a table-of-contents block built from {@code columns(auto(),
     * weight(1), auto())} rows with dotted-leader fill lines.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/layout", "row-columns.pdf");

        DocumentTextStyle entry = DocumentTextStyle.DEFAULT.withSize(11).withColor(INK);
        DocumentStroke dots = DocumentStroke.of(MUTED, 1.3);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(380, 260)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p.text("Table of contents")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18)));
                page.addParagraph(p -> p.text("columns(auto(), weight(1), auto()) + line().fill()")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9).withColor(MUTED))
                        .padding(DocumentInsets.bottom(8)));

                for (String[] item : ENTRIES) {
                    tocRow(page, entry, dots, item[0], item[1]);
                }
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    private static void tocRow(PageFlowBuilder page,
                               DocumentTextStyle entry,
                               DocumentStroke dots,
                               String label,
                               String pageNumber) {
        page.addRow(r -> r.gap(6).columns(auto(), weight(1), auto())
                .padding(DocumentInsets.symmetric(0, 5))
                .addParagraph(p -> p.text(label).textStyle(entry))
                .addLine(l -> l.fill().height(11).stroke(dots)
                        .dashed(0.1, 4).lineCap(DocumentLineCap.ROUND))
                .addParagraph(p -> p.text(pageNumber).textStyle(entry)));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
