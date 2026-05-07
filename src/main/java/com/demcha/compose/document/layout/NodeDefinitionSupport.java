package com.demcha.compose.document.layout;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.layout.payloads.SideBorders;
import com.demcha.compose.document.layout.payloads.TableRowFragmentPayload;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.document.layout.payloads.TransformBeginPayload;
import com.demcha.compose.document.layout.payloads.TransformEndPayload;
import com.demcha.compose.document.node.DocumentBarcodeOptions;
import com.demcha.compose.document.node.DocumentBarcodeType;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTransform;
import com.demcha.compose.engine.components.content.ImageData;
import com.demcha.compose.engine.components.content.barcode.BarcodeData;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared support routines for built-in {@link NodeDefinition} implementations.
 *
 * <p>The implementation classes live in {@code document.layout.definitions},
 * while the low-level adapter seam remains package-private in
 * {@code document.layout}. This internal helper keeps that boundary narrow as
 * the Phase E.1 split moves node definitions out of
 * {@link BuiltInNodeDefinitions}.</p>
 *
 * @author Artem Demchyshyn
 */
public final class NodeDefinitionSupport {
    /**
     * Tolerance used by built-in layout definitions for zero-size checks.
     */
    public static final double EPS = 1e-6;

    private NodeDefinitionSupport() {
    }

    /**
     * Converts a public document stroke to the engine-level stroke payload.
     *
     * @param stroke document stroke, or {@code null}
     * @return engine stroke, or {@code null}
     */
    public static Stroke toStroke(DocumentStroke stroke) {
        return DocumentNodeAdapters.toStroke(stroke);
    }

    /**
     * Converts public document insets to engine-level padding.
     *
     * @param insets document insets, or {@code null}
     * @return engine padding
     */
    public static Padding toPadding(DocumentInsets insets) {
        return DocumentNodeAdapters.toPadding(insets);
    }

    /**
     * Converts a public document image source to the engine-level image payload.
     *
     * @param imageData document image source
     * @return engine image data
     */
    public static ImageData toImageData(DocumentImageData imageData) {
        return DocumentNodeAdapters.toImageData(imageData);
    }

    /**
     * Wraps an atomic leaf fragment with transform-begin / transform-end markers
     * when the transform is non-identity.
     *
     * @param leaf resolved leaf fragment
     * @param placement resolved fragment placement
     * @param transform optional render-time transform
     * @return the leaf alone for identity transforms, otherwise begin/leaf/end
     */
    public static List<LayoutFragment> wrapAtomicWithTransform(LayoutFragment leaf,
                                                               FragmentPlacement placement,
                                                               DocumentTransform transform) {
        if (transform == null || transform.isIdentity()) {
            return List.of(leaf);
        }
        return List.of(
                new LayoutFragment(
                        placement.path(),
                        0,
                        leaf.localX(),
                        leaf.localY(),
                        leaf.width(),
                        leaf.height(),
                        new TransformBeginPayload(transform, placement.path())),
                new LayoutFragment(
                        placement.path(),
                        1,
                        leaf.localX(),
                        leaf.localY(),
                        leaf.width(),
                        leaf.height(),
                        leaf.payload()),
                new LayoutFragment(
                        placement.path(),
                        2,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        new TransformEndPayload(placement.path())));
    }

    /**
     * Emits an optional background/border decoration fragment.
     *
     * @param fillColor optional fill color
     * @param stroke optional uniform stroke
     * @param cornerRadius optional per-corner radius
     * @param sideBorders optional per-side borders
     * @param placement resolved placement
     * @return one shape fragment or an empty list when nothing should render
     */
    public static List<LayoutFragment> emitDecorationFragment(Color fillColor,
                                                              Stroke stroke,
                                                              DocumentCornerRadius cornerRadius,
                                                              SideBorders sideBorders,
                                                              FragmentPlacement placement) {
        return emitDecorationFragment(fillColor, stroke, cornerRadius == null ? 0.0 : cornerRadius.radius(),
                cornerRadius, sideBorders, placement);
    }

