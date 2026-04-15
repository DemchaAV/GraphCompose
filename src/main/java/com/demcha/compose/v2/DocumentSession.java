package com.demcha.compose.v2;

import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontFamilyDefinition;
import com.demcha.compose.font_library.FontLibrary;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfFont;
import com.demcha.compose.layout_core.system.measurement.FontLibraryTextMeasurementSystem;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;
import com.demcha.compose.v2.backends.PdfFixedLayoutBackend;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable v2 composition session with semantic authoring, layout compilation,
 * and backend rendering/export entrypoints.
 */
public final class DocumentSession implements AutoCloseable {
    private final Path defaultOutputFile;
    private final NodeRegistry registry;
    private final LayoutCompiler compiler;
    private final List<DocumentNode> roots = new ArrayList<>();
    private final List<FontFamilyDefinition> customFontFamilies = new ArrayList<>();

    private PDRectangle pageSize;
    private Margin margin;
    private LayoutCanvas canvas;

    private PDDocument measurementDocument;
    private FontLibrary fontLibrary;
    private TextMeasurementSystem textMeasurementSystem;

    private long revision;
    private LayoutGraph cachedLayout;
    private long cachedLayoutRevision = -1;
    private LayoutSnapshot cachedSnapshot;
    private long cachedSnapshotRevision = -1;
    private byte[] cachedPdfBytes;
    private long cachedPdfRevision = -1;

    public DocumentSession(Path defaultOutputFile,
                           PDRectangle pageSize,
                           Margin margin,
                           Collection<FontFamilyDefinition> customFontFamilies) {
        this.defaultOutputFile = defaultOutputFile;
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.margin = margin == null ? Margin.zero() : margin;
        this.canvas = LayoutCanvas.from(pageSize, this.margin);
        this.registry = BuiltInNodeDefinitions.registerDefaults(new NodeRegistry());
        this.compiler = new LayoutCompiler(registry);
        this.customFontFamilies.addAll(List.copyOf(customFontFamilies));
        refreshMeasurementServices();
    }

    public DocumentSession add(DocumentNode node) {
        roots.add(Objects.requireNonNull(node, "node"));
        invalidate();
        return this;
    }

    public DocumentSession addAll(Collection<? extends DocumentNode> nodes) {
        for (DocumentNode node : nodes) {
            add(node);
        }
        return this;
    }

    public DocumentSession clear() {
        roots.clear();
        invalidate();
        return this;
    }

    public DocumentSession pageSize(PDRectangle pageSize) {
        this.pageSize = Objects.requireNonNull(pageSize, "pageSize");
        this.canvas = LayoutCanvas.from(this.pageSize, margin);
        invalidate();
        return this;
    }

    public DocumentSession margin(Margin margin) {
        this.margin = margin == null ? Margin.zero() : margin;
        this.canvas = LayoutCanvas.from(pageSize, this.margin);
        invalidate();
        return this;
    }

    public DocumentSession registerFontFamily(FontFamilyDefinition definition) {
        customFontFamilies.add(Objects.requireNonNull(definition, "definition"));
        refreshMeasurementServices();
        invalidate();
        return this;
    }

    public <E extends DocumentNode> DocumentSession registerNodeDefinition(NodeDefinition<E> definition) {
        registry.register(Objects.requireNonNull(definition, "definition"));
        invalidate();
        return this;
    }

    public NodeRegistry registry() {
        return registry;
    }

    public DocumentGraph documentGraph() {
        return new DocumentGraph(List.copyOf(roots));
    }

    public LayoutCanvas canvas() {
        return canvas;
    }

    public List<DocumentNode> roots() {
        return List.copyOf(roots);
    }

    public LayoutGraph layoutGraph() {
        if (cachedLayout != null && cachedLayoutRevision == revision) {
            return cachedLayout;
        }
        V2Context context = new V2Context();
        cachedLayout = compiler.compile(documentGraph(), context, context);
        cachedLayoutRevision = revision;
        return cachedLayout;
    }

