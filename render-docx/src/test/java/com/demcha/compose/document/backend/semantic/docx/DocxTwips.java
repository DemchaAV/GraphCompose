package com.demcha.compose.document.backend.semantic.docx;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads a twip value back out of an exported file.
 *
 * <p>XmlBeans hands a measure back as the schema's union, and the export only ever writes
 * plain twips, which come back as a {@link Number}. Reading one as text and parsing it —
 * what these tests used to do, each helper its own copy — works until the value is not a
 * number, and then fails as a parse error far from what was wrong. Asserting the type says
 * what was wrong.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxTwips {

    private DocxTwips() {
    }

    /**
     * @param measure a measure read from the file
     * @return its value in twips
     */
    static long of(Object measure) {
        assertThat(measure)
                .as("a twip value this export writes as a plain number")
                .isInstanceOf(Number.class);
        return ((Number) measure).longValue();
    }
}
