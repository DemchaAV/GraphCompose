package com.demcha.compose.document.api;

import com.demcha.compose.document.backend.fixed.BackendProviders;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderer;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.exceptions.DocumentRenderingException;
import com.demcha.compose.document.output.DocumentDebugOptions;
import com.demcha.compose.document.output.DocumentOutputOptions;
import com.demcha.compose.document.snapshot.LayoutSnapshot;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A single PDF assembled from several independent {@link DocumentSession}
 * sections, each with its own page size, margins, fonts, and chrome (header /
 * footer / page numbering / watermark).
 *
 * <p>The sections are concatenated <em>inside the engine</em> — there is no
 * external PDF merge — so anchors, internal links, and the bookmark outline
 * resolve across section boundaries against the combined document. This lets a
 * full-bleed cover (one page size) sit in front of a margined body (another
 * page size) with continuous, clickable navigation, which a single
 * {@link DocumentSession} cannot express because its geometry is document-wide.</p>
 *
 * <p>Per-section page numbering follows each section's own footer configuration:
 * the {@code {page}} / {@code {pages}} tokens count from that section's first
 * page, so a roman-numbered front matter can precede an arabic-numbered body.
 * Document-global concerns — PDF metadata and protection — are taken from the
 * first section that declares them. Anchor names must be unique across sections;
 * a collision keeps the last section's anchor (and logs a warning). Custom font
 * families are merged across sections by name; if two sections define the same
 * family differently, the first definition wins.</p>
 *
 * <p>This document owns its sections: {@link #close()} closes every section.
 * Build it with {@link com.demcha.compose.GraphCompose#documents()}.</p>
 *
 * <p><b>Thread-safety:</b> single-threaded, like {@link DocumentSession}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class MultiSectionDocument implements AutoCloseable {

    private final Path defaultOutputFile;
    private final List<DocumentSession> sections;
    private final FixedLayoutRenderer backend =
            BackendProviders.fixedLayout().create(DocumentOutputOptions.EMPTY, DocumentDebugOptions.none());
    private boolean closed;

    MultiSectionDocument(Path defaultOutputFile, List<DocumentSession> sections) {
        this.defaultOutputFile = defaultOutputFile;
        this.sections = List.copyOf(sections);
    }

    /**
     * Renders the combined multi-section document to PDF bytes.
     *
     * @return rendered PDF bytes
     * @throws DocumentRenderingException if rendering fails
     */
    public byte[] toPdfBytes() throws DocumentRenderingException {
        return render("render PDF bytes", () -> backend.renderSections(renderUnits()));
    }

    /**
     * Streams the combined document to the caller-owned stream (not closed here).
     *
     * @param output destination stream that receives the rendered PDF bytes
     * @throws DocumentRenderingException if rendering fails
     */
    public void writePdf(OutputStream output) throws DocumentRenderingException {
        Objects.requireNonNull(output, "output");
        render("write PDF to stream", () -> {
            backend.writeSections(renderUnits(), output);
            return null;
        });
    }

    /**
     * Builds the combined document into the default output file configured on the
     * builder.
     *
     * @throws IllegalStateException      if no default output file was configured
     * @throws DocumentRenderingException if rendering fails
     */
    public void buildPdf() throws DocumentRenderingException {
        ensureOpen();
        if (defaultOutputFile == null) {
            throw new IllegalStateException(
                    "No default output file was configured for this multi-section document.");
        }
        buildPdf(defaultOutputFile);
    }

    /**
     * Builds the combined document into the supplied output file.
     *
     * @param outputFile destination PDF path
     * @throws DocumentRenderingException if rendering fails
     */
    public void buildPdf(Path outputFile) throws DocumentRenderingException {
        Objects.requireNonNull(outputFile, "outputFile");
        render("build PDF at '" + outputFile + "'", () -> {
            try (OutputStream output = Files.newOutputStream(outputFile)) {
                backend.writeSections(renderUnits(), output);
            }
            return null;
        });
    }

    /**
     * Returns one {@link LayoutSnapshot} per section, in document order, for
     * layout regression testing.
     *
     * @return per-section layout snapshots
     */
    public List<LayoutSnapshot> sectionSnapshots() {
        ensureOpen();
        return sections.stream().map(DocumentSession::layoutSnapshot).toList();
    }

    /**
     * Closes every section. Idempotent; the first failure is rethrown after all
     * sections have been attempted.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        for (DocumentSession section : sections) {
            try {
                section.close();
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = ex;
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private List<SectionUnit> renderUnits() {
        ensureOpen();
        return sections.stream().map(DocumentSession::toSectionRenderUnit).toList();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("This multi-section document has been closed.");
        }
    }

    private static <R> R render(String action, RenderingBody<R> body) throws DocumentRenderingException {
        try {
            return body.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRenderingException("Failed to " + action + ": " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface RenderingBody<R> {
        R run() throws Exception;
    }
}