    /**
     * Emits an optional background/border decoration fragment.
     *
     * @param fillColor optional fill color
     * @param stroke optional uniform stroke
     * @param cornerRadius uniform corner radius
     * @param placement resolved placement
     * @return one shape fragment or an empty list when nothing should render
     */
    public static List<LayoutFragment> emitDecorationFragment(Color fillColor,
                                                              Stroke stroke,
                                                              double cornerRadius,
                                                              FragmentPlacement placement) {
        return emitDecorationFragment(fillColor, stroke, cornerRadius, null, null, placement);
    }

    /**
     * Emits an optional background/border decoration fragment.
     *
     * @param fillColor optional fill color
     * @param stroke optional uniform stroke
     * @param cornerRadius uniform corner radius
     * @param sideBorders optional per-side borders
     * @param placement resolved placement
     * @return one shape fragment or an empty list when nothing should render
     */
    public static List<LayoutFragment> emitDecorationFragment(Color fillColor,
                                                              Stroke stroke,
                                                              double cornerRadius,
                                                              SideBorders sideBorders,
                                                              FragmentPlacement placement) {
        return emitDecorationFragment(fillColor, stroke, cornerRadius, null, sideBorders, placement);
    }

    /**
     * Converts public per-side borders to engine strokes.
     *
     * @param borders public border bundle
     * @return engine side borders, or {@code null} when no side is set
     */
    public static SideBorders toSideBorders(DocumentBorders borders) {
        if (borders == null || !borders.hasAny()) {
            return null;
        }
        return new SideBorders(
                toStroke(borders.top()),
                toStroke(borders.right()),
                toStroke(borders.bottom()),
                toStroke(borders.left()));
    }

    /**
     * Converts public barcode options to the engine barcode payload.
     *
     * @param options public barcode options
     * @return engine barcode data
     */
    public static BarcodeData toBarcodeData(DocumentBarcodeOptions options) {
        DocumentBarcodeType type = options.getType() == null ? DocumentBarcodeType.QR_CODE : options.getType();
        return BarcodeData.of(
                options.getContent(),
                switch (type) {
                    case CODE_128 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.CODE_128;
                    case CODE_39 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.CODE_39;
                    case EAN_13 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.EAN_13;
                    case EAN_8 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.EAN_8;
                    case UPC_A -> com.demcha.compose.engine.components.content.barcode.BarcodeType.UPC_A;
                    case PDF_417 -> com.demcha.compose.engine.components.content.barcode.BarcodeType.PDF_417;
                    case DATA_MATRIX -> com.demcha.compose.engine.components.content.barcode.BarcodeType.DATA_MATRIX;
                    case QR_CODE -> com.demcha.compose.engine.components.content.barcode.BarcodeType.QR_CODE;
                },
                options.getForeground() == null ? Color.BLACK : options.getForeground().color(),
                options.getBackground() == null ? Color.WHITE : options.getBackground().color(),
                options.getQuietZoneMargin());
    }

    /**
     * Resolves image display dimensions from requested size, scale, intrinsic
     * metadata, and available width.
     *
     * @param node image node to measure
     * @param availableWidth parent available width
     * @return resolved image dimensions excluding padding
     */
    public static ImageDimensions resolveImageDimensions(ImageNode node, double availableWidth) {
        ImageData imageData = toImageData(node.imageData());
        int intrinsicWidth = Math.max(1, imageData.getMetadata().width());
        int intrinsicHeight = Math.max(1, imageData.getMetadata().height());
        double ratio = intrinsicWidth / (double) intrinsicHeight;

        double requestedWidth = node.width() == null ? Double.NaN : node.width();
        double requestedHeight = node.height() == null ? Double.NaN : node.height();

        double width;
        double height;

        if (!Double.isNaN(requestedWidth) && !Double.isNaN(requestedHeight)) {
            width = requestedWidth;
            height = requestedHeight;
        } else if (!Double.isNaN(requestedWidth)) {
            width = requestedWidth;
            height = requestedWidth / ratio;
        } else if (!Double.isNaN(requestedHeight)) {
            height = requestedHeight;
            width = requestedHeight * ratio;
        } else if (node.scale() != null) {
            width = intrinsicWidth * node.scale();
            height = intrinsicHeight * node.scale();
        } else {
            width = intrinsicWidth;
            height = intrinsicHeight;
        }

        double maxWidth = Math.max(0.0, availableWidth - node.padding().horizontal());
        if (maxWidth > EPS && width > maxWidth) {
            double scale = maxWidth / width;
            width *= scale;
            height *= scale;
        }

        return new ImageDimensions(width, height);
    }

