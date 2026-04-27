package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.font.FontFamilyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the rendering and export pipeline exposed by
 * {@link DocumentSession}. Pulled out of the session so the public facade
 * stays focused on authoring, configuration, and lifecycle.
 *
 * <p>The class is package-private and reads everything it needs through a
 * small {@link Context} interface that the session implements internally.
 * That keeps the rendering pipeline decoupled from authoring state without
 * inflating the constructor argument list.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocumentRenderingFacade {
    private static final Logger LIFECYCLE_LOG = LoggerFactory.getLogger("com.demcha.compose.document.lifecycle");

    private final Context context;

    DocumentRenderingFacade(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    <R> R render(FixedLayoutBackend<R> backend, Path outputFile) throws Exception {
        context.ensureOpen();
        Objects.requireNonNull(backend, "backend");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug(
                "document.render.start sessionId={} backend={} revision={} roots={} outputConfigured={}",
                context.sessionId(),
                backend.name(),
                context.revision(),
                context.rootCount(),
                outputFile != null);
        try {
            R result = backend.render(context.layoutGraph(), new FixedLayoutRenderContext(
                    context.canvas(),
                    context.customFontFamilies(),
                    outputFile,
                    null));
            LIFECYCLE_LOG.debug(
                    "document.render.end sessionId={} backend={} revision={} durationMs={}",
                    context.sessionId(),
                    backend.name(),
                    context.revision(),
                    elapsedMillis(startNanos));
            return result;
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.render.failed sessionId={} backend={} revision={} errorType={}",
                    context.sessionId(),
                    backend.name(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    <R> R export(SemanticBackend<R> backend, Path outputFile) throws Exception {
        context.ensureOpen();
        Objects.requireNonNull(backend, "backend");
        return backend.export(context.documentGraph(),
                new SemanticExportContext(
                        context.canvas(),
                        context.customFontFamilies(),
                        outputFile,
                        context.outputOptions()));
    }

    byte[] toPdfBytes() throws Exception {
        context.ensureOpen();
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.pdf.bytes.start sessionId={} revision={} roots={}",
                context.sessionId(), context.revision(), context.rootCount());
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writePdf(output);
            byte[] bytes = output.toByteArray();
            LIFECYCLE_LOG.debug(
                    "document.pdf.bytes.end sessionId={} revision={} byteCount={} durationMs={}",
                    context.sessionId(),
                    context.revision(),
                    bytes.length,
                    elapsedMillis(startNanos));
            return bytes;
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.pdf.bytes.failed sessionId={} revision={} errorType={}",
                    context.sessionId(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    void writePdf(OutputStream output) throws Exception {
        context.ensureOpen();
        context.ensureRenderable();
        OutputStream target = Objects.requireNonNull(output, "output");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.pdf.stream.start sessionId={} revision={} roots={}",
                context.sessionId(), context.revision(), context.rootCount());
        try {
            context.conveniencePdfBackend().write(context.layoutGraph(), new FixedLayoutRenderContext(
                    context.canvas(),
                    context.customFontFamilies(),
                    null,
                    target));
            LIFECYCLE_LOG.debug(
                    "document.pdf.stream.end sessionId={} revision={} durationMs={}",
                    context.sessionId(),
                    context.revision(),
                    elapsedMillis(startNanos));
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.pdf.stream.failed sessionId={} revision={} errorType={}",
                    context.sessionId(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    void buildPdf(Path outputFile) throws Exception {
        context.ensureOpen();
        context.ensureRenderable();
        Path target = Objects.requireNonNull(outputFile, "outputFile");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.pdf.build.start sessionId={} revision={} roots={}",
                context.sessionId(), context.revision(), context.rootCount());
        try (OutputStream output = Files.newOutputStream(target)) {
            writePdf(output);
            LIFECYCLE_LOG.debug(
                    "document.pdf.build.end sessionId={} revision={} durationMs={}",
                    context.sessionId(),
                    context.revision(),
                    elapsedMillis(startNanos));
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.pdf.build.failed sessionId={} revision={} errorType={}",
                    context.sessionId(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /**
     * Context callbacks that the rendering facade reads from
     * {@link DocumentSession}. The session implements this interface so the
     * facade stays free of authoring state.
     */
    interface Context {
        void ensureOpen();

        void ensureRenderable();

        String sessionId();

        long revision();

        int rootCount();

        LayoutCanvas canvas();

        List<FontFamilyDefinition> customFontFamilies();

        LayoutGraph layoutGraph();

        DocumentGraph documentGraph();

        DocumentOutputOptions outputOptions();

        PdfFixedLayoutBackend conveniencePdfBackend();
    }
}
