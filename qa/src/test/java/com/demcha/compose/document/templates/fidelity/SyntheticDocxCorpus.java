package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.chart.BarGrouping;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSize;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.LegendPosition;
import com.demcha.compose.document.chart.LineInterpolation;
import com.demcha.compose.document.chart.PointMarker;
import com.demcha.compose.document.chart.SliceLabelMode;
import com.demcha.compose.document.chart.ValueLabelMode;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static com.demcha.compose.document.style.DocumentRowColumn.fixed;
import static com.demcha.compose.document.style.DocumentRowColumn.weight;

/**
 * Documents of the DOCX fidelity corpus built for a shape no template preset has on its own
 * fixture, so that an editor setting one off the page fails the gate as a preset would.
 *
 * <p>Three set a way the page breaks inside a panel or a column: a page break among a panel's
 * children and inside a panel nested in it, page breaks the page sets as boxes of no height in a
 * row's column and in a stack's layer, and a card whose table the layout runs over three pages,
 * with other margins from the second. Three others set a shape container holding text in the
 * body: a box narrower than the page, a box clipped to its outline, and a card. One more lays an
 * icon over a card's top right corner, in a layer stack. Their text is set in a face the export
 * embeds, so both editors break at the page's words the lines they set at the page's width.</p>
 *
 * <p>The export writes the three boxes as panels, their text inside the outline: Word sets the
 * narrow box's note and the card's text at the page's words, and the paragraph under each box
 * where the page sets it; of the clipped box, whose clip is written as the panel's exact height,
 * both editors show the lines the page shows. The gate reads lines of text only: whether the
 * text stays inside the outline, and whether the card grows when text is added to it in an
 * editor, it does not see.</p>
 *
 * <p>The card under the icon is held where the page sets it, the icon's own place aside: see
 * {@link #iconOverCard}.</p>
 *
 * <p>Eleven more compose the way a user builds a document: a kept panel of icon rows the layout
 * moves to the next page, nested rows, a banner and initials in a circle, kept blocks and a
 * repeated table header over pages, a turned picture and shape, a column painted down every page,
 * a header holding a logo, margins that change from page to page with a footer reading the page's
 * number of the pages, paragraphs set right to left, a table of contents with links, and long
 * free text. Where the export writes a form in another, or drops part of it, its report names it;
 * the gate holds where each editor sets the lines round it. No two paragraphs of a document open
 * on the same two sentences ({@link #prose}). Two of these hold a later page's lines a line or two
 * low in both editors: see {@link #sidebarOverPages} and {@link #longFreeText}.</p>
 */
final class SyntheticDocxCorpus {

