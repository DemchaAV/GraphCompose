package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.FixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.font.FontFamilyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
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

    /** Provider format keys for the fixed-layout convenience paths. */
    private static final String PDF = "pdf";
    private static final String PPTX = "pptx";

    private final Context context;

    DocumentRenderingFacade(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
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
        return renderBytes(PDF);
    }

    void writePdf(OutputStream output) throws Exception {
        writeFixedLayout(PDF, output);
    }

    void buildPdf(Path outputFile) throws Exception {
        buildFixedLayout(PDF, outputFile);
    }

    byte[] toPptxBytes() throws Exception {
        return renderBytes(PPTX);
    }

    void writePptx(OutputStream output) throws Exception {
        writeFixedLayout(PPTX, output);
    }

    void buildPptx(Path outputFile) throws Exception {
        buildFixedLayout(PPTX, outputFile);
    }

    private byte[] renderBytes(String format) throws Exception {
        context.ensureOpen();
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.{}.bytes.start sessionId={} revision={} roots={}",
                format, context.sessionId(), context.revision(), context.rootCount());
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writeFixedLayout(format, output);
            byte[] bytes = output.toByteArray();
            LIFECYCLE_LOG.debug(
                    "document.{}.bytes.end sessionId={} revision={} byteCount={} durationMs={}",
                    format,
                    context.sessionId(),
                    context.revision(),
                    bytes.length,
                    elapsedMillis(startNanos));
            return bytes;
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.{}.bytes.failed sessionId={} revision={} errorType={}",
                    format,
                    context.sessionId(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    private void writeFixedLayout(String format, OutputStream output) throws Exception {
        context.ensureOpen();
        context.ensureRenderable();
        OutputStream target = Objects.requireNonNull(output, "output");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.{}.stream.start sessionId={} revision={} roots={}",
                format, context.sessionId(), context.revision(), context.rootCount());
        try {
            context.convenienceBackend(format).write(context.layoutGraph(), new FixedLayoutRenderContext(
                    context.canvas(),
                    context.customFontFamilies(),
                    null,
                    target));
            LIFECYCLE_LOG.debug(
                    "document.{}.stream.end sessionId={} revision={} durationMs={}",
                    format,
                    context.sessionId(),
                    context.revision(),
                    elapsedMillis(startNanos));
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.{}.stream.failed sessionId={} revision={} errorType={}",
                    format,
                    context.sessionId(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    private void buildFixedLayout(String format, Path outputFile) throws Exception {
        context.ensureOpen();
        context.ensureRenderable();
        Path target = Objects.requireNonNull(outputFile, "outputFile");
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.{}.build.start sessionId={} revision={} roots={}",
                format, context.sessionId(), context.revision(), context.rootCount());
        try {
            AtomicFileOutput.write(target, output -> writeFixedLayout(format, output));
            LIFECYCLE_LOG.debug(
                    "document.{}.build.end sessionId={} revision={} durationMs={}",
                    format,
                    context.sessionId(),
                    context.revision(),
                    elapsedMillis(startNanos));
        } catch (Exception ex) {
            LIFECYCLE_LOG.error(
                    "document.{}.build.failed sessionId={} revision={} errorType={}",
                    format,
                    context.sessionId(),
                    context.revision(),
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    List<BufferedImage> renderImages(int dpi, boolean transparent, int pageIndex) throws Exception {
        context.ensureOpen();
        context.ensureRenderable();
        long startNanos = System.nanoTime();
        LIFECYCLE_LOG.debug("document.images.start sessionId={} revision={} roots={} dpi={} transparent={} pageIndex={}",
                context.sessionId(), context.revision(), context.rootCount(), dpi, transparent, pageIndex);
        try {
            List<BufferedImage> images = context.convenienceBackend(PDF).renderToImages(
                    context.layoutGraph(),
                    new FixedLayoutRenderContext(context.canvas(), context.customFontFamilies(), null, null),
                    dpi,
                    transparent,
                    pageIndex);
            LIFECYCLE_LOG.debug("document.images.end sessionId={} revision={} pageCount={} durationMs={}",
                    context.sessionId(), context.revision(), images.size(), elapsedMillis(startNanos));
            return images;
        } catch (Exception ex) {
            LIFECYCLE_LOG.error("document.images.failed sessionId={} revision={} errorType={}",
                    context.sessionId(), context.revision(), ex.getClass().getSimpleName(), ex);
            throw ex;
        }
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

        /**
         * Resolves the session-chrome-configured fixed-layout backend for the
         * given format key ({@code "pdf"}, {@code "pptx"}, ...).
         */
        FixedLayoutRenderer convenienceBackend(String format);
    }
}
