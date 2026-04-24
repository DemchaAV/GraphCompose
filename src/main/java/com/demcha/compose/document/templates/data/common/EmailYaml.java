package com.demcha.compose.document.templates.data.common;

import lombok.Data;

/**
 * Mutable email/contact payload for canonical CV and cover-letter templates.
 *
 * <p><b>Pipeline role:</b> carries author-provided email metadata from template
 * inputs into scene composers before any layout decisions are made.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
@Data
public class EmailYaml {
    private String to;
    private String subject;
    private String body;
    private String displayText;

    /**
     * Creates an empty mutable email payload.
     */
    public EmailYaml() {
    }

    /**
     * Starts a fluent email builder.
     *
     * @return email builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a mailto link whose display text is the email address.
     *
     * @param to target email address
     * @return email payload
     */
    public static EmailYaml mailto(String to) {
        return builder()
                .to(to)
                .displayText(to)
                .build();
    }

    /**
     * Creates a mailto link with custom display text.
     *
     * @param to target email address
     * @param displayText visible link text
     * @return email payload
     */
    public static EmailYaml mailto(String to, String displayText) {
        return builder()
                .to(to)
                .displayText(displayText)
                .build();
    }

    /**
     * Fluent builder for email contact metadata.
     */
    public static final class Builder {
        private String to;
        private String subject;
        private String body;
        private String displayText;

        private Builder() {
        }

        /**
         * Sets the target email address.
         *
         * @param to target email address
         * @return this builder
         */
        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Sets the mail subject.
         *
         * @param subject subject text
         * @return this builder
         */
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the mail body.
         *
         * @param body body text
         * @return this builder
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the visible link text.
         *
         * @param displayText visible link text
         * @return this builder
         */
        public Builder displayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

        /**
         * Builds the mutable email payload.
         *
         * @return email payload
         */
        public EmailYaml build() {
            EmailYaml email = new EmailYaml();
            email.setTo(to);
            email.setSubject(subject);
            email.setBody(body);
            email.setDisplayText(displayText);
            return email;
        }
    }
}
