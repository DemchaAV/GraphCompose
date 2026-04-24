package com.demcha.compose.document.backend.fixed.pdf.options;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Canonical document-level metadata applied to rendered PDF files.
 *
 * <p>The options map to the PDF information dictionary and do not influence
 * semantic layout or pagination. Instances are immutable and can be reused
 * across multiple document sessions.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PdfMetadataOptions {
    private final String title;
    private final String author;
    private final String subject;
    private final String keywords;

    @Builder.Default
    private final String creator = "GraphCompose";

    @Builder.Default
    private final String producer = "GraphCompose + Apache PDFBox";

    private PdfMetadataOptions() {
        this.title = null;
        this.author = null;
        this.subject = null;
        this.keywords = null;
        this.creator = "GraphCompose";
        this.producer = "GraphCompose + Apache PDFBox";
    }
}
