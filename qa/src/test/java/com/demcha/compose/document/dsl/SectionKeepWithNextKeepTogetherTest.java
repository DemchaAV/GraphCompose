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
 * Verifies that the {@code keepWithNext()} lookahead accounts for a following
 * sibling that asked to be kept whole.
 *
 * <p>The two rules are independent mechanisms that used to disagree. The
 * lookahead asks "does the heading plus the next block's <em>first line</em>
 * fit?", while a {@code keepTogether()} block has no first line to seam after —
 * it relocates entire. So the lookahead answered "yes, one line fits", left the
 * heading in place, and the compiler then moved the whole block to the next
 * page, stranding the heading it was supposed to protect.</p>
 *
 * <p>The window is narrow and depends on the filler height, which is why this
 * sweeps rather than pinning a single case: at 300&times;400 with 20pt margins
 * and a 200pt body the orphan appeared for every filler from 120 to 210pt and
 * nowhere else. A single sample would pass against the buggy compiler for most
 * choices of filler.</p>
 *
 * <p>Assertions read the page of the inner content <em>leaf</em>, not the
 * section wrapper — see {@link SectionKeepWithNextTest} for why the wrapper's
 * start page does not reflect where its content lands.</p>
 */
class SectionKeepWithNextKeepTogetherTest {

    private static final DocumentColor GREY = DocumentColor.rgb(220, 220, 220);
    private static final DocumentColor INK = DocumentColor.rgb(20, 80, 95);

    /** Page 300x400 with 20pt margins leaves 360pt of inner height. */
    private static final double PAGE_WIDTH = 300;
    private static final double PAGE_HEIGHT = 400;
    private static final double INNER_HEIGHT = 360;

    private static int page(LayoutGraph graph, String name) {
        PlacedNode node = graph.nodes().stream()
                .filter(n -> name.equals(n.semanticName()))
                .findFirst().orElseThrow();
        return node.startPage();
    }

    /**
     * A {@code keepWithNext()} header above a two-part body that fits on a page.
     *
     * @param bodyKeepTogether whether the body relocates whole instead of splitting
     * @param fillerHeight     height of the block that pushes the header down the page
     * @return the compiled layout graph
     */
    private static LayoutGraph build(boolean bodyKeepTogether, double fillerHeight) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(6)
                    .addSection("Filler", s -> s.addShape(260, fillerHeight, GREY))
                    .addSection("Header", s -> s.keepWithNext()
                            .addShape(shape -> shape.name("HeaderMark")
                                    .size(260, 30).fillColor(INK)))
                    .addSection("Body", s -> s
                            .keepTogether(bodyKeepTogether)
                            .spacing(0)
                            .addShape(shape -> shape.name("BodyMark")
                                    .size(260, 100).fillColor(INK))
                            .addShape(shape -> shape.name("BodyTail")
                                    .size(260, 100).fillColor(GREY)))
                    .build();
            return document.layoutGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The regression. Before the fix the header stranded for every filler height
     * from 120 to 210pt: the lookahead counted only the body's first 100pt shape,
     * decided one line fit, and the compiler then moved all 200pt of the body.
     */
    @Test
    void headerStaysWithAKeepTogetherBodyAtEveryFillerHeight() {
        for (double filler = 10; filler <= 330; filler += 10) {
            LayoutGraph graph = build(true, filler);
            assertThat(page(graph, "HeaderMark"))
                    .describedAs("filler=%.0f: a keepTogether body relocates whole, "
                                 + "so the header must travel with it", filler)
                    .isEqualTo(page(graph, "BodyMark"));
        }
    }

    /**
     * The same sweep with a splittable body, which never regressed. Pins that the
     * fix did not buy the atomic case at the cost of the ordinary one: a body that
     * can split still anchors the header by its first slice alone.
     */
    @Test
    void headerStaysWithASplittableBodyAtEveryFillerHeight() {
        for (double filler = 10; filler <= 330; filler += 10) {
            LayoutGraph graph = build(false, filler);
            assertThat(page(graph, "HeaderMark"))
                    .describedAs("filler=%.0f: a splittable body's first slice "
                                 + "travels with the header", filler)
                    .isEqualTo(page(graph, "BodyMark"));
        }
    }

    /**
     * A body that is taller than a page has its keep-together ignored by the
     * compiler, so the lookahead must keep estimating it by its first slice. This
     * is the case the fix must NOT change — sizing it by the whole block would
     * exceed the page, fail the {@code needed <= activeInnerHeight} guard, and
     * strand headers that relocate correctly today.
     */
    @Test
    void pageSpanningKeepTogetherBodyStillAnchorsByItsFirstSlice() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(6)
                    .addSection("Filler", s -> s.addShape(260, 300, GREY))
                    .addSection("Header", s -> s.keepWithNext()
                            .addShape(shape -> shape.name("HeaderMark")
                                    .size(260, 30).fillColor(INK)))
                    .addSection("Body", s -> s.keepTogether(true).spacing(0)
                            .addParagraph(p -> p.name("BodyMark")
                                    .text(("A page-spanning paragraph. ").repeat(220))
                                    .margin(DocumentInsets.zero())))
                    .build();
            LayoutGraph graph = document.layoutGraph();
            assertThat(page(graph, "HeaderMark"))
                    .describedAs("keep-together is inert above a page-spanning body, "
                                 + "so the first line still anchors the header")
                    .isEqualTo(page(graph, "BodyMark"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Best-effort boundary: when the header and the whole block cannot share even
     * an empty page, {@code keepWithNext} declines to relocate rather than strand
     * the header on a page of its own. Documents the limit instead of leaving it
     * to be discovered.
     */
    @Test
    void headerDoesNotRelocateWhenItCannotShareAPageWithTheWholeBody() {
        double bodyHeight = INNER_HEIGHT - 20;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(6)
                    .addSection("Filler", s -> s.addShape(260, 100, GREY))
                    .addSection("Header", s -> s.keepWithNext()
                            .addShape(shape -> shape.name("HeaderMark")
                                    .size(260, 30).fillColor(INK)))
                    .addSection("Body", s -> s.keepTogether(true).spacing(0)
                            .addShape(shape -> shape.name("BodyMark")
                                    .size(260, bodyHeight).fillColor(INK)))
                    .build();
            LayoutGraph graph = document.layoutGraph();
            assertThat(page(graph, "HeaderMark"))
                    .describedAs("header + body exceed a full page, so the rule stays "
                                 + "best-effort and the header holds its place")
                    .isLessThan(page(graph, "BodyMark"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
