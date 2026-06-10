package com.demcha.compose.document.layout;

import com.demcha.compose.document.layout.definitions.BarcodeDefinition;
import com.demcha.compose.document.layout.definitions.CanvasLayerDefinition;
import com.demcha.compose.document.layout.definitions.ChartDefinition;
import com.demcha.compose.document.layout.definitions.ContainerDefinition;
import com.demcha.compose.document.layout.definitions.EllipseDefinition;
import com.demcha.compose.document.layout.definitions.ImageDefinition;
import com.demcha.compose.document.layout.definitions.LayerStackDefinition;
import com.demcha.compose.document.layout.definitions.LineDefinition;
import com.demcha.compose.document.layout.definitions.ListDefinition;
import com.demcha.compose.document.layout.definitions.PageBreakDefinition;
import com.demcha.compose.document.layout.definitions.ParagraphDefinition;
import com.demcha.compose.document.layout.definitions.PolygonDefinition;
import com.demcha.compose.document.layout.definitions.RowDefinition;
import com.demcha.compose.document.layout.definitions.SectionDefinition;
import com.demcha.compose.document.layout.definitions.ShapeContainerDefinition;
import com.demcha.compose.document.layout.definitions.ShapeDefinition;
import com.demcha.compose.document.layout.definitions.SpacerDefinition;
import com.demcha.compose.document.layout.definitions.TableDefinition;

import java.util.Objects;

/**
 * Registers the built-in canonical node definitions on a {@link NodeRegistry}.
 *
 * <p>The actual layout logic lives in {@code document.layout.definitions} (one
 * file per node type) and {@link TextFlowSupport} / {@link NodeDefinitionSupport}
 * for shared helpers. This class is the single registration entry point.</p>
 *
 * @author Artem Demchyshyn
 */
public final class BuiltInNodeDefinitions {
    private BuiltInNodeDefinitions() {
    }

    /**
     * Registers every built-in canonical node definition with the supplied registry.
     *
     * @param registry mutable registry to populate
     * @return the same registry after registration
     */
    public static NodeRegistry registerDefaults(NodeRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return registry
                .register(new ParagraphDefinition())
                .register(new ListDefinition())
                .register(new ShapeDefinition())
                .register(new SpacerDefinition())
                .register(new LineDefinition())
                .register(new EllipseDefinition())
                .register(new ImageDefinition())
                .register(new BarcodeDefinition())
                .register(new PageBreakDefinition())
                .register(new ContainerDefinition())
                .register(new SectionDefinition())
                .register(new RowDefinition())
                .register(new LayerStackDefinition())
                .register(new ShapeContainerDefinition())
                .register(new TableDefinition())
                .register(new CanvasLayerDefinition())
                .register(new PolygonDefinition())
                .register(new ChartDefinition());
    }
}
