package com.demcha.compose.document.output;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Backend-neutral document metadata applied to rendered output.
 *
 * <p>Mirrors the PDF information dictionary but is renderer-neutral so the
 * canonical API can apply the same value to PDF, DOCX, and any future
 * backend.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentMetadata {
    private final String title;
    private final String author;
    private final String subject;
    private final String keywords;

    @Builder.Default
    private final String creator = "GraphCompose";

    @Builder.Default
    private final String producer = "GraphCompose";

    private DocumentMetadata() {
        this.title = null;
        this.author = null;
        this.subject = null;
        this.keywords = null;
        this.creator = "GraphCompose";
        this.producer = "GraphCompose";
    }
}
