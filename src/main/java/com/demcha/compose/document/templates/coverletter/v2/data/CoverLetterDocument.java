package com.demcha.compose.document.templates.coverletter.v2.data;

import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User-facing data record for a Templates v2 cover letter.
 *
 * <p>Reuses {@link CvIdentity} for the top-of-document identity block
 * so a writer hands the <em>same</em> identity object to both their CV
 * preset and their paired cover-letter preset. Because the masthead
 * (name, contact, links) then renders through the identical widget
 * path, the CV and the letter read as one matched set — which is the
 * whole point of pairing them.</p>
 *
 * <p>The letter-specific content is deliberately tiny: an opening
 * greeting, an ordered list of body paragraphs, and a closing sign-off.
 * This mirrors {@code CvDocument} (identity + sections) but with the
 * far simpler single-flow shape of a letter.</p>
 *
 * @param identity top-of-document identity block (required) — share
 *                 the same instance with the paired CV preset
 * @param greeting opening line (e.g. {@code "Dear Hiring Team,"}); a
 *                 blank value suppresses the line; may carry inline
 *                 markdown ({@code **bold**}, {@code *italic*})
 * @param body     ordered body paragraphs; blank paragraphs are skipped
 *                 at render; each may carry inline markdown
 * @param closing  sign-off line (e.g. {@code "Sincerely, Alex"}); a
 *                 blank value suppresses the line; may carry inline
 *                 markdown
 */
public record CoverLetterDocument(CvIdentity identity,
                                  String greeting,
                                  List<String> body,
                                  String closing) {

    /**
     * Compact constructor that normalises null strings to empty and
     * defensively copies the body list.
     *
     * @throws NullPointerException if {@code identity} is null
     */
    public CoverLetterDocument {
        Objects.requireNonNull(identity, "identity");
        greeting = greeting == null ? "" : greeting;
        closing = closing == null ? "" : closing;
        body = body == null ? List.of() : List.copyOf(body);
    }

    /**
     * Creates a new fluent builder.
     *
     * @return new fluent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link CoverLetterDocument}.
     */
    public static final class Builder {
        private CvIdentity identity;
        private String greeting = "";
        private final List<String> body = new ArrayList<>();
        private String closing = "";

        private Builder() {
        }

        /**
         * Sets the shared identity block.
         *
         * @param value non-null identity (reuse the paired CV's instance)
         * @return this builder
         */
        public Builder identity(CvIdentity value) {
            this.identity = value;
            return this;
        }

        /**
         * Sets the opening greeting line.
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
            this.body.add(value);
            return this;
        }

        /**
         * Sets the closing sign-off line.
         *
         * @param value closing; null treated as empty
         * @return this builder
         */
        public Builder closing(String value) {
            this.closing = value == null ? "" : value;
            return this;
        }

        /**
         * Builds an immutable {@link CoverLetterDocument}.
         *
         * @return new document
         */
        public CoverLetterDocument build() {
            return new CoverLetterDocument(identity, greeting, body, closing);
        }
    }
}
