package com.demcha.compose.document.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link MultiSectionDocument} from independently authored
 * {@link DocumentSession} sections, in the order they are added.
 *
 * <p>Each section is a fully configured session — its own page size, margins,
 * fonts, footer / page numbering, and other chrome. Author each section as a
 * normal document, then add it here; the resulting {@link MultiSectionDocument}
 * concatenates them into one PDF with cross-section navigation.</p>
 *
 * <pre>{@code
 * DocumentSession cover = GraphCompose.document().pageSize(612, 792).margin(DocumentInsets.of(0)).create();
 * cover.pageFlow(page -> page.addSection(s -> s.anchor("cover").addParagraph("Title")));
 *
 * DocumentSession body = GraphCompose.document().pageSize(420, 595).margin(DocumentInsets.of(36)).create();
 * body.footer(DocumentHeaderFooter.builder().centerText("{page}").build());
 * body.pageFlow(page -> page.addSection(s -> s.anchor("intro").addParagraph("Chapter 1")));
 *
 * try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
 *     doc.buildPdf();
 * }
 * }</pre>
 *
 * <p>Obtain a builder from {@link com.demcha.compose.GraphCompose#documents()}.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class MultiSectionDocumentBuilder {

    private final Path outputFile;
    private final List<DocumentSession> sections = new ArrayList<>();

    /**
     * Creates a builder with no default output file. Prefer
     * {@link com.demcha.compose.GraphCompose#documents()}.
     */
    public MultiSectionDocumentBuilder() {
        this(null);
    }

    /**
     * Creates a builder with a default output file used by
     * {@link MultiSectionDocument#buildPdf()}. Prefer
     * {@link com.demcha.compose.GraphCompose#documents(Path)}.
     *
     * @param outputFile default PDF output path, or {@code null}
     */
    public MultiSectionDocumentBuilder(Path outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Appends a fully authored section. Ownership transfers to the resulting
     * {@link MultiSectionDocument}, which closes the section in its own
     * {@link MultiSectionDocument#close()}.
     *
     * @param section the section's document session
     * @return this builder
     */
    public MultiSectionDocumentBuilder section(DocumentSession section) {
        sections.add(Objects.requireNonNull(section, "section"));
        return this;
    }

    /**
     * Creates the multi-section document.
     *
     * @return the assembled document
     * @throws IllegalStateException if no sections were added
     */
    public MultiSectionDocument create() {
        if (sections.isEmpty()) {
            throw new IllegalStateException("A multi-section document needs at least one section.");
        }
        return new MultiSectionDocument(outputFile, sections);
    }
}
