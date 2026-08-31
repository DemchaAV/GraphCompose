package com.demcha.examples.features.chrome;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the page zone: a footer whose content is a node subtree
 * rather than three text slots, so it can hold a badge, a link and a page number
 * on one line.
 *
 * <pre>{@code
 * session.chrome().zone(DocumentPageZone.footer(34, page -> new RowBuilder()
 *     .gap(8)
 *     .addParagraph(p -> p.text("Confidential"))
 *     .flexSpacer()
 *     .addParagraph(p -> p.inlineChip("v2.4", chipInk, chipFill))
 *     .addParagraph(p -> p.inlineLink("acme.example", new DocumentLinkOptions("https://acme.example")))
 *     .add(page.pageNumber())
 *     .build()));
 * }</pre>
 *
 * <p>The band goes through the same layout and render path as the body, which is
 * why the chip is a real chip and the link is a real annotation rather than
 * characters that look like one. {@code page.pageNumber()} returns a node rather
 * than an {@code int} so the same zone also exports to DOCX, where it becomes
 * Word's live {@code PAGE} field.</p>
 *
 * <p>The rest of the zone surface rides along: {@code DocumentPageZone.header}
 * builds the running head from the same row DSL,
 * {@code appliesTo(page -> !page.isFirst())} keeps it off the cover, and the head
 * carries an {@code addPageReference} that resolves against the body's anchors —
 * every page names where the appendix landed without anyone maintaining the
 * number.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PageZoneExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(96, 102, 112);
    private static final DocumentColor CHIP_INK = DocumentColor.rgb(24, 60, 90);
    private static final DocumentColor CHIP_FILL = DocumentColor.rgb(226, 236, 245);

    private PageZoneExample() {
    }

    /**
     * Renders a four-page note whose footer carries a notice, a version badge, a
     * link and the page number, all as nodes — under a running head that skips
     * the cover and points at the appendix through a body anchor.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/chrome", "page-zone.pdf");

        DocumentTextStyle title = DocumentTextStyle.DEFAULT.withSize(20).withColor(INK);
        DocumentTextStyle body = DocumentTextStyle.DEFAULT.withSize(11).withColor(INK);
        DocumentTextStyle chrome = DocumentTextStyle.DEFAULT.withSize(9).withColor(MUTED);

        try (DocumentSession session = GraphCompose.document(pdfFile)
                .pageSize(340, 250)
                .margin(DocumentInsets.of(28))
                .create()) {

            session.chrome().zone(DocumentPageZone.footer(34, page -> new RowBuilder()
                    .name("FooterZone")
                    .gap(8)
                    .addParagraph(p -> p.text("Confidential").textStyle(chrome))
                    .flexSpacer()
                    .addParagraph(p -> p.textStyle(chrome).inlineChip("v2.4", CHIP_INK, CHIP_FILL))
                    .addParagraph(p -> p.textStyle(chrome).inlineLink("acme.example",
                            new DocumentLinkOptions("https://acme.example")))
                    .add(page.pageNumber(chrome))
                    .build()));

            // The running head skips the cover — appliesTo decides painting per
            // page — and its right side is a page reference resolved from the
            // body's "appendix" anchor, so every page names where it landed.
            session.chrome().zone(DocumentPageZone.header(24, page -> new RowBuilder()
                            .name("HeaderZone")
                            .addParagraph(p -> p.text("Page zones").textStyle(chrome))
                            .flexSpacer()
                            .addParagraph(p -> p.text("Appendix · p.").textStyle(chrome))
                            .addPageReference("appendix", chrome, TextAlign.LEFT)
                            .build())
                    .toBuilder()
                    .appliesTo(page -> !page.isFirst())
                    .build());

            session.pageFlow(page -> {
                page.addParagraph(p -> p.text("Page zones").textStyle(title));
                page.addParagraph(p -> p.text("The footer below is a row of nodes, not three text "
                                + "slots: a notice, a badge, a link and this page's number. The "
                                + "running head starts overleaf — appliesTo keeps it off this cover.")
                        .textStyle(body).padding(DocumentInsets.top(6)));
                page.addPageBreak(b -> b.name("toSecond"));

                page.addParagraph(p -> p.text("Drawn by the body's handlers").textStyle(title));
                page.addParagraph(p -> p.text("The chip has its rounded fill and the link is a real "
                                + "annotation, because the band is laid out and painted the same way "
                                + "the body is.")
                        .textStyle(body).padding(DocumentInsets.top(6)));
                page.addPageBreak(b -> b.name("toThird"));

                page.addParagraph(p -> p.text("One zone, every backend").textStyle(title));
                page.addParagraph(p -> p.text("page.pageNumber() returns a node, so this same footer "
                                + "exports to DOCX as Word's live PAGE field rather than a number "
                                + "that would be wrong on every page but one.")
                        .textStyle(body).padding(DocumentInsets.top(6)));
                page.addPageBreak(b -> b.name("toAppendix"));
            });

            session.pageFlow(page -> {
                page.name("Appendix").anchor("appendix");
                page.addParagraph(p -> p.text("Appendix").textStyle(title));
                page.addParagraph(p -> p.text("The head's page reference points here. It is the "
                                + "body's own anchor machinery — the zone compiles with the "
                                + "document's resolved anchors, so \"Appendix · p.4\" is looked up, "
                                + "not typed.")
                        .textStyle(body).padding(DocumentInsets.top(6)));
            });

            session.buildPdf();
        }

        return pdfFile;
    }

    /**
     * Entry point.
     *
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
