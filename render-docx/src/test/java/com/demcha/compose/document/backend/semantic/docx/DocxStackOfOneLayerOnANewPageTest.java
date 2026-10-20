package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The first paragraph, picture or spacer of a stack of one layer the layout moves to a new page
 * keeps the space the layout leaves above it there, in Word.
 *
 * <p>A stack of one layer is written in the flow, and Word drops its first paragraph's space
 * above at the top of a page as any other's. Measured in Word 16 and LibreOffice on Windows, the
 * first line of a paragraph with a 20pt margin opening such a stack on a new page stood 20pt
 * high in both, a picture with one 20pt high, and one in a stack with a 14pt margin 14pt high.
 * Held in a line kept with it, as in the flow, each stood where the page sets it. A stack with a
 * 14pt margin and an 8pt spacer before its text stood 22.9pt high, the spacer left at the foot of
 * the page above; its line holding the margin, kept with the text, it stood 1.2pt high. A layer's
 * offset in its stack is named and not written, and the line holds none of it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxStackOfOneLayerOnANewPageTest {

    private static final double EDGE = 12;

    /**
     * The page's content is 160pt tall: 300 x 200 with a 20pt margin. A spacer fills 150pt of it,
     * and nothing taller than 10pt fits under it.
     */
    private static final double CONTENT = 160;

    @Test
    void aParagraphsMarginOpeningTheStackIsHeldInALineKeptWithIt() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.topLeft(new ParagraphBuilder()
                .name("Opening").text("Opening").margin(DocumentInsets.top(EDGE)).build()));
        assertThat(startPage(content, "Opening")).as("the premise: the stack opens the second page").isEqualTo(1);
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
    void aPicturesMarginOpeningTheStackIsHeld() throws Exception {
        try (XWPFDocument document = export(movedToANewPage(stack -> stack.topLeft(new ImageBuilder()
                .source(picture()).size(60, 60).margin(DocumentInsets.top(EDGE)).build())))) {
            XWPFParagraph picture = pictureIn(document);

            assertThat(lineBefore(document, picture)).isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(picture))).isZero();
        }
    }

    @Test
    void theStacksOwnMarginIsHeld() throws Exception {
        try (XWPFDocument document = export(movedToANewPage(stack -> stack.margin(DocumentInsets.top(EDGE))
                .topLeft(new ParagraphBuilder().text("Opening").build())))) {
            XWPFParagraph opening = paragraphWith(document, "Opening");

            assertThat(lineBefore(document, opening)).as("the stack's margin").isEqualTo(Math.round(EDGE * 20));
            assertThat(before(spacing(opening))).isZero();
        }
    }

    @Test
    void theStacksPaddingIsHeldWithTheParagraphsMargin() throws Exception {
        try (XWPFDocument document = export(movedToANewPage(stack -> stack.padding(DocumentInsets.top(9))
                .topLeft(new ParagraphBuilder().text("Opening").margin(DocumentInsets.top(6)).build())))) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).as("9pt and 6pt").isEqualTo(300);
        }
    }

    @Test
    void onlyTheFirstBlockOpensThePage() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.topLeft(new SectionBuilder()
                .addParagraph(p -> p.name("Opening").text("Opening").margin(DocumentInsets.top(EDGE)))
                .addParagraph(p -> p.text("Second").margin(DocumentInsets.top(EDGE)))
                .build()));
        assertThat(startPage(content, "Opening")).as("the premise: the stack opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph second = paragraphWith(document, "Second");

            assertThat(paragraphBefore(document, second).getText()).as("no line before the second")
                    .isEqualTo("Opening");
        }
    }

    @Test
    void anOffsetDownTheStackIsNotHeld() throws Exception {
        // The layer stands 12pt down its stack, which is named, not written: the line holds the
        // paragraph's 12pt margin and none of the offset, where the note says Word writes it.
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.position(new ParagraphBuilder()
                .name("Opening").text("Opening").margin(DocumentInsets.top(EDGE)).build(), 0, EDGE, LayerAlign.TOP_LEFT));
        assertThat(startPage(content, "Opening")).as("the premise: the stack opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).as("its margin alone")
                    .isEqualTo(Math.round(EDGE * 20));
        }
        assertThat(DocxExports.report(300, 200, 20, content).bySubject().get("layer offset"))
                .as("the offset named").singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("12pt lower than its stack starts"));
    }

    @Test
    void anOffsetUpTheStackLeavesTheStacksEdgeHeld() throws Exception {
        // Set 6pt above where its stack's 12pt margin starts it, the layer is written where the
        // stack starts, below the whole margin.
        try (XWPFDocument document = export(movedToANewPage(stack -> stack.margin(DocumentInsets.top(EDGE))
                .position(new ParagraphBuilder().text("Opening").build(), 0, -6, LayerAlign.TOP_LEFT)))) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening"))).as("the stack's margin")
                    .isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void anOffsetAloneLeavesNoLine() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.position(new ParagraphBuilder()
                .name("Opening").text("Opening").build(), 0, EDGE, LayerAlign.TOP_LEFT));
        assertThat(startPage(content, "Opening")).as("the premise: the stack opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            assertThat(document.getParagraphs()).extracting(XWPFParagraph::getText)
                    .as("the filler and the paragraph").containsExactly("", "Opening");
        }
    }

    @Test
    void anOffsetEndsWithItsStack() throws Exception {
        // A stack with its layer set 12pt down opens the first page; the paragraph opening the
        // second is in the flow, and its whole margin is held.
        try (XWPFDocument document = export(page -> page
                .addLayerStack(stack -> stack.position(new ParagraphBuilder().text("Moved").build(),
                        0, EDGE, LayerAlign.TOP_LEFT))
                .spacer(0, CONTENT - 20)
                .addParagraph(p -> p.text("Opening").margin(DocumentInsets.top(EDGE))))) {
            assertThat(lineBefore(document, paragraphWith(document, "Opening")))
                    .as("its margin, none of the stack's offset taken off it").isEqualTo(Math.round(EDGE * 20));
        }
    }

    @Test
    void aSpacerOpeningTheStackHoldsItsEdgeAndGoesWithWhatFollows() throws Exception {
        // Measured: a stack with a 14pt margin and an 8pt spacer before its text stood 22pt high
        // in Word and LibreOffice, the spacer left at the foot of the page above.
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.margin(DocumentInsets.top(EDGE))
                .topLeft(new SectionBuilder().spacer(0, 8)
                        .addParagraph(p -> p.name("Opening").text("Opening")).build()));
        assertThat(startPage(content, "Opening")).as("the premise: the stack opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph spacer = paragraphBefore(document, paragraphWith(document, "Opening"));
            CTSpacing spacing = spacing(spacer);

            assertThat(DocxTwips.of(spacing.getLine())).as("the stack's margin, and the spacer's hairline")
                    .isEqualTo(Math.round(EDGE * 20) + HAIRLINE);
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).as("kept with the text under it").isTrue();
        }
    }

    @Test
    void aSpacerEndingASectionTheLayersTextFollowsGoesWithIt() throws Exception {
        try (XWPFDocument document = export(movedToANewPage(stack -> stack.margin(DocumentInsets.top(EDGE))
                .topLeft(new SectionBuilder().addSection(inner -> inner.spacer(0, 8))
                        .addParagraph("Opening").build())))) {
            XWPFParagraph spacer = paragraphBefore(document, paragraphWith(document, "Opening"));

            assertThat(spacer.getText()).isEmpty();
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).as("kept with the text after its section").isTrue();
        }
    }

    @Test
    void aSpacerAloneInItsStackIsKeptWithNothing() throws Exception {
        // What follows the stack is not the stack's: the spacer is not kept with it.
        try (XWPFDocument document = export(page -> page
                .spacer(0, CONTENT - 10)
                .addLayerStack(stack -> stack.margin(DocumentInsets.top(EDGE))
                        .topLeft(new SectionBuilder().spacer(0, 8).build()))
                .addParagraph("After"))) {
            XWPFParagraph spacer = paragraphBefore(document, paragraphWith(document, "After"));

            assertThat(DocxTwips.of(spacing(spacer).getLine())).as("its edge held all the same")
                    .isEqualTo(Math.round(EDGE * 20) + HAIRLINE);
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).as("nothing in its stack follows it").isFalse();
        }
    }

    @Test
    void aSpacerUnderAnOffsetHoldsNoneOfIt() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.margin(DocumentInsets.top(EDGE))
                .position(new SectionBuilder().spacer(0, 8).addParagraph("Opening").build(), 0, EDGE,
                        LayerAlign.TOP_LEFT));
        try (XWPFDocument document = export(content)) {
            XWPFParagraph spacer = paragraphBefore(document, paragraphWith(document, "Opening"));

            assertThat(DocxTwips.of(spacing(spacer).getLine())).as("the stack's margin, not its layer's offset")
                    .isEqualTo(Math.round(EDGE * 20) + HAIRLINE);
        }
        assertThat(DocxExports.report(300, 200, 20, content).bySubject().get("layer offset"))
                .as("the offset named").isNotNull();
    }

    @Test
    void aSpacerOpeningABandIsLeftToTheBand() throws Exception {
        Consumer<PageFlowBuilder> content = movedToANewPage(stack -> stack.margin(DocumentInsets.top(EDGE))
                .topLeft(new SectionBuilder().spacer(0, 8).addParagraph(p -> p.name("Opening").text("Opening")).build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()));
        assertThat(startPage(content, "Opening")).as("the premise: the band opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(content)) {
            XWPFParagraph spacer = paragraphBefore(document, paragraphWith(document, "Opening"));

            assertThat(DocxTwips.of(spacing(spacer).getLine())).as("its hairline alone").isEqualTo(HAIRLINE);
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).isFalse();
        }
    }

    @Test
    void aPullOutOfTheBlockOnThePageAboveStaysThere() throws Exception {
        // The paragraph above pulls what follows it 5pt up. On its page, Word sets no block over
        // another, and the report says so; the stack the layout moves to the next page starts
        // that page at its top, and nothing stands it lower.
        Consumer<PageFlowBuilder> content = page -> page
                .spacer(0, CONTENT - 20)
                .addParagraph(p -> p.text("Above").margin(new DocumentInsets(0, 0, -5, 0)))
                .addLayerStack(stack -> stack.topLeft(new ParagraphBuilder().name("Opening").text("Opening")
                        .build()));
        assertThat(startPage(content, "Opening")).as("the premise: the stack opens the second page").isEqualTo(1);

        assertThat(DocxExports.report(300, 600, 20, content).bySubject().get("space above"))
                .as("on the page of the block above, the pull is named").isNotNull();
        assertThat(DocxExports.report(300, 200, 20, content).bySubject().get("space above"))
                .as("on the next page, nothing is").isNull();
    }

    @Test
    void aParagraphOpeningABandIsLeftToTheBand() throws Exception {
        // A stack of two layers is a band, which sets its layers from its own top.
        Consumer<PageFlowBuilder> moved = movedToANewPage(stack -> stack
                .topLeft(new ParagraphBuilder().name("Opening").text("Opening").margin(DocumentInsets.top(EDGE)).build())
                .bottomRight(new ParagraphBuilder().text("Caption").build()));
        assertThat(startPage(moved, "Opening")).as("the premise: the band opens the second page").isEqualTo(1);
        try (XWPFDocument document = export(moved);
             XWPFDocument onOnePage = DocxExports.withLayout(300, 600, 20, moved)) {
            assertThat(document.getParagraphs().indexOf(paragraphWith(document, "Opening")))
                    .as("no line of the flow's held before it")
                    .isEqualTo(onOnePage.getParagraphs().indexOf(paragraphWith(onOnePage, "Opening")));
        }
    }

    /** A stack under a 150pt filler, which the layout moves to the second page. */
    private static Consumer<PageFlowBuilder> movedToANewPage(
            java.util.function.UnaryOperator<com.demcha.compose.document.dsl.LayerStackBuilder> stack) {
        return page -> page.spacer(0, CONTENT - 10).addLayerStack(builder -> stack.apply(builder));
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(300, 200, 20, content);
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
        BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", png);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }
}
