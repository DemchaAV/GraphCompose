package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.output.DocumentWatermarkPosition;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the canonical, backend-neutral PDF chrome surface
 * on {@link DocumentSession}: metadata, watermark, header / footer, and
 * paragraph-level bookmark options that materialise as PDF outline
 * entries.
 *
 * <p>The rendered PDF carries author/title/keywords metadata in the
 * information dictionary, a 45-degree "DRAFT" watermark behind every
 * page, a bordered header + footer with page-number tokens, and three
 * outline bookmarks that a PDF reader exposes as a navigable side
 * panel.</p>
 *
 * <p>Document protection (passwords, permissions) is configured the
 * same way through {@link DocumentSession#protect}; this example skips
 * it deliberately so the rendered PDF stays readable without a password
 * prompt. Uncomment the sample block at the bottom of {@link #generate()}
 * to see the canonical protection surface.</p>
 */
public final class PdfChromeExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(112, 116, 128);
    private static final DocumentColor BRAND = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 153, 76);
    private static final DocumentColor WATERMARK = DocumentColor.rgb(196, 153, 76);

    private PdfChromeExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("pdf-chrome.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(48, 34, 48, 34)
                .create()) {

            // 1. Document-level metadata — backend-neutral.
            document.metadata(DocumentMetadata.builder()
                    .title("GraphCompose chrome showcase")
                    .author("Artem Demchyshyn")
                    .subject("Demonstrates metadata, watermark, header/footer, and bookmarks")
                    .keywords("graphcompose, pdf, metadata, watermark, header, footer, bookmark")
                    .creator("GraphCompose Examples")
                    .producer("GraphCompose")
                    .build());

            // 2. Page-wide watermark that paints behind document content.
            document.watermark(DocumentWatermark.builder()
                    .text("DRAFT")
                    .fontSize(96f)
                    .rotation(45f)
                    .color(WATERMARK)
                    .opacity(0.12f)
                    .layer(DocumentWatermarkLayer.BEHIND_CONTENT)
                    .position(DocumentWatermarkPosition.CENTER)
                    .build());

            // 3. Header / footer chrome — backend-neutral with the standard
            //    {page} / {pages} / {date} placeholder tokens.
            document.header(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.HEADER)
                    .leftText("GraphCompose · Chrome showcase")
                    .rightText("{date}")
                    .fontSize(9f)
                    .textColor(MUTED)
                    .showSeparator(true)
                    .separatorColor(THEME.palette().rule())
                    .separatorThickness(0.5f)
                    .build());

            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .centerText("Page {page} of {pages}")
                    .fontSize(9f)
                    .textColor(MUTED)
                    .showSeparator(true)
                    .separatorColor(THEME.palette().rule())
                    .separatorThickness(0.5f)
                    .build());

            // 4. Body content — top-level paragraphs use bookmark options
            //    that materialise as PDF outline entries.
            document.pageFlow()
                    .name("ChromeShowcase")
                    .spacing(14)
                    .addSection("Hero", section -> section
                            .softPanel(THEME.palette().surfaceMuted(), 10, 16)
                            .accentLeft(ACCENT, 4)
                            .spacing(6)
                            .addParagraph(p -> p
                                    .text("PDF chrome showcase")
                                    .textStyle(THEME.text().h1())
                                    .bookmark(new DocumentBookmarkOptions("Chrome showcase", 0))
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("All four canonical chrome surfaces — ")
                                    .bold("metadata")
                                    .plain(", ")
                                    .bold("watermark")
                                    .plain(", ")
                                    .bold("header / footer")
                                    .plain(", and ")
                                    .bold("bookmarks")
                                    .plain(" — wired through the same ")
                                    .accent("DocumentSession", BRAND)
                                    .plain(" mutators every backend honours.")))

                    .addSection("Metadata", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(com.demcha.compose.document.style.DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(BRAND, 3)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("Document metadata")
                                    .textStyle(panelHeadline())
                                    .bookmark(new DocumentBookmarkOptions("Metadata", 1))
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Open this PDF in a viewer that shows ")
                                    .bold("Document Properties")
                                    .plain(" — title, author, subject, keywords, creator, and producer all flow from the canonical ")
                                    .accent("DocumentMetadata", BRAND)
                                    .plain(" value.")))

                    .addSection("Watermark", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(com.demcha.compose.document.style.DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(BRAND, 3)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("Watermark")
                                    .textStyle(panelHeadline())
                                    .bookmark(new DocumentBookmarkOptions("Watermark", 1))
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("The diagonal ")
                                    .accent("DRAFT", WATERMARK)
                                    .plain(" watermark behind this paragraph is text-based — switch to image mode by configuring ")
                                    .bold("imagePath")
                                    .plain(" or ")
                                    .bold("imageBytes")
                                    .plain(" on the watermark builder. ")
                                    .bold("BEHIND_CONTENT")
                                    .plain(" sits behind the body; ")
                                    .bold("OVER_CONTENT")
                                    .plain(" paints on top.")))

                    .addSection("HeadersAndFooters", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(com.demcha.compose.document.style.DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(BRAND, 3)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("Header & footer")
                                    .textStyle(panelHeadline())
                                    .bookmark(new DocumentBookmarkOptions("Header and footer", 1))
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Top of every page carries the document title and the rendered ")
                                    .bold("{date}")
                                    .plain(" token; bottom carries ")
                                    .bold("Page {page} of {pages}")
                                    .plain(". Both zones share the same ")
                                    .accent("DocumentHeaderFooter", BRAND)
                                    .plain(" value type and accept independent fontSize / textColor / separator settings.")))

                    .addSection("Bookmarks", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(com.demcha.compose.document.style.DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(BRAND, 3)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("Bookmarks")
                                    .textStyle(panelHeadline())
                                    .bookmark(new DocumentBookmarkOptions("Bookmarks", 1))
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("Every ")
                                    .bold("ParagraphBuilder.bookmark(...)")
                                    .plain(" call materialises a PDF outline entry. Open the rendered PDF in a viewer with an outline panel to see ")
                                    .accent("Chrome showcase", BRAND)
                                    .plain(" expand into Metadata, Watermark, Header & Footer, and Bookmarks sub-entries.")))

                    .addSection("Protection", section -> section
                            .softPanel(DocumentColor.WHITE, 6, 12)
                            .stroke(com.demcha.compose.document.style.DocumentStroke.of(THEME.palette().rule(), 0.5))
                            .accentLeft(MUTED, 3)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("Protection (not enabled in this PDF)")
                                    .textStyle(panelHeadline())
                                    .bookmark(new DocumentBookmarkOptions("Protection", 1))
                                    .margin(DocumentInsets.zero()))
                            .addRich(rich -> rich
                                    .plain("To encrypt the PDF, configure ")
                                    .accent("DocumentSession.protect(DocumentProtection.builder()...)", BRAND)
                                    .plain(" with userPassword / ownerPassword / canPrint / canCopyContent / canModify / canFillForms / keyLength. This example skips the protect call so the rendered PDF stays readable without a password.")))

                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addParagraph(p -> p
                                    .text("All five surfaces are renderer-neutral — DOCX honours metadata, ignores watermark/header/footer/bookmarks per Apache POI capability matrix.")
                                    .textStyle(caption())
                                    .lineSpacing(1.4)
                                    .margin(DocumentInsets.zero())))
                    .build();

            // 5. Sample protection wiring — uncomment to encrypt the PDF.
            //
            // document.protect(DocumentProtection.builder()
            //         .userPassword("preview")
            //         .ownerPassword("change-me")
            //         .canPrint(true)
            //         .canCopyContent(false)
            //         .canModify(false)
            //         .canFillForms(true)
            //         .keyLength(128)
            //         .build());

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentTextStyle panelHeadline() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(11)
                .color(INK)
                .build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(8.6)
                .color(MUTED)
                .build();
    }
}
