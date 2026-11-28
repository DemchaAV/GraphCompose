package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The face a deck writes a markdown paragraph's runs in: a decorated paragraph whose text holds a
 * mark the session reads markdown on keeps its face, as the page lays it out.
 */
class PptxMarkdownParagraphFaceTest {

    @Test
    void aDecoratedParagraphHoldingAMarkIsWrittenInItsOwnFace() throws Exception {
        List<XSLFTextRun> runs = runsOf(
                new String[]{"snake_case_name", "Mixed *Emph* Tail", "Plain *Em* snake_case"},
                new DocumentTextDecoration[]{DocumentTextDecoration.BOLD, DocumentTextDecoration.BOLD,
                        DocumentTextDecoration.DEFAULT});

        assertThat(runs).filteredOn(run -> run.getRawText().contains("snake_case_name"))
                .as("bold, though the text holds an underscore")
                .extracting(XSLFTextRun::isBold, XSLFTextRun::isItalic)
                .isNotEmpty().containsOnly(tuple(true, false));
        assertThat(runs).filteredOn(run -> run.getRawText().contains("Emph"))
                .as("emphasis added to the paragraph's bold")
                .extracting(XSLFTextRun::isBold, XSLFTextRun::isItalic)
                .isNotEmpty().containsOnly(tuple(true, true));
        assertThat(runs).filteredOn(run -> run.getRawText().contains("Tail"))
                .extracting(XSLFTextRun::isBold, XSLFTextRun::isItalic)
                .isNotEmpty().containsOnly(tuple(true, false));
        // A paragraph in the default face reads its marks as it always did.
        assertThat(runs).filteredOn(run -> run.getRawText().trim().equals("Em"))
                .extracting(XSLFTextRun::isBold, XSLFTextRun::isItalic)
                .isNotEmpty().containsOnly(tuple(false, true));
        assertThat(runs).filteredOn(run -> run.getRawText().contains("snake_case")
                        && !run.getRawText().contains("snake_case_name"))
                .extracting(XSLFTextRun::isBold, XSLFTextRun::isItalic)
                .isNotEmpty().containsOnly(tuple(false, false));
    }

    private static List<XSLFTextRun> runsOf(String[] texts, DocumentTextDecoration[] decorations) throws Exception {
        byte[] deck;
        try (DocumentSession session = GraphCompose.document().pageSize(600, 400)
                .margin(DocumentInsets.of(24)).create()) {
            session.pageFlow(page -> {
                for (int i = 0; i < texts.length; i++) {
                    String text = texts[i];
                    DocumentTextDecoration decoration = decorations[i];
                    page.addParagraph(p -> p.text(text)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).size(14)
                                    .decoration(decoration).build()));
                }
            });
            deck = session.toPptxBytes();
        }
        List<XSLFTextRun> runs = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape text) {
                    for (XSLFTextParagraph paragraph : text.getTextParagraphs()) {
                        runs.addAll(paragraph.getTextRuns());
                    }
                }
            }
        }
        return runs;
    }
}
