package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A band of layers the layout moves to a new page keeps the space the layout leaves above it
 * there, in Word.
 *
 * <p>A stack of more than one layer is written as a band, its layers one after the other, and Word
 * drops the space above its first paragraph at the top of a page as any other's. Measured in Word
 * 16 and LibreOffice on Windows, a band with a 14pt margin that Word set on a new page whole stood
 * 14pt high in both; so did one nested in a stack of one layer with that margin, and one in a
 * section with a 10pt margin, 10pt. Held in a line kept with its first block — a paragraph, a
 * picture, or a spacer's own line — each stood where the page sets it, within 1.2pt.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBandOnANewPageTest {

    private static final double EDGE = 12;

    /**
     * The page's content is 160pt tall: 300 x 200 with a 20pt margin. A spacer fills 150pt of it,
     * and nothing taller than 10pt fits under it.
     */
    private static final double CONTENT = 160;

    @Test
    void theBandsMarginIsHeldInALineKeptWithItsFirstParagraph() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(band(stack -> stack.margin(DocumentInsets.top(EDGE))));
        assertThat(startPage(content, "Opening")).as("the premise: the band opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph opening = paragraphWith(document, "Opening");
            XWPFParagraph line = paragraphBefore(document, opening);
            CTSpacing spacing = spacing(line);

            assertThat(line.getText()).isEmpty();
            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the margin, a line's height").isEqualTo(Math.round(EDGE * 20));
            assertThat(line.getCTP().getPPr().isSetKeepNext()).as("kept with the paragraph").isTrue();
            assertThat(before(spacing)).as("the filler's height stays above the line").isEqualTo(owed(CONTENT - 10));
            assertThat(before(spacing(opening))).as("its edge is the line's").isZero();
        }
    }

    @Test
    void theBandsPaddingIsHeldWithItsFirstParagraphsMargin() throws Exception {
        // 9pt of the band's padding and 6pt of the paragraph's margin: the band owes the first above
        // the paragraph, which owes the second itself.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .padding(DocumentInsets.top(9))
                .topLeft(new ParagraphBuilder().name("Opening").text("Opening").margin(DocumentInsets.top(6)).build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()).build());
        assertOpensTheSecondPage(content, "Opening");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph opening = paragraphWith(document, "Opening");
            CTSpacing line = spacing(paragraphBefore(document, opening));

            assertThat(DocxTwips.of(line.getLine())).as("9pt and 6pt").isEqualTo(300);
            assertThat(before(line)).as("the filler's height stays above the line, whole").isEqualTo(owed(CONTENT - 10));
            assertThat(before(spacing(opening))).isZero();
        }
    }

    @Test
    void aBackdropDrawnFirstLeavesTheEdgeToTheFirstParagraph() throws Exception {
        // Measured: a band with a 14pt margin and a backdrop laid first, moved whole to a new page,
        // stood 14pt high in Word and LibreOffice. The backdrop is drawn on that page first, and
        // the paragraph after it starts no page of its own, though nothing above it is written there.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .margin(DocumentInsets.top(EDGE))
                .back(new com.demcha.compose.document.dsl.ShapeBuilder().size(200, 30)
                        .fillColor(com.demcha.compose.document.style.DocumentColor.LIGHT_GRAY).build())
                .topLeft(new ParagraphBuilder().name("Opening").text("Opening").build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()).build());
        assertThat(startPage(content, "Opening")).as("the premise: the band opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph opening = paragraphWith(document, "Opening");

            assertThat(lineBefore(document, opening)).as("the band's margin").isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(opening))).isZero();
        }
    }

    @Test
    void aPictureOpeningTheBandIsHeld() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .margin(DocumentInsets.top(EDGE))
                .topLeft(new ImageBuilder().name("Picture").source(picture()).size(60, 30).build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()).build());
        assertOpensTheSecondPage(content, "Picture");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph picture = pictureIn(document);

            assertThat(lineBefore(document, picture)).isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(picture))).isZero();
        }
    }

    @Test
    void aPicturesOwnMarginIsHeldWithTheBands() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .margin(DocumentInsets.top(EDGE))
                .topLeft(new ImageBuilder().name("Picture").source(picture()).size(60, 30)
                        .margin(DocumentInsets.top(5)).build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()).build());
        assertOpensTheSecondPage(content, "Picture");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph picture = pictureIn(document);

            assertThat(lineBefore(document, picture)).as("12pt and 5pt").isEqualTo(340);
            assertThat(before(spacing(picture))).isZero();
        }
    }

    @Test
    void aSpacerOpeningTheBandHoldsItsEdgeAndGoesWithWhatFollows() throws Exception {
        // Measured: held so, a band with a 14pt margin and an 8pt spacer before its text stood 1.2pt
        // high in Word and LibreOffice, as a stack of one layer opening with one does.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .margin(DocumentInsets.top(EDGE))
                .topLeft(new SectionBuilder().spacer(0, 8).addParagraph(p -> p.name("Opening").text("Opening")).build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()).build());
        assertThat(startPage(content, "Opening")).as("the premise: the band opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph spacer = paragraphBefore(document, paragraphWith(document, "Opening"));

            assertThat(DocxTwips.of(spacing(spacer).getLine())).as("the band's margin, and the spacer's hairline")
                    .isEqualTo(Math.round(EDGE * 20) + HAIRLINE);
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).as("kept with the text under it").isTrue();
        }
    }

    @Test
    void aBandInAStackOfOneLayerHoldsTheStacksMargin() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .margin(DocumentInsets.top(EDGE)).topLeft(band(stack -> stack)).build());
        assertOpensTheSecondPage(content, "Opening");
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void aBandInASectionHoldsTheSectionsMargin() throws Exception {
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 10)
                .addSection(section -> section.margin(DocumentInsets.top(10)).add(band(stack -> stack)));
        assertOpensTheSecondPage(content, "Opening");
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).isEqualTo(200);
        }
    }

    @Test
    void onlyTheBandsFirstBlockOpensThePage() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(band(stack -> stack.margin(DocumentInsets.top(EDGE))));
        assertOpensTheSecondPage(content, "Opening");
        try (XWPFDocument document = export(content)) {
            XWPFParagraph caption = paragraphWith(document, "Caption");

            assertThat(paragraphBefore(document, caption).getText()).as("no line before the second layer")
                    .isEqualTo("Opening");
        }
    }

    @Test
    void aTableOpeningTheBandLeavesTheLayerAfterItUnheld() throws Exception {
        // The table is the band's first block, and opens the page; the caption under it does not.
        Consumer<PageFlowBuilder> content = movedToANewPage(new LayerStackBuilder()
                .topLeft(new com.demcha.compose.document.dsl.TableBuilder().name("Table").columns(
                                com.demcha.compose.document.table.DocumentTableColumn.auto())
                        .row("Cell").build())
                .bottomRight(new ParagraphBuilder().text("Caption").margin(DocumentInsets.top(EDGE)).build()).build());
        assertOpensTheSecondPage(content, "Table");
        try (XWPFDocument document = export(content)) {
            List<org.apache.poi.xwpf.usermodel.IBodyElement> body = document.getBodyElements();
            int caption = body.indexOf(paragraphWith(document, "Caption"));

            assertThat(body.get(caption - 1)).as("the table, and no line held between them")
                    .isInstanceOf(org.apache.poi.xwpf.usermodel.XWPFTable.class);
        }
    }

    @Test
    void aBandOnItsPageHoldsNoLine() throws Exception {
        // The same band, the page tall enough to hold it under the filler.
        try (XWPFDocument document = DocxExports.withLayout(300, 600, 20,
                movedToANewPage(band(stack -> stack.margin(DocumentInsets.top(EDGE)))))) {
            XWPFParagraph before = paragraphBefore(document, paragraphWith(document, "Opening"));

            assertThat(DocxTwips.of(spacing(before).getLine())).as("the filler's hairline, no line held")
                    .isEqualTo(HAIRLINE);
        }
    }

    @Test
    void aBandInARowsCellHoldsNoLine() throws Exception {
        // The row's table opens the page; the band's paragraphs are its cell's.
        Consumer<PageFlowBuilder> content = page -> page.spacer(0, CONTENT - 10)
                .addRow(row -> row.add(band(stack -> stack.margin(DocumentInsets.top(EDGE)))).addParagraph("Beside"));
        assertThat(startPage(content, "Opening")).as("the premise: the row opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(0);

            assertThat(cell.getParagraphs().get(0).getText()).as("the band's first paragraph opens its cell")
                    .isEqualTo("Opening");
        }
    }

    /** A band of two layers: a paragraph at its top, a caption at its foot. */
    private static LayerStackNode band(UnaryOperator<LayerStackBuilder> stack) {
        return stack.apply(new LayerStackBuilder())
                .topLeft(new ParagraphBuilder().name("Opening").text("Opening").build())
                .bottomRight(new ParagraphBuilder().text("Caption").build())
                .build();
    }

    /** A band under a 150pt filler, which the layout moves to the second page. */
    private static Consumer<PageFlowBuilder> movedToANewPage(LayerStackNode band) {
        return page -> page.spacer(0, CONTENT - 10).add(band);
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(300, 200, 20, content);
    }

    /** The premise of a test of a block opening a page: the layout starts it on the second. */
    private static void assertOpensTheSecondPage(Consumer<PageFlowBuilder> content, String name) {
        assertThat(startPage(content, name)).as("the premise: %s opens the second page", name).isEqualTo(1);
    }

    private static int startPage(Consumer<PageFlowBuilder> content, String name) {
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200).margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(content::accept);
            return session.layoutGraph().nodes().stream()
                    .filter(node -> name.equals(node.semanticName())).findFirst().orElseThrow().startPage();
        }
    }

    /** The height of the exact line holding a block's top edge, in twips. */
    private static long lineBefore(XWPFDocument document, XWPFParagraph paragraph) {
        CTSpacing spacing = spacing(paragraphBefore(document, paragraph));
        assertThat(spacing.getLineRule()).as("a line holding its edge").isEqualTo(STLineSpacingRule.EXACT);
        return DocxTwips.of(spacing.getLine());
    }

    /** A spacer's paragraph is a tenth of a point tall, in twips (see holdToHairline). */
    private static final long HAIRLINE = 2;

    /**
     * The space owed above the block after the filler, in twips: {@code points} of it, less the
     * filler's own hairline, which is part of its height.
     */
    private static long owed(double points) {
        return Math.round(points * 20) - HAIRLINE;
    }

    private static XWPFParagraph paragraphWith(XWPFDocument document, String text) {
        return document.getParagraphs().stream().filter(p -> p.getText().equals(text)).findFirst().orElseThrow();
    }

    private static XWPFParagraph pictureIn(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(paragraph -> paragraph.getRuns().stream().anyMatch(run -> !run.getEmbeddedPictures().isEmpty()))
                .findFirst().orElseThrow();
    }

    private static XWPFParagraph paragraphBefore(XWPFDocument document, XWPFParagraph paragraph) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        return paragraphs.get(paragraphs.indexOf(paragraph) - 1);
    }

    private static CTSpacing spacing(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr().getSpacing();
    }

    private static long before(CTSpacing spacing) {
        return spacing != null && spacing.isSetBefore() ? DocxTwips.of(spacing.getBefore()) : 0;
    }

    private static DocumentImageData picture() {
        BufferedImage image = new BufferedImage(60, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", png);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }
}
