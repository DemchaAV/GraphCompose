package com.demcha.compose.document.debug.snapshot;

import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphLineGeometry;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.snapshot.LayoutCanvasSnapshot;
import com.demcha.compose.document.snapshot.LayoutDiagnosticSnapshot;
import com.demcha.compose.document.snapshot.LayoutInsetsSnapshot;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshotOptions;
import com.demcha.compose.document.snapshot.LayoutTextLineSnapshot;
import com.demcha.compose.document.snapshot.LayoutTypographySnapshot;
import com.demcha.compose.engine.components.content.text.TextDecoration;
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
import java.util.Map;
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
     * <p>Unchanged since 2.0, and deliberately so: optional diagnostics live on
     * {@link LayoutDiagnosticSnapshot} rather than in this shape, so upgrading
     * GraphCompose never rewrites a baseline a consumer already has on disk.</p>
     */
    public static final String FORMAT_VERSION = "2.0";

    /**
     * Schema version of the diagnostic envelope produced by
     * {@link #extractDiagnostics(LayoutGraph, LayoutSnapshotOptions)}.
     *
     * <p>Versioned independently of {@link #FORMAT_VERSION}: adding a diagnostic
     * section moves this number and leaves the layout snapshot's alone.</p>
     *
     * @since 2.2.2
     */
    public static final String DIAGNOSTIC_FORMAT_VERSION = "1.0";

    /**
     * The decoration each standard-14 face alias asks for. Naming
     * {@code HELVETICA_BOLD} selects the {@code HELVETICA} family and nothing
     * else — the bold comes from the decoration — so the alias is only a
     * substitution when the decoration does not supply the face it named.
     */
    private static final Map<FontName, TextDecoration> FACE_ALIAS_DECORATIONS = Map.ofEntries(
            Map.entry(FontName.HELVETICA_BOLD, TextDecoration.BOLD),
            Map.entry(FontName.HELVETICA_OBLIQUE, TextDecoration.ITALIC),
            Map.entry(FontName.HELVETICA_BOLD_OBLIQUE, TextDecoration.BOLD_ITALIC),
            Map.entry(FontName.TIMES_BOLD, TextDecoration.BOLD),
            Map.entry(FontName.TIMES_ITALIC, TextDecoration.ITALIC),
            Map.entry(FontName.TIMES_BOLD_ITALIC, TextDecoration.BOLD_ITALIC),
            Map.entry(FontName.COURIER_BOLD, TextDecoration.BOLD),
            Map.entry(FontName.COURIER_OBLIQUE, TextDecoration.ITALIC),
            Map.entry(FontName.COURIER_BOLD_OBLIQUE, TextDecoration.BOLD_ITALIC));

    private LayoutGraphSnapshotExtractor() {
    }

    /**
     * Converts a canonical layout graph into a stable snapshot model.
     *
     * <p>This is the snapshot every committed baseline was recorded against, and
     * its shape does not change when diagnostics are requested.</p>
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
                        .toList());
    }

    /**
     * Converts a canonical layout graph into a layout snapshot plus whichever
     * optional diagnostic sections {@code options} asks for.
     *
     * <p>{@link LayoutDiagnosticSnapshot#layout()} is byte-for-byte the snapshot
     * {@link #extract(LayoutGraph)} returns; a section a caller did not ask for is
     * an empty list.</p>
     *
     * @param graph   resolved layout graph
     * @param options which optional sections to include
     * @return layout snapshot wrapped with its diagnostics
     * @since 2.2.2
     */
    public static LayoutDiagnosticSnapshot extractDiagnostics(LayoutGraph graph,
                                                              LayoutSnapshotOptions options) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(options, "options");
        return new LayoutDiagnosticSnapshot(
                DIAGNOSTIC_FORMAT_VERSION,
                extract(graph),
                options.typography() ? toTypography(graph) : List.of());
    }

    /**
     * Projects every resolved paragraph fragment into the typography list.
     *
     * <p>Text is read off fragments rather than nodes because that is where it
     * lives: a paragraph split across a page boundary emits one fragment per
     * page, each with its own lines.</p>
     *
     * <p>Ordered by path, then page, then emission ordinal, so the list does not
     * depend on the order pagination emitted fragments in — a snapshot whose
     * ordering did would churn its baseline every time an unrelated node moved
     * between pages. Page has to come before the ordinal, and has to be in the
     * key at all: a split paragraph restarts {@code fragmentIndex} at zero on
     * each page, so {@code (path, fragmentIndex)} alone is not unique and leaves
     * the tie to emission order, and a split list numbered per page would
     * interleave its pages.</p>
     */
    private static List<LayoutTypographySnapshot> toTypography(LayoutGraph graph) {
        List<LayoutTypographySnapshot> runs = new ArrayList<>();
        for (PlacedFragment fragment : graph.fragments()) {
            if (fragment.payload() instanceof ParagraphFragmentPayload paragraph) {
                runs.add(toTypographySnapshot(fragment, paragraph));
            }
        }
        runs.sort(Comparator.comparing(LayoutTypographySnapshot::path)
                .thenComparingInt(LayoutTypographySnapshot::page)
                .thenComparingInt(LayoutTypographySnapshot::fragmentIndex));
        return List.copyOf(runs);
    }

    private static LayoutTypographySnapshot toTypographySnapshot(PlacedFragment fragment,
                                                                ParagraphFragmentPayload paragraph) {
        Padding padding = paragraph.padding() == null ? Padding.zero() : paragraph.padding();
        double innerX = fragment.x() + padding.left();
        double innerWidth = Math.max(0.0d, fragment.width() - padding.horizontal());
        double contentTop = ParagraphLineGeometry.contentTop(fragment.y(), fragment.height(), padding.top());

        TextStyle base = paragraph.textStyle() == null ? TextStyle.DEFAULT_STYLE : paragraph.textStyle();
        // The base style is what the author declared; the first span is what the engine
        // actually measured, after an autoSize shrink and after any span-level override.
        // Reporting the base size beside line boxes measured at a different one would be
        // a record that contradicts itself.
        TextStyle effective = firstSpanStyle(paragraph, base);
        FontName declared = base.fontName();
        FontName resolvedFamily = FontLibrary.resolveFamily(effective.fontName());
        TextDecoration decoration = effective.decoration() == null
                ? TextDecoration.DEFAULT
                : effective.decoration();
        TextVerticalAlign verticalAlign = paragraph.verticalAlign() == null
                ? TextVerticalAlign.DEFAULT
                : paragraph.verticalAlign();
        // A non-default vertical alignment shifts every baseline in the fragment by a
        // correction read from the backend font's cap height. Nothing renderer-neutral
        // can compute it, so the baselines below are the unseated ones and say so.
        boolean baselineExact = verticalAlign == TextVerticalAlign.DEFAULT;

        List<LayoutTextLineSnapshot> lines = new ArrayList<>(paragraph.lines().size());
        double lineTop = contentTop;
        for (int index = 0; index < paragraph.lines().size(); index++) {
            ParagraphLine line = paragraph.lines().get(index);
            double lineHeight = line.lineHeight();
            double lineX = ParagraphLineGeometry.lineStartX(paragraph.align(), innerX, innerWidth, line.width());
            lines.add(new LayoutTextLineSnapshot(
                    index,
                    normalize(lineX),
                    normalize(lineTop - lineHeight),
                    normalize(line.width()),
                    normalize(lineHeight),
                    normalize(ParagraphLineGeometry.baselineY(lineTop, lineHeight, line.baselineOffsetFromBottom())),
                    baselineExact));
            lineTop = ParagraphLineGeometry.nextLineTop(lineTop, lineHeight, paragraph.lineGap());
        }

        // Derived from the rounded lines, not from a parallel accumulation of raw
        // doubles: rounding the box and its lines independently let a line sit a
        // thousandth outside the box that reports it.
        double boxLeft = lines.stream().mapToDouble(LayoutTextLineSnapshot::x).min()
                .orElseGet(() -> normalize(innerX));
        double boxRight = lines.stream().mapToDouble(line -> line.x() + line.width()).max()
                .orElseGet(() -> normalize(innerX));
        double boxBottom = lines.isEmpty()
                ? normalize(contentTop)
                : lines.get(lines.size() - 1).y();
        double boxTop = lines.isEmpty()
                ? normalize(contentTop)
                : lines.get(0).y() + lines.get(0).height();

        return new LayoutTypographySnapshot(
                fragment.path(),
                fragment.fragmentIndex(),
                fragment.pageIndex(),
                declared == null ? null : declared.name(),
                resolvedFamily.name(),
                decoration.name(),
                isFontSubstituted(declared, resolvedFamily, decoration),
                normalize(effective.size()),
                paragraph.lines().size(),
                boxLeft,
                boxBottom,
                normalize(boxRight - boxLeft),
                normalize(boxTop - boxBottom),
                verticalAlign.name(),
                List.copyOf(lines));
    }

    /**
     * Returns the style the engine actually measured this fragment's text with,
     * falling back to the paragraph's base style when it has no text spans.
     */
    private static TextStyle firstSpanStyle(ParagraphFragmentPayload paragraph, TextStyle base) {
        for (ParagraphLine line : paragraph.lines()) {
            for (ParagraphSpan span : line.spans()) {
                if (span instanceof ParagraphTextSpan textSpan) {
                    return textSpan.textStyle();
                }
            }
        }
        return base;
    }

    /**
     * Reports whether the face the declaration asks for is not the face the text
     * was laid out in.
     *
     * <p>Naming no font at all resolves to Helvetica, which is a rewrite worth
     * reporting. Naming a standard-14 <em>face</em> resolves to its family and
     * leaves the face to the decoration, so it is a rewrite only when the
     * decoration does not supply that face: {@code HELVETICA_BOLD} with
     * {@code BOLD} draws exactly what it named, while {@code HELVETICA_BOLD} with
     * no decoration draws regular.</p>
     */
    private static boolean isFontSubstituted(FontName declared,
                                             FontName resolvedFamily,
                                             TextDecoration decoration) {
        if (declared == null || FontName.DEFAULT.equals(declared)) {
            return true;
        }
        if (resolvedFamily.equals(declared)) {
            return false;
        }
        TextDecoration implied = FACE_ALIAS_DECORATIONS.get(declared);
        if (implied == null) {
            return true;
        }
        return implied != decoration;
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



