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
 * Holds a table cell's frame to declaring which way it reads.
 *
 * <p>The cell's frame is pinned where the layout put it, so a slide already agrees with
 * the PDF about where the text sits. What it does not agree about without this is what
 * happens inside the frame: PowerPoint resolves neutrals against the paragraph's base
 * direction, which defaults to left-to-right, so a Hebrew cell ending in a full stop put
 * the stop on the wrong end of its own box.</p>
 *
 * <p>The text goes over logical and unshaped — PowerPoint reorders and joins it — with one
 * exception carried over from the paragraph handler: PowerPoint does not mirror a neutral
 * it has placed, so a bracket is swapped before the hand-off.</p>
 */
class PptxTableCellDirectionTest {

    private static final String HEBREW = "שלום עולם";
    private static final String LATIN = "Hello world";
    /** Hebrew closing on a bracket: the neutral whose facing is at issue. */
    private static final String BRACKETED = "שנה טובה (2026)";

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
        return render(table -> table
                .columns(DocumentTableColumn.fixed(300))
                .defaultCellStyle(cellStyle(direction))
                .row(text));
    }

    private static byte[] renderPair(String first, String second) {
        return render(table -> table
                .columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(200))
                .defaultCellStyle(cellStyle(TextDirection.AUTO))
                .row(first, second));
    }

    private static DocumentTableStyle cellStyle(TextDirection direction) {
        DocumentTableStyle.Builder builder = DocumentTableStyle.builder()
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.DAVID_LIBRE)
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
