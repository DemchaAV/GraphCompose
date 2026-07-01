package com.demcha.examples.features.navigation;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.examples.support.theme.BusinessTheme;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for in-PDF navigation (v1.9): named {@code anchor(...)}
 * destinations plus internal {@code linkTo(...)} links that jump to them.
 *
 * <p>Demonstrates the two patterns the feature unlocks: a clickable table of
 * contents whose entries jump to section anchors, and a bidirectional footnote
 * (body reference jumps to the note; the note's marker jumps back to the body).
 * Open the rendered PDF in a viewer and click the blue links to navigate.</p>
 */
public final class InPdfNavigationExample {
    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor LINK = DocumentColor.rgb(20, 90, 170);
    private static final DocumentColor PANEL = DocumentColor.rgb(244, 247, 252);

    private static final DocumentTextStyle LINK_STYLE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA)
            .size(10.5)
            .color(LINK)
            .build();

    private InPdfNavigationExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/navigation", "in-pdf-navigation.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .pageBackground(THEME.pageBackground())
                .margin(40, 40, 40, 40)
                .create()) {

            document.pageFlow()
                    .name("InPdfNavigation")
                    .spacing(14)
                    .addParagraph(p -> p
                            .text("In-PDF navigation")
                            .textStyle(THEME.text().h1())
                            .margin(DocumentInsets.zero()))

                    // Clickable table of contents — each entry is an internal link
                    // to a section anchor declared further down the page.
                    .addSection("Contents", section -> section
                            .softPanel(PANEL, 8, 14)
                            .spacing(5)
                            .addParagraph(p -> p
                                    .text("Contents")
                                    .textStyle(THEME.text().h3())
                                    .margin(DocumentInsets.zero()))
                            .addRich(RichText.text("1. ").linkTo("Overview", LINK_STYLE, "overview"))
                            .addRich(RichText.text("2. ").linkTo("Details", LINK_STYLE, "details"))
                            .addRich(RichText.text("3. ").linkTo("Notes", LINK_STYLE, "notes")))

                    .addSection("Overview", section -> section
                            .anchor("overview")
                            .spacing(4)
                            .addParagraph(p -> p.text("Overview").textStyle(THEME.text().h2()))
                            .addParagraph(p -> p
                                    .anchor("fnref-1")
                                    .inlineText("GraphCompose now renders in-document navigation",
                                            body())
                                    .inlineLinkTo(" [1]", "fn-1")
                                    .inlineText(" entirely with native PDF go-to actions.", body())))

                    .addSection("Details", section -> section
                            .anchor("details")
                            .spacing(4)
                            .addParagraph(p -> p.text("Details").textStyle(THEME.text().h2()))
                            .addParagraph(p -> p
                                    .text("Anchors resolve in a deferred pass, so a link may target an "
                                          + "anchor that appears later in the document (a forward reference).")
                                    .textStyle(body()))
                            .addRich(RichText.text("Jump back to the ")
                                    .linkTo("contents", LINK_STYLE, "overview")
                                    .plain("."))
                            .addRich(RichText.text("Inline graphics navigate too — ")
                                    .shapeLinkTo(ShapeOutline.circle(7), LINK, "notes")
                                    .plain(" click the dot to jump to the notes.")))

                    .addSection("Notes", section -> section
                            .anchor("notes")
                            .softPanel(PANEL, 8, 14)
                            .spacing(4)
                            .addParagraph(p -> p.text("Notes").textStyle(THEME.text().h3()))
                            .addParagraph(p -> p
                                    .anchor("fn-1")
                                    .inlineLinkTo("[1]", "fnref-1")
                                    .inlineText(" Click the marker to jump back to the citation in the "
                                                + "overview.", body())))

                    .addSection("Footer", section -> section
                            .accentTop(THEME.palette().rule(), 0.6)
                            .padding(new DocumentInsets(8, 0, 0, 0))
                            .addRich(RichText.text("External links still work: ")
                                    .link("project home",
                                            new DocumentLinkOptions(
                                                    "https://github.com/DemchaAV/GraphCompose"))))
                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }

    private static DocumentTextStyle body() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(10.5).color(INK).build();
    }
}
