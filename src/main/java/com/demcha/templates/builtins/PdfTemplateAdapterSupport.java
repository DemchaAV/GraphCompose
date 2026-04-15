package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Shared support for thin compose-first template adapters that still expose
 * deprecated PDF-centric convenience overloads.
 */
abstract class PdfTemplateAdapterSupport {

    @FunctionalInterface
    protected interface ComposerAction {
        void compose(DocumentComposer composer) throws Exception;
    }

    @FunctionalInterface
    protected interface PdfComposerFactory {
        PdfComposer create(Path path, boolean guideLines);
    }

    @FunctionalInterface
    protected interface SessionAction {
        void compose(DocumentSession session) throws Exception;
    }

    protected final PDDocument renderToDocument(boolean guideLines,
                                                String failureMessage,
                                                PdfComposerFactory factory,
                                                ComposerAction action) {
        PdfComposer composer = null;
        try {
            composer = factory.create(null, guideLines);
            action.compose(composer);
            return composer.toPDDocument();
        } catch (Exception e) {
            if (composer != null) {
                closeQuietly(composer, e);
            }
            throw new RuntimeException(failureMessage, e);
        }
    }

    protected final void renderToFile(Path path,
                                      boolean guideLines,
                                      String failureMessage,
                                      String successMessage,
                                      PdfComposerFactory factory,
                                      ComposerAction action) {
        try (PdfComposer composer = factory.create(path, guideLines)) {
            action.compose(composer);
            composer.build();
            logger().info(successMessage, path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    protected final PDDocument renderToDocumentSession(boolean guideLines,
                                                       String failureMessage,
                                                       PDRectangle pageSize,
                                                       float top,
                                                       float right,
                                                       float bottom,
                                                       float left,
                                                       SessionAction action) {
        try (DocumentSession session = createDocumentSession(null, pageSize, top, right, bottom, left)) {
            action.compose(session);
            return Loader.loadPDF(session.toPdfBytes());
        } catch (Exception e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    protected final void renderToFileSession(Path path,
                                             boolean guideLines,
                                             String failureMessage,
                                             String successMessage,
                                             PDRectangle pageSize,
                                             float top,
                                             float right,
                                             float bottom,
                                             float left,
                                             SessionAction action) {
        try (DocumentSession session = createDocumentSession(path, pageSize, top, right, bottom, left)) {
            action.compose(session);
            session.buildPdf(path);
            logger().info(successMessage, path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    protected final PdfComposer createPdfComposer(Path path,
                                                  boolean guideLines,
                                                  PDRectangle pageSize,
                                                  float margin) {
        return createPdfComposer(path, guideLines, pageSize, margin, margin, margin, margin);
    }

    protected final PdfComposer createPdfComposer(Path path,
                                                  boolean guideLines,
                                                  PDRectangle pageSize,
                                                  float top,
                                                  float right,
                                                  float bottom,
                                                  float left) {
        GraphCompose.PdfBuilder builder = path != null ? GraphCompose.pdf(path) : GraphCompose.pdf();
        return builder.pageSize(pageSize)
                .margin(top, right, bottom, left)
                .markdown(true)
                .guideLines(guideLines)
                .create();
    }

    protected final DocumentSession createDocumentSession(Path path,
                                                          PDRectangle pageSize,
                                                          float top,
                                                          float right,
                                                          float bottom,
                                                          float left) {
        GraphCompose.DocumentBuilder builder = path != null ? GraphCompose.document(path) : GraphCompose.document();
        return builder.pageSize(pageSize)
                .margin(top, right, bottom, left)
                .create();
    }

    private Logger logger() {
        return LoggerFactory.getLogger(getClass());
    }

    private void closeQuietly(PdfComposer composer, Exception failure) {
        try {
            composer.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
