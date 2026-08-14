package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds a table cell's frame to declaring which way it reads, and to handing over the line
 * as written.
 *
 * <p>The frame is pinned where the layout put it, so a slide already agrees with the PDF
 * about where the text sits. What it does not agree about without this is what happens
 * inside the frame: PowerPoint resolves neutrals against the paragraph's base direction,
 * which defaults to left-to-right, so a Hebrew cell ending in a full stop put the stop on
 * the wrong end of its own box.</p>
 *
 * <p>Everything else is left to PowerPoint, and this is where the cell parts company with
 * the paragraph handler. A paragraph arrives as one frame per span, so no frame holds a
 * bracket pair for PowerPoint to resolve and the mirroring has to be done first; a cell is
 * one frame holding a whole line, which is the input PowerPoint's own algorithm is complete
 * for. Mirroring it first swaps the brackets twice. For the same reason the formatting
 * controls survive the hand-off: the joining controls are the instruction to a shaper that
 * has not run, and the direction marks are the instruction to an algorithm that has not
 * run either.</p>
 */
class PptxTableCellDirectionTest {

    private static final String HEBREW = "שלום עולם";
    private static final String LATIN = "Hello world";
    /** Hebrew closing on a bracket: the neutral whose facing is at issue. */
    private static final String BRACKETED = "שנה טובה (2026)";
    /** Built from code points: a literal control would be invisible in the source. */
    private static final String ZWNJ = String.valueOf((char) 0x200C);
    private static final String LRI = String.valueOf((char) 0x2066);
    private static final String PDI = String.valueOf((char) 0x2069);
    /** Two Arabic letters the author has told the shaper not to connect. */
    private static final String WITH_ZWNJ = "ب" + ZWNJ + "ه";
    /** A neutral stretch the author has isolated from the line it sits in. */
    private static final String WITH_ISOLATE = "שלום " + LRI + "(a > b)" + PDI;

    @Test
    void aRightToLeftCellDeclaresItsDirection() throws Exception {
        List<XSLFTextParagraph> frames = paragraphsOf(render(HEBREW, TextDirection.RTL));

        assertThat(frames).isNotEmpty();
        assertThat(frames).allMatch(PptxTableCellDirectionTest::declaresRightToLeft,
                "every frame of a right-to-left cell says so");
    }

    @Test
    void aCellWithNoDeclaredDirectionSaysNothing() throws Exception {
        assertThat(paragraphsOf(render(LATIN, null)))
                .describedAs("every deck exported before direction existed must keep "
                        + "producing the same frames")
                .isNotEmpty()
                .noneMatch(PptxTableCellDirectionTest::declaresRightToLeft);
    }

    @Test
    void theTextIsHandedOverInLogicalOrder() throws Exception {
        assertThat(texts(paragraphsOf(render(HEBREW, TextDirection.RTL))))
                .describedAs("PowerPoint reorders it itself; handing over the display "
                        + "order would reorder it twice and scramble the cell")
                .contains(HEBREW);
    }

    @Test
    void aBracketInARightToLeftCellIsLeftForPowerPointToMirror() throws Exception {
        List<String> frames = texts(paragraphsOf(render(BRACKETED, TextDirection.RTL)));

        assertThat(frames)
                .describedAs("a cell is one frame holding a whole line, which is the input "
                        + "PowerPoint's own algorithm is complete for — pre-mirroring it "
                        + "swaps the brackets a second time, and \"(2026)\" was drawn as "
                        + "\")2026(\" on a slide until this stopped")
                .anyMatch(text -> text.contains("(2026)"))
                .noneMatch(text -> text.contains(")2026("));
    }

    @Test
    void theArabicJoiningControlsSurviveTheHandOff() throws Exception {
        // ZWNJ says these two letters must not connect. PowerPoint shapes the text itself,
        // so the control is an instruction to its shaper — dropped on the way over, it
        // receives a word it joins straight back up, and the author's spelling is gone.
        assertThat(texts(paragraphsOf(render(WITH_ZWNJ, TextDirection.RTL, FontName.AMIRI))))
                .describedAs("the render sanitizer drops these because a PDF draws glyphs "
                        + "and a zero-width control has none; the consumer that shapes for "
                        + "itself needs them")
                .anyMatch(text -> text.contains(ZWNJ));
    }

    @Test
    void theBidiControlsSurviveTheHandOffToo() throws Exception {
        // The cell is handed over as a whole logical line precisely so PowerPoint runs its
        // own algorithm over it. That makes an isolate part of its input, not something the
        // engine has already consumed — which is what separates this path from a paragraph
        // span, where the order is settled before the hand-off.
        assertThat(texts(paragraphsOf(render(WITH_ISOLATE, TextDirection.RTL))))
                .describedAs("an isolate is the author's instruction about a neutral stretch "
                        + "of text, and nothing has read it yet")
                .anyMatch(text -> text.contains(LRI) && text.contains(PDI));
    }

    @Test
    void autoIsAnsweredByTheCellItLandedIn() throws Exception {
        List<XSLFTextParagraph> frames = paragraphsOf(renderPair(HEBREW, LATIN));

        assertThat(frames.stream().filter(f -> f.getText().contains(HEBREW)).toList())
                .describedAs("the Hebrew cell reads right to left")
                .isNotEmpty()
                .allMatch(PptxTableCellDirectionTest::declaresRightToLeft);
        assertThat(frames.stream().filter(f -> f.getText().contains(LATIN)).toList())
                .describedAs("and the Latin cell beside it does not")
                .isNotEmpty()
                .noneMatch(PptxTableCellDirectionTest::declaresRightToLeft);
    }

    private static boolean declaresRightToLeft(XSLFTextParagraph paragraph) {
        var properties = paragraph.getXmlObject().getPPr();
        return properties != null && properties.isSetRtl() && properties.getRtl();
    }

    private static List<String> texts(List<XSLFTextParagraph> paragraphs) {
        return paragraphs.stream().map(XSLFTextParagraph::getText).toList();
    }

    private static List<XSLFTextParagraph> paragraphsOf(byte[] pptx) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            List<XSLFTextParagraph> paragraphs = new ArrayList<>();
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape text) {
                    for (XSLFTextParagraph paragraph : text.getTextParagraphs()) {
                        if (!paragraph.getText().isBlank()) {
                            paragraphs.add(paragraph);
                        }
                    }
                }
            }
            return paragraphs;
        }
    }

    private static byte[] render(String text, TextDirection direction) {
        return render(text, direction, FontName.DAVID_LIBRE);
    }

    private static byte[] render(String text, TextDirection direction, FontName font) {
        return render(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(direction, font))
                .row(text));
    }

    private static byte[] renderPair(String first, String second) {
        return render(table -> table
                .columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(200))
                .defaultCellStyle(cellStyle(TextDirection.AUTO))
                .row(first, second));
    }

    private static DocumentTableStyle cellStyle(TextDirection direction) {
        return cellStyle(direction, FontName.DAVID_LIBRE);
    }

    private static DocumentTableStyle cellStyle(TextDirection direction, FontName font) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .textStyle(DocumentTextStyle.builder()
                        .fontName(font)
                        .size(13)
                        .build());
        if (direction != null) {
            builder.direction(direction);
        }
        return builder.build();
    }

    private static byte[] render(Consumer<com.demcha.compose.document.dsl.TableBuilder> spec) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page.addTable(spec));
            return document.toPptxBytes();
        }
    }
}
