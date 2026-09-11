package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runnable showcase for letter spacing ({@code @since 2.4.0}).
 *
 * <p>Spaced caps used to be drawn by rewriting the string with a space between
 * every pair of letters. That draws the right picture and ruins the file: the
 * name in a CV came back out of it as {@code "J A N E   D O E"}, so search,
 * copy/paste, a screen reader and an applicant-tracking parser all missed the
 * one field the document is looked up by.</p>
 *
 * <p>{@code DocumentLetterSpacing} moves the pen instead of the text. This
 * example renders the same headline three ways, then reads its own output back
 * and prints what each format says the text is &mdash; which is the whole
 * point, and not something a look at the page can tell you.</p>
 */
public final class LetterSpacingExample {

    private static final String NAME = "Jane O'Doe-Smith 3rd";
    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);

    private LetterSpacingExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "letter-spacing.pdf");
        byte[] pdf;

        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(40, 40, 40, 40)
                .create()) {
            compose(document);
            pdf = document.toPdfBytes();
        }
        Files.write(outputFile, pdf);

        // The picture is on the page; this is the half of the feature that is
        // not. Printed rather than asserted, because an example should show the
        // thing it claims.
        System.out.println("PDF text layer  : \"" + extracted(pdf) + "\"");
        System.out.println("PPTX text       : \"" + pptxText() + "\"");
        System.out.println("DOCX text       : \"" + docxText() + "\"");

        return outputFile;
    }

    private static void compose(DocumentSession document) {
        document.pageFlow()
                .name("LetterSpacingShowcase")
                .spacing(18)
                .addSection("Intro", section -> section
                        .spacing(6)
                        .addParagraph(p -> p
                                .text("Letter spacing")
                                .textStyle(heading())
                                .margin(DocumentInsets.zero()))
                        .addParagraph(p -> p
                                .text("The same name, set three ways. Select any of them and "
                                        + "paste: the clipboard holds the name, not the spacing.")
                                .textStyle(body())
                                .margin(DocumentInsets.zero())))
                .addSection("None", section -> specimen(section,
                        "no tracking", DocumentLetterSpacing.NONE))
                .addSection("Editorial", section -> specimen(section,
                        "ofFontSize(0.18) — what the built-in CV presets use",
                        DocumentLetterSpacing.ofFontSize(0.18)))
                .addSection("Wide", section -> specimen(section,
                        "ofFontSize(0.4) — deliberately extreme",
                        DocumentLetterSpacing.ofFontSize(0.4)))
                .addSection("Tight", section -> specimen(section,
                        "points(-0.4) — negative tracking tightens",
                        DocumentLetterSpacing.points(-0.4)))
                .build();
    }

    private static void specimen(com.demcha.compose.document.dsl.SectionBuilder section,
                                 String caption,
                                 DocumentLetterSpacing spacing) {
        section.spacing(4)
                .addParagraph(p -> p
                        .text(caption)
                        .textStyle(caption())
                        .margin(DocumentInsets.zero()))
                .addParagraph(p -> p
                        .text(NAME.toUpperCase(java.util.Locale.ROOT))
                        .textStyle(display().withLetterSpacing(spacing))
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
    }

    private static String extracted(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            for (String line : text.split("\\R")) {
                if (line.contains("O'DOE")) {
                    return line.trim();
                }
            }
            return "(not found)";
        }
    }

    private static String pptxText() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(40, 40, 40, 40).create()) {
            compose(document);
            byte[] pptx = document.toPptxBytes();
            try (var show = new org.apache.poi.xslf.usermodel.XMLSlideShow(
                    new java.io.ByteArrayInputStream(pptx))) {
                for (var shape : show.getSlides().get(0).getShapes()) {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape
                            && textShape.getText().contains("O'DOE")) {
                        return textShape.getText().trim();
                    }
                }
            }
        }
        return "(not found)";
    }

    private static String docxText() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(40, 40, 40, 40).create()) {
            compose(document);
            byte[] docx = document.export(new DocxSemanticBackend());
            try (var word = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                    new java.io.ByteArrayInputStream(docx))) {
                for (var paragraph : word.getParagraphs()) {
                    if (paragraph.getText().contains("O'DOE")) {
                        return paragraph.getText().trim();
                    }
                }
            }
        }
        return "(not found)";
    }

    private static DocumentTextStyle display() {
        return DocumentTextStyle.builder()
                .fontName(FontName.LATO).size(22)
                .decoration(DocumentTextDecoration.BOLD).color(INK).build();
    }

    private static DocumentTextStyle heading() {
        return DocumentTextStyle.builder()
                .fontName(FontName.LATO).size(26)
                .decoration(DocumentTextDecoration.BOLD).color(BRAND).build();
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder()
                .fontName(FontName.LATO).size(10).color(MUTED).build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.LATO).size(8.5)
                .decoration(DocumentTextDecoration.BOLD).color(MUTED).build();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Wrote " + generate());
    }
}
