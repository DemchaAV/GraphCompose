package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A container's fill and borders reach the paragraphs inside it.
 *
 * <p>Word has no element that wraps a run of paragraphs, but it shades and borders each
 * one, and consecutive paragraphs sharing a fill render as a single band. Until this
 * landed the exporter treated a section as a transparent wrapper and dropped its paint
 * silently, so a card exported as bare text with no sign that anything was missing.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxContainerPaintTest {

    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor ACCENT = DocumentColor.rgb(26, 86, 148);

    @Test
    void filledSectionShouldShadeEveryParagraphInsideIt() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("First"))
                .addParagraph(p -> p.text("Second"))));

        assertThat(paragraphs).hasSize(2);
        for (XWPFParagraph paragraph : paragraphs) {
            assertThat(shadingFill(paragraph))
                    .as("every paragraph in the panel carries its fill")
                    .isEqualTo("EEF3F9");
        }
    }

    @Test
    void sectionBorderShouldReachTheParagraphAsAParagraphBorder() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Card", card -> card
                .fillColor(SURFACE)
                .accentLeft(ACCENT, 3)
                .addParagraph(p -> p.text("Bordered"))));

        CTPPr properties = paragraphs.get(0).getCTP().getPPr();
        assertThat(properties.isSetPBdr()).as("the accent reaches w:pBdr").isTrue();
        assertThat(properties.getPBdr().isSetLeft()).as("on the side it was asked for").isTrue();
        assertThat(hex(properties.getPBdr().getLeft().getColor())).isEqualTo("1A5694");
        // w:sz counts eighths of a point, so a 3pt accent is 24.
        assertThat(properties.getPBdr().getLeft().getSz().intValue()).isEqualTo(24);
        assertThat(properties.getPBdr().isSetRight())
                .as("a side the author did not ask for stays unwritten")
                .isFalse();
    }

    @Test
    void uniformStrokeShouldStandInForAllFourSides() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Card", card -> card
                .stroke(DocumentStroke.of(ACCENT, 1))
                .addParagraph(p -> p.text("Outlined"))));

        var edges = paragraphs.get(0).getCTP().getPPr().getPBdr();
        assertThat(edges.isSetTop()).isTrue();
        assertThat(edges.isSetBottom()).isTrue();
        assertThat(edges.isSetLeft()).isTrue();
        assertThat(edges.isSetRight()).isTrue();
    }

    @Test
    void unpaintedSectionShouldLeaveItsParagraphsAlone() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Plain", plain -> plain
                .padding(DocumentInsets.of(8))
                .addParagraph(p -> p.text("Nothing to paint"))));

        CTPPr properties = paragraphs.get(0).getCTP().getPPr();
        // A wrapper with no paint must not start writing empty shading or borders: the
        // export of a plain section is what it always was.
        assertThat(properties == null || !properties.isSetShd()).isTrue();
        assertThat(properties == null || !properties.isSetPBdr()).isTrue();
    }

    @Test
    void innerPanelShouldWinOverTheOneAroundIt() throws Exception {
        DocumentColor inner = DocumentColor.rgb(255, 240, 200);
        List<XWPFParagraph> paragraphs = bodyOf(page -> page.addSection("Outer", outer -> outer
                .fillColor(SURFACE)
                .addParagraph(p -> p.text("Outer text"))
                .addSection("Inner", in -> in
                        .fillColor(inner)
                        .addParagraph(p -> p.text("Inner text")))));

        assertThat(shadingFill(paragraphs.get(0))).isEqualTo("EEF3F9");
        assertThat(shadingFill(paragraphs.get(1)))
                .as("a paragraph wears the panel it actually sits in")
                .isEqualTo("FFF0C8");
    }

    @Test
    void paintShouldNotLeakToParagraphsAfterTheContainer() throws Exception {
        List<XWPFParagraph> paragraphs = bodyOf(page -> {
            page.addSection("Card", card -> card
                    .fillColor(SURFACE)
                    .addParagraph(p -> p.text("Inside")));
            page.addParagraph(p -> p.text("After"));
        });

        assertThat(shadingFill(paragraphs.get(0))).isEqualTo("EEF3F9");
        CTPPr after = paragraphs.get(1).getCTP().getPPr();
        assertThat(after == null || !after.isSetShd())
                .as("the panel ends where the container ends")
                .isTrue();
    }

    private static String shadingFill(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetShd()) {
            return null;
        }
        return hex(properties.getShd().getFill());
    }

    /**
     * Reads an {@code ST_HexColor} back as the six hex digits it was written as.
     *
     * <p>The type is an xmlbeans union, so a concrete colour comes back as the three
     * bytes rather than as the string the writer passed in; {@code String.valueOf} on it
     * yields an array identity, which is how this test first "failed" against a value
     * that was correct.</p>
     */
    private static String hex(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02X", b & 0xFF));
            }
            return out.toString();
        }
        return String.valueOf(value);
    }

    private static List<XWPFParagraph> bodyOf(
            Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) throws Exception {
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return document.getParagraphs().stream()
                    .filter(p -> !p.getText().isBlank())
                    .toList();
        }
    }
}
