package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.function.Consumer;

/**
 * A document of the DOCX fidelity corpus built as a CV sets its dated entries: a title, a spacer
 * that grows, and a date the page sets hard right — in the flow, and in a stack in each column of a
 * two-column row, the way a row is set beneath a row's cell.
 *
 * <p>It is drawn from a CV template made outside this repository, whose titles and dates are set
 * at 9.75pt. Word states a size to the half
 * point and set them at 10, a little wider than the page's columns, which are exactly as wide as
 * their text: in Word 16 the dates lost their last letters at their cells' edges, and so did a
 * title running up to its column's spacer, while LibreOffice 24.2 drew them whole. That template
 * sets a face this repository does not ship; this document sets Lato, which the export embeds, at
 * the same sizes — the cut is the half point's, not the face's.</p>
 *
 * <p>Its education row's title and date fill their column, so its spacer has no room to give:
 * Word still cuts them there, and the report names it, which the gate holds as well.</p>
 */
final class TitleDateRows {

    private static final DocumentColor NAVY = DocumentColor.rgb(12, 26, 48);
    private static final DocumentColor BODY = DocumentColor.rgb(60, 68, 88);
    private static final double BODY_SIZE = 9.75;
    private static final double ENTRY_TITLE_SIZE = 11.5;
    private static final double HEADING_SIZE = 11;

    private TitleDateRows() {
    }

    static List<DocxCorpusDocument> documents() {
        return List.of(new DocxCorpusDocument("synthetic", "title_date_rows", 40, TitleDateRows::compose));
    }

    private static void compose(DocumentSession document) {
        document.pageFlow(page -> {
            page.name("TitleDateRows").spacing(6);
            page.addParagraph("WORK EXPERIENCE", style(HEADING_SIZE, NAVY, true));
            String[][] jobs = {
                    {"Job Title", "Jan 2022 – Present"},
                    {"Job Title", "Jun 2019 – Dec 2021"},
                    {"Job Title", "Jan 2017 – May 2019"}
            };
            for (int index = 0; index < jobs.length; index++) {
                String[] job = jobs[index];
                page.addRow("Job" + (index + 1), row -> titleDate(row,
                        title -> title.inlineText(job[0], style(ENTRY_TITLE_SIZE, NAVY, true)), job[1]));
                page.addParagraph("Company Name  |  City, State, Country", style(BODY_SIZE, BODY, false));
            }
            page.addRow("EducationCertifications", row -> {
                row.spacing(20).weights(1.15, 1);
                row.addSection("Education", column -> {
                    column.spacing(4).addParagraph("EDUCATION", style(HEADING_SIZE, NAVY, true));
                    beneathARowCell(column, "Degree", entry -> titleDate(entry, title -> title
                            .inlineText("Degree Name", style(BODY_SIZE, NAVY, true))
                            .inlineText(" (e.g., Bachelor of Science)", style(BODY_SIZE, BODY, false)),
                            "Sep 2013 – May 2017"));
                    column.addParagraph("University Name  |  City, State, Country", style(BODY_SIZE, BODY, false));
                });
                row.addSection("Certifications", column -> {
                    column.spacing(4).addParagraph("CERTIFICATIONS", style(HEADING_SIZE, NAVY, true));
                    for (int index = 1; index <= 2; index++) {
                        beneathARowCell(column, "Certification" + index, entry -> titleDate(entry, title -> title
                                .inlineText("Certification Name", style(BODY_SIZE, NAVY, true))
                                .inlineText("  |  Issuing Organization", style(BODY_SIZE, BODY, false)), "Year"));
                    }
                });
            });
            page.addParagraph("PROJECTS", style(HEADING_SIZE, NAVY, true));
            String[] years = {"2023 – 2024", "2021 – 2022"};
            for (int index = 0; index < years.length; index++) {
                String year = years[index];
                page.addRow("Project" + (index + 1), row -> titleDate(row,
                        title -> title.inlineText("Project Name", style(BODY_SIZE, NAVY, true)), year));
                page.addParagraph("Short description of the project, your role and the impact.",
                        style(BODY_SIZE, BODY, false));
            }
        });
    }

    /** A title on the left and its date hard right, bottom-aligned, a spacer that grows between them. */
    private static void titleDate(RowBuilder row, Consumer<ParagraphBuilder> title, String date) {
        row.spacing(0).verticalAlign(RowVerticalAlign.BOTTOM);
        row.addParagraph(paragraph -> title.accept(paragraph.name("EntryTitle")));
        row.flexSpacer();
        row.addParagraph(paragraph -> paragraph.name("EntryDate").text(date)
                .textStyle(style(BODY_SIZE, BODY, false)));
    }

    /** A row beneath a row's cell, which lays out only in a stack's layer. */
    private static void beneathARowCell(SectionBuilder column, String name, Consumer<RowBuilder> spec) {
        SectionBuilder layer = new SectionBuilder().name(name + "Layer").spacing(0);
        layer.addRow(name, spec);
        column.addLayerStack(stack -> stack.name(name + "Stack").layer(layer.build(), LayerAlign.TOP_LEFT, 0));
    }

    private static DocumentTextStyle style(double size, DocumentColor color, boolean bold) {
        return DocumentTextStyle.builder()
                .fontName(FontName.LATO)
                .decoration(bold ? DocumentTextDecoration.BOLD : DocumentTextDecoration.DEFAULT)
                .size(size)
                .color(color)
                .build();
    }
}
