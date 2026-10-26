package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.LayerStackBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Documents that refer to the pages of their anchors: an anchor on every kind of node that declares
 * one, references from a table of contents, the body, a table's composed cell, a layer stack's
 * column, a panel and a row's column, and the anchors the export writes no bookmark for.
 *
 * <p>The template corpus declares no anchor: no preset refers to a page. These documents do, so
 * that {@link DocxPageReferenceGuardTest} reads a page reference of every kind the export writes.
 * A reference whose anchor has no bookmark stands right after a paragraph of its own, which names
 * it, so its number can be found and read.</p>
 */
final class PageReferenceDocxDocuments {

    /** An anchor named by a sentence, longer than Word lets a bookmark's name be. */
    private static final String TERMS = "Terms and conditions of the agreement between both parties";
    /** Another, of text, cut to a name of its own. */
    private static final String SCHEDULE = "Payment schedule for the second phase of the work";

    private static final DocumentInsets MARGIN = DocumentInsets.of(30);
    private static final DocumentInsets LEFT_COLUMN = new DocumentInsets(0, 190, 0, 0);
    private static final DocumentInsets RIGHT_COLUMN = new DocumentInsets(0, 0, 0, 170);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentColor ACCENT = DocumentColor.rgb(200, 60, 60);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    private PageReferenceDocxDocuments() {
    }

    /**
     * A page reference a document holds, and how the export is to write it.
     *
     * @param anchor   the anchor it reads the page of
     * @param bookmark the bookmark its {@code PAGEREF} field names, or {@code null} where the export
     *                 writes the anchor no bookmark and the reference is written as its number, with
     *                 a note
     * @param reads    what it reads in the file — its field until an editor updates it, or its text —
     *                 or {@code null} for the page the layout resolved for its anchor, and nothing
     *                 where the layout resolved none
     * @param label    the text of the paragraph right before its own, or {@code null} where the
     *                 paragraph before it is not one of its own: a table of contents' entry
     */
    record Reference(String anchor, String bookmark, String reads, String label) {

        static Reference field(String anchor, String bookmark) {
            return new Reference(anchor, Objects.requireNonNull(bookmark, "bookmark"), null, null);
        }

        static Reference asText(String anchor, String label) {
            return new Reference(anchor, null, null, Objects.requireNonNull(label, "label"));
        }
    }

    /**
     * A document that refers to pages, and every page reference it holds, in no particular order.
     *
     * @param name       the document's name
     * @param compose    sets its page and composes it into a fresh session
     * @param references its page references
     * @param premises   what its export's report says of the shapes it is built to hold, each a part
     *                   of a note's text: without them, the document no longer holds those shapes
     */
    record ReferringDocument(String name, Consumer<DocumentSession> compose, List<Reference> references,
                             List<String> premises) {

        ReferringDocument {
            references = List.copyOf(references);
            premises = List.copyOf(premises);
        }

        /** The references written as fields. */
        List<Reference> fields() {
            return references.stream().filter(reference -> reference.bookmark() != null).toList();
        }

        /** How many of its references are written as their number, with a note. */
        long asText() {
            return references.stream().filter(reference -> reference.bookmark() == null).count();
        }
    }

