package com.demcha.compose.document.backend.semantic.docx.probe;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Path;

import static com.demcha.compose.document.style.DocumentRowColumn.weight;

/**
 * Source documents for the editable-DOCX probe corpus.
 *
 * <p>Each fixture is authored once and rendered twice — through the PDF backend for the
 * reference look, and through a DOCX exporter for the editing behaviour — so the two
 * outputs can only differ because of the exporter, never because of the input. Nothing
 * here is hand-placed: every block is an ordinary flow node, which is the point. A
 * fixture that pinned coordinates would prove that a drawing can be copied, not that a
 * Word document can be produced.</p>
 *
 * <p>The fixtures deliberately mix the constructs whose Word representations disagree:
 * flowing paragraphs and a real table stay native in any design, while a panel, a
 * clipped outline and a non-installed font family are where a semantic exporter has to
 * choose. Keeping them in one document makes the choice visible in a single render.</p>
 *
 * @author Artem Demchyshyn
 */
public final class EditableExportFixtures {

    /** A4 width in points — the corpus renders at a realistic page size, not a stub. */
    public static final double PAGE_WIDTH = 595;

    /** A4 height in points. */
    public static final double PAGE_HEIGHT = 842;

    /** Page margin in points, shared by every fixture so content widths are comparable. */
    public static final double PAGE_MARGIN = 42;

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(108, 116, 128);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor CHIP_FILL = DocumentColor.rgb(219, 233, 246);