    public LayoutSnapshot layoutSnapshot() {
        if (cachedSnapshot != null && cachedSnapshotRevision == revision) {
            return cachedSnapshot;
        }
        cachedSnapshot = LayoutGraphSnapshotExtractor.extract(layoutGraph());
        cachedSnapshotRevision = revision;
        return cachedSnapshot;
    }

    public <R> R render(FixedLayoutBackend<R> backend) throws Exception {
        return render(backend, defaultOutputFile);
    }

    public <R> R render(FixedLayoutBackend<R> backend, Path outputFile) throws Exception {
        Objects.requireNonNull(backend, "backend");
        return backend.render(layoutGraph(), new FixedLayoutRenderContext(canvas, customFontFamilies, outputFile));
    }

    public <R> R export(SemanticBackend<R> backend) throws Exception {
        return export(backend, defaultOutputFile);
    }

    public <R> R export(SemanticBackend<R> backend, Path outputFile) throws Exception {
        Objects.requireNonNull(backend, "backend");
        return backend.export(documentGraph(), new SemanticExportContext(canvas, customFontFamilies, outputFile));
    }

    public byte[] toPdfBytes() throws Exception {
        if (cachedPdfBytes != null && cachedPdfRevision == revision) {
            return cachedPdfBytes.clone();
        }
        cachedPdfBytes = render(new PdfFixedLayoutBackend(), null);
        cachedPdfRevision = revision;
        return cachedPdfBytes.clone();
    }

    public void buildPdf() throws Exception {
        if (defaultOutputFile == null) {
            throw new IllegalStateException("No default output file was configured for this document session.");
        }
        buildPdf(defaultOutputFile);
    }

    public void buildPdf(Path outputFile) throws Exception {
        render(new PdfFixedLayoutBackend(), Objects.requireNonNull(outputFile, "outputFile"));
    }

    @Override
    public void close() throws Exception {
        if (measurementDocument != null) {
            measurementDocument.close();
        }
    }

    private void refreshMeasurementServices() {
        try {
            if (measurementDocument != null) {
                measurementDocument.close();
            }
        } catch (Exception ignored) {
            // Best-effort close while reloading measurement resources.
        }

        measurementDocument = new PDDocument();
        fontLibrary = DefaultFonts.library(measurementDocument, customFontFamilies);
        textMeasurementSystem = new FontLibraryTextMeasurementSystem(fontLibrary, PdfFont.class);
    }

    private void invalidate() {
        revision++;
        cachedLayout = null;
        cachedSnapshot = null;
        cachedPdfBytes = null;
    }

    private final class V2Context implements PrepareContext, FragmentContext {
        private final Map<PreparedNodeCacheKey, PreparedNode<?>> preparedNodes = new HashMap<>();

        @Override
        public <E extends DocumentNode> PreparedNode<E> prepare(E node, BoxConstraints constraints) {
            PreparedNodeCacheKey cacheKey = new PreparedNodeCacheKey(node, normalizeWidth(constraints.availableWidth()));
            PreparedNode<?> cached = preparedNodes.get(cacheKey);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                PreparedNode<E> typed = (PreparedNode<E>) cached;
                return typed;
            }

            @SuppressWarnings("unchecked")
            NodeDefinition<E> definition = (NodeDefinition<E>) registry.definitionFor(node);
            PreparedNode<E> prepared = Objects.requireNonNull(
                    definition.prepare(node, this, constraints),
                    "Node definition prepare(...) must not return null for " + node.nodeKind());
            preparedNodes.put(cacheKey, prepared);
            return prepared;
        }

        @Override
        public FontLibrary fonts() {
            return fontLibrary;
        }

        @Override
        public TextMeasurementSystem textMeasurement() {
            return textMeasurementSystem;
        }

        @Override
        public LayoutCanvas canvas() {
            return canvas;
        }
    }

    private long normalizeWidth(double value) {
        return Math.round(value * 1_000.0);
    }

    private record PreparedNodeCacheKey(DocumentNode node, long widthKey) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PreparedNodeCacheKey that)) {
                return false;
            }
            return widthKey == that.widthKey && node == that.node;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(node) + Long.hashCode(widthKey);
        }
    }
}
