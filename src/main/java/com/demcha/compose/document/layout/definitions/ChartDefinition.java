package com.demcha.compose.document.layout.definitions;

import com.demcha.compose.document.chart.ChartDefaults;
import com.demcha.compose.document.chart.ChartLayoutResolver;
import com.demcha.compose.document.chart.ChartPrimitive;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.chart.ChartStyle;
import com.demcha.compose.document.chart.ChartTheme;
import com.demcha.compose.document.layout.BoxConstraints;
import com.demcha.compose.document.layout.ChartTextMetricsSupport;
import com.demcha.compose.document.layout.FragmentContext;
import com.demcha.compose.document.layout.FragmentPlacement;
import com.demcha.compose.document.layout.LayoutFragment;
import com.demcha.compose.document.layout.MeasureResult;
import com.demcha.compose.document.layout.NodeDefinition;
import com.demcha.compose.document.layout.PaginationPolicy;
import com.demcha.compose.document.layout.PrepareContext;
import com.demcha.compose.document.layout.PreparedNode;
import com.demcha.compose.document.layout.PreparedNodeLayout;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NodeDefinition} for {@link ChartNode}. The whole point of the chart
 * subsystem lives here and only here: a chart is <em>compiled into existing
 * primitives</em> during {@code prepare}, never carried into a render backend.
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Coalesce the effective {@link ChartStyle} over the built-in
 *       {@link ChartDefaults#DEFAULT_THEME} tokens (the active document theme is
 *       baked into nodes at authoring time and is not reachable here).</li>
 *   <li>Run {@code ChartLayoutResolver} — a pure function of
 *       {@code (spec, style, theme, w, h, metrics)} — which emits a flat list of
 *       positioned primitive {@link DocumentNode}s ({@code ShapeNode} bars,
 *       {@code LineNode} grid/polylines, {@code ParagraphNode} labels) in
 *       bottom-up inner-box coordinates.</li>
 *   <li>Prepare each primitive child and cache it with its placement in
 *       {@link PreparedChartLayout} — the same composite pattern the table uses
 *       for cell content.</li>
 *   <li>{@code emitFragments} dispatches to each prepared child via
 *       {@link FragmentContext#emitChildFragments(PreparedNode, FragmentPlacement)}
 *       and translates the returned fragments into the chart's local frame.</li>
 * </ol>
 *
 * <p>Because the output is ordinary geometry + text, every existing snapshot and
 * visual-parity mechanism covers charts unchanged, any fixed-layout backend
 * renders them with no chart-specific code, and determinism is structural, not
 * promised. (Semantic exports bypass layout entirely — the DOCX backend falls
 * back to the chart's data table.)</p>
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public final class ChartDefinition implements NodeDefinition<ChartNode> {

    /** Creates the chart layout definition. */
    public ChartDefinition() {
    }

    @Override
    public Class<ChartNode> nodeType() {
        return ChartNode.class;
    }

    @Override
    public PaginationPolicy paginationPolicy(ChartNode node) {
        // Charts place whole or move to the next page — like an inline image.
        return PaginationPolicy.ATOMIC;
    }

    @Override
    public PreparedNode<ChartNode> prepare(ChartNode node, PrepareContext ctx, BoxConstraints constraints) {
        ChartSpec spec = node.spec();
        ChartTheme theme = ChartDefaults.DEFAULT_THEME;
        ChartStyle style = theme.toChartStyle().mergedUnder(node.style());

        double innerWidth = Math.max(1.0, constraints.availableWidth() - node.padding().horizontal());
        double innerHeight = Math.max(1.0, spec.size().resolveHeight(innerWidth));

        List<ChartPrimitive> primitives = ChartLayoutResolver.resolve(
                spec, style, theme, innerWidth, innerHeight,
                new ChartTextMetricsSupport(ctx.textMeasurement()));

        List<PreparedPrimitive> prepared = new ArrayList<>(primitives.size());
        for (ChartPrimitive primitive : primitives) {
            PreparedNode<DocumentNode> child = ctx.prepare(
                    primitive.node(), BoxConstraints.natural(primitive.width()));
            prepared.add(new PreparedPrimitive(child, primitive));
        }

        MeasureResult measure = new MeasureResult(
                innerWidth + node.padding().horizontal(),
                innerHeight + node.padding().vertical());
        return PreparedNode.leaf(node, measure, new PreparedChartLayout(List.copyOf(prepared)));
    }

    @Override
    public List<LayoutFragment> emitFragments(PreparedNode<ChartNode> prepared,
                                              FragmentContext ctx,
                                              FragmentPlacement placement) {
        ChartNode node = prepared.node();
        PreparedChartLayout layout = prepared.requirePreparedLayout(PreparedChartLayout.class);
        double padLeft = node.padding().left();
        double padBottom = node.padding().bottom();

        List<LayoutFragment> fragments = new ArrayList<>();
        int index = 0;
        for (PreparedPrimitive entry : layout.primitives()) {
            ChartPrimitive primitive = entry.primitive();
            double childLocalX = padLeft + primitive.x();
            double childLocalY = padBottom + primitive.y();
            String childPath = placement.path() + ".c" + index;
            FragmentPlacement childPlacement = new FragmentPlacement(
                    childPath,
                    placement.path(),
                    index,
                    placement.depth() + 1,
                    placement.pageIndex(),
                    placement.x() + childLocalX,
                    placement.y() + childLocalY,
                    primitive.width(),
                    primitive.height(),
                    placement.startPage(),
                    placement.endPage(),
                    Margin.zero(),
                    Padding.zero());
            List<LayoutFragment> childFragments = ctx.emitChildFragments(entry.prepared(), childPlacement);
            for (LayoutFragment cf : childFragments) {
                fragments.add(new LayoutFragment(
                        childPath,
                        cf.fragmentIndex(),
                        cf.localX() + childLocalX,
                        cf.localY() + childLocalY,
                        cf.width(),
                        cf.height(),
                        cf.payload()));
            }
            index++;
        }
        return List.copyOf(fragments);
    }

    /**
     * Prepared chart payload: the resolver's primitive children, each prepared
     * and paired with its bottom-up placement inside the chart's inner box.
     *
     * @param primitives prepared primitive children in emission order
     */
    public record PreparedChartLayout(List<PreparedPrimitive> primitives) implements PreparedNodeLayout {
    }

    /**
     * One prepared primitive child plus its resolver-assigned placement.
     *
     * @param prepared prepared child node
     * @param primitive the resolver primitive (node + bottom-up box)
     */
    public record PreparedPrimitive(PreparedNode<DocumentNode> prepared, ChartPrimitive primitive) {
    }
}