    /**
     * Measures a vertical composite node by preparing children inside the
     * padding-adjusted inner width.
     *
     * @param children semantic child nodes
     * @param spacing vertical spacing between children
     * @param padding engine padding
     * @param ctx prepare context
     * @param constraints parent constraints
     * @return measured outer size
     */
    public static MeasureResult measureComposite(List<DocumentNode> children,
                                                 double spacing,
                                                 Padding padding,
                                                 PrepareContext ctx,
                                                 BoxConstraints constraints) {
        double innerWidth = Math.max(0.0, constraints.availableWidth() - padding.horizontal());
        double totalHeight = padding.vertical();
        double maxWidth = 0.0;

        for (int index = 0; index < children.size(); index++) {
            DocumentNode child = children.get(index);
            PreparedNode<DocumentNode> childPrepared = ctx.prepare(
                    child,
                    BoxConstraints.natural(Math.max(0.0, innerWidth - child.margin().horizontal())));
            totalHeight += child.margin().vertical() + childPrepared.measureResult().height();
            maxWidth = Math.max(maxWidth, child.margin().horizontal() + childPrepared.measureResult().width());
            if (index < children.size() - 1) {
                totalHeight += spacing;
            }
        }

        return new MeasureResult(
                Math.min(constraints.availableWidth(), padding.horizontal() + maxWidth),
                totalHeight);
    }

    /**
     * Measures a horizontal row by preparing each child in its weighted slot.
     *
     * @param node row node
     * @param padding engine padding
     * @param ctx prepare context
     * @param constraints parent constraints
     * @return measured outer row size
     */
    public static MeasureResult measureRow(RowNode node,
                                           Padding padding,
                                           PrepareContext ctx,
                                           BoxConstraints constraints) {
        double availableWidth = Math.max(0.0, constraints.availableWidth() - padding.horizontal());
        int n = node.children().size();
        double gap = node.gap();
        double totalGap = n > 1 ? gap * (n - 1) : 0.0;
        double slotsTotal = Math.max(0.0, availableWidth - totalGap);
        double[] slotWidths = new double[n];
        if (node.weights().isEmpty()) {
            double share = n > 0 ? slotsTotal / n : 0.0;
            for (int i = 0; i < n; i++) {
                slotWidths[i] = share;
            }
        } else {
            double total = 0.0;
            for (Double w : node.weights()) {
                total += w;
            }
            for (int i = 0; i < n; i++) {
                slotWidths[i] = total > 0.0 ? slotsTotal * (node.weights().get(i) / total) : (n > 0 ? slotsTotal / n : 0.0);
            }
        }

        double maxChildHeight = 0.0;
        for (int i = 0; i < n; i++) {
            DocumentNode child = node.children().get(i);
            double childInner = Math.max(0.0, slotWidths[i] - child.margin().horizontal());
            PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
            double childOuter = childPrepared.measureResult().height() + child.margin().vertical();
            if (childOuter > maxChildHeight) {
                maxChildHeight = childOuter;
            }
        }

        return new MeasureResult(constraints.availableWidth(), padding.vertical() + maxChildHeight);
    }

