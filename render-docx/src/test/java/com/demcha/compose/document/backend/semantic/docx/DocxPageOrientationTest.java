package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A landscape page is stated as landscape.
 *
 * <p>Word draws the page from its width and height, but reads the orientation from
 * {@code w:orient} — for Page Setup, for printing, for the paper tray. The export wrote
 * portrait for every page, so a landscape document opened the right shape and printed on its
 * side.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxPageOrientationTest {

    @Test
    void aWidePageIsLandscape() throws Exception {
        CTPageSz size = pageSizeOf(842, 595);

        assertThat(size.getOrient()).isEqualTo(STPageOrientation.LANDSCAPE);
        assertThat(DocxTwips.of(size.getW()))
                .as("width and height stay as the page gives them")
                .isEqualTo(842 * 20L);
        assertThat(DocxTwips.of(size.getH())).isEqualTo(595 * 20L);
    }

    @Test
    void aTallPageIsPortrait() throws Exception {
        assertThat(pageSizeOf(595, 842).getOrient()).isEqualTo(STPageOrientation.PORTRAIT);
    }

    @Test
    void aSquarePageIsPortraitAsWordTreatsOne() throws Exception {
        assertThat(pageSizeOf(600, 600).getOrient()).isEqualTo(STPageOrientation.PORTRAIT);
    }

    private static CTPageSz pageSizeOf(double width, double height) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(width, height, 36,
                page -> page.addParagraph(p -> p.text("Page")))) {
            return (CTPageSz) document.getDocument().getBody().getSectPr().getPgSz().copy();
        }
    }
}