    private static final DocumentTextStyle TEXT = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
    private static final DocumentTextStyle HEADING = DocumentTextStyle.builder().fontName(FontName.LATO).size(12)
            .decoration(DocumentTextDecoration.BOLD).build();
    private static final DocumentTableStyle CELLS = DocumentTableStyle.builder().textStyle(TEXT).build();
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INNER = DocumentColor.rgb(224, 233, 244);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentInsets MARGIN = DocumentInsets.of(36);
    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<circle cx='5' cy='5' r='5' fill='#1a5694'/></svg>");
    private static final DocumentColor DARK = DocumentColor.rgb(30, 40, 60);
    private static final DocumentTextStyle WHITE = DocumentTextStyle.builder().fontName(FontName.LATO).size(14)
            .decoration(DocumentTextDecoration.BOLD).color(DocumentColor.rgb(255, 255, 255)).build();
    private static final DocumentTextStyle HEBREW = DocumentTextStyle.builder().fontName(FontName.DAVID_LIBRE).size(11).build();
    private static final DocumentTextStyle ARABIC = DocumentTextStyle.builder().fontName(FontName.AMIRI).size(11).build();
    private static final String HEBREW_PROSE = "צוותי המסירה מדווחים על ההתקדמות מדי שבוע ומעבירים חסמים לוועדת התוכנית, "
                                               + "הנפגשת אחת לשבועיים כדי לבחון סיכונים, תקציב ושינויים בהיקף העבודה.";
    private static final String ARABIC_PROSE = "تقدم فرق التسليم تقارير أسبوعية عن التقدم وترفع العوائق إلى مجلس البرنامج، "
                                               + "الذي يجتمع كل أسبوعين لمراجعة المخاطر والميزانية والتغييرات في النطاق.";
    private static final byte[] PICTURE = picture();
    /** Sentences {@link #prose} builds paragraphs of. */
    private static final List<String> SENTENCES = List.of(
            "Delivery teams report progress weekly and escalate blockers through the programme board.",
            "The board meets every second Tuesday to review risks, budget and changes of scope.",
            "Each workstream keeps a short log of decisions, owners and the dates they were agreed.",
            "Suppliers send invoices at the end of each month, quoting the order they belong to.",
            "Finance checks every invoice against the budget line before it is approved for payment.",
            "A change of scope larger than a week of effort needs the sponsor's written approval.",
            "Risks are scored by likelihood and impact, and the five highest are reviewed first.",
            "New starters are given a buddy, a laptop and a reading list on their first morning.",
            "The release calendar is frozen two weeks before each quarterly planning session.",
            "Customer feedback is collected after every release and summarised for the next review.",
            "Office moves are planned with facilities a full quarter before the lease runs out.",
            "Training budgets are shared across teams, and unused days carry over for one year.",
            "Security reviews run before any service is exposed to traffic from outside the company.",
            "Status reports use three colours, and amber needs a recovery plan within five days.",
            "Archived projects keep their documents for seven years, then are destroyed securely.",
            "Holiday requests are approved by the team lead and shown on the shared calendar.");

    private SyntheticDocxCorpus() {
    }

    /** The documents of the {@code synthetic} family, one for each shape. */
    static List<DocxCorpusDocument> documents() {
        return List.of(
                synthetic("break_in_panel", SyntheticDocxCorpus::breakInAPanel),
                synthetic("break_in_column", SyntheticDocxCorpus::breakInAColumn),
                synthetic("card_over_pages", SyntheticDocxCorpus::cardOverPages),
                synthetic("narrow_container", SyntheticDocxCorpus::narrowContainer),
                synthetic("clipped_container", SyntheticDocxCorpus::clippedContainer),
                synthetic("rounded_card", SyntheticDocxCorpus::roundedCard),
                synthetic("icon_over_card", SyntheticDocxCorpus::iconOverCard),
                synthetic("kept_icon_panel", SyntheticDocxCorpus::keptIconPanel),
                synthetic("nested_rows", SyntheticDocxCorpus::nestedRows),
                synthetic("banner_and_initials", SyntheticDocxCorpus::bannerAndInitials),
                synthetic("kept_blocks_over_pages", SyntheticDocxCorpus::keptBlocksOverPages),
                synthetic("rotated_image_and_shape", SyntheticDocxCorpus::rotatedImageAndShape),
                synthetic("sidebar_over_pages", SyntheticDocxCorpus::sidebarOverPages),
                synthetic("header_logo_and_filled_row", SyntheticDocxCorpus::headerLogoAndFilledRow),
                synthetic("sections_and_fields", SyntheticDocxCorpus::sectionsAndFields),
                synthetic("rtl_paragraphs", SyntheticDocxCorpus::rtlParagraphs),
                synthetic("links_and_toc", SyntheticDocxCorpus::linksAndToc),
                synthetic("long_free_text", SyntheticDocxCorpus::longFreeText),
                synthetic("bar_and_line_charts", SyntheticDocxCorpus::barAndLineCharts),
                synthetic("pie_and_donut_charts", SyntheticDocxCorpus::pieAndDonutCharts));
    }