    /** The documents, one for each group of shapes. */
    static List<ReferringDocument> documents() {
        return List.of(
                new ReferringDocument("anchor_kinds", PageReferenceDocxDocuments::anchorKinds, List.of(
                        Reference.field("para", "para"),
                        Reference.field("section", "section"),
                        Reference.field("container", "container"),
                        Reference.field("table", "table"),
                        Reference.field("image", "image"),
                        Reference.field("barcode", "barcode"),
                        Reference.field("rule", "rule"),
                        Reference.field("drawings", "drawings"),
                        Reference.field("drawn box", "drawn_box"),
                        Reference.field(TERMS, "Terms_and_conditions_of_the_agreement_be"),
                        Reference.field(SCHEDULE, "Payment_schedule_for_the_second_phase_of"),
                        // A drawn shape's own anchor has no bookmark for a field to name.
                        Reference.asText("shape", "See the shape:"),
                        Reference.asText("ellipse", "See the ellipse:")),
                        List.of()),
                new ReferringDocument("reference_places", PageReferenceDocxDocuments::referencePlaces, List.of(
                        Reference.field("cell", "cell"),
                        Reference.field("column", "column"),
                        Reference.field("drawn column", "drawn_column"),
                        Reference.field("panel", "panel"),
                        Reference.field("row", "row"),
                        // The one anchor, referred to from a composed cell, a stack's column, a panel,
                        // a row's column and the body. The layout holds no text for what a table's
                        // composed cell holds: the field there reads nothing until it is updated, and
                        // a reference there written as its number is empty.
                        new Reference("target", "target", "", null),
                        Reference.field("target", "target"),
                        Reference.field("target", "target"),
                        Reference.field("target", "target"),
                        Reference.field("target", "target"),
                        new Reference("mark", null, "", "From a table's cell, to a shape:")),
                        List.of()),
                new ReferringDocument("unmarked", PageReferenceDocxDocuments::unmarked, List.of(
                        // Blocks that write no paragraph, and whose first drawing no paragraph
                        // carries as their own: one that draws nothing, one whose first drawing fills a
                        // page nothing is written on, one composed in a table's cell.
                        Reference.asText("empty", "See empty:"),
                        Reference.asText("tall", "See tall:"),
                        Reference.asText("composed", "See composed:"),
                        // An anchor the document does not have, and one that cleans to another's name:
                        // the link to "a." is named first, so "a" is marked under a name of its own,
                        // which its field names, and the link to "a." lands on no bookmark.
                        Reference.asText("nowhere", "See nowhere:"),
                        Reference.asText("a.", "See a.:"),
                        Reference.field("a", "a_2"),
                        // Word reads names without case: a block of drawings declared after a
                        // paragraph of the same name in another case takes a name of its own.
                        Reference.field("Intro", "Intro"),
                        Reference.field("intro", "intro_2")),
                        List.of("it stands on a page no paragraph is written on")),
                new ReferringDocument("margin_sections", PageReferenceDocxDocuments::marginSections, List.of(
                        // A block whose shape a row carries, in a table split where page 2 starts for
                        // its other margins; a block of drawings opening its page over a table split
                        // where page 5 starts; an anchor in a header row the page repeats.
                        Reference.field("carried", "carried"),
                        Reference.field("above", "above"),
                        Reference.field("header", "header")),
                        List.of("written as a Word section that starts at the row page 2 starts with, the table split there",
                                "written as a Word section that starts at the row page 5 starts with, the table split there, "
                                + "the part after it opening with its header row",
                                "anchored in a table cell, the page having no other paragraph",
                                "\"header\" is in a table's header row, which the page repeats")));
    }

