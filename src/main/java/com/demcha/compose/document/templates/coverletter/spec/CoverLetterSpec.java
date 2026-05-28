package com.demcha.compose.document.templates.coverletter.spec;

import java.util.List;
import java.util.Objects;

/**
 * User-facing data record for a cover letter in Templates v2.
 *
 * <p>A cover letter is structurally simpler than a CV: a header
 * identifying the writer, a greeting line ({@code "Dear Hiring
 * Manager,"}), a sequence of body paragraphs, and a closing line
 * ({@code "Sincerely, Artem"}). Each segment is rendered by the
 * paired preset using the same typography palette as its CV
 * counterpart so a writer can ship a CV and matching cover letter
 * with consistent visual identity.</p>
 *
 * @param header          identity block (required)
 * @param greeting        first body line (required, may be blank to
 *                        suppress); typically {@code "Dear Hiring
 *                        Manager,"}
 * @param bodyParagraphs  ordered list of body paragraphs; blank
 *                        paragraphs are kept to preserve the
 *                        writer's intended whitespace; may carry
 *                        markdown markers ({@code **bold**},
 *                        {@code *italic*})
 * @param closing         last body line (required, may be blank);
 *                        typically {@code "Sincerely, Alex"}
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard) — the layered model
 *             {@link com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument}
 *             plus the {@code coverletter.v2} presets. Kept for backward
 *             compatibility; scheduled for removal in a future major. See
 *             {@code docs/templates/v2-layered/}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public record CoverLetterSpec(
        CoverLetterHeader header,
        String greeting,
        List<String> bodyParagraphs,
        String closing) {

    /**
     * Compact constructor that normalises null strings to empty and
     * defensively copies the body paragraphs list.
     *
     * @throws NullPointerException if {@code header} or {@code bodyParagraphs} is null
     */
    public CoverLetterSpec {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(bodyParagraphs, "bodyParagraphs");
        greeting = greeting == null ? "" : greeting;
        closing = closing == null ? "" : closing;
        bodyParagraphs = List.copyOf(bodyParagraphs);
    }

    /**
     * Returns a fluent builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link CoverLetterSpec}.
     */
    public static final class Builder {
        private CoverLetterHeader header;
        private String greeting = "";
        private final java.util.List<String> bodyParagraphs = new java.util.ArrayList<>();
        private String closing = "";

        private Builder() {
        }

        /**
         * Sets the document header.
         *
         * @param value non-null header
         * @return this builder
         */
        public Builder header(CoverLetterHeader value) {
            this.header = value;
            return this;
        }

        /**
         * Sets the greeting line.
         *
         * @param value greeting; null treated as empty
         * @return this builder
         */
        public Builder greeting(String value) {
            this.greeting = value == null ? "" : value;
            return this;
        }

        /**
         * Appends one body paragraph.
         *
         * @param value non-null paragraph text
         * @return this builder
         */
        public Builder paragraph(String value) {
            Objects.requireNonNull(value, "paragraph");
            this.bodyParagraphs.add(value);
            return this;
        }

        /**
         * Sets the closing line.
         *
         * @param value closing; null treated as empty
         * @return this builder
         */
        public Builder closing(String value) {
            this.closing = value == null ? "" : value;
            return this;
        }

        /**
         * Builds an immutable {@link CoverLetterSpec}.
         *
         * @return new spec
         */
        public CoverLetterSpec build() {
            return new CoverLetterSpec(header, greeting, bodyParagraphs, closing);
        }
    }
}
