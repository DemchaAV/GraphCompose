package com.demcha.examples.features.text;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * One page per script, showing what each bundled family actually renders.
 *
 * <p>A catalogue rather than a test: each card names the family, sets a line of real text
 * in it, and says in one line what makes that script's rendering non-obvious. Two of the
 * five need the engine to do something beyond picking glyphs — Arabic has to be shaped
 * into contextual forms, and both right-to-left scripts have to be reordered — and the
 * card is where that shows.</p>
 *
 * <p>Every family here ships in {@code graph-compose-fonts} 1.1.0. None of them covers
 * another's script: one paragraph is drawn in one family and the engine does not fall back
 * across families, so a line mixing two of these scripts needs a font of your own.</p>
 */
public final class WorldScriptsExample {

    private static final DocumentColor INK = DocumentColor.rgb(28, 32, 44);
    private static final DocumentColor MUTED = DocumentColor.rgb(122, 128, 142);
    private static final DocumentColor ACCENT = DocumentColor.rgb(21, 101, 192);
    private static final DocumentColor CARD = DocumentColor.rgb(247, 249, 252);
    private static final DocumentColor RULE = DocumentColor.rgb(224, 228, 236);

    private WorldScriptsExample() {
    }

    /**
     * Renders the catalogue.
     *
     * @return the written file
     * @throws Exception if the document cannot be written
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "world-scripts.pdf");

        try (DocumentSession document = open(outputFile)) {
            compose(document);
            document.buildPdf();
        }

        return outputFile;
    }

    /**
     * Opens a session with this document's page geometry.
     *
     * <p>Split out so the visual tests hold the same document this writes rather than
     * a copy of it: pass {@code null} for an in-memory session the tests can measure.</p>
     *
     * @param outputFile file to write, or {@code null} for an in-memory session
     * @return an open session
     */
    public static DocumentSession open(Path outputFile) {
        return (outputFile == null ? GraphCompose.document() : GraphCompose.document(outputFile))
                .pageSize(DocumentPageSize.A4)
                .margin(52, 48, 52, 48)
                .create();
    }

    /**
     * Builds the document into an open session.
     *
     * @param document session to compose into
     */
    public static void compose(DocumentSession document) {
            document.pageFlow()
                    .name("WorldScripts")
                    .spacing(9)

                    .addParagraph(p -> p.text("Scripts the bundled fonts cover")
                            .textStyle(title()))
                    .addParagraph(p -> p.text(
                            "Five families ship in graph-compose-fonts 1.1.0. Each carries its own "
                            + "script plus Latin, and none carries another's.")
                            .textStyle(subtitle()))
                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(6, 0)))

                    .addRow(card(
                            "AMIRI", "Arabic",
                            "مرحبا بالعالم",
                            FontName.AMIRI, TextDirection.RTL,
                            "Shaped into contextual forms before measurement, so the letters join."))

                    .addRow(card(
                            "DAVID_LIBRE", "Hebrew",
                            "שלום עולם",
                            FontName.DAVID_LIBRE, TextDirection.RTL,
                            "Reordered for display; the Latin and the digits inside keep running forwards."))

                    .addRow(card(
                            "NOTO_SANS_GEORGIAN", "Georgian",
                            "გამარჯობა",
                            FontName.NOTO_SANS_GEORGIAN, TextDirection.LTR,
                            "Both cases: Mtavruli, the capitals headings use, is a block of its own."))

                    .addRow(card(
                            "NOTO_SANS_ARMENIAN", "Armenian",
                            "Բարև աշխարհ",
                            FontName.NOTO_SANS_ARMENIAN, TextDirection.LTR,
                            "Both cases, and the ech-yiwn ligature that is the Armenian word for \"and\"."))

                    .addRow(card(
                            "GOTHIC_A1", "Korean",
                            "안녕하세요 · Müller",
                            FontName.GOTHIC_A1, TextDirection.LTR,
                            "All 11 172 syllables, and the accented Latin a line may mix in."))

                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(8, 0)))
                    .addParagraph(p -> p.text(
                            "Chinese and Japanese have no bundled family: the official static Noto CJK "
                            + "faces use CFF outlines a PDF cannot embed, and the variable ones draw at "
                            + "their default weight, which is Thin. Register your own with "
                            + "FontFamilyDefinition.")
                            .textStyle(note()))
                    .addParagraph(p -> p.text("GraphCompose · features/text/world-scripts")
                            .align(TextAlign.LEFT).textStyle(footer()))
                    .build();
    }

    /** A4 width less the two 48pt side margins; a divider is drawn, so it needs one. */
    private static final double COLUMN = 499;

    /**
     * One card: the constant on the left, the rendered line and its note on the right.
     *
     * <p>The sample sits in its own paragraph so it can carry its script's direction —
     * a right-to-left line then starts at the right edge of its column without anyone
     * positioning it.</p>
     */
    private static java.util.function.Consumer<com.demcha.compose.document.dsl.RowBuilder> card(
            String constant, String script, String sample, FontName font,
            TextDirection direction, String note) {

        return row -> row
                .fillColor(CARD)
                .cornerRadius(6)
                .padding(DocumentInsets.symmetric(12, 14))
                .gap(16)
                .weights(1.15, 1.85, 1.3)
                .addParagraph(p -> p
                        .rich(rich -> rich
                                .style(script + "\n", DocumentTextStyle.builder()
                                        .fontName(FontName.HELVETICA).size(12).color(INK).build())
                                .style("FontName." + constant, DocumentTextStyle.builder()
                                        .fontName(FontName.JETBRAINS_MONO).size(8).color(ACCENT).build())))
                .addParagraph(p -> p
                        .text(sample)
                        .direction(direction)
                        .textStyle(DocumentTextStyle.builder().fontName(font).size(17).color(INK).build()))
                .addParagraph(p -> p.text(note).textStyle(note()));
    }

    private static DocumentTextStyle title() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(22).color(INK).build();
    }

    private static DocumentTextStyle subtitle() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).color(MUTED).build();
    }

    private static DocumentTextStyle note() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(9).color(MUTED).build();
    }

    private static DocumentTextStyle footer() {
        return DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(8).color(MUTED).build();
    }

    /**
     * Renders the catalogue to the example output directory.
     *
     * @param args ignored
     * @throws Exception if the document cannot be written
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