    private static final DocumentTextStyle TITLE =
            DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(21).color(INK).build();
    private static final DocumentTextStyle HEADING =
            DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(13).color(INK).build();
    private static final DocumentTextStyle BODY =
            DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(10.5).color(INK).build();
    private static final DocumentTextStyle BODY_BOLD =
            DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(10.5).color(INK).build();
    private static final DocumentTextStyle BODY_ITALIC =
            DocumentTextStyle.builder().fontName(FontName.HELVETICA_OBLIQUE).size(10.5).color(ACCENT).build();
    private static final DocumentTextStyle CHROME =
            DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8.5).color(MUTED).build();

    /**
     * A family bundled with GraphCompose and absent from a stock Windows or Linux
     * install, so a paragraph set in it can only look right if the exporter carried the
     * face into the file.
     */
    private static final DocumentTextStyle UNINSTALLED_FAMILY =
            DocumentTextStyle.builder().fontName(FontName.LATO).size(10.5).color(INK).build();

    private EditableExportFixtures() {
    }

    /**
     * The two-page baseline: heading, mixed runs, a two-column row, a panel that has to
     * grow, a real table, an image, a list and a page-number footer.
     *
     * <p>The caller owns the returned session and closes it.</p>
     *
     * @param pdfFile default output file for {@code buildPdf()}
     * @return an open session carrying the whole fixture
     */
    public static DocumentSession mixedTwoPager(Path pdfFile) {
        DocumentSession session = GraphCompose.document(pdfFile)
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(PAGE_MARGIN))
                .create();

        session.chrome().zone(DocumentPageZone.footer(30, page -> new RowBuilder()
                .name("FooterZone")
                .gap(8)
                .addParagraph(p -> p.text("Editable export probe").textStyle(CHROME))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .build()));

        session.pageFlow(page -> {
            page.name("Body");

            page.addParagraph(p -> p.text("Quarterly service report").textStyle(TITLE));

            // Mixed runs in one paragraph. In Word this must stay one w:p whose runs
            // carry their own styles — not one frame per rendered line.
            page.addParagraph(p -> p.textStyle(BODY)
                    .padding(DocumentInsets.top(8))
                    .inlineText("This paragraph mixes ")
                    .inlineText("bold", BODY_BOLD)
                    .inlineText(", ")
                    .inlineText("italic accent", BODY_ITALIC)
                    .inlineText(" and inline ")
                    .inlineCode("code()")
                    .inlineText(" in one block, followed by a chip ")
                    .inlineChip("v2", ACCENT, CHIP_FILL)
                    .inlineText(" so the editing probe can lengthen a sentence that already "
                            + "carries several run styles and watch whether the wrap, the "
                            + "styles and the block below it all behave."));

            // Two columns of unequal length: the row must keep them side by side and
            // must not lose the longer column's tail at a page boundary.
            page.addRow("TwoColumns", r -> r.gap(18).columns(weight(3), weight(2))
                    .padding(DocumentInsets.symmetric(14, 0))
                    .addSection(left -> left
                            .addParagraph(p -> p.text("Scope").textStyle(HEADING))
                            .addParagraph(p -> p.textStyle(BODY).padding(DocumentInsets.top(4))
                                    .text("The left column is the longer of the two. It exists so "
                                            + "an edit can make one column outgrow the other and "
                                            + "the probe can record what the row does about it: "
                                            + "whether both columns keep flowing, whether the "
                                            + "shorter one stays put, and whether anything is "
                                            + "clipped when the pair no longer fits.")))
                    .addSection(right -> right
                            .addParagraph(p -> p.text("Period").textStyle(HEADING))
                            .addParagraph(p -> p.textStyle(BODY).padding(DocumentInsets.top(4))
                                    .text("Q3, closing 30 September. Shorter on purpose."))));

            // The growing card: the acceptance example from the editing contract. Adding
            // a sentence here must move the fill and the border with the text.
            page.addSection("GrowingCard", card -> card
                    .softPanel(SURFACE, 8, 14)
                    .accentLeft(ACCENT, 3)
                    .margin(DocumentInsets.symmetric(6, 0))
                    .addParagraph(p -> p.text("Notice").textStyle(HEADING))
                    .addParagraph(p -> p.textStyle(BODY).padding(DocumentInsets.top(5))
                            .text("Lengthen this sentence in Word and the panel behind it has to "
                                    + "grow with it. A panel that keeps its old height and clips "
                                    + "the new text is the failure this corpus exists to catch.")));

            page.addParagraph(p -> p.text("Billing").textStyle(HEADING)
                    .padding(DocumentInsets.top(16)));

            page.addTable(t -> t.name("Billing")
                    .autoColumns(3)
                    .headerRow("Item", "Qty", "Amount")
                    .repeatHeader()
                    .row("Platform subscription", "12", "1 440.00")
                    .row("Priority support", "12", "720.00")
                    .row("Onboarding workshop", "1", "350.00")
                    .row("Additional storage, billed monthly in arrears", "9", "216.00")
                    .totalRow("Total", "", "2 726.00")
                    .margin(DocumentInsets.top(6)));

            page.addPageBreak(b -> b.name("toSecond"));

            page.addParagraph(p -> p.text("Attachments and notes").textStyle(TITLE));

            page.addImage(image -> image
                    .source(DocumentImageData.fromBytes(sampleImagePng()))
                    .width(180)
                    .height(101)
                    .margin(DocumentInsets.symmetric(12, 0)));

            page.addParagraph(p -> p.text("Checklist").textStyle(HEADING));

            page.addList(list -> list
                    .name("Checklist")
                    .textStyle(BODY)
                    .itemSpacing(3)
                    .padding(DocumentInsets.top(4))
                    .items("Usage reconciled against the metering export",
                            "Support response times inside the agreed window",
                            "Storage growth reviewed with the account team",
                            "Next review scheduled for the first week of the quarter"));

            page.addParagraph(p -> p.textStyle(UNINSTALLED_FAMILY)
                    .padding(DocumentInsets.top(14))
                    .text("This line is set in Lato, a family GraphCompose bundles and a stock "
                            + "desktop does not install. If it renders in something else, the "
                            + "export did not carry the face."));
        });

        return session;
    }

    /**
     * The small boundary fixture: a rounded panel and a clipped outline, kept out of the
     * baseline so a known-hard case cannot quietly degrade the main result.
     *
     * <p>The caller owns the returned session and closes it.</p>
     *
     * @param pdfFile default output file for {@code buildPdf()}
     * @return an open session carrying the boundary cases
     */
    public static DocumentSession boundaryCases(Path pdfFile) {
        DocumentSession session = GraphCompose.document(pdfFile)
                .pageSize(360, 460)
                .margin(DocumentInsets.of(28))
                .create();

        session.pageFlow(page -> {
            page.name("Boundaries");

            page.addParagraph(p -> p.text("Boundary cases").textStyle(HEADING));

            // Rounded panel: Word can shade and border a paragraph, but not round it,
            // so this is the first construct that has to pick a representation.
            page.addSection("RoundedCard", card -> card
                    .softPanel(SURFACE, 14, 12)
                    .margin(DocumentInsets.top(10))
                    .addParagraph(p -> p.textStyle(BODY)
                            .text("A 14pt corner radius on a panel that also has to grow with "
                                    + "its text. Word shading is rectangular.")));

            // Clipped outline: the label is longer than the circle, so CLIP_PATH has ink
            // to cut. This is the case the export must not answer by rasterising text.
            page.addCircle(120, ACCENT, circle -> circle
                    .name("ClippedCircle")
                    .center(new com.demcha.compose.document.dsl.ParagraphBuilder()
                            .text("A label far longer than the circle that holds it")
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(FontName.HELVETICA)
                                    .size(9)
                                    .color(DocumentColor.WHITE)
                                    .build())
                            .build()));
        });

        return session;
    }

    /**
     * A deterministic PNG — a plain gradient, generated rather than committed so the
     * corpus carries no binary fixture.
     *
     * @return PNG bytes, 320x180
     */
    public static byte[] sampleImagePng() {
        int width = 320;
        int height = 180;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int r = 26 + (x * 160) / width;
                int g = 86 + (y * 120) / height;
                int b = 148 + (x * 60) / width;
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("sample PNG could not be encoded", e);
        }
    }
}
