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
import com.demcha.examples.support.ExampleVersion;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * A Hebrew invoice — the document type where right-to-left text meets numbers.
 *
 * <p>Almost every line here mixes scripts: a Hebrew description beside a Latin product
 * name, a date, a quantity, a total. That is the case the bidirectional algorithm exists
 * for, and the one a Hebrew-only sample never reaches. The paragraph runs right to left
 * while the digits and the Latin inside it keep running forwards, and nothing in this file
 * positions them — the algorithm does.</p>
 *
 * <p>The line items are built from <b>rows</b> rather than from a table. That is a choice
 * about layout and no longer one about direction — a table cell declares its own with
 * {@code DocumentTableStyle.direction(...)}. Rows stay because each column of an invoice
 * wants its own alignment and its own style, which a row of paragraphs gives directly.</p>
 *
 * <p>Set in {@code FontName.DAVID_LIBRE}, bundled since {@code graph-compose-fonts} 1.1.0.
 * Hebrew has no contextual forms, so the family is held only to covering the block —
 * unlike Arabic, which additionally needs the presentation forms.</p>
 */
public final class HebrewInvoiceExample {

    private static final DocumentColor INK = DocumentColor.rgb(26, 30, 42);
    private static final DocumentColor MUTED = DocumentColor.rgb(124, 130, 144);
    private static final DocumentColor ACCENT = DocumentColor.rgb(15, 118, 110);
    private static final DocumentColor RULE = DocumentColor.rgb(223, 227, 235);
    private static final DocumentColor HEAD = DocumentColor.rgb(240, 253, 250);
    private static final DocumentColor ZEBRA = DocumentColor.rgb(248, 250, 252);

    /** A4 width less the two 46pt side margins; a divider is drawn, so it needs one. */
    private static final double COLUMN = 503;

    /**
     * The isolate initiator and terminator, built from their code points because they are
     * zero-width and a literal would be invisible in this file.
     *
     * <p>A Latin name or a number inside a right-to-left sentence is fine on its own — the
     * algorithm keeps it running forwards. What is not fine is the punctuation touching it:
     * a trailing full stop is neutral, so it belongs to the Hebrew around it and travels to
     * the far end of the line, printing "GraphCompose Ltd" with its period on the left.
     * Wrapping the run in an isolate says "resolve this on its own", and the stop stays
     * where it was typed.</p>
     */
    private static final String LRI = String.valueOf((char) 0x2066);
    private static final String PDI = String.valueOf((char) 0x2069);

    /** Wraps a Latin or numeric run so the punctuation touching it stays put. */
    private static String isolated(String run) {
        return LRI + run + PDI;
    }

    private HebrewInvoiceExample() {
    }

