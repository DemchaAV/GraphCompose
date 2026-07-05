package com.demcha.compose.document.backend.fixed;

import com.demcha.compose.document.layout.LayoutGraph;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.List;

/**
 * A configured fixed-layout backend ready to render a resolved document — the
 * backend-neutral surface the document API's convenience output methods drive.
 *
 * <p>A {@link FixedLayoutBackendProvider} produces one of these from a session's
 * chrome, so {@code DocumentSession} and {@code MultiSectionDocument} can render
 * to bytes, stream, rasterize, or concatenate sections without importing a
 * concrete backend. It extends {@link FixedLayoutBackend} with the extra
 * convenience operations the single {@code render(...)} contract does not
 * cover.</p>
 *
 * @since 2.0.0
 */
public interface FixedLayoutRenderer extends FixedLayoutBackend<byte[]> {

    /**
     * Renders one resolved graph and writes the result to the stream carried by
     * the render context.
     *
     * @param graph   resolved layout graph
     * @param context render-pass configuration and stream target
     * @throws Exception if rendering fails
     */
    void write(LayoutGraph graph, FixedLayoutRenderContext context) throws Exception;

    /**
     * Rasterizes a resolved graph to one image per page (or a single page).
     *
     * @param graph       resolved layout graph
     * @param context     render-pass configuration
     * @param dpi         raster resolution in dots per inch
     * @param transparent {@code true} for an alpha channel instead of a white background
     * @param pageIndex   zero-based page to render, or a negative value for all pages
     * @return one image per rendered page
     * @throws Exception                 if rendering or rasterization fails
     * @throws IndexOutOfBoundsException if {@code pageIndex} is out of range
     */
    List<BufferedImage> renderToImages(LayoutGraph graph,
                                       FixedLayoutRenderContext context,
                                       int dpi,
                                       boolean transparent,
                                       int pageIndex) throws Exception;

    /**
     * Renders several sections concatenated into a single document and returns
     * the bytes.
     *
     * @param sections ordered sections to concatenate
     * @return rendered document bytes
     * @throws Exception if rendering fails
     */
    byte[] renderSections(List<SectionUnit> sections) throws Exception;

    /**
     * Renders several sections concatenated into a single document and streams
     * the result to the caller-owned stream (not closed here).
     *
     * @param sections ordered sections to concatenate
     * @param output   destination stream
     * @throws Exception if rendering fails
     */
    void writeSections(List<SectionUnit> sections, OutputStream output) throws Exception;
}
