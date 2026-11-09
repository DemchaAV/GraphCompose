package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.demcha.compose.document.style.DocumentRowColumn.fixed;
import static com.demcha.compose.document.style.DocumentRowColumn.weight;

/**
 * Documents of the DOCX fidelity corpus that set a group the layout holds on one page at the foot
 * of a page: ten forms of group, each at two places — as low on page 1 as it fits, and one line
 * of filler lower, where the layout moves the whole group onto page 2.
 *
 * <p>The forms are those a keep reaches in the export: a block kept together holding a table, a
 * row of four lines, or a table composed in a cell; a panel kept together holding a table; a band
 * of two layers; a shape container holding text; a one-layer stack holding a table; a heading kept
 * with the paragraph after it; a panel kept with what follows it, holding a table; and a panel
 * kept together holding rows of an icon beside a label. Above each group stand only lines of
 * filler; after it, the paragraph it is kept with where it has one, then one closing line. The
 * panel of icon rows is the shape of {@link SyntheticDocxCorpus}'s kept icon panel, which stands
 * only where the layout moves it; here it also stands as low as it fits.</p>
 *
 * <p>The gate holds where each editor sets every line of them, the group's included, so a change
 * that lets an editor break a group the page holds whole, or set it on another page than it does
 * now, fails it. Their text is set in a face the export embeds: in a face an editor substitutes, a cell's text
 * can break where the page's does not, and its row then grows, which moves the group for a reason
 * that is the face's and not the keep's.</p>
 */
final class KeptGroupsAtThePageFoot {

    /** The name every group is placed under, so that the layout's placement of it can be read. */
    static final String GROUP = "Kept group";

    private static final double WIDTH = 300;
    private static final double HEIGHT = 200;
    private static final DocumentInsets MARGIN = DocumentInsets.of(20);
    private static final DocumentTextStyle TEXT = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
    private static final DocumentTableStyle CELLS = DocumentTableStyle.builder().textStyle(TEXT).build();
    private static final DocumentColor PANEL = DocumentColor.rgb(230, 236, 245);
    private static final DocumentColor CONTAINER = DocumentColor.rgb(250, 240, 230);
    /** A disc beside a label, which the export draws floating and anchors to a paragraph of its row. */
    private static final SvgIcon ICON = SvgIcon.parse("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'>"
                                                      + "<circle cx='5' cy='5' r='5' fill='#1a5694'/></svg>");

    /**
     * One document: a form of group at one of its two places.
     *
     * @param form the group's form
     * @param fits whether this is the place where the group fits on page 1, or the one a line
     *             lower, where the layout moves it whole onto page 2
     */
    record AtTheFoot(Form form, boolean fits) {

        /** The lines of filler above the group: the most it fits under, or one more. */
        int filler() {
            return fits ? form.mostFiller() : form.mostFiller() + 1;
        }

        /** The page, from 0, that the layout sets the whole group on. */
        int groupPage() {
            return fits ? 0 : 1;
        }

        DocxCorpusDocument document() {
            String name = form.name() + (fits ? "_fits_at_the_foot" : "_moves_to_page_2");
            return new DocxCorpusDocument("synthetic", name, -1, this::compose);
        }

        private void compose(DocumentSession session) {
            session.pageSize(WIDTH, HEIGHT).margin(MARGIN);
            session.pageFlow(page -> {
                for (int index = 0; index < filler(); index++) {
                    page.addParagraph("Filler line " + index, TEXT);
                }
                form.group().accept(page);
                page.addParagraph("Closing line", TEXT);
            });
        }
    }

    /**
     * A form of group, and the most lines of filler it fits under on page 1.
     *
     * @param name       the form's name in the document's stem
     * @param mostFiller the most lines of filler the group fits under on page 1
     * @param group      adds the group to the page's flow
     */
    record Form(String name, int mostFiller, Consumer<PageFlowBuilder> group) {
    }

    private KeptGroupsAtThePageFoot() {
    }