    /**
     * Measures a stack by preparing each layer inside the padding-adjusted
     * available width and taking the largest child outer box.
     *
     * @param node layer stack node
     * @param padding engine padding
     * @param ctx prepare context
     * @param constraints parent constraints
     * @return measured outer stack size
     */
    public static MeasureResult measureStack(LayerStackNode node,
                                             Padding padding,
                                             PrepareContext ctx,
                                             BoxConstraints constraints) {
        double availableWidth = Math.max(0.0, constraints.availableWidth() - padding.horizontal());
        double maxOuterWidth = 0.0;
        double maxOuterHeight = 0.0;
        for (LayerStackNode.Layer layer : node.layers()) {
            DocumentNode child = layer.node();
            double childInner = Math.max(0.0, availableWidth - child.margin().horizontal());
            PreparedNode<DocumentNode> childPrepared = ctx.prepare(child, BoxConstraints.natural(childInner));
            double outerWidth = childPrepared.measureResult().width() + child.margin().horizontal();
            double outerHeight = childPrepared.measureResult().height() + child.margin().vertical();
            if (outerWidth > maxOuterWidth) {
                maxOuterWidth = outerWidth;
            }
            if (outerHeight > maxOuterHeight) {
                maxOuterHeight = outerHeight;
            }
        }
        double width = Math.min(constraints.availableWidth(), padding.horizontal() + maxOuterWidth);
        return new MeasureResult(width, padding.vertical() + maxOuterHeight);
    }

    /**
     * Prepares a table node through the package-local table layout support.
     *
     * @param node table node
     * @param ctx prepare context
     * @param constraints parent constraints
     * @return prepared table node
     */
    public static PreparedNode<TableNode> prepareTable(TableNode node,
                                                       PrepareContext ctx,
                                                       BoxConstraints constraints) {
        TableLayoutSupport.ResolvedTableLayoutWithContents resolved = TableLayoutSupport.resolveTableLayout(
                node, ctx, ctx.textMeasurement(), constraints.availableWidth());
        TableLayoutSupport.ResolvedTableLayout layout = resolved.layout();
        MeasureResult measure = new MeasureResult(
                layout.finalWidth() + node.padding().horizontal(),
                layout.totalHeight() + node.padding().vertical());
        return PreparedNode.leaf(node, measure,
                new TableLayoutSupport.PreparedTableLayout(
                        layout,
                        node.bookmarkOptions() != null,
                        resolved.preparedContents()));
    }

    /**
     * Splits a prepared table while preserving repeated-header semantics.
     *
     * @param prepared prepared table node
     * @param request split request
     * @return split result
     */
    public static PreparedSplitResult<TableNode> splitTable(PreparedNode<TableNode> prepared, SplitRequest request) {
        TableNode node = prepared.node();
        TableLayoutSupport.PreparedTableLayout preparedTable = prepared
                .requirePreparedLayout(TableLayoutSupport.PreparedTableLayout.class);
        TableLayoutSupport.ResolvedTableLayout layout = preparedTable.resolvedLayout();
        double innerAvailableHeight = Math.max(0.0, request.remainingHeight() - node.padding().vertical());

        int totalRows = node.rows().size();
        int headerCount = node.repeatedHeaderRowCount();
        if (headerCount > totalRows) {
            headerCount = totalRows;
        }

        int rowCount = 0;
        double consumed = 0.0;
        for (double rowHeight : layout.rowHeights()) {
            if (consumed + rowHeight > innerAvailableHeight + EPS) {
                break;
            }
            consumed += rowHeight;
            rowCount++;
        }

        if (rowCount <= 0) {
            return new PreparedSplitResult<>(null, prepared);
        }
        if (rowCount >= totalRows) {
            return PreparedSplitResult.whole(prepared);
        }

        if (headerCount > 0 && rowCount <= headerCount) {
            return new PreparedSplitResult<>(null, prepared);
        }

        PreparedNode<TableNode> head = TableLayoutSupport.sliceTablePreparedNode(
                node, layout, preparedTable.preparedContents(), 0, rowCount, true, false, 0);
        PreparedNode<TableNode> tail = TableLayoutSupport.sliceTablePreparedNode(
                node, layout, preparedTable.preparedContents(),
                rowCount, totalRows, false, true, headerCount);
        return new PreparedSplitResult<>(head, tail);
    }

