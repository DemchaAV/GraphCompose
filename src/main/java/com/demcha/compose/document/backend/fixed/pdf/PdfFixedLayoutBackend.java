package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfImageFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfParagraphFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfShapeFragmentRenderHandler;
import com.demcha.compose.document.backend.fixed.pdf.handlers.PdfTableRowFragmentRenderHandler;
import com.demcha.compose.document.exceptions.UnsupportedNodeCapabilityException;
import com.demcha.compose.document.layout.BuiltInNodeDefinitions;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.font_library.DefaultFonts;
import com.demcha.compose.font_library.FontLibrary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handler-based fixed-layout PDF backend for the canonical semantic document API.
 *
 * <p>This backend consumes a fully resolved {@link LayoutGraph} and delegates
 * fragment painting to payload-specific handlers. The backend itself is
 * responsible for document lifecycle, page creation, page-scoped stream
 * management, and shared resource caches such as decoded images and resolved
 * fonts.</p>
 *
 * <p><b>Thread-safety:</b> instances are immutable after construction and can be
 * reused, but each {@link #render(LayoutGraph, FixedLayoutRenderContext)} call
 * creates a new page-scoped render session.</p>
 */
public final class PdfFixedLayoutBackend implements FixedLayoutBackend<byte[]> {
    private final Map<Class<?>, PdfFragmentRenderHandler<?>> handlers;

    /**
     * Creates a backend with the built-in paragraph, shape, image, and table handlers.
     */
    public PdfFixedLayoutBackend() {
        this(List.of(
                new PdfParagraphFragmentRenderHandler(),
                new PdfShapeFragmentRenderHandler(),
                new PdfImageFragmentRenderHandler(),
                new PdfTableRowFragmentRenderHandler()));
    }

    PdfFixedLayoutBackend(Collection<? extends PdfFragmentRenderHandler<?>> handlers) {
        Map<Class<?>, PdfFragmentRenderHandler<?>> registry = new LinkedHashMap<>();
        for (PdfFragmentRenderHandler<?> handler : handlers) {
            PdfFragmentRenderHandler<?> previous = registry.put(handler.payloadType(), Objects.requireNonNull(handler, "handler"));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate PDF handler for payload type " + handler.payloadType().getName());
            }
        }
        this.handlers = Map.copyOf(registry);
    }

    @Override
    public String name() {
        return "pdf-fixed-layout";
    }

    /**
     * Renders the resolved layout graph into PDF bytes and optionally writes the
     * result to the configured output file.
     *
     * @param graph resolved layout graph produced by the semantic compiler
     * @param context fixed-layout render configuration including page canvas and output target
     * @return rendered PDF document bytes
     * @throws Exception if PDF creation, rendering, or saving fails
     */
    @Override
    public byte[] render(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(context, "context");

        try (PDDocument document = new PDDocument()) {
            FontLibrary fonts = DefaultFonts.library(document, context.customFontFamilies());
            List<PDPage> pages = createPages(document, graph);

            try (PdfRenderSession session = new PdfRenderSession(document, pages)) {
                PdfRenderEnvironment environment = new PdfRenderEnvironment(document, fonts, session);
                for (PlacedFragment fragment : graph.fragments()) {
                    renderFragment(fragment, environment);
                }
            }

            if (context.outputFile() != null) {
                document.save(context.outputFile().toFile());
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }

    private List<PDPage> createPages(PDDocument document, LayoutGraph graph) {
        int pageCount = Math.max(graph.totalPages(), 1);
        PDRectangle pageSize = new PDRectangle((float) graph.canvas().width(), (float) graph.canvas().height());
        List<PDPage> pages = new ArrayList<>(pageCount);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            pages.add(page);
        }
        return List.copyOf(pages);
    }

    private void renderFragment(PlacedFragment fragment, PdfRenderEnvironment environment) throws Exception {
        Object payload = fragment.payload();
        PdfFragmentRenderHandler<Object> handler = handlerFor(payload);
        handler.render(fragment, payload, environment);
    }

    @SuppressWarnings("unchecked")
    private PdfFragmentRenderHandler<Object> handlerFor(Object payload) {
        if (payload == null) {
            throw new UnsupportedNodeCapabilityException("PDF backend does not support null fragment payloads.");
        }

        PdfFragmentRenderHandler<?> direct = handlers.get(payload.getClass());
        if (direct != null) {
            return (PdfFragmentRenderHandler<Object>) direct;
        }

        if (payload instanceof BuiltInNodeDefinitions.ParagraphFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.ParagraphFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.ShapeFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.ShapeFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.ImageFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.ImageFragmentPayload.class);
        }
        if (payload instanceof BuiltInNodeDefinitions.TableRowFragmentPayload) {
            return (PdfFragmentRenderHandler<Object>) handlers.get(BuiltInNodeDefinitions.TableRowFragmentPayload.class);
        }

        throw new UnsupportedNodeCapabilityException("PDF backend does not support fragment payload: " + payload.getClass().getName());
    }
}
