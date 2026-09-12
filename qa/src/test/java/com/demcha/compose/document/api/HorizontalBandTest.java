package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.HorizontalBandContentNode;
import com.demcha.compose.document.layout.HorizontalBandsNode;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

/**
 * A column resolved by a row, used by content that comes after it.
 *
 * <p>A row works out where each column starts and how wide it is — from points, from shares,
 * or from a mixture — and then forgets the arithmetic. Content that has to line up with one
 * of those columns and is also long enough to cross a page cannot live inside the row: a row
 * is laid out on one page and nothing about that is changing here. So the row publishes its
 * columns, and a later sibling lays itself out inside one of them while staying an ordinary
 * vertical block.</p>
 *
 * <p>The published band is the <b>slot</b>, before the child in it applies its own margin, so
 * it means the same thing whatever the column happened to contain. Everything here is
 * asserted against the row's own resolved geometry rather than against numbers computed a
 * second way — the point of the mechanism is that there is only one copy of the formula.</p>
 */
class HorizontalBandTest {

    // --- the geometry the row resolved -----------------------------------------

    @Test
    void contentTakesTheColumnsXAndWidthFromTheRowThatResolvedIt() {
        // Fixed first column: the band is the slot the row gave it, and the content that
        // follows starts exactly there and is exactly that wide.
        Object key = new Object();
        LayoutGraph graph = document(360, 300, flow -> {
            flow.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.fixed(80), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("left"));
                r.addSection(cell -> cell.addParagraph("right"));
            })));
            flow.add(new HorizontalBandContentNode("", key, 1,
                    section(s -> s.addParagraph("in the second column"))));
        });

        PlacedNode secondColumn = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.placementX())
                .as("the column's own x, not the page's")
                .isEqualTo(secondColumn.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .as("and it ends where the row ends, so the width is the column's")
                .isEqualTo(rowRightEdge(graph), within(1e-9));
    }

    @Test
    void aWeightedColumnWorksTheSameWayAndIsNotRecomputed() {
        // The case a build-time indent cannot express: the column is a share of what the row
        // has left, and nobody knows the number until the row is laid out. Read back, it is
        // the same number the row used.
        Object key = new Object();
        LayoutGraph graph = document(360, 300, flow -> {
            flow.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.weight(0.25), DocumentRowColumn.weight(0.75));
                r.addSection(cell -> cell.addParagraph("left"));
                r.addSection(cell -> cell.addParagraph("right"));
            })));
            flow.add(new HorizontalBandContentNode("", key, 1,
                    section(s -> s.addParagraph("in the weighted column"))));
        });

        PlacedNode secondColumn = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.placementX()).isEqualTo(secondColumn.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .isEqualTo(rowRightEdge(graph), within(1e-9));
        assertThat(content.placementWidth())
                .as("the premise: a share of the row, not the whole of it")
                .isLessThan(320.0);
    }

    @Test
    void aMixedRowResolvesEachColumnAndTheThirdIsStillTheThird() {
        // Points, a share and a share: the arrangement a leading column produces. Asserted
        // against all three of the row's own columns so a mechanism that quietly counted
        // slots differently would not agree.
        Object key = new Object();
        LayoutGraph graph = document(420, 300, flow -> {
            flow.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(0.15),
                        DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("date"));
                r.addSection(cell -> cell.addParagraph("mark"));
                r.addSection(cell -> cell.addParagraph("title"));
            })));
            flow.add(new HorizontalBandContentNode("", key, 2,
                    section(s -> s.addParagraph("under the third column"))));
        });

        PlacedNode third = rowColumn(graph, 2);
        PlacedNode content = bandContent(graph);
        assertThat(content.placementX()).isEqualTo(third.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .isEqualTo(rowRightEdge(graph), within(1e-9));
        assertThat(content.placementX())
                .as("and it really is the third: well right of the first two")
                .isGreaterThan(rowColumn(graph, 1).placementX());
    }

    // --- pagination -------------------------------------------------------------

    @Test
    void aLongBodyCrossesPagesAndKeepsTheSameColumnOnEveryOne() {
        // The whole reason the content is not in the row. Three pages, one x, one width, and
        // no complaint that an atomic block does not fit.
        Object key = new Object();
        LayoutGraph graph = document(320, 150, flow -> {
            flow.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("mark"));
                r.addSection(cell -> cell.addParagraph("title"));
            })));
            flow.add(new HorizontalBandContentNode("", key, 1,
                    section(s -> s.addParagraph(longBody(60)))));
        });

        assertThat(graph.totalPages()).as("the premise: it spans pages").isGreaterThanOrEqualTo(3);
        PlacedNode column = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.startPage()).isZero();
        assertThat(content.endPage()).as("one block, several pages").isGreaterThanOrEqualTo(2);
        assertThat(content.placementX())
                .as("no horizontal drift onto the continuation pages")
                .isEqualTo(column.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .as("and the width is not reset to the parent's on continuation")
                .isEqualTo(rowRightEdge(graph), within(1e-9));

        // Read off the text itself rather than the wrapper: every line of every page starts
        // at the column, which is what a reader would check.
        List<PlacedNode> lines = graph.nodes().stream()
                .filter(n -> "ParagraphNode".equals(n.nodeKind()))
                .filter(n -> n.placementWidth() > 1.0)
                .filter(n -> n.startPage() > 0 || n.placementY() < column.placementY())
                .toList();
        assertThat(lines).isNotEmpty();
        assertThat(lines).allSatisfy(line -> assertThat(line.placementX())
                .isEqualTo(column.placementX(), within(1e-9)));
    }

    @Test
    void aPageBreakBetweenTheRowAndItsContentDoesNotLoseTheColumn() {
        // The row is on one page and the content starts on the next. The column is a
        // horizontal fact and pages are a vertical one, so the break is irrelevant — but
        // only if the band outlives it, which is the point of storing it for the whole
        // compilation rather than for the page it was resolved on.
        Object key = new Object();
        LayoutGraph graph = document(320, 150, flow -> {
            flow.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("mark"));
                r.addSection(cell -> cell.addParagraph("title"));
            })));
            flow.addParagraph(longBody(14));
            flow.add(new HorizontalBandContentNode("", key, 1, section(s -> s.addParagraph("after the break"))));
        });

        PlacedNode column = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.startPage()).as("the premise: it starts on a later page").isGreaterThan(0);
        assertThat(content.placementX()).isEqualTo(column.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .isEqualTo(rowRightEdge(graph), within(1e-9));
    }

    // --- relocation ---------------------------------------------------------------

    @Test
    void aKeptTogetherBlockThatRelocatesPublishesItsColumnsOnceAndUsesThem() {
        // A block that does not fit in what is left of the page but fits on a fresh one.
        // Whether that costs a false "two rows under one identity" depends on something the
        // mechanism cannot see from the outside: whether the engine decides to move the
        // block before compiling it, or compiles it and then abandons the attempt. Only the
        // first is safe for anything a compile records, so it is asserted rather than
        // assumed — and the assertion is that this lays out at all.
        Object key = new Object();
        LayoutGraph graph = document(320, 200, flow -> {
            for (int i = 0; i < 6; i++) {
                flow.addParagraph("Filler line " + i + " taking a line of its own on this page.");
            }
            flow.addSection(block -> {
                block.keepTogether();
                block.add(new HorizontalBandsNode("", key, row(r -> {
                    r.columns(DocumentRowColumn.fixed(70), DocumentRowColumn.weight(1.0));
                    r.addSection(cell -> cell.addParagraph("mark"));
                    r.addSection(cell -> cell.addParagraph("title"));
                })));
                block.add(new HorizontalBandContentNode("", key, 1,
                        section(s -> s.addParagraph("A body of two lines, kept with its row."))));
            });
        });

        PlacedNode column = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.startPage())
                .as("the premise: the block did move to a fresh page")
                .isGreaterThan(0);
        assertThat(column.startPage())
                .as("and the row moved with it, which is what kept-together means")
                .isEqualTo(content.startPage());
        assertThat(content.placementX())
                .as("the column of the placement that survived, not of an abandoned one")
                .isEqualTo(column.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .isEqualTo(rowRightEdge(graph), within(1e-9));
    }

    @Test
    void theSameRelocationWithSharesInsteadOfPoints() {
        // The half that cannot be checked at build time: the columns are shares, so the
        // number only exists after the row is laid out — and after it is laid out on the
        // page it ended up on.
        Object key = new Object();
        LayoutGraph graph = document(320, 200, flow -> {
            for (int i = 0; i < 6; i++) {
                flow.addParagraph("Filler line " + i + " taking a line of its own on this page.");
            }
            flow.addSection(block -> {
                block.keepTogether();
                block.add(new HorizontalBandsNode("", key, row(r -> {
                    r.columns(DocumentRowColumn.weight(0.3), DocumentRowColumn.weight(0.7));
                    r.addSection(cell -> cell.addParagraph("mark"));
                    r.addSection(cell -> cell.addParagraph("title"));
                })));
                block.add(new HorizontalBandContentNode("", key, 1,
                        section(s -> s.addParagraph("A body of two lines, kept with its row."))));
            });
        });

        PlacedNode column = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.startPage()).isGreaterThan(0);
        assertThat(content.placementX()).isEqualTo(column.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .isEqualTo(rowRightEdge(graph), within(1e-9));
    }

    @Test
    void aDocumentLaidOutMoreThanOncePublishesItsColumnsAfreshEachTime() {
        // A page reference makes the whole document a fixed point: it is compiled, the page
        // numbers are read off, and it is compiled again until they stop moving. Each of
        // those is a compile, and each republishes the same identity — which is only safe
        // because what a compile records belongs to that compile and nothing else.
        Object key = new Object();
        LayoutGraph graph = document(320, 220, flow -> {
            flow.addPageReference("later");
            flow.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.fixed(70), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("mark"));
                r.addSection(cell -> cell.addParagraph("title"));
            })));
            flow.add(new HorizontalBandContentNode("", key, 1,
                    section(s -> s.addParagraph("A body under the second column."))));
            flow.addParagraph("filler").addParagraph("filler").addParagraph("filler");
            flow.addSection("later", s -> s.addParagraph("The anchor this refers to."));
        });

        PlacedNode column = rowColumn(graph, 1);
        PlacedNode content = bandContent(graph);
        assertThat(content.placementX()).isEqualTo(column.placementX(), within(1e-9));
        assertThat(content.placementX() + content.placementWidth())
                .isEqualTo(rowRightEdge(graph), within(1e-9));
    }

    // --- identity ----------------------------------------------------------------

    @Test
    void twoOwnersOnOnePageKeepTheirColumnsApart() {
        // Same shape, same slot index, two identities: each consumer gets its own row's
        // column. Nothing here is distinguishable by name, path or index — only by object.
        Object first = new Object();
        Object second = new Object();
        LayoutGraph graph = document(400, 400, flow -> {
            flow.add(new HorizontalBandsNode("", first, row(r -> {
                r.columns(DocumentRowColumn.fixed(40), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("a"));
                r.addSection(cell -> cell.addParagraph("A"));
            })));
            flow.add(new HorizontalBandContentNode("", first, 1, section(s -> s.addParagraph("under A"))));
            flow.add(new HorizontalBandsNode("", second, row(r -> {
                r.columns(DocumentRowColumn.fixed(180), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("b"));
                r.addSection(cell -> cell.addParagraph("B"));
            })));
            flow.add(new HorizontalBandContentNode("", second, 1, section(s -> s.addParagraph("under B"))));
        });

        List<PlacedNode> contents = graph.nodes().stream()
                .filter(n -> "HorizontalBandContentNode".equals(n.nodeKind())).toList();
        assertThat(contents).hasSize(2);
        assertThat(contents.get(0).placementX())
                .as("the first consumer took the first row's column")
                .isEqualTo(rowColumn(graph, 1, 0).placementX(), within(1e-9));
        assertThat(contents.get(1).placementX())
                .as("and the second the second's, 140pt further in")
                .isEqualTo(rowColumn(graph, 1, 1).placementX(), within(1e-9));
        assertThat(contents.get(1).placementX() - contents.get(0).placementX())
                .isEqualTo(140.0, within(1e-9));
    }

    @Test
    void anOwnerInsideASectionIsStillItsOwnOwner() {
        // Nested producers: one at the top level and one inside a padded section. The inner
        // pair is offset by the padding, the outer is not, and neither reads the other.
        Object outer = new Object();
        Object inner = new Object();
        LayoutGraph graph = document(400, 400, flow -> {
            flow.add(new HorizontalBandsNode("", outer, row(r -> {
                r.columns(DocumentRowColumn.fixed(50), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("o"));
                r.addSection(cell -> cell.addParagraph("O"));
            })));
            flow.add(new HorizontalBandContentNode("", outer, 1, section(s -> s.addParagraph("outer"))));
            flow.addSection(card -> {
                card.padding(DocumentInsets.of(24));
                card.add(new HorizontalBandsNode("", inner, row(r -> {
                    r.columns(DocumentRowColumn.fixed(50), DocumentRowColumn.weight(1.0));
                    r.addSection(cell -> cell.addParagraph("i"));
                    r.addSection(cell -> cell.addParagraph("I"));
                })));
                card.add(new HorizontalBandContentNode("", inner, 1, section(s -> s.addParagraph("inner"))));
            });
        });

        List<PlacedNode> contents = graph.nodes().stream()
                .filter(n -> "HorizontalBandContentNode".equals(n.nodeKind())).toList();
        assertThat(contents).hasSize(2);
        assertThat(contents.get(1).placementX() - contents.get(0).placementX())
                .as("the card's padding moved the inner pair together, and only that")
                .isEqualTo(24.0, within(1e-9));
    }

    // --- containers ----------------------------------------------------------------

    @Test
    void aContainerInsetMovesTheRowAndTheContentTogether() {
        // Padding, margin and a card: the band carries the geometry the row was actually
        // laid out with, so an inset that moves the row moves the content by the same
        // number. Nothing derives an x from the page.
        double plain = bandContentX(section -> { });
        assertThat(bandContentX(section -> section.padding(DocumentInsets.of(16))))
                .as("padding")
                .isEqualTo(plain + 16.0, within(1e-9));
        assertThat(bandContentX(section -> section.margin(DocumentInsets.of(12))))
                .as("margin")
                .isEqualTo(plain + 12.0, within(1e-9));
        assertThat(bandContentX(section -> section.padding(DocumentInsets.of(10)).cornerRadius(6)))
                .as("a card")
                .isEqualTo(plain + 10.0, within(1e-9));
    }

    // --- fail closed -------------------------------------------------------------------

    @Test
    void contentThatNamesAColumnNobodyPublishedIsRefused() {
        Object never = new Object();
        assertThatIllegalStateException()
                .isThrownBy(() -> document(360, 300, flow ->
                        flow.add(new HorizontalBandContentNode("", never, 0,
                                section(s -> s.addParagraph("orphan"))))))
                .withMessageContaining("never")
                .withMessageContaining("published");
    }

    @Test
    void contentThatComesBeforeItsRowIsRefused() {
        // Not a fallback to the parent's width: the row that would answer has not been laid
        // out yet, and guessing would be a layout that looks deliberate and is not.
        Object key = new Object();
        assertThatIllegalStateException()
                .isThrownBy(() -> document(360, 300, flow -> {
                    flow.add(new HorizontalBandContentNode("", key, 1, section(s -> s.addParagraph("early"))));
                    flow.add(new HorizontalBandsNode("", key, row(r -> {
                        r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(1.0));
                        r.addSection(cell -> cell.addParagraph("a"));
                        r.addSection(cell -> cell.addParagraph("b"));
                    })));
                }))
                .withMessageContaining("laid out before it");
    }

    @Test
    void twoRowsUnderOneIdentityAreRefused() {
        Object key = new Object();
        assertThatIllegalStateException()
                .isThrownBy(() -> document(360, 300, flow -> {
                    for (int i = 0; i < 2; i++) {
                        flow.add(new HorizontalBandsNode("", key, row(r -> {
                            r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(1.0));
                            r.addSection(cell -> cell.addParagraph("a"));
                            r.addSection(cell -> cell.addParagraph("b"));
                        })));
                    }
                }))
                .withMessageContaining("under one identity");
    }

    @Test
    void aColumnTheRowDoesNotHaveIsRefused() {
        Object key = new Object();
        assertThatIllegalStateException()
                .isThrownBy(() -> document(360, 300, flow -> {
                    flow.add(new HorizontalBandsNode("", key, row(r -> {
                        r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(1.0));
                        r.addSection(cell -> cell.addParagraph("a"));
                        r.addSection(cell -> cell.addParagraph("b"));
                    })));
                    flow.add(new HorizontalBandContentNode("", key, 5, section(s -> s.addParagraph("nope"))));
                }))
                .withMessageContaining("asks for column 5");
    }

    @Test
    void wrappingSomethingWithoutColumnsIsRefusedWhereItIsWritten() {
        // At construction, not at layout: a wrapper around a paragraph would publish nothing
        // and leave the consumer to fail later with a puzzle instead of the mistake.
        assertThat(catchIllegalArgument(() -> new HorizontalBandsNode("", new Object(),
                new ParagraphBuilder().text("not a row").build())))
                .contains("has none");
    }

    // --- helpers ---------------------------------------------------------------------------

    private static String catchIllegalArgument(Runnable action) {
        try {
            action.run();
            return "";
        } catch (IllegalArgumentException expected) {
            return String.valueOf(expected.getMessage());
        }
    }

    private static double bandContentX(Consumer<SectionBuilder> shape) {
        Object key = new Object();
        LayoutGraph graph = document(400, 300, flow -> flow.addSection(container -> {
            shape.accept(container);
            container.add(new HorizontalBandsNode("", key, row(r -> {
                r.columns(DocumentRowColumn.fixed(60), DocumentRowColumn.weight(1.0));
                r.addSection(cell -> cell.addParagraph("a"));
                r.addSection(cell -> cell.addParagraph("b"));
            })));
            container.add(new HorizontalBandContentNode("", key, 1, section(s -> s.addParagraph("body"))));
        }));
        return bandContent(graph).placementX();
    }

    private static DocumentNode row(Consumer<RowBuilder> spec) {
        RowBuilder builder = new RowBuilder();
        spec.accept(builder);
        return builder.build();
    }

    private static DocumentNode section(Consumer<SectionBuilder> spec) {
        SectionBuilder builder = new SectionBuilder();
        spec.accept(builder);
        return builder.build();
    }

    private static String longBody(int sentences) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < sentences; i++) {
            body.append("Sentence ").append(i).append(" of a body that keeps going. ");
        }
        return body.toString();
    }

    /** Where the only row's content ends — the right edge every last column shares. */
    private static double rowRightEdge(LayoutGraph graph) {
        PlacedNode row = graph.nodes().stream()
                .filter(n -> "RowNode".equals(n.nodeKind()))
                .findFirst().orElseThrow();
        return row.placementX() + row.placementWidth();
    }

    /** The n-th column of the only row, as the row placed it. */
    private static PlacedNode rowColumn(LayoutGraph graph, int index) {
        return rowColumn(graph, index, 0);
    }

    private static PlacedNode rowColumn(LayoutGraph graph, int index, int rowOrdinal) {
        List<PlacedNode> columns = graph.nodes().stream()
                .filter(n -> n.parentPath() != null && n.parentPath().matches(".*RowNode\\[\\d+]$"))
                .filter(n -> n.childIndex() == index)
                .toList();
        return columns.get(rowOrdinal);
    }

    private static PlacedNode bandContent(LayoutGraph graph) {
        return graph.nodes().stream()
                .filter(n -> "HorizontalBandContentNode".equals(n.nodeKind()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no band content in the graph"));
    }

    private static LayoutGraph document(double width, double height, Consumer<PageFlowBuilder> content) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(width, height).margin(DocumentInsets.of(20)).create()) {
            PageFlowBuilder flow = session.pageFlow();
            content.accept(flow);
            flow.build();
            return session.layoutGraph();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("layout failed", failure);
        }
    }
}