    /**
     * Emits row fragments for a prepared table.
     *
     * @param prepared prepared table node
     * @param placement resolved fragment placement
     * @return renderer-facing table row fragments
     */
    public static List<LayoutFragment> emitTableFragments(PreparedNode<TableNode> prepared,
                                                          FragmentContext ctx,
                                                          FragmentPlacement placement) {
        TableNode node = prepared.node();
        TableLayoutSupport.PreparedTableLayout preparedLayout = prepared
                .requirePreparedLayout(TableLayoutSupport.PreparedTableLayout.class);
        TableLayoutSupport.ResolvedTableLayout layout = preparedLayout.resolvedLayout();

        List<LayoutFragment> fragments = new ArrayList<>(layout.rows().size());
        double innerHeight = layout.totalHeight();
        double rowTopOffset = 0.0;

        for (int rowIndex = 0; rowIndex < layout.rows().size(); rowIndex++) {
            double rowHeight = layout.rowHeights().get(rowIndex);
            double localY = node.padding().bottom() + (innerHeight - rowTopOffset - rowHeight);
            fragments.add(new LayoutFragment(
                    placement.path(),
                    rowIndex,
                    node.padding().left(),
                    localY,
                    layout.finalWidth(),
                    rowHeight,
                    new TableRowFragmentPayload(
                            layout.rows().get(rowIndex),
                            rowIndex == 0,
                            node.linkOptions(),
                            rowIndex == 0 && preparedLayout.emitBookmark()
                                    ? node.bookmarkOptions()
                                    : null)));
            // Composed cells: dispatch each prepared child's fragments
            // at the cell's absolute table-local position. The child's
            // FragmentPlacement is constructed from the cell's width,
            // height, and position relative to the placement path.
            List<TableResolvedCell> rowCells = layout.rows().get(rowIndex);
            for (TableResolvedCell cell : rowCells) {
                int columnIndex = locateCellColumn(layout, rowIndex, cell);
                TableLayoutSupport.CellKey key = new TableLayoutSupport.CellKey(rowIndex, columnIndex);
                PreparedNode<?> child = preparedLayout.preparedContents().get(key);
                if (child == null) {
                    continue;
                }
                fragments.addAll(emitComposedCellFragments(
                        child, ctx, placement,
                        node.padding().left() + cell.x(),
                        localY + cell.yOffset(),
                        cell.width(),
                        cell.height(),
                        cell.style()));
            }
            rowTopOffset += rowHeight;
        }

        return List.copyOf(fragments);
    }

    /**
     * Computes the column index of a resolved cell by summing the
     * column widths to the left of {@code cell.x()}. Used to recover
     * the cell's logical column from its placed-fragment data so the
     * preparedContents map (keyed by {@code (row, column)}) can be
     * looked up.
     */
    private static int locateCellColumn(TableLayoutSupport.ResolvedTableLayout layout,
                                        int rowIndex,
                                        TableResolvedCell cell) {
        double cumulative = 0.0;
        List<Double> widths = layout.columnWidths();
        for (int col = 0; col < widths.size(); col++) {
            if (Math.abs(cumulative - cell.x()) < 1e-6) {
                return col;
            }
            cumulative += widths.get(col);
        }
        // Fallback when arithmetic drift accumulates: pick the column
        // whose left edge is closest to the cell's x.
        double bestDelta = Double.POSITIVE_INFINITY;
        int bestCol = 0;
        cumulative = 0.0;
        for (int col = 0; col < widths.size(); col++) {
            double delta = Math.abs(cumulative - cell.x());
            if (delta < bestDelta) {
                bestDelta = delta;
                bestCol = col;
            }
            cumulative += widths.get(col);
        }
        return bestCol;
    }

