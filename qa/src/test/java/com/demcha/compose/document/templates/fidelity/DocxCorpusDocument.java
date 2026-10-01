package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * One document of the DOCX fidelity corpus: a template preset composed on its fixture.
 *
 * @param family  the template family, such as {@code cv} or {@code invoice}
 * @param name    the preset, unique within its family
 * @param margin  the page margin on A4, in points, or a negative value for the session's own
 *                page and margins
 * @param compose composes the document into a fresh session
 */
public record DocxCorpusDocument(String family, String name, double margin, Consumer<DocumentSession> compose) {

    public DocxCorpusDocument {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(compose, "compose");
    }

    /** The file stem the document's PDF and DOCX are written under. */
    public String stem() {
        return family + "-" + name;
    }
}
