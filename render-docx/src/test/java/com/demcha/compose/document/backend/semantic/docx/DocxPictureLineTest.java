package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A line holding a picture and no text is as tall in Word as its picture, where the picture
 * fills the page's line.
 *
 * <p>Word sizes such a line itself, and the paragraph's mark is a run in it: the mark's font
 * reached below the picture. {@code SlateOrange}'s skills, an icon beside each label, came out
 * 0.2pt taller each, and the tenth 2.5pt low.</p>
 */
class DocxPictureLineTest {

    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
            + "<circle cx='12' cy='12' r='10' fill='#1A5694'/></svg>");

    /** Half points of a one-point font. */
    private static final long ONE_POINT = 2;

    @Test
    void anIconFillingItsLineSetsTheLineAlone() throws Exception {
        // A 12pt icon in a 10pt style: the page's line is the icon's height.
        XWPFParagraph icon = iconCell(12, 10);

        assertThat(markSize(icon)).as("the mark's font is a point").isEqualTo(ONE_POINT);
        assertThat(DocxTwips.of(icon.getRuns().get(0).getCTR().getRPr().getSzArray(0).getVal()))
                .as("so is the picture's run").isEqualTo(ONE_POINT);
    }

    @Test
    void aSmallIconKeepsTheLineItsFontGivesIt() throws Exception {
        // A 4pt dot in a 12pt style: the page's line is the font's, and so is Word's.
        XWPFParagraph dot = iconCell(4, 12);

        assertThat(markSize(dot)).isNotEqualTo(ONE_POINT);
    }

    @Test
    void anIconBesideALinksTextKeepsTheTextsSize() throws Exception {
        // Written with no layout, the line is Word's to size. An internal link's runs are not the
        // paragraph's own, and its text is still text.
        try (XWPFDocument document = DocxExports.withoutLayout(400, 300, 20, page -> page
                .addParagraph(p -> p.lineSpacing(0)
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(10))
                        .inlineSvgIcon(ICON, 12, InlineImageAlignment.CENTER)
                        .inlineLinkTo("Projects", "projects"))
                .addParagraph(p -> p.text("Projects").anchor("projects")))) {
            XWPFParagraph paragraph = document.getParagraphs().get(0);
            var link = paragraph.getCTP().getHyperlinkArray(0).getRArray(0);

            assertThat(link.getTArray(0).getStringValue()).isEqualTo("Projects");
            assertThat(link.isSetRPr() && link.getRPr().sizeOfSzArray() > 0
                       && DocxTwips.of(link.getRPr().getSzArray(0).getVal()) == ONE_POINT)
                    .as("the link's text is not set at a point").isFalse();
            assertThat(markSize(paragraph)).isNotEqualTo(ONE_POINT);
        }
    }

    @Test
    void aLineOfPicturesInAFaceNoFontHoldsStillExports() throws Exception {
        // No layout and no measured fonts: the line's height is not known, and the export goes on.
        DocumentTextStyle unknown = new DocumentTextStyle(FontName.of("NoSuchFace"), 10,
                DocumentTextDecoration.DEFAULT, DocumentColor.rgb(0, 0, 0));
        try (XWPFDocument document = DocxExports.withoutLayout(400, 300, 20, page -> page
                .addParagraph(p -> p.lineSpacing(0)
                        .textStyle(unknown)
                        .inlineSvgIcon(ICON, 12, InlineImageAlignment.CENTER)))) {
            assertThat(document.getParagraphs()).isNotEmpty();
        }
    }

    private static long markSize(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetRPr() || properties.getRPr().sizeOfSzArray() == 0) {
            return -1;
        }
        return DocxTwips.of(properties.getRPr().getSzArray(0).getVal());
    }

    private static XWPFParagraph iconCell(double iconSize, double styleSize) throws Exception {
        DocumentTableStyle cell = DocumentTableStyle.builder().padding(DocumentInsets.zero()).build();
        ParagraphBuilder icon = new ParagraphBuilder()
                .lineSpacing(0)
                .textStyle(DocumentTextStyle.DEFAULT.withSize(styleSize))
                .inlineSvgIcon(ICON, iconSize, InlineImageAlignment.CENTER);
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(20), DocumentTableColumn.fixed(120))
                .rowCells(DocumentTableCell.node(icon.build()).withStyle(cell),
                        DocumentTableCell.text("Label").withStyle(cell))))) {
            return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().get(0);
        }
    }
}