    /** The ten forms, in the order their documents enter the corpus. */
    static List<Form> forms() {
        return List.of(
                new Form("kept_table", 7, page -> page
                        .addSection(GROUP, group -> group.keepTogether()
                                .addParagraph("GTOP heading", TEXT)
                                .addTable(KeptGroupsAtThePageFoot::threeRows))),
                new Form("kept_tall_row", 7, page -> page
                        .addSection(GROUP, group -> group.keepTogether()
                                .addParagraph("GTOP heading", TEXT)
                                .addTable(table -> table.autoColumns(2).defaultCellStyle(CELLS).rowCells(
                                        DocumentTableCell.lines("C1 one", "C2 two", "C3 three", "C4 four"),
                                        DocumentTableCell.text("Beside"))))),
                new Form("kept_nested_table", 6, page -> page
                        .addSection(GROUP, group -> group.keepTogether()
                                .addParagraph("GTOP heading", TEXT)
                                .addTable(table -> table.autoColumns(2).defaultCellStyle(CELLS).rowCells(
                                        DocumentTableCell.node(new TableBuilder().autoColumns(1).defaultCellStyle(CELLS)
                                                .row("N1 nested").row("N2 nested").row("N3 nested").build()),
                                        DocumentTableCell.text("Beside"))))),
                new Form("kept_panel_with_table", 6, page -> page
                        .addSection(GROUP, group -> group.keepTogether()
                                .fillColor(PANEL).padding(DocumentInsets.of(6))
                                .addParagraph("GTOP heading", TEXT)
                                .addTable(KeptGroupsAtThePageFoot::threeRows))),
                new Form("band", 10, page -> page
                        .addLayerStack(stack -> stack.name(GROUP)
                                .topLeft(new SectionBuilder()
                                        .addParagraph("GTOP opening", TEXT).addParagraph("R2 body", TEXT)
                                        .addParagraph("R3 tail", TEXT).build())
                                .bottomRight(new ParagraphBuilder().text("Caption").textStyle(TEXT).build()))),
                new Form("shape_container", 9, page -> page
                        .add(new ShapeContainerBuilder().name(GROUP)
                                .rectangle(220, 50).fillColor(CONTAINER)
                                .topLeft(new SectionBuilder()
                                        .addParagraph("GTOP opening", TEXT).addParagraph("R2 body", TEXT)
                                        .addParagraph("R3 tail", TEXT).build())
                                .build())),
                new Form("stack_holding_a_table", 7, page -> page
                        .addLayerStack(stack -> stack.name(GROUP)
                                .topLeft(new SectionBuilder()
                                        .addParagraph("GTOP opening", TEXT)
                                        .addTable(KeptGroupsAtThePageFoot::threeRows)
                                        .build()))),
                new Form("heading_kept_with_next", 11, page -> page
                        .addSection(GROUP, group -> group.keepWithNext()
                                .addParagraph("GTOP heading", TEXT))
                        .addParagraph("R2 body follows the heading", TEXT)),
                new Form("panel_kept_with_next", 5, page -> page
                        .addSection(GROUP, group -> group.keepWithNext()
                                .fillColor(PANEL).padding(DocumentInsets.of(6))
                                .addParagraph("GTOP heading", TEXT)
                                .addTable(KeptGroupsAtThePageFoot::threeRows))),
                new Form("kept_panel_of_icon_rows", 7, page -> page
                        .addSection(GROUP, group -> {
                            group.keepTogether().fillColor(PANEL).padding(DocumentInsets.of(6)).spacing(4)
                                    .addParagraph("GTOP heading", TEXT);
                            for (String label : List.of("R1 icon beside", "R2 icon beside", "R3 icon beside")) {
                                group.addRow(row -> row.spacing(6).columns(fixed(14), weight(1))
                                        .add(ICON.node(10))
                                        .addParagraph(label, TEXT));
                            }
                        })));
    }

    /** Each form at both places: as low as it fits on page 1, and one line of filler lower. */
    static List<AtTheFoot> fixtures() {
        List<AtTheFoot> fixtures = new ArrayList<>();
        for (Form form : forms()) {
            fixtures.add(new AtTheFoot(form, true));
            fixtures.add(new AtTheFoot(form, false));
        }
        return fixtures;
    }

    /** The documents the corpus holds, two for each form. */
    static List<DocxCorpusDocument> documents() {
        return fixtures().stream().map(AtTheFoot::document).toList();
    }

    private static void threeRows(TableBuilder table) {
        table.autoColumns(2).defaultCellStyle(CELLS)
                .row("R1 alpha", "R1 beta")
                .row("R2 alpha", "R2 beta")
                .row("R3 alpha", "R3 beta");
    }
}
