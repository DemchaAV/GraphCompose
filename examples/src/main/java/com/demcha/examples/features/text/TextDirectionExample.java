package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for paragraph writing direction ({@code @since 2.2.0}).
 *
 * <p>Each row pairs the rendered result with the call that produced it, so the PDF reads
 * like a quick reference. The interesting rows are the ones where direction and
 * alignment disagree, and the mixed line where a Latin word and a number keep running
 * forwards inside right-to-left text.</p>
 *
 * <p>Hebrew is set in {@code FontName.DAVID_LIBRE} and Arabic in {@code FontName.AMIRI},
 * both bundled since {@code graph-compose-fonts} 1.1.0. No bundled family covers both
 * scripts, so a paragraph mixing them needs a font registered through
 * {@code FontFamilyDefinition}. Arabic is shaped by the engine into its contextual
 * forms, which is why the Arabic family has to carry the presentation forms.</p>
 */
public final class TextDirectionExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(122, 126, 138);

    private static final String HEBREW = "שלום עולם";
    private static final String ARABIC = "مرحبا بالعالم";

    private TextDirectionExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "text-direction.pdf");

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(40, 40, 40, 40)
                .create()) {

            document.pageFlow()
                    .name("TextDirectionShowcase")
                    .spacing(10)
                    .addParagraph(p -> p.text("Writing direction").textStyle(title()))

                    .addParagraph(p -> p.text(".text(hebrew)").textStyle(caption()))
                    .addParagraph(p -> p.text(HEBREW).textStyle(hebrew()))

                    .addParagraph(p -> p.text(".direction(RTL)").textStyle(caption()))
                    .addParagraph(p -> p
                            .text(HEBREW)
                            .direction(TextDirection.RTL)
                            .textStyle(hebrew()))

                    .addParagraph(p -> p.text(".direction(AUTO)  - read off the first strong character")
                            .textStyle(caption()))
                    .addParagraph(p -> p
                            .text(HEBREW)
                            .direction(TextDirection.AUTO)
                            .textStyle(hebrew()))

                    .addParagraph(p -> p.text(".direction(RTL).align(LEFT)  - an explicit alignment wins")
                            .textStyle(caption()))
                    .addParagraph(p -> p
                            .text(HEBREW)
                            .direction(TextDirection.RTL)
                            .align(TextAlign.LEFT)
                            .textStyle(hebrew()))

                    .addParagraph(p -> p.text(".direction(RTL)  - Latin and digits keep running forwards")
                            .textStyle(caption()))
                    .addParagraph(p -> p
                            .text(HEBREW + " GraphCompose 2026 " + HEBREW)
                            .direction(TextDirection.RTL)
                            .textStyle(hebrew()))

                    .addParagraph(p -> p.text(".direction(RTL)  - Arabic, joined by the engine")
                            .textStyle(caption()))
                    .addParagraph(p -> p
                            .text(ARABIC)
                            .direction(TextDirection.RTL)
                            .textStyle(arabic()))

                    .build();

            document.buildPdf();
        }

        return outputFile;
    }

    private static DocumentTextStyle title() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(18).color(INK).build();
    }

    private static DocumentTextStyle caption() {
        return DocumentTextStyle.builder().fontName(FontName.COURIER).size(9).color(MUTED).build();
    }

    private static DocumentTextStyle hebrew() {
        return DocumentTextStyle.builder().fontName(FontName.DAVID_LIBRE).size(18).color(INK).build();
    }

    private static DocumentTextStyle arabic() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(18).color(INK).build();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
