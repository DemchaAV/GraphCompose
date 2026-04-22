package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfMetadataOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfProtectionOptions;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfWatermarkOptions;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers.PdfHeaderFooterRenderer;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.helpers.PdfWatermarkRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Applies canonical document-level PDF chrome after the semantic backend has
 * finished fragment rendering.
 *
 * <p>The processor reuses the production-grade PDF backend helpers internally
 * while exposing only canonical {@code document.*} DTOs to callers.</p>
 */
public final class PdfDocumentPostProcessor {
    private PdfDocumentPostProcessor() {
    }

    /**
     * Applies canonical document-level PDF options to an already rendered
     * PDFBox document in place.
     *
     * @param document target PDFBox document
     * @param canvas semantic layout canvas used to derive content margins
     * @param metadataOptions canonical metadata options, or {@code null}
     * @param watermarkOptions canonical watermark options, or {@code null}
     * @param protectionOptions canonical protection options, or {@code null}
     * @param headerFooterOptions repeating header/footer options
     * @throws IOException if PDFBox post-processing fails
     */
    public static void apply(PDDocument document,
                             LayoutCanvas canvas,
                             PdfMetadataOptions metadataOptions,
                             PdfWatermarkOptions watermarkOptions,
                             PdfProtectionOptions protectionOptions,
                             Collection<PdfHeaderFooterOptions> headerFooterOptions) throws IOException {
        if (watermarkOptions != null) {
            PdfWatermarkRenderer.apply(document, PdfOptionsAdapter.toEngine(watermarkOptions));
        }

        if (headerFooterOptions != null && !headerFooterOptions.isEmpty()) {
            Margin canvasMargin = canvas.margin();
            float marginLeft = canvasMargin != null ? (float) canvasMargin.left() : 24f;
            float marginRight = canvasMargin != null ? (float) canvasMargin.right() : 24f;
            List<com.demcha.compose.layout_core.components.content.header_footer.HeaderFooterConfig> configs =
                    headerFooterOptions.stream()
                            .map(PdfOptionsAdapter::toEngine)
                            .toList();
            PdfHeaderFooterRenderer.apply(document, configs, marginLeft, marginRight);
        }

        if (metadataOptions != null) {
            PDDocumentInformation info = document.getDocumentInformation();
            if (metadataOptions.getTitle() != null) {
                info.setTitle(metadataOptions.getTitle());
            }
            if (metadataOptions.getAuthor() != null) {
                info.setAuthor(metadataOptions.getAuthor());
            }
            if (metadataOptions.getSubject() != null) {
                info.setSubject(metadataOptions.getSubject());
            }
            if (metadataOptions.getKeywords() != null) {
                info.setKeywords(metadataOptions.getKeywords());
            }
            if (metadataOptions.getCreator() != null) {
                info.setCreator(metadataOptions.getCreator());
            }
            if (metadataOptions.getProducer() != null) {
                info.setProducer(metadataOptions.getProducer());
            }
        }

        if (protectionOptions != null) {
            applyProtection(document, protectionOptions);
        }
    }

    /**
     * Applies canonical document-level PDF options to already rendered PDF bytes
     * and returns a new byte array.
     *
     * @param pdfBytes rendered PDF bytes
     * @param canvas semantic layout canvas used to derive content margins
     * @param metadataOptions canonical metadata options, or {@code null}
     * @param watermarkOptions canonical watermark options, or {@code null}
     * @param protectionOptions canonical protection options, or {@code null}
     * @param headerFooterOptions repeating header/footer options
     * @return post-processed PDF bytes
     * @throws IOException if the PDF cannot be loaded or post-processed
     */
    public static byte[] apply(byte[] pdfBytes,
                               LayoutCanvas canvas,
                               PdfMetadataOptions metadataOptions,
                               PdfWatermarkOptions watermarkOptions,
                               PdfProtectionOptions protectionOptions,
                               Collection<PdfHeaderFooterOptions> headerFooterOptions) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            apply(document, canvas, metadataOptions, watermarkOptions, protectionOptions, headerFooterOptions);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }

    /**
     * Returns {@code true} when any document-level post-processing option is set.
     *
     * @param metadataOptions canonical metadata options, or {@code null}
     * @param watermarkOptions canonical watermark options, or {@code null}
     * @param protectionOptions canonical protection options, or {@code null}
     * @param headerFooterOptions repeating header/footer options
     * @return {@code true} when post-processing work is required
     */
    public static boolean hasPostProcessing(PdfMetadataOptions metadataOptions,
                                            PdfWatermarkOptions watermarkOptions,
                                            PdfProtectionOptions protectionOptions,
                                            Collection<PdfHeaderFooterOptions> headerFooterOptions) {
        return metadataOptions != null
                || watermarkOptions != null
                || protectionOptions != null
                || (headerFooterOptions != null && !headerFooterOptions.isEmpty());
    }

    private static void applyProtection(PDDocument document, PdfProtectionOptions options) throws IOException {
        AccessPermission permission = new AccessPermission();
        permission.setCanPrint(options.isCanPrint());
        permission.setCanExtractContent(options.isCanCopyContent());
        permission.setCanModify(options.isCanModify());
        permission.setCanFillInForm(options.isCanFillForms());
        permission.setCanExtractForAccessibility(options.isCanExtractForAccessibility());
        permission.setCanAssembleDocument(options.isCanAssemble());
        permission.setCanPrintFaithful(options.isCanPrintHighQuality());

        StandardProtectionPolicy policy = new StandardProtectionPolicy(
                options.getOwnerPassword(),
                options.getUserPassword(),
                permission);
        policy.setEncryptionKeyLength(options.getKeyLength());
        document.protect(policy);
    }
}