    /**
     * Renders the invoice.
     *
     * @return the written file
     * @throws Exception if the document cannot be written
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("features/text", "hebrew-invoice.pdf");

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
                .margin(48, 46, 48, 46)
                .create();
    }

    /**
     * Builds the document into an open session.
     *
     * @param document session to compose into
     */
    public static void compose(DocumentSession document) {
            document.pageFlow()
                    .name("HebrewInvoice")
                    .spacing(8)

                    .addParagraph(p -> p.text("חשבונית מס").direction(TextDirection.RTL)
                            .textStyle(text(26, INK)))
                    .addParagraph(p -> p.text("מספר " + isolated("2026-0184") + " · תאריך 12 באוגוסט 2026")
                            .direction(TextDirection.RTL).textStyle(text(11, MUTED)))
                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(8, 0)))

                    // Who the invoice is from and to: two right-to-left columns, each
                    // starting at the right edge of its own cell without being told to.
                    .addRow(r -> r
                            .gap(20).weights(1.0, 1.0)
                            .addParagraph(p -> p.text("לכבוד\nחברת אורים בע\"מ\nרחוב הרצל "
                                    + isolated("12") + ", תל אביב")
                                    .direction(TextDirection.RTL).textStyle(text(11, INK)))
                            .addParagraph(p -> p.text("מאת\n" + isolated("GraphCompose Ltd.")
                                    + "\nח.פ. " + isolated("515-880-042"))
                                    .direction(TextDirection.RTL).textStyle(text(11, INK))))

                    .addSpacer(s -> s.height(6))

                    .addRow(headerRow())
                    .addRow(item("רישיון שנתי · GraphCompose Pro", "2", "1,200.00", "2,400.00", false))
                    .addRow(item("תמיכה טכנית · 12 חודשים", "1", "980.00", "980.00", true))
                    .addRow(item("הדרכה · 3 ימי סדנה", "3", "1,450.00", "4,350.00", false))
                    .addRow(item("שדרוג לגרסה " + ExampleVersion.withoutQualifier(), "1", "0.00", "0.00", true))

                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(6, 0)))

                    .addRow(total("סכום ביניים", "7,730.00"))
                    .addRow(total("מע\"מ 18%", "1,391.40"))
                    .addRow(total("סה\"כ לתשלום", "9,121.40"))

                    .addSpacer(s -> s.height(10))
                    .addParagraph(p -> p.text(
                            "התשלום בתוך 30 יום מתאריך החשבונית. חשבונית זו הופקה על ידי GraphCompose "
                            + ExampleVersion.withoutQualifier()
                            + ", והספרות והשמות הלטיניים שבתוכה שומרים על כיוונם גם כשהשורה כולה "
                            + "נקראת מימין לשמאל.")
                            .direction(TextDirection.RTL).textStyle(text(10, MUTED)))

                    .addDivider(d -> d.width(COLUMN).color(RULE).thickness(1)
                            .margin(DocumentInsets.symmetric(8, 0)))
                    .addParagraph(p -> p.text("GraphCompose · features/text/hebrew-invoice")
                            .align(TextAlign.LEFT).textStyle(text(8, MUTED)))
                    .build();
    }

    /**
     * The column headings.
     *
     * <p>Ordered description-first in the source. A right-to-left reader sees the
     * description on the right because each cell's own paragraph starts there — the cells
     * themselves are laid out left to right, which is what keeps the money column in the
     * same place a Latin invoice would put it.</p>
     */
    private static Consumer<com.demcha.compose.document.dsl.RowBuilder> headerRow() {
        return r -> r
                .fillColor(HEAD)
                .padding(DocumentInsets.symmetric(7, 10))
                .gap(10)
                .weights(2.6, 0.7, 1.0, 1.1)
                .addParagraph(p -> p.text("תיאור").direction(TextDirection.RTL)
                        .textStyle(text(10, ACCENT)))
                .addParagraph(p -> p.text("כמות").direction(TextDirection.RTL)
                        .textStyle(text(10, ACCENT)))
                .addParagraph(p -> p.text("מחיר").direction(TextDirection.RTL)
                        .textStyle(text(10, ACCENT)))
                .addParagraph(p -> p.text("סה\"כ").direction(TextDirection.RTL)
                        .textStyle(text(10, ACCENT)));
    }

    private static Consumer<com.demcha.compose.document.dsl.RowBuilder> item(
            String description, String quantity, String price, String sum, boolean shaded) {

        return r -> {
            r.padding(DocumentInsets.symmetric(6, 10)).gap(10).weights(2.6, 0.7, 1.0, 1.1);
            if (shaded) {
                r.fillColor(ZEBRA);
            }
            r.addParagraph(p -> p.text(description).direction(TextDirection.RTL)
                            .textStyle(text(11, INK)))
                    .addParagraph(p -> p.text(quantity).direction(TextDirection.RTL)
                            .textStyle(text(11, INK)))
                    .addParagraph(p -> p.text(price).direction(TextDirection.RTL)
                            .textStyle(text(11, INK)))
                    .addParagraph(p -> p.text(sum).direction(TextDirection.RTL)
                            .textStyle(text(11, INK)));
        };
    }

    private static Consumer<com.demcha.compose.document.dsl.RowBuilder> total(
            String label, String amount) {

        return r -> r
                .padding(DocumentInsets.symmetric(3, 10))
                .gap(10)
                .weights(4.3, 1.1)
                .addParagraph(p -> p.text(label).direction(TextDirection.RTL)
                        .textStyle(text(11, MUTED)))
                .addParagraph(p -> p.text(amount).direction(TextDirection.RTL)
                        .textStyle(text(11, INK)));
    }

    private static DocumentTextStyle text(double size, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(FontName.DAVID_LIBRE).size(size).color(color).build();
    }

    /**
     * Renders the invoice to the example output directory.
     *
     * @param args ignored
     * @throws Exception if the document cannot be written
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
