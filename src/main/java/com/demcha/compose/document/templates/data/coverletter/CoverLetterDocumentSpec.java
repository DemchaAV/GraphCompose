package com.demcha.compose.document.templates.data.coverletter;

import com.demcha.compose.document.templates.data.common.Header;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public compose-first cover-letter input.
 *
 * <p><b>Authoring role:</b> keeps cover-letter composition document-shaped:
 * one header, one body, and optional job context for personalization.</p>
 *
 * @param header contact/profile header rendered before the letter body
 * @param body cover-letter body text
 * @param jobDetails target role metadata used by templates
 * @author Artem Demchyshyn
 */
public record CoverLetterDocumentSpec(
        Header header,
        String body,
        JobDetails jobDetails
) {

    /**
     * Creates a normalized cover-letter spec.
     */
    public CoverLetterDocumentSpec {
        body = Objects.requireNonNullElse(body, "");
    }

    /**
     * Creates a document spec from the common cover-letter inputs.
     *
     * @param header contact/profile header
     * @param body cover-letter body text
     * @param jobDetails target role metadata
     * @return document spec
     */
    public static CoverLetterDocumentSpec of(Header header, String body, JobDetails jobDetails) {
        return new CoverLetterDocumentSpec(header, body, jobDetails);
    }

    /**
     * Starts a fluent cover-letter spec builder.
     *
     * @return document builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for cover-letter document specs.
     */
    public static final class Builder {
        private Header header;
        private String body;
        private JobDetails jobDetails;

        private Builder() {
        }

        public Builder header(Header header) {
            this.header = header;
            return this;
        }

        public Builder header(Consumer<Header.Builder> spec) {
            Header.Builder builder = Header.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return header(builder.build());
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder letter(String body) {
            return body(body);
        }

        public Builder jobDetails(JobDetails jobDetails) {
            this.jobDetails = jobDetails;
            return this;
        }

        public Builder job(Consumer<JobDetails.Builder> spec) {
            JobDetails.Builder builder = JobDetails.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return jobDetails(builder.build());
        }

        public CoverLetterDocumentSpec build() {
            return new CoverLetterDocumentSpec(header, body, jobDetails);
        }
    }
}
