package com.demcha.compose.document.backend.fixed.pdf.options;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
/**
 * Canonical document-level metadata applied to rendered PDF files.
 *
 * <p>The options map to the PDF information dictionary and do not influence
 * semantic layout or pagination. Instances are immutable and can be reused
 * across multiple document sessions.</p>
 */
public final class PdfMetadataOptions {
    private final String title;
    private final String author;
    private final String subject;
    private final String keywords;

    @Builder.Default
    private final String creator = "GraphCompose";

    @Builder.Default
    private final String producer = "GraphCompose + Apache PDFBox";
}