    /**
     * Five charts between paragraphs over three pages: grouped bars labelled outside, horizontal
     * stacked bars, a line with point markers and labels, a smoothed line over an area, and a
     * monotone line. A chart does not split, so the layout moves one that does not fit whole onto
     * the next page; the paragraphs round them are where the gate sees whether Word and
     * LibreOffice give each chart the page's box. The export writes each as a Word chart.
     */
    private static void barAndLineCharts(DocumentSession session) {
        session.pageSize(400, 560).margin(MARGIN);
        ChartData quarters = ChartData.builder().categories("Q1", "Q2", "Q3", "Q4")
                .series("Sales", 120, 180, 150, 210).series("Costs", 90, 110, 130, 120).build();
        ChartData teams = ChartData.builder().categories("Alpha", "Beta", "Gamma")
                .series("Build", 3, 5, 2).series("Test", 2, 1, 4).series("Ship", 1, 2, 2).build();
        ChartData visits = ChartData.builder().categories("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                .series("Visits", 10, 12, 40, 41, 42, 15).build();
        ChartStyle markers = ChartStyle.builder().pointMarker(PointMarker.circle(6)).build();
        session.pageFlow(page -> {
            page.spacing(8);
            page.addParagraph(p -> p.text("Quarterly figures").textStyle(HEADING));
            page.addParagraph(p -> text(p, prose(31)));
            page.add(new ChartNode(ChartSpec.bar().data(quarters).legend(LegendPosition.BOTTOM)
                    .valueLabels(ValueLabelMode.OUTSIDE).build()));
            page.addParagraph(p -> text(p, prose(32)));
            page.add(new ChartNode(ChartSpec.bar().data(teams).horizontal(true).grouping(BarGrouping.STACKED)
                    .legend(LegendPosition.RIGHT).build()));
            page.addParagraph(p -> text(p, prose(33)));
            page.add(new ChartNode("", ChartSpec.line().data(quarters).legend(LegendPosition.TOP)
                    .valueLabels(ValueLabelMode.OUTSIDE).build(), markers, null, null));
            page.addParagraph(p -> text(p, prose(34)));
            page.add(new ChartNode(ChartSpec.line().data(visits).interpolation(LineInterpolation.SMOOTH).area(true)
                    .legend(LegendPosition.BOTTOM).build()));
            page.addParagraph(p -> text(p, prose(35)));
            page.add(new ChartNode("", ChartSpec.line().data(visits).interpolation(LineInterpolation.MONOTONE)
                    .build(), markers, null, null));
            page.addParagraph(p -> text(p, prose(36)));
        });
    }

    /**
     * A pie with its shares outside its slices and a legend at its right, and a donut with a
     * category and share on each slice and a total in its hole, between paragraphs. The donut
     * does not fit under the pie, so the layout moves it whole onto page 2.
     */
    private static void pieAndDonutCharts(DocumentSession session) {
        session.pageSize(400, 560).margin(MARGIN);
        ChartData regions = ChartData.builder().categories("North", "South", "East", "West")
                .series("Share", 45, 25, 20, 10).build();
        session.pageFlow(page -> {
            page.spacing(8);
            page.addParagraph(p -> p.text("Sales by region").textStyle(HEADING));
            page.addParagraph(p -> text(p, prose(41)));
            page.add(new ChartNode(ChartSpec.pie().data(regions).sliceLabels(SliceLabelMode.PERCENT)
                    .legend(LegendPosition.RIGHT).size(ChartSize.aspectRatio(16, 10)).build()));
            page.addParagraph(p -> text(p, prose(42)));
            page.add(new ChartNode(ChartSpec.pie().data(regions).donutRatio(0.6).centerText("100%")
                    .sliceLabels(SliceLabelMode.CATEGORY_PERCENT).legend(LegendPosition.BOTTOM).build()));
            page.addParagraph(p -> text(p, prose(43)));
        });
    }

    /**
     * A panel kept together, an accent down its left edge, holding a heading and five rows of an
     * icon beside a label: it does not fit under the seven paragraphs above it, so the layout
     * moves it whole onto page 2, and the paragraph after it with it.
     */
    private static void keptIconPanel(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> {
            page.spacing(6);
            for (int index = 0; index < 7; index++) {
                int seed = index;
                page.addParagraph(p -> text(p, prose(seed)));
            }
            page.addSection(panel -> {
                panel.softPanel(SURFACE, 0, 12).accentLeft(INK, 3).keepTogether().spacing(4);
                panel.addParagraph(p -> p.text("Key contacts").textStyle(HEADING));
                for (String contact : List.of("Programme lead - Alex Morgan", "Finance - Sam Patel",
                        "Engineering - Jordan Lee", "Operations - Casey Kim", "Legal - Robin Diaz")) {
                    panel.addRow(row -> row.spacing(6).columns(fixed(14), weight(1))
                            .add(ICON.node(10))
                            .addParagraph(p -> text(p, contact)));
                }
            });
            page.addParagraph(p -> text(p, "After the panel. " + prose(7)));
        });
    }

