package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.payloads.LineFragmentPayload;
import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rule composed in a table's cell is written once, as its paragraph's border, where the page
 * draws it.
 *
 * <p>Its table draws what its composed cells paint as shapes, rules among them, and the cell
 * wrote each rule as a border too: every line in such a cell came out twice in Word, and one
 * centred in its cell was drawn centred and written again at the cell's left.</p>
 */
class DocxCellRuleTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final String LINE = "prst=\"line\"";

    @Test
    void aLineComposedInACellIsWrittenOnceAsItsRule() throws Exception {
        Export export = inACell(new LineBuilder().horizontal(60).thickness(1).color(INK).build());

        assertThat(export.rules()).isEqualTo(1);
        assertThat(export.body()).as("not drawn too").doesNotContain(LINE).doesNotContain("<wp:anchor");
    }

    @Test
    void aLineCentredInItsCellIsWrittenWhereThePageCentresIt() throws Exception {
        Export export = inACell(new AlignNode(new LineBuilder().horizontal(60).thickness(1).color(INK).build(),
                HorizontalAlign.CENTER));
        PlacedFragment line = export.fragments().stream()
                .filter(fragment -> fragment.payload() instanceof LineFragmentPayload).findFirst().orElseThrow();
        // The cell's text starts 4pt in from its edge at 20, as its margin in the file does.
        double start = line.x() + ((LineFragmentPayload) line.payload()).startX() - 24;

        // Each indent taken in by what Word draws its border past it (wordsRuleReach).
        CTInd indent = export.rule().getCTP().getPPr().getInd();
        assertThat(DocxTwips.of(indent.getLeft()))
                .isEqualTo(Math.round((start + DocxSemanticBackend.WORD_RULE_REACH_LEFT_POINTS) * 20));
        assertThat(DocxTwips.of(indent.getRight())).as("as far from the right")
                .isEqualTo(Math.round((start + DocxSemanticBackend.WORD_RULE_REACH_RIGHT_POINTS) * 20));
        assertThat(export.body()).doesNotContain(LINE);
    }

    @Test
    void aBarComposedInACellIsWrittenOnceAsItsRule() throws Exception {
        Export export = inACell(new ShapeBuilder().size(100, 2).fillColor(INK).build());

        assertThat(export.rules()).isEqualTo(1);
        assertThat(export.body()).doesNotContain("<wp:anchor");
    }

    @Test
    void aRuleTakesItsOwnDrawingNotAnotherLineBeforeIt() throws Exception {
        // A vertical line is drawn, not a rule, and stays drawn; the rule after it is written.
        Export export = inACell(new SectionBuilder()
                .addLine(l -> l.vertical(20).thickness(1).color(INK))
                .addLine(l -> l.horizontal(100).thickness(2).color(INK)).build());

        assertThat(export.rules()).isEqualTo(1);
        assertThat(export.body().split(LINE, -1)).as("one line drawn").hasSize(2);
        assertThat(export.body()).as("the vertical one").contains("<wp:extent cx=\"0\" cy=\"" + Units.toEMU(20) + "\"");
        assertThat(DocxTwips.of(export.rule().getCTP().getPPr().getInd().getRight()))
                .as("the rule 100pt long in its 192pt cell, Word's reach in")
                .isEqualTo(92 * 20 + Math.round(DocxSemanticBackend.WORD_RULE_REACH_RIGHT_POINTS * 20));

        // A rule running to the end of its width has no length of its own to tell it by.
        Export filling = inACell(new SectionBuilder()
                .addLine(l -> l.vertical(20).thickness(1).color(INK))
                .addLine(l -> l.thickness(1).color(INK).fill()).build());
        assertThat(filling.body()).as("the vertical one, still drawn")
                .contains("<wp:extent cx=\"0\" cy=\"" + Units.toEMU(20) + "\"");
        assertThat(filling.body().split(LINE, -1)).as("and only it: the rule took its own").hasSize(2);
    }

    @Test
    void aRuleTakesItsOwnDrawingNotALevelLineDrawnOverSomethingBeforeIt() throws Exception {
        // The underline is a layer of a mark, drawn over its icon: no rule, though level too. It
        // parts from the rule after it by its length alone, then by its thickness alone.
        Export shorter = underlinedThenRuled(12, 1, 100, 1);
        Export thinner = underlinedThenRuled(100, 1, 100, 2);

        assertThat(shorter.rules()).isEqualTo(1);
        assertThat(shorter.body()).as("the 12pt underline, drawn")
                .contains("<wp:extent cx=\"" + Units.toEMU(12) + "\" cy=\"0\"")
                .doesNotContain("<wp:extent cx=\"" + Units.toEMU(100) + "\" cy=\"0\"");
        assertThat(thinner.rules()).isEqualTo(1);
        assertThat(thinner.body()).as("the 1pt underline, drawn").contains("<a:ln w=\"" + Units.toEMU(1) + "\"")
                .doesNotContain("<a:ln w=\"" + Units.toEMU(2) + "\"");
    }

    @Test
    void twoRulesInACellEachTakeTheirOwnDrawing() throws Exception {
        Export alike = inACell(new SectionBuilder()
                .addLine(l -> l.horizontal(100).thickness(1).color(INK))
                .addParagraph("Between")
                .addLine(l -> l.horizontal(100).thickness(1).color(INK)).build());
        Export shortThenFilling = inACell(new SectionBuilder()
                .addLine(l -> l.horizontal(100).thickness(1).color(INK))
                .addParagraph("Between")
                .addLine(l -> l.thickness(1).color(INK).fill()).build());

        for (Export export : List.of(alike, shortThenFilling)) {
            assertThat(export.rules()).isEqualTo(2);
            assertThat(export.body()).doesNotContain(LINE);
            assertThat(export.report().bySubject()).doesNotContainKey("LineNode");
        }
    }

    @Test
    void aRuleReachingPastItsCellIsNotTakenForAnotherDrawingAlike() throws Exception {
        // Pulled 6pt left past the cell's 4pt margin, the rule's drawing reaches past the cell,
        // and still counts: the underline as thick, laid over a mark, is not taken for it.
        Export export = inACell(new SectionBuilder()
                .add(new LayerStackBuilder().name("Mark")
                        .layer(new ShapeBuilder().size(100, 12).fillColor(INK).build())
                        .layer(new LineBuilder().horizontal(100).thickness(1).color(INK).build(), LayerAlign.BOTTOM_LEFT)
                        .build())
                .addLine(l -> l.thickness(1).color(INK).fill().margin(new DocumentInsets(0, 0, 0, -6))).build());
        PlacedFragment rule = export.fragments().stream()
                .filter(fragment -> fragment.payload() instanceof LineFragmentPayload && fragment.width() > 150)
                .findFirst().orElseThrow();

        assertThat(rule.x()).as("premise: the rule's drawing starts past the cell's edge at 20").isLessThan(19);
        assertThat(export.body()).as("the underline, still drawn")
                .contains("<wp:extent cx=\"" + Units.toEMU(100) + "\" cy=\"0\"");
        assertThat(export.report().bySubject().get("LineNode")).extracting(DocxExportReport.Note::detail)
                .anyMatch(detail -> detail.endsWith("so it stands twice in Word"));
    }

    @Test
    void aRuleThatCannotBeToldFromAnotherDrawingInItsCellIsNamedAsStandingTwice() throws Exception {
        // The underline is drawn alike: taken for the rule's, it would be lost. Both stay.
        Export export = underlinedThenRuled(100, 1, 100, 1);

        assertThat(export.rules()).isEqualTo(1);
        assertThat(export.body().split(LINE, -1)).as("the underline and the rule, both drawn").hasSize(3);
        assertThat(export.report().bySubject().get("LineNode")).extracting(DocxExportReport.Note::detail)
                .contains("composed in a table cell, it is written as a paragraph's border and drawn by its table, "
                          + "which draws what its composed cells paint, as well: this export does not tell that "
                          + "drawing apart from another in the cell, so it stands twice in Word");
    }

    @Test
    void aRuleInAPanelInsideItsCellIsNamedAsStandingTwice() throws Exception {
        // The panel is a table of its own in the cell, which the rule is not placed from.
        Export export = inACell(new SectionBuilder().name("Card").fillColor(DocumentColor.rgb(238, 243, 249))
                .addParagraph("In the card")
                .addLine(l -> l.horizontal(100).thickness(1).color(INK)).build());

        assertThat(export.body()).contains(LINE);
        assertThat(export.report().bySubject().get("LineNode")).extracting(DocxExportReport.Note::detail)
                .anyMatch(detail -> detail.contains("written in a panel or a row inside its cell, it is not placed "
                                                    + "from that drawing, so it stands twice in Word"));
    }

    @Test
    void aLineTooThickForARuleInACellIsNamedAsTakingNoRoom() throws Exception {
        Export export = inACell(new SectionBuilder().addParagraph("Above")
                .addLine(l -> l.horizontal(100).thickness(20).color(INK)).build());

        assertThat(export.body()).as("drawn at its thickness").contains("<a:ln w=\"" + Units.toEMU(20) + "\"");
        assertThat(export.rules()).isZero();
        assertThat(export.report().bySubject().get("LineNode")).extracting(DocxExportReport.Note::detail)
                .anyMatch(detail -> detail.contains("stroked thicker than Word's thickest border, it is drawn rather "
                                                    + "than written as a rule, and takes no room in its cell"));
    }

    private static Export underlinedThenRuled(double underline, double underlineStroke, double rule, double ruleStroke)
            throws Exception {
        return inACell(new SectionBuilder()
                .add(new LayerStackBuilder().name("Mark")
                        .layer(new ShapeBuilder().size(underline, 12).fillColor(INK).build())
                        .layer(new LineBuilder().horizontal(underline).thickness(underlineStroke).color(INK).build(),
                                LayerAlign.BOTTOM_LEFT)
                        .build())
                .addLine(l -> l.horizontal(rule).thickness(ruleStroke).color(INK)).build());
    }

    @Test
    void aRuleInARepeatedHeaderRowIsWrittenOnceOnEveryPage() throws Exception {
        // Word repeats the header row, its rule with it: the page's copies are not drawn again.
        Export export = export(page -> page.addTable(table -> {
            table.columns(DocumentTableColumn.fixed(200)).repeatHeader(1)
                    .headerCells(DocumentTableCell.node(new LineBuilder().horizontal(100).thickness(1).color(INK).build()));
            for (int row = 0; row < 80; row++) {
                table.row("Row " + row);
            }
        }));

        assertThat(export.fragments().stream().filter(f -> f.payload() instanceof LineFragmentPayload).count())
                .as("premise: the page draws the header's rule on more than one page").isGreaterThan(1);
        assertThat(export.rules()).as("written in its header cell").isEqualTo(1);
        assertThat(export.body()).doesNotContain(LINE);
    }

    @Test
    void aTableWhoseCellsDrawOnlyRulesIsNotSaidToDrawShapes() throws Exception {
        Export rules = inACell(new LineBuilder().horizontal(60).thickness(1).color(INK).build());
        Export disc = inACell(new SectionBuilder()
                .addLine(l -> l.horizontal(100).thickness(1).color(INK))
                .addCircle(10, INK).build());

        assertThat(rules.report().bySubject()).doesNotContainKey("cell drawing");
        assertThat(disc.report().bySubject().get("cell drawing")).as("the disc is drawn").hasSize(1);
    }

    private static Export inACell(DocumentNode content) throws Exception {
        return export(page -> page.addTable(table -> table.columns(DocumentTableColumn.fixed(200))
                .rowCells(DocumentTableCell.node(content))));
    }

    private record Export(XWPFDocument document, DocxExportReport report, List<PlacedFragment> fragments) {

        String body() {
            return document.getDocument().xmlText();
        }

        long rules() {
            return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(DocxCellRuleTest::isARule).count();
        }

        XWPFParagraph rule() {
            return document.getTables().get(0).getRow(0).getCell(0).getParagraphs().stream()
                    .filter(DocxCellRuleTest::isARule).findFirst().orElseThrow();
        }
    }

    private static boolean isARule(XWPFParagraph paragraph) {
        return paragraph.getCTP().getPPr() != null && paragraph.getCTP().getPPr().isSetPBdr();
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            byte[] bytes = session.export(new DocxSemanticBackend(report::set));
            return new Export(new XWPFDocument(new ByteArrayInputStream(bytes)), report.get(),
                    session.layoutGraph().fragments());
        }
    }
}