    /**
     * Every kind of node that declares an anchor, the references to them in a table of contents on
     * page 1: text on page 2, pictures and a rule on page 3, blocks of drawings alone on page 4, and
     * anchors named by sentences and drawn shapes on page 5.
     */
    private static void anchorKinds(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> {
            page.addTableOfContents(toc -> toc.title("Contents")
                    .entry("A paragraph", "para")
                    .entry("A section", "section")
                    .entry("A container", "container")
                    .entry("A table", "table")
                    .entry("A picture", "image")
                    .entry("A barcode", "barcode")
                    .entry("A rule", "rule")
                    .entry("A section of drawings alone", "drawings")
                    .entry("A container of drawings alone", "drawn box")
                    .entry("Drawings named by a sentence", TERMS)
                    .entry("Text named by a sentence", SCHEDULE));
            page.addParagraph("See the shape:").addPageReference("shape");
            page.addParagraph("See the ellipse:").addPageReference("ellipse");
            page.addPageBreak(pageBreak -> { });

            page.addParagraph(p -> p.text("A paragraph, anchored.").anchor("para"));
            page.addSection(s -> s.anchor("section").addParagraph("A section of text.").addParagraph("Its second line."));
            page.add(new ContainerNode("Box", List.of(new ParagraphBuilder().text("A container of text.").build()), 0,
                    DocumentInsets.zero(), DocumentInsets.zero(), null, null, null, null, "container"));
            page.addTable(t -> t.anchor("table").autoColumns(2).row("Net", "100").row("Tax", "20"));
            page.addPageBreak(pageBreak -> { });

            page.addParagraph("Pictures and a rule.");
            page.addImage(image -> image.source(DocumentImageData.fromBytes(png())).width(40).height(20).anchor("image"));
            page.addBarcode(barcode -> barcode.qrCode().data("GC-PAGEREF").size(60, 60).anchor("barcode"));
            page.addLine(line -> line.horizontal(200).thickness(1).anchor("rule"));
            page.addParagraph("Under the rule.");
            page.addPageBreak(pageBreak -> { });

            lines(page, "Top of page four", 3);
            page.add(drawings("drawings"));
            page.addParagraph("Between the blocks of drawings.");
            page.add(new ContainerNode("Drawn box", List.of(drawings(null)), 0, DocumentInsets.zero(),
                    DocumentInsets.zero(), null, null, null, null, "drawn box"));
            page.addParagraph("After the drawings.");
            page.addPageBreak(pageBreak -> { });

            lines(page, "Top of page five", 2);
            page.add(drawings(TERMS));
            page.addSection(s -> s.anchor(SCHEDULE).addParagraph("The schedule, anchored by a sentence."));
            page.addShape(shape -> shape.size(60, 20).fillColor(INK).anchor("shape"));
            page.addEllipse(ellipse -> ellipse.circle(20).fillColor(ACCENT).anchor("ellipse"));
            page.addParagraph("The last paragraph.");
        });
    }

    /**
     * Page references standing in a table's composed cell, a stack's column, a panel and a row's
     * column, to an anchor on page 2, and one in a composed cell to a drawn shape there; and on page
     * 2, anchors in each of those places, named in a table of contents on page 1.
     */
    private static void referencePlaces(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> {
            page.addTableOfContents(toc -> toc.title("Places")
                    .entry("In a table's cell", "cell")
                    .entry("In a stack's column", "column")
                    .entry("Drawings alone in a stack's column", "drawn column")
                    .entry("In a panel", "panel")
                    .entry("In a row's column", "row"));
            page.addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new SectionBuilder()
                    .addParagraph("From a table's cell:").add(reference("target"))
                    .addParagraph("From a table's cell, to a shape:").add(reference("mark")).build())));
            page.add(new LayerStackBuilder()
                    .layer(new SectionBuilder().margin(LEFT_COLUMN).addParagraph("From a stack's column:")
                            .add(reference("target")).build(), LayerAlign.TOP_LEFT)
                    .layer(new SectionBuilder().margin(RIGHT_COLUMN).addParagraph("Its other column.").build(),
                            LayerAlign.TOP_LEFT)
                    .build());
            page.addSection(panel -> panel.fillColor(SURFACE).padding(DocumentInsets.of(6))
                    .addParagraph("From a panel:").add(reference("target")));
            page.addRow(row -> row.spacing(12)
                    .addSection(column -> column.addParagraph("From a row's column:").add(reference("target")))
                    .addParagraph("Beside it."));
            page.addParagraph("From the body:").addPageReference("target");
            page.addPageBreak(pageBreak -> { });

            page.addParagraph(p -> p.text("The target, on page 2.").anchor("target"));
            page.addTable(t -> t.autoColumns(2).rowCells(
                    DocumentTableCell.node(new ParagraphBuilder().text("In a table's cell.").anchor("cell").build()),
                    DocumentTableCell.text("Beside it.")));
            page.add(new LayerStackBuilder()
                    .layer(new SectionBuilder().anchor("column").margin(LEFT_COLUMN)
                            .addParagraph("In a stack's column.").build(), LayerAlign.TOP_LEFT)
                    .layer(new SectionBuilder().anchor("drawn column").margin(RIGHT_COLUMN)
                            .addCircle(30, INK).build(), LayerAlign.TOP_LEFT)
                    .build());
            page.addSection(panel -> panel.anchor("panel").fillColor(SURFACE).padding(DocumentInsets.of(6))
                    .addParagraph("In a panel."));
            page.addRow(row -> row.spacing(12)
                    .addSection(column -> column.anchor("row").addParagraph("In a row's column."))
                    .addParagraph("Beside it."));
            // Taller than a rule: the export writes a filled shape 12pt tall or less in the flow as
            // a paragraph's border, which holds a bookmark.
            page.addShape(shape -> shape.size(40, 24).fillColor(ACCENT).anchor("mark"));
            page.addParagraph("After the shape.");
        });
    }

    /**
     * Anchors the export writes no bookmark for, each referred to as the number it reads with a
     * note, beside anchors whose names clean or fold to another's.
     */
    private static void unmarked(DocumentSession session) {
        // Tall enough for the shape of 550pt that fills page 3 alone.
        session.pageSize(400, 600).margin(DocumentInsets.of(20));
        session.pageFlow(page -> {
            page.addParagraph(p -> p.text("A link to a name that cleans to another's.").linkTo("a."));
            page.addTableOfContents(toc -> toc.title("Unmarked")
                    .entry("The anchor a", "a")
                    .entry("A paragraph", "Intro")
                    .entry("Drawings named in another case", "intro"));
            page.addParagraph("See empty:").addPageReference("empty");
            page.addParagraph("See tall:").addPageReference("tall");
            page.addParagraph("See composed:").addPageReference("composed");
            page.addParagraph("See nowhere:").addPageReference("nowhere");
            page.addParagraph("See a.:").addPageReference("a.");
            page.addPageBreak(pageBreak -> { });

            page.addParagraph(p -> p.text("The anchor a, on page 2.").anchor("a"));
            page.addParagraph(p -> p.text("Intro, on page 2.").anchor("Intro"));
            page.add(drawings("intro"));
            page.addParagraph("After the drawings named intro.");
            page.addSection(s -> s.anchor("empty"));
            page.addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(drawings("composed"))));
            page.addPageBreak(pageBreak -> { });

            // Its tall shape fills page 3, which no paragraph is written on; its circle goes to page 4.
            page.add(new SectionBuilder().anchor("tall")
                    .addShape(120, 550, DocumentColor.LIGHT_GRAY)
                    .addCircle(24, DocumentColor.LIGHT_GRAY).build());
            page.addParagraph("After the tall drawings.");
        });
    }

    /**
     * Pages with other margins, which the export writes as sections of their own, splitting a table
     * where such a page starts: a block of drawings a row of a split table carries, one opening its
     * page over a table split under a repeated header row, and an anchor in a header row the page
     * repeats.
     */
    private static void marginSections(DocumentSession session) {
        session.pageSize(400, 300).margin(DocumentInsets.of(20));
        session.pageMargins(List.of(
                PageMarginRule.from(2, new DocumentInsets(45, 20, 34, 20)),
                PageMarginRule.from(5, new DocumentInsets(30, 20, 30, 20))));
        session.pageFlow(page -> {
            page.addParagraph("See pages").addPageReference("carried").addPageReference("above")
                    .addPageReference("header");
            // Its shape stands at page 2's foot, which a row of the table's second part carries.
            page.addTable(table -> {
                table.columns(DocumentTableColumn.fixed(200));
                for (int row = 1; row <= 17; row++) {
                    table.rowCells(DocumentTableCell.text("Row " + row));
                }
            });
            page.add(drawings("carried"));
            page.addParagraph("After the drawings a row carries.");
            page.addPageBreak(pageBreak -> { });

            // Opening its page, over a table split where page 5 starts, under a copy of its header row.
            page.add(drawings("above"));
            page.addTable(table -> {
                table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100))
                        .header("Item", "Amount").repeatHeader();
                for (int row = 1; row <= 20; row++) {
                    table.rowCells(DocumentTableCell.text("Row " + row), DocumentTableCell.text(String.valueOf(row)));
                }
            });
            page.addParagraph("After the table.");
            page.addPageBreak(pageBreak -> { });

            // A header row holding an anchor, which the page repeats: the file holds it once.
            page.addTable(table -> {
                table.columns(DocumentTableColumn.fixed(200), DocumentTableColumn.fixed(100))
                        .repeatHeader()
                        .headerCells(DocumentTableCell.node(new ParagraphBuilder().text("Item").anchor("header").build()),
                                DocumentTableCell.text("Amount"));
                for (int row = 1; row <= 20; row++) {
                    table.rowCells(DocumentTableCell.text("Line " + row), DocumentTableCell.text(String.valueOf(row)));
                }
            });
        });
    }

    /** Every page reference a document's flow holds, a table's composed cells included. */
    static int pageReferencesIn(List<DocumentNode> roots) {
        return (int) nodesUnder(roots).stream().filter(PageReferenceNode.class::isInstance).count();
    }

    /**
     * The anchors the nodes of a document's flow declare, each with the kind of the node declaring it:
     * every node whose record has an {@code anchor} of its own, a page reference's, which names one,
     * aside.
     */
    static Map<String, String> anchored(List<DocumentNode> roots) {
        Map<String, String> anchors = new TreeMap<>();
        for (DocumentNode node : nodesUnder(roots)) {
            if (node instanceof PageReferenceNode || !node.getClass().isRecord()) {
                continue;
            }
            for (RecordComponent component : node.getClass().getRecordComponents()) {
                if (component.getName().equals("anchor") && component.getType() == String.class) {
                    try {
                        Object anchor = component.getAccessor().invoke(node);
                        if (anchor != null) {
                            anchors.put((String) anchor, node.nodeKind());
                        }
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return anchors;
    }

    private static List<DocumentNode> nodesUnder(List<DocumentNode> roots) {
        List<DocumentNode> found = new ArrayList<>();
        List<DocumentNode> nodes = new ArrayList<>(roots);
        while (!nodes.isEmpty()) {
            DocumentNode node = nodes.remove(nodes.size() - 1);
            found.add(node);
            nodes.addAll(node.children());
            if (node instanceof TableNode table) {
                table.rows().forEach(row -> row.stream().filter(DocumentTableCell::hasComposedContent)
                        .forEach(cell -> nodes.add(cell.content())));
            }
        }
        return found;
    }

    private static DocumentNode reference(String anchor) {
        return new PageReferenceNode(anchor, DocumentTextStyle.DEFAULT, TextAlign.LEFT);
    }

    private static DocumentNode drawings(String anchor) {
        SectionBuilder section = new SectionBuilder().addShape(120, 30, INK).addCircle(24, ACCENT);
        if (anchor != null) {
            section.anchor(anchor);
        }
        return section.build();
    }

    private static void lines(PageFlowBuilder page, String prefix, int count) {
        for (int line = 1; line <= count; line++) {
            page.addParagraph(prefix + ", line " + line + ".");
        }
    }

    private static byte[] png() {
        BufferedImage image = new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                image.setRGB(x, y, x < 4 ? 0x1A5694 : 0xC83C3C);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
