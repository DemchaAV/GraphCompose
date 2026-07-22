package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the opt-in {@code keepWithNext()} pagination behavior: a section that
 * would otherwise strand as the last block on its page &mdash; apart from the
 * first line of the block it introduces &mdash; relocates to the next page so the
 * heading stays glued to its content. The rule is inert when nothing follows and
 * best-effort when the heading plus one following line cannot share a page. This
 * is the fix for a boxed section title torn from its body across a page break.
 *
 * <p>Assertions read the page of the inner content <em>leaf</em> (the named shape
 * or paragraph), not the section wrapper: a zero-reservation section whose content
 * spills to the next page still records its box start on the prior page, so the
 * wrapper's start page would not reflect where the content actually lands.</p>
 */
class SectionKeepWithNextTest {

    private static final DocumentColor GREY = DocumentColor.rgb(220, 220, 220);
    private static final DocumentColor INK = DocumentColor.rgb(20, 80, 95);

    private static int page(LayoutGraph graph, String name) {
        PlacedNode node = graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst().orElseThrow();
        return node.startPage();
    }

    /**
     * Header near the page bottom, followed by an atomic body block that does not
     * fit below it: {@code keepWithNext()} pulls the header down to the next page
     * so header and body land on the same page.
     */
    @Test
    void relocatesHeaderToStayWithAtomicBody() {
        LayoutGraph on = atomicBody(true);
        assertThat(page(on, "HeaderMark")).isEqualTo(page(on, "BodyMark"));
    }

    /**
     * Default flow (no opt-in): the header sits at the page bottom while the body
     * that will not fit below it flows to the next page &mdash; the orphan this
     * feature removes. Guards that the behavior is off unless requested.
     */
    @Test
    void withoutKeepWithNextHeaderStrandsFromBody() {
        LayoutGraph off = atomicBody(false);
        assertThat(page(off, "HeaderMark")).isLessThan(page(off, "BodyMark"));
    }

    /**
     * The body is a paragraph far taller than a page, so {@code keepTogether()}
     * would be ignored (nothing can keep a page-spanning block whole). Yet
     * {@code keepWithNext()} still relocates the header, because only the paragraph's
     * <em>first line</em> must join it &mdash; the distinguishing behavior.
     */
    @Test
    void relocatesHeaderBasedOnFirstLineOfPageSpanningBody() {
        LayoutGraph on = paragraphBody(true);
        assertThat(page(on, "HeaderMark")).isEqualTo(page(on, "BodyMark"));

        LayoutGraph off = paragraphBody(false);
        assertThat(page(off, "HeaderMark")).isLessThan(page(off, "BodyMark"));
    }

    /**
     * The body's first unit is a page-spanning table (taller than a page, so
     * {@code keepTogether()} would be inert). {@code keepWithNext()} still
     * relocates the header, because only the table's first slice — its repeated
     * header row plus first body row — must join it. Without a real first-slice
     * estimate the whole-table height would exceed a page and the header would
     * strand, so this is the distinguishing behavior for a splittable table body.
     */
    @Test
    void relocatesHeaderBasedOnFirstSliceOfPageSpanningTable() {
        LayoutGraph on = tableBody(true);
        assertThat(page(on, "HeaderMark")).isEqualTo(page(on, "TableMark"));

        LayoutGraph off = tableBody(false);
        assertThat(page(off, "HeaderMark")).isLessThan(page(off, "TableMark"));
    }

    /**
     * The body's first unit is a page-spanning list. {@code keepWithNext()}
     * relocates the header to join the list's first item, where the whole-list
     * height estimate would otherwise strand it — the list counterpart of the
     * table case above.
     */
    @Test
    void relocatesHeaderBasedOnFirstItemOfPageSpanningList() {
        LayoutGraph on = listBody(true);
        assertThat(page(on, "HeaderMark")).isEqualTo(page(on, "ListMark"));

        LayoutGraph off = listBody(false);
        assertThat(page(off, "HeaderMark")).isLessThan(page(off, "ListMark"));
    }

    /**
     * A multi-part heading — several consecutive keep-with-next siblings (a top
     * rule, a banner, a bottom rule) — relocates as a whole unit: the break is
     * hoisted before the first member, so no part is stranded above the body.
     */
    @Test
    void relocatesWholeHeaderRunNotJustLastElement() {
        LayoutGraph on = headerRun(true);
        int head1 = page(on, "Head1Mark");
        assertThat(page(on, "Head2Mark")).isEqualTo(head1);
        assertThat(page(on, "BodyMark")).isEqualTo(head1);

        LayoutGraph off = headerRun(false);
        // Default flow: both header parts sit at the page bottom while the body strands.
        assertThat(page(off, "Head1Mark")).isEqualTo(page(off, "Head2Mark"));
        assertThat(page(off, "BodyMark")).isGreaterThan(page(off, "Head2Mark"));
    }