    /**
     * A row of two columns: on the left a heading, a panel holding a table whose columns size to
     * their content, and a paragraph; on the right a heading, a list, a picture and a one-layer
     * stack holding a row of an icon beside a label.
     */
    private static void nestedRows(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> p.text("Nested layout").textStyle(HEADING))
                .addRow(row -> row.spacing(12).columns(weight(3), weight(2))
                        .addSection(left -> left.spacing(4)
                                .addParagraph(p -> p.text("Budget").textStyle(HEADING))
                                .addSection(card -> card.softPanel(SURFACE, 0, 8)
                                        .addTable(table -> table.autoColumns(2).defaultCellStyle(CELLS).headerStyle(CELLS)
                                                .headerRow("Item", "Cost")
                                                .row("Design", "4 000").row("Build", "12 500").row("Run", "2 300")))
                                .addParagraph(p -> text(p, prose(1))))
                        .addSection(right -> right.spacing(4)
                                .addParagraph(p -> p.text("Checklist").textStyle(HEADING))
                                .addList(list -> list.textStyle(TEXT).items("Scope agreed", "Budget approved", "Team staffed"))
                                .addImage(image -> image.source(DocumentImageData.fromBytes(PICTURE)).size(110, 62))
                                .addLayerStack(stack -> stack.layer(new RowBuilder().spacing(4).columns(fixed(14), weight(1))
                                        .add(ICON.node(10))
                                        .addParagraph(p -> text(p, "Status: on track")).build()))))
                .addParagraph(p -> text(p, "Below the row. " + prose(2))));
    }

    /**
     * A dark rounded banner as wide as the page, its title centred in it, and a row holding
     * initials centred in a circle beside a paragraph.
     */
    private static void bannerAndInitials(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addContainer(banner -> banner.roundedRect(328, 60, 10).fillColor(DARK)
                        .center(new ParagraphBuilder().text("Annual programme review").textStyle(WHITE).build()))
                .addParagraph(p -> text(p, prose(3)))
                .addRow(row -> row.spacing(10).columns(fixed(48), weight(1))
                        .add(new ShapeContainerBuilder().circle(44).fillColor(INK)
                                .center(new ParagraphBuilder().text("AM").textStyle(WHITE).build()).build())
                        .addParagraph(p -> text(p, "Alex Morgan, programme lead. " + prose(4))))
                .addParagraph(p -> text(p, "After the initials. " + prose(5))));
    }

    /**
     * A table of thirty rows the layout runs over two pages, its header repeated, a heading kept
     * with the paragraph after it, and a panel of six paragraphs kept together that does not fit
     * under them, so the layout moves it whole onto page 3. A footer on every page holds a label
     * and the page's number.
     */
    private static void keptBlocksOverPages(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.chrome().zone(DocumentPageZone.footer(24, context -> new RowBuilder().spacing(8)
                .addParagraph(p -> text(p, "Kept blocks"))
                .add(context.pageNumber(TEXT)).build()));
        session.pageFlow(page -> {
            page.spacing(6);
            page.addTable(table -> {
                table.columns(DocumentTableColumn.fixed(40), DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(88))
                        .defaultCellStyle(CELLS).headerStyle(CELLS).headerRow("No", "Task", "Owner").repeatHeader();
                for (int row = 1; row <= 30; row++) {
                    table.row(String.valueOf(row), "Task number " + row + (row % 7 == 0 ? " (review)" : ""),
                            "Team " + (row % 4));
                }
            });
            page.addSection(heading -> heading.keepWithNext()
                    .addParagraph(p -> p.text("Heading kept with the next block").textStyle(HEADING)));
            for (int index = 0; index < 3; index++) {
                int seed = index;
                page.addParagraph(p -> text(p, prose(seed)));
            }
            page.addSection(panel -> {
                panel.keepTogether().softPanel(SURFACE, 0, 10).spacing(2);
                for (int line = 1; line <= 6; line++) {
                    int number = line;
                    panel.addParagraph(p -> text(p, "Kept line " + number + ". " + prose(10 + number)));
                }
            });
            page.addParagraph(p -> text(p, "After the kept panel."));
        });
    }

    /**
     * A picture turned by 15 degrees and a filled shape turned by 20, each between paragraphs.
     * The export draws both upright and names, for each, the turn it does not carry.
     */
    private static void rotatedImageAndShape(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(10)
                .addParagraph(p -> text(p, "Before the turned picture, on a line of its own."))
                .addImage(image -> image.source(DocumentImageData.fromBytes(PICTURE)).size(120, 68)
                        .transform(DocumentTransform.rotate(15)))
                .addParagraph(p -> text(p, "After the turned picture. " + prose(1)))
                .addShape(shape -> shape.size(160, 40).fillColor(INK).transform(DocumentTransform.rotate(20)))
                .addParagraph(p -> text(p, "After the turned shape. " + prose(2))));
    }

    /**
     * Twenty-two numbered paragraphs over three pages, a column painted down the left edge of
     * every page and a footer holding a label and the page's number.
     *
     * <p>Its baseline holds page 2's lines a line low in both editors, and page 3's two lines
     * low. The page sets the last line of a paragraph alone at the top of page 2; Word and
     * LibreOffice each carry a line more over that break, so that no line of a paragraph stands
     * alone there. That runs their page 2 a line long: its last line goes over to page 3 and
     * leaves the first line of the next paragraph alone at page 2's foot, which they carry over
     * too. The export writes no {@code w:widowControl}, in the document or its styles.</p>
     */
    private static void sidebarOverPages(DocumentSession session) {
        session.pageSize(400, 500).margin(new DocumentInsets(36, 36, 36, 60));
        session.pageBackgrounds(List.of(PageBackgroundFill.leftColumn(0.08, INK)));
        session.chrome().zone(DocumentPageZone.footer(24, context -> new RowBuilder().spacing(8)
                .addParagraph(p -> text(p, "Sidebar"))
                .add(context.pageNumber(TEXT)).build()));
        session.pageFlow(page -> {
            page.spacing(6);
            for (int number = 1; number <= 22; number++) {
                int seed = number;
                page.addParagraph(p -> text(p, seed + ". " + prose(seed)));
            }
        });
    }

    /** A header holding a logo beside a line of text, and a row filled and outlined round its two columns. */
    private static void headerLogoAndFilledRow(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.chrome().zone(DocumentPageZone.header(40, context -> new RowBuilder().spacing(8)
                .columns(fixed(48), weight(1))
                .addImage(image -> image.source(DocumentImageData.fromBytes(PICTURE)).size(48, 27))
                .addParagraph(p -> text(p, "Header with a logo")).build()));
        session.pageFlow(page -> page.spacing(10)
                .addRow(row -> row.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1)).padding(DocumentInsets.of(8))
                        .columns(weight(1), weight(1))
                        .addParagraph(p -> text(p, "Filled row, left"))
                        .addParagraph(p -> text(p, "Filled row, right")))
                .addParagraph(p -> text(p, prose(1))));
    }

    /**
     * Twenty-six numbered paragraphs over four pages, under a footer reading the page's number of
     * the document's pages. The top and bottom margins change from page 2, to 70 and 50pt, and
     * again from page 3, to 36 and 70pt; the export writes each change as a Word section.
     */
    private static void sectionsAndFields(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageMargins(List.of(PageMarginRule.from(2, new DocumentInsets(70, 36, 50, 36)),
                PageMarginRule.from(3, new DocumentInsets(36, 36, 70, 36))));
        session.chrome().zone(DocumentPageZone.footer(24, context -> new RowBuilder().spacing(4)
                .addParagraph(p -> text(p, "Page"))
                .add(context.pageNumber(TEXT))
                .addParagraph(p -> text(p, "of"))
                .add(context.pageTotal(TEXT)).build()));
        session.pageFlow(page -> {
            page.spacing(6);
            for (int number = 1; number <= 26; number++) {
                int seed = number;
                page.addParagraph(p -> text(p, seed + ". " + prose(seed)));
            }
        });
    }

    /**
     * Paragraphs set right to left: one in Hebrew, one in Arabic, and one in Hebrew holding Latin
     * letters and numbers, between paragraphs set left to right.
     */
    private static void rtlParagraphs(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Right-to-left paragraphs follow."))
                .addParagraph(p -> p.text(HEBREW_PROSE).textStyle(HEBREW).direction(TextDirection.RTL))
                .addParagraph(p -> p.text(ARABIC_PROSE).textStyle(ARABIC).direction(TextDirection.RTL))
                .addParagraph(p -> p.text("גרסה 2.5 שוחררה ב-2026 עם תמיכה ב-DOCX.").textStyle(HEBREW)
                        .direction(TextDirection.RTL))
                .addParagraph(p -> text(p, "After the right-to-left paragraphs. " + prose(1))));
    }

    /**
     * A table of contents naming three headings over three pages, a paragraph linking to one of
     * them, and a link to a web page.
     */
    private static void linksAndToc(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> {
            page.spacing(6);
            page.addTableOfContents(toc -> toc.title("Contents").titleStyle(HEADING).entryStyle(TEXT)
                    .pageNumberStyle(TEXT)
                    .entry("Scope", "scope").entry("Budget", "budget").entry("Schedule", "schedule"));
            page.addParagraph(p -> p.textStyle(TEXT).inlineText("See ").inlineLinkTo("the schedule", "schedule")
                    .inlineText(" and the project site."));
            page.addLink("Project site", "https://example.com/programme");
            List<String> headings = List.of("Scope", "Budget", "Schedule");
            for (int section = 0; section < headings.size(); section++) {
                String heading = headings.get(section);
                page.addParagraph(p -> p.text(heading).textStyle(HEADING).anchor(heading.toLowerCase(Locale.ROOT)));
                for (int index = 0; index < 6; index++) {
                    int seed = 6 * section + index;
                    page.addParagraph(p -> text(p, prose(seed)));
                }
            }
        });
    }

    /**
     * Twenty numbered paragraphs of six sentences over five pages, nothing but text.
     *
     * <p>Its baseline holds page 5's lines a line low in both editors: the page sets a
     * paragraph's first line alone at the foot of page 4, and Word and LibreOffice each carry it
     * over the break onto page 5, so that no line of a paragraph stands alone there. The export
     * writes no {@code w:widowControl}, in the document or its styles.</p>
     */
    private static void longFreeText(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> {
            page.spacing(6);
            for (int number = 1; number <= 20; number++) {
                int seed = number;
                page.addParagraph(p -> text(p, seed + ". " + prose(seed) + " " + prose(seed + 100)));
            }
        });
    }

    /**
     * A layer stack laying an icon over the top right corner of a card, between a paragraph above
     * it and one under it. The paragraph above is the document's first of body text and its
     * longest, so an edit that lengthens either moves the card, and the icon has to move with it.
     *
     * <p>Its baseline holds the card where the page sets it, within 0.06pt. It held the card
     * 13.99pt low in Word and 13.98pt in LibreOffice, and the paragraph under it 28.02 and
     * 27.98pt: the stack is written as a band, and the band measured the space round it through
     * the card's padding to the text in it, which the card writes again as its cell's margins.
     * The space is now measured to the card's box ({@code DocxLayerColumns#edgeIn}). The icon
     * is carried by the card's text, so it stands on the card's corner as each editor sets the
     * card, and moves with it when the text above is edited. The gate reads lines of text only:
     * where the icon stands it does not see, and {@link DocxDrawingAnchorGuardTest} holds the
     * anchor it is carried by.</p>
     */
    private static void iconOverCard(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card: this paragraph is the longest in the document, so "
                                           + "an edit that lengthens the longest paragraph lengthens it and moves "
                                           + "the card and the icon over its corner down the page together."))
                .addLayerStack(stack -> stack
                        .layer(new SectionBuilder().softPanel(SURFACE, 0, 14)
                                .addParagraph(p -> text(p, "A card under an icon laid over its top right "
                                                           + "corner, its text held in by its padding."))
                                .build())
                        .layer(ICON.node(18), LayerAlign.TOP_RIGHT))
                .addParagraph(p -> text(p, "After the card: this paragraph stands under it on the page.")));
    }

    /**
     * A rounded box narrower than the page between a paragraph above it and one under it, its
     * note set left in a block centred in the box and wrapped over three lines inside it.
     */
    private static void narrowContainer(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the box, on a line of its own."))
                .addContainer(box -> box.roundedRect(220, 90, 10).fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .center(paragraph("A note centred in a box narrower than the page: the page wraps "
                                          + "it over three lines inside the box, each line held within "
                                          + "its outline.")))
                .addParagraph(p -> text(p, "After the box: this paragraph stands under it on the page, "
                                           + "and none of its lines runs over the box.")));
    }

    /**
     * A rounded box clipped to its outline, holding more text than it has room for: the page
     * centres the text across the box, sets it from the box's top, as it is taller than the box,
     * and cuts the lines that run past its foot. A paragraph stands above the box and one under
     * it.
     */
    private static void clippedContainer(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the clipped box, on a line of its own."))
                .addContainer(box -> box.roundedRect(200, 60, 12).fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .center(paragraph("Clipped text in a rounded box too short to hold it. The page "
                                          + "sets it from the top of the box and cuts it at the outline, so "
                                          + "the lines that run past the foot of the box are not seen. Only "
                                          + "the lines the box holds are read, and the box stays as tall as "
                                          + "it was made, whatever it is given to hold.")))
                .addParagraph(p -> text(p, "After the clipped box: this paragraph stands under it on the page, "
                                           + "and none of its lines runs over the box.")));
    }

    /**
     * A rounded card as wide as the page between its margins, its heading and text set from its
     * top left corner and filling it, under its default clip, between a paragraph above it and
     * one under it. Text added to the card in an editor needs it taller.
     */
    private static void roundedCard(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card, on a line of its own."))
                .addContainer(card -> card.roundedRect(328, 80, 12).fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .topLeft(new SectionBuilder().padding(DocumentInsets.of(12)).spacing(4)
                                .addParagraph(p -> p.text("A rounded card").textStyle(HEADING))
                                .addParagraph(p -> text(p, "Its text runs over three lines from the card's top "
                                                           + "left corner, and the card is just tall enough to hold "
                                                           + "them with its padding round them: a line more, and "
                                                           + "the card needs to be taller."))
                                .build()))
                .addParagraph(p -> text(p, "After the card: this paragraph stands under it on the page, "
                                           + "and none of its lines runs over the card.")));
    }

    /**
     * A card with a page break among its children, and a card nested in it with a break of its
     * own: the card runs over three pages, each part where the page sets it.
     */
    private static void breakInAPanel(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card, on the first page."))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(8)).spacing(6)
                        .addParagraph(p -> text(p, "The card opens on the first page."))
                        .addParagraph(p -> text(p, "Its second line stands under the first."))
                        .addPageBreak(pageBreak -> { })
                        .addParagraph(p -> text(p, "After the card's own break, on the second page."))
                        .addSection(inner -> inner.fillColor(INNER).padding(DocumentInsets.of(6)).spacing(4)
                                .addParagraph(p -> text(p, "The inner card opens under it."))
                                .addPageBreak(pageBreak -> { })
                                .addParagraph(p -> text(p, "After the inner card's break, on the third page.")))
                        .addParagraph(p -> text(p, "The card closes on the third page.")))
                .addParagraph(p -> text(p, "After the card, on the third page.")));
    }

    /**
     * Page breaks the page sets as boxes of no height: in a row's column and in a stack's one
     * layer. The page goes on with what follows on the same page, and so does the editor.
     */
    private static void breakInAColumn(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "A row and a stack, each with a break in it."))
                .addRow(row -> row.spacing(12)
                        .addSection(column -> column.spacing(4)
                                .addParagraph(p -> text(p, "Part A of the column."))
                                .addPageBreak(pageBreak -> { })
                                .addParagraph(p -> text(p, "Part B of the column.")))
                        .addParagraph(p -> text(p, "The column beside it.")))
                .addLayerStack(stack -> stack.topLeft(new SectionBuilder().spacing(4)
                        .addParagraph(p -> text(p, "The layer before its break."))
                        .addPageBreak(pageBreak -> { })
                        .addParagraph(p -> text(p, "The layer after its break."))
                        .build()))
                .addParagraph(p -> text(p, "After the row and the stack, on the same page.")));
    }

    /**
     * A bordered card whose table of sixty rows the layout runs over three pages, its header
     * repeated at the top of each, the pages after the first set against a larger top margin.
     *
     * <p>Spaced so that page 1's rows leave room under them for the paragraph that ends a Word
     * section at the cut, as gaps of 2 to 6pt and 12 to 16pt do: at 8 or 10pt the export names
     * page 2's margins instead, and both editors set page 2 with page 1's. A change to a table's
     * metrics can move page 1's last row into that case: the gate then fails this document with
     * every line of page 2 about 53pt high. Where the export's report names page 2's margins as
     * leaving page 1's rows too near its foot, the fixture is to be re-spaced; where it does not,
     * the section at the cut was lost, and the change is at fault.</p>
     */
    private static void cardOverPages(DocumentSession session) {
        session.pageSize(400, 600).margin(MARGIN);
        session.pageMargins(List.of(PageMarginRule.from(2, new DocumentInsets(90, 36, 36, 36))));
        session.pageFlow(page -> page.spacing(4)
                .addParagraph(p -> text(p, "The card's table runs over three pages."))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(6))
                        .addTable(table -> {
                            table.columns(DocumentTableColumn.fixed(220), DocumentTableColumn.fixed(90))
                                    .defaultCellStyle(CELLS).headerStyle(CELLS)
                                    .header("Item", "Amount").repeatHeader();
                            for (int row = 1; row <= 60; row++) {
                                table.row("Line item " + row, String.valueOf(row * 7));
                            }
                        }))
                .addParagraph(p -> text(p, "After the card, on its last page.")));
    }

    /**
     * A paragraph of three of the sixteen {@link #SENTENCES}, chosen by the seed: two seeds whose
     * difference is not a multiple of 80 never open on the same two sentences. The gate pairs
     * lines of the same letters on a page each with the editor's nearest, so paragraphs repeating
     * one text would pair a line moved a line down with its neighbour's place and hide the move.
     */
    private static String prose(int seed) {
        int first = Math.floorMod(seed, 16);
        int second = (first + 1 + Math.floorMod(seed / 16 + seed * 5, 15)) % 16;
        int third = (second + 1 + Math.floorMod(seed / 16 * 7 + seed * 3 + 4, 15)) % 16;
        while (third == first || third == second) {
            third = (third + 1) % 16;
        }
        return SENTENCES.get(first) + " " + SENTENCES.get(second) + " " + SENTENCES.get(third);
    }

    /** A 160 by 90 pixel picture: a white disc on a blue field. */
    private static byte[] picture() {
        BufferedImage image = new BufferedImage(160, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(26, 86, 148));
        graphics.fillRect(0, 0, 160, 90);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(50, 15, 60, 60);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static ParagraphBuilder text(ParagraphBuilder paragraph, String text) {
        return paragraph.text(text).textStyle(TEXT);
    }

    private static ParagraphNode paragraph(String text) {
        return text(new ParagraphBuilder(), text).build();
    }

    private static DocxCorpusDocument synthetic(String name, Consumer<DocumentSession> compose) {
        return new DocxCorpusDocument("synthetic", name, -1, compose);
    }
}
