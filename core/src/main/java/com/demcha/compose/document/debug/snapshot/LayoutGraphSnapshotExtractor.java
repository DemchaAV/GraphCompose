package com.demcha.compose.document.debug.snapshot;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphLineGeometry;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.snapshot.LayoutCanvasSnapshot;
import com.demcha.compose.document.snapshot.LayoutInsetsSnapshot;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.snapshot.LayoutTextLineSnapshot;
import com.demcha.compose.document.snapshot.LayoutTypographySnapshot;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Converts the canonical layout graph into the existing snapshot format.
 *
 * @author Artem Demchyshyn
 */
public final class LayoutGraphSnapshotExtractor {
    /**
     * Snapshot format version emitted by the canonical graph extractor.
     *
     * <p>2.1 adds the {@code typography} list. Node entries are unchanged, so a
     * reader that only looks at nodes needs no update.</p>
     */
    public static final String FORMAT_VERSION = "2.1";

    private LayoutGraphSnapshotExtractor() {
    }

    /**
     * Converts a canonical layout graph into a stable snapshot model.
     *
     * @param graph resolved layout graph
     * @return layout snapshot
     */
    public static LayoutSnapshot extract(LayoutGraph graph) {
        Objects.requireNonNull(graph, "graph");
        return new LayoutSnapshot(
                FORMAT_VERSION,
                new LayoutCanvasSnapshot(
                        normalize(graph.canvas().width()),
                        normalize(graph.canvas().height()),
                        normalize(graph.canvas().innerWidth()),
                        normalize(graph.canvas().innerHeight()),
                        from(graph.canvas().margin())),
                graph.totalPages(),
                graph.nodes().stream()
                        .map(LayoutGraphSnapshotExtractor::toNodeSnapshot)
                        .toList(),
                toTypography(graph));
    }

    /**
     * Projects every resolved paragraph fragment into the typography list.
     *
     * <p>Text is read off fragments rather than nodes because that is where it
     * lives: a paragraph split across a page boundary emits one fragment per
     * page, each with its own lines. Sorted by path then fragment index so the
     * list is deterministic regardless of the order pagination emitted them in —
     * a snapshot whose ordering depended on that would churn its baseline every
     * time an unrelated node moved between pages.</p>
     */
    private static List<LayoutTypographySnapshot> toTypography(LayoutGraph graph) {
        List<LayoutTypographySnapshot> runs = new ArrayList<>();
        for (PlacedFragment fragment : graph.fragments()) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                runs.add(toTypographySnapshot(fragment, paragraph));
            }
        }
        runs.sort(Comparator.comparing(LayoutTypographySnapshot::path)
                .thenComparingInt(LayoutTypographySnapshot::fragmentIndex));
        return List.copyOf(runs);
    }

    private static LayoutTypographySnapshot toTypographySnapshot(PlacedFragment fragment,
                                                                ParagraphFragmentPayload paragraph) {
        Padding padding = paragraph.padding() == null ? Padding.zero() : paragraph.padding();
        double innerX = fragment.x() + padding.left();
        double innerWidth = Math.max(0.0d, fragment.width() - padding.horizontal());
        double contentTop = ParagraphLineGeometry.contentTop(fragment.y(), fragment.height(), padding.top());

        TextStyle style = paragraph.textStyle() == null ? TextStyle.DEFAULT_STYLE : paragraph.textStyle();
        FontName declared = style.fontName();
        FontName resolved = FontLibrary.resolveFamily(declared);
        // A non-default vertical alignment shifts every baseline in the fragment by a
        // correction read from the backend font's cap height. Nothing renderer-neutral
        // can compute it, so the baselines below are the unseated ones and say so.
        boolean baselineExact = paragraph.verticalAlign() == TextVerticalAlign.DEFAULT;

        List<LayoutTextLineSnapshot> lines = new ArrayList<>(paragraph.lines().size());
        double lineTop = contentTop;
        // The text box is the box the ink occupies, not the box it was laid out into.
        // Mixing the two — a content-box x beside an ink width — produced a rectangle
        // that did not contain its own lines whenever alignment moved them.
        double inkLeft = Double.POSITIVE_INFINITY;
        double inkRight = Double.NEGATIVE_INFINITY;
        double inkTop = contentTop;
        double inkBottom = contentTop;
        for (int index = 0; index < paragraph.lines().size(); index++) {
            ParagraphLine line = paragraph.lines().get(index);
            double lineHeight = line.lineHeight();
            double lineX = ParagraphLineGeometry.lineStartX(paragraph.align(), innerX, innerWidth, line.width());
            double lineBottom = lineTop - lineHeight;
            lines.add(new LayoutTextLineSnapshot(
                    index,
                    normalize(lineX),
                    normalize(lineBottom),
                    normalize(line.width()),
                    normalize(lineHeight),
                    normalize(ParagraphLineGeometry.baselineY(lineTop, lineHeight, line.baselineOffsetFromBottom())),
                    baselineExact));
            if (index == 0) {
                inkTop = lineTop;
            }
            inkLeft = Math.min(inkLeft, lineX);
            inkRight = Math.max(inkRight, lineX + line.width());
            inkBottom = lineBottom;
            lineTop = ParagraphLineGeometry.nextLineTop(lineTop, lineHeight, paragraph.lineGap());
        }
        if (paragraph.lines().isEmpty()) {
            inkLeft = innerX;
            inkRight = innerX;
        }

        return new LayoutTypographySnapshot(
                fragment.path(),
                fragment.fragmentIndex(),
                fragment.pageIndex(),
                declared == null ? null : declared.name(),
                resolved.name(),
                declared != null && !resolved.equals(declared),
                normalize(style.size()),
                paragraph.lines().size(),
                normalize(inkLeft),
                normalize(inkBottom),
                normalize(inkRight - inkLeft),
                normalize(inkTop - inkBottom),
                (paragraph.verticalAlign() == null ? TextVerticalAlign.DEFAULT : paragraph.verticalAlign()).name(),
                List.copyOf(lines));
    }

    private static LayoutNodeSnapshot toNodeSnapshot(PlacedNode node) {
        return new LayoutNodeSnapshot(
                node.path(),
                node.semanticName(),
                node.nodeKind(),
                node.parentPath(),
                node.childIndex(),
                node.depth(),
                node.layer(),
                normalize(node.computedX()),
                normalize(node.computedY()),
                normalize(node.placementX()),
                normalize(node.placementY()),
                normalize(node.placementWidth()),
                normalize(node.placementHeight()),
                node.startPage(),
                node.endPage(),
                normalize(node.contentWidth()),
                normalize(node.contentHeight()),
                from(node.margin()),
                from(node.padding()));
    }

    private static LayoutInsetsSnapshot from(Margin margin) {
        Margin safeMargin = margin == null ? Margin.zero() : margin;
        return new LayoutInsetsSnapshot(
                normalize(safeMargin.top()),
                normalize(safeMargin.right()),
                normalize(safeMargin.bottom()),
                normalize(safeMargin.left()));
    }

    private static LayoutInsetsSnapshot from(Padding padding) {
        Padding safePadding = padding == null ? Padding.zero() : padding;
        return new LayoutInsetsSnapshot(
                normalize(safePadding.top()),
                normalize(safePadding.right()),
                normalize(safePadding.bottom()),
                normalize(safePadding.left()));
    }

    static double normalize(double value) {
        if (Math.abs(value) < 0.0005d) {
            return 0.0d;
        }
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }
}