    /**
     * A trailing {@code keepWithNext()} header with no following sibling is never
     * relocated: with no line to keep it company there is no orphan to avoid, so
     * the header stays where it lands.
     */
    @Test
    void inertWhenNothingFollows() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    // Leaves a 40pt gap on page 0 — exactly the header height.
                    .addSection("Filler", s -> s.addShape(260, 308, GREY))
                    .addSection("Header", s -> s.keepWithNext()
                            .addShape(shape -> shape.name("HeaderMark").size(260, 40).fillColor(INK)))
                    .build();

            assertThat(page(document.layoutGraph(), "HeaderMark")).isEqualTo(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When the header plus one following line cannot share a page, relocating to a
     * fresh page would not help — so the rule stays inert and the header flows in
     * place rather than pointlessly jumping to a page it still cannot share with
     * its body.
     */
    @Test
    void bestEffortWhenHeaderPlusFirstLineExceedAPage() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(0)
                    // Tiny filler so the page is "used" but the near-full-page header still fits.
                    .addSection("Filler", s -> s.addShape(260, 3, GREY))
                    .addSection("Header", s -> s.keepWithNext()
                            .addShape(shape -> shape.name("HeaderMark").size(260, 355).fillColor(INK)))
                    .addSection("Body", s -> s.addShape(shape -> shape.name("BodyMark").size(260, 80).fillColor(INK)))
                    .build();

            LayoutGraph graph = document.layoutGraph();
            // Header stays on page 0 (not relocated); body still moves, as in default flow.
            assertThat(page(graph, "HeaderMark")).isEqualTo(0);
            assertThat(page(graph, "BodyMark")).isEqualTo(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Filler fills page 0 to a 40pt gap; header (40pt) fits, header + body (80pt) does not. */
    private static LayoutGraph atomicBody(boolean keepWithNext) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    .addSection("Filler", s -> s.addShape(260, 250, GREY))
                    .addSection("Header", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("HeaderMark").size(260, 40).fillColor(INK));
                    })
                    .addSection("Body", s -> s.addShape(shape -> shape.name("BodyMark").size(260, 80).fillColor(INK)))
                    .build();
            return document.layoutGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Two 30pt header parts fit in the ~100pt gap; header run + 80pt body does not. */
    private static LayoutGraph headerRun(boolean keepWithNext) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    .addSection("Filler", s -> s.addShape(260, 248, GREY))
                    .addSection("Head1", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("Head1Mark").size(260, 30).fillColor(INK));
                    })
                    .addSection("Head2", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("Head2Mark").size(260, 30).fillColor(INK));
                    })
                    .addSection("Body", s -> s.addShape(shape -> shape.name("BodyMark").size(260, 80).fillColor(INK)))
                    .build();
            return document.layoutGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Header sits flush to the page bottom; the body is a paragraph taller than a page. */
    private static LayoutGraph paragraphBody(boolean keepWithNext) {
        String pageSpanning = "Experience line. ".repeat(200);
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    // 308 + spacing(12) + header(40) = 360 inner height: header lands flush to the bottom.
                    .addSection("Filler", s -> s.addShape(260, 308, GREY))
                    .addSection("Header", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("HeaderMark").size(260, 40).fillColor(INK));
                    })
                    .addSection("Body", s -> s.addParagraph(p -> p.name("BodyMark").text(pageSpanning)))
                    .build();
            return document.layoutGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Header sits flush to the page bottom; the body is a table taller than a page. */
    private static LayoutGraph tableBody(boolean keepWithNext) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    // 308 + spacing(12) + header(40) = 360 inner height: header lands flush to the bottom.
                    .addSection("Filler", s -> s.addShape(260, 308, GREY))
                    .addSection("Header", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("HeaderMark").size(260, 40).fillColor(INK));
                    })
                    .addSection("Body", s -> s.addTable(t -> {
                        t.name("TableMark").autoColumns(2).header("Column A", "Column B").repeatHeader();
                        for (int i = 0; i < 40; i++) {
                            t.row("Row " + i, "Value " + i);
                        }
                    }))
                    .build();
            return document.layoutGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Header sits flush to the page bottom; the body is a list taller than a page. */
    private static LayoutGraph listBody(boolean keepWithNext) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    // 308 + spacing(12) + header(40) = 360 inner height: header lands flush to the bottom.
                    .addSection("Filler", s -> s.addShape(260, 308, GREY))
                    .addSection("Header", s -> {
                        if (keepWithNext) {
                            s.keepWithNext();
                        }
                        s.addShape(shape -> shape.name("HeaderMark").size(260, 40).fillColor(INK));
                    })
                    .addSection("Body", s -> s.addList(l -> {
                        l.name("ListMark");
                        for (int i = 0; i < 40; i++) {
                            l.addItem("List item number " + i + " with enough text to measure a real line height");
                        }
                    }))
                    .build();
            return document.layoutGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
