package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code hangingIndent} is fixed-layout geometry, and the semantic DOCX export
 * does not lay text out — so it exports a list exactly the same way whether the
 * flag is set or not.
 *
 * <p>That is a decision rather than an omission, and it was made against
 * measurements. Word places content at absolute indents and has no way to be told
 * "start the text one marker width plus a gap from here"; every mechanism that
 * looks like it would (a hanging indent, a hanging indent with a tab stop, real
 * Word numbering) positions content absolutely, so the distance left beside the
 * marker is always the column minus the marker's own width — a number only Word
 * knows. Reproducing the geometry would mean measuring the marker, and the
 * semantic backend has no font runtime to measure with, by design: its only
 * dependencies are the core model and POI.</p>
 *
 * <p>The approximations were built and rendered through Word before being
 * rejected. A reserved-column approximation renders a gap that is not the
 * configured one — a 0pt gap came out as 5.72pt, a 4pt gap as 9.68pt — and a
 * marker wider than the column misaligns outright, its tab overshooting to Word's
 * default half-inch grid while the wrapped lines stay at the paragraph indent.
 * Shipping that would mean {@code markerGap(8)} rendering as something other
 * than 8.</p>
 *
 * <p>So what this test pins is that the DOCX output is <em>unchanged</em>, and
 * that nothing is lost: same paragraphs, same text, same nesting, no stray
 * indentation properties. If native DOCX list geometry is built later, this test
 * is the one that has to be deliberately rewritten.</p>
 */
class DocxHangingIndentIsIgnoredTest {

    @Test
    void aFlatListExportsIdenticallyWithAndWithoutHangingIndent() throws Exception {
        Consumer<ListBuilder> shape = list -> list.name("Flat").bullet()
                .items("Alpha", "Beta", "Gamma");

        assertThat(listTexts(shape.andThen(l -> l.hangingIndent(true).markerGap(16))))
                .isEqualTo(listTexts(shape))
                .containsExactly("• Alpha", "• Beta", "• Gamma");
    }

    @Test
    void aMultiCharacterMarkerExportsIdenticallyAndKeepsItsMarkerInTheText() throws Exception {
        Consumer<ListBuilder> shape = list -> list.name("Wide").marker("=>").items("Alpha");

        assertThat(listTexts(shape.andThen(l -> l.hangingIndent(true).markerGap(8))))
                .isEqualTo(listTexts(shape))
                .containsExactly("=> Alpha");
    }

    @Test
    void theGapIsNotRepresentedAtAllSoEveryValueExportsTheSame() throws Exception {
        // If the gap ever leaked into DOCX as spaces or indentation, these would
        // stop agreeing — which is the failure this test exists to catch.
        List<String> zero = listTexts(l -> l.bullet().hangingIndent(true).markerGap(0).items("Alpha"));
        for (double gap : List.of(4.0, 8.0, 16.0)) {
            assertThat(listTexts(l -> l.bullet().hangingIndent(true).markerGap(gap).items("Alpha")))
                    .as("gap %s", gap)
                    .isEqualTo(zero);
        }
        assertThat(zero).containsExactly("• Alpha");
    }

    @Test
    void aLongItemStaysOneParagraphWithEveryCharacterOfItsText() throws Exception {
        String text = "Long item text that the PDF backend wraps across several visual lines "
                      + "and that Word wraps for itself, so no character of it may be lost here.";

        List<String> texts = listTexts(l -> l.bullet().hangingIndent(true).items(text));
        assertThat(texts).hasSize(1);
        assertThat(texts.get(0)).isEqualTo("• " + text);
    }

    @Test
    void nestedListsKeepTheirCascadeAndTheirTwoAsciiSpacesPerLevel() throws Exception {
        Consumer<ListBuilder> shape = list -> list.name("Outline")
                .addItem("alpha", l1 -> l1.addItem("beta", l2 -> l2.addItem("gamma")));

        assertThat(listTexts(shape.andThen(l -> l.hangingIndent(true).markerGap(12))))
                .isEqualTo(listTexts(shape))
                .containsExactly("• alpha", "  ◦ beta", "    ▪ gamma");
    }

    @Test
    void aMarkerlessListAndAMarkerOnlyRowBothSurviveUnchanged() throws Exception {
        assertThat(listTexts(l -> l.noMarker().hangingIndent(true).markerGap(16).items("Alpha")))
                .containsExactly("Alpha");

        // The flat path drops a blank item whatever its marker — that is the
        // legacy rule, and opting in does not change the DOCX side of it.
        assertThat(listTexts(l -> l.bullet().hangingIndent(true).items("Alpha", "   ", "Beta")))
                .containsExactly("• Alpha", "• Beta");

        // A nested parent with an empty label keeps its marker row and children.
        assertThat(listTexts(l -> l.hangingIndent(true)
                .addItem("", c -> c.addItem("Child"))))
                .containsExactly("• ", "  ◦ Child");
    }

    @Test
    void noIndentationOrNumberingPropertyIsWrittenEitherWay() throws Exception {
        try (XWPFDocument document = export(flow -> flow.addList(list -> list
                .name("Flat").bullet().hangingIndent(true).markerGap(16)
                .items("Alpha", "Beta")))) {

            List<XWPFParagraph> paragraphs = document.getParagraphs().stream()
                    .filter(p -> !p.getText().isBlank())
                    .toList();
            assertThat(paragraphs).hasSize(2);
            for (XWPFParagraph paragraph : paragraphs) {
                CTPPr properties = paragraph.getCTP().getPPr();
                assertThat(properties == null || !properties.isSetInd())
                        .as("no w:ind — the geometry is not approximated here")
                        .isTrue();
                assertThat(properties == null || !properties.isSetNumPr())
                        .as("no w:numPr")
                        .isTrue();
                assertThat(properties == null || !properties.isSetTabs())
                        .as("no tab stops")
                        .isTrue();
                assertThat(paragraph.getRuns()).as("one run, as before").hasSize(1);
            }
            assertThat(document.getNumbering()).as("no numbering.xml").isNull();
        }
    }

    @Test
    void aMarkerGapNeverBecomesSpacesInTheRunText() throws Exception {
        // The specific failure worth naming: a gap smuggled in as padding would
        // still "look right" in a viewer and be wrong in the file.
        List<String> texts = listTexts(l -> l.bullet().hangingIndent(true).markerGap(16).items("Alpha"));
        assertThat(texts.get(0))
                .isEqualTo("• Alpha")
                .as("no padding run, no non-breaking spaces, no tab")
                .doesNotContain("  ")
                .doesNotContain(" ")
                .doesNotContain("\t");
    }

    // ------------------------------------------------------------------

    private static List<String> listTexts(Consumer<ListBuilder> spec) throws Exception {
        try (XWPFDocument document = export(flow -> flow.addList(spec))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> !text.isEmpty())
                    .toList();
        }
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> author) throws Exception {
        byte[] docxBytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .create()) {
            PageFlowBuilder flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docxBytes = session.export(new DocxSemanticBackend());
        }
        return new XWPFDocument(new ByteArrayInputStream(docxBytes));
    }
}