    /**
     * Emits fragments for a composed cell's prepared child by
     * dispatching to the child's {@code NodeDefinition.emitFragments}
     * via {@link FragmentContext#emitChildFragments}. Positions the
     * child at the cell's content area (inside cell padding).
     */
    private static List<LayoutFragment> emitComposedCellFragments(
            PreparedNode<?> child,
            FragmentContext ctx,
            FragmentPlacement parentPlacement,
            double cellLocalX,
            double cellLocalY,
            double cellWidth,
            double cellHeight,
            com.demcha.compose.engine.components.content.table.TableCellLayoutStyle style) {
        com.demcha.compose.engine.components.style.Padding padding =
                style.padding() == null
                        ? com.demcha.compose.engine.components.style.Padding.zero()
                        : style.padding();
        double childWidth = Math.max(0.0, cellWidth - padding.horizontal());
        double childHeight = Math.max(0.0, child.measureResult().height());
        // The cell paints its content area at (cellLocalX + padding.left,
        // cellLocalY + padding.bottom). Build a placement at that
        // absolute location so the child node's emitFragments lays
        // its content out as if it were a top-level placement of the
        // cell's content area.
        double childAbsoluteX = parentPlacement.x() + cellLocalX + padding.left();
        double childAbsoluteY = parentPlacement.y() + cellLocalY + padding.bottom();
        String childPath = parentPlacement.path() + ".cell" + cellLocalX + "x" + cellLocalY;
        FragmentPlacement childPlacement = new FragmentPlacement(
                childPath,
                parentPlacement.path(),
                0,
                parentPlacement.depth() + 1,
                parentPlacement.pageIndex(),
                childAbsoluteX,
                childAbsoluteY,
                childWidth,
                childHeight,
                parentPlacement.startPage(),
                parentPlacement.endPage(),
                com.demcha.compose.engine.components.style.Margin.zero(),
                com.demcha.compose.engine.components.style.Padding.zero());
        // The child emits fragments with localX/localY relative to its
        // own placement. Translate those into the parent table fragment's
        // local coordinate system (relative to parentPlacement) so the
        // LayoutCompiler converts them to the correct absolute position
        // when placing the table's emitted fragments.
        List<LayoutFragment> childFragments = ctx.emitChildFragments(child, childPlacement);
        if (childFragments.isEmpty()) {
            return List.of();
        }
        double offsetX = cellLocalX + padding.left();
        double offsetY = cellLocalY + padding.bottom();
        List<LayoutFragment> translated = new ArrayList<>(childFragments.size());
        for (LayoutFragment cf : childFragments) {
            translated.add(new LayoutFragment(
                    childPath,
                    cf.fragmentIndex(),
                    cf.localX() + offsetX,
                    cf.localY() + offsetY,
                    cf.width(),
                    cf.height(),
                    cf.payload()));
        }
        return translated;
    }

    private static List<LayoutFragment> emitDecorationFragment(Color fillColor,
                                                               Stroke stroke,
                                                               double cornerRadius,
                                                               DocumentCornerRadius perCornerRadius,
                                                               SideBorders sideBorders,
                                                               FragmentPlacement placement) {
        boolean hasFill = fillColor != null;
        boolean hasStroke = stroke != null
                && stroke.strokeColor() != null
                && stroke.strokeColor().color() != null
                && stroke.width() > 0;
        boolean hasSideBorders = sideBorders != null && sideBorders.hasAny();
        if ((!hasFill && !hasStroke && !hasSideBorders) || placement.width() <= EPS || placement.height() <= EPS) {
            return List.of();
        }

        ShapeFragmentPayload payload = perCornerRadius != null
                ? new ShapeFragmentPayload(fillColor, stroke, perCornerRadius, null, null, sideBorders)
                : new ShapeFragmentPayload(fillColor, stroke, cornerRadius, null, null, sideBorders);
        return List.of(new LayoutFragment(
                placement.path(),
                0,
                0.0,
                0.0,
                placement.width(),
                placement.height(),
                payload));
    }

    /**
     * Resolved image dimensions excluding node padding.
     *
     * @param width resolved width
     * @param height resolved height
     */
    public record ImageDimensions(double width, double height) {
    }
}
