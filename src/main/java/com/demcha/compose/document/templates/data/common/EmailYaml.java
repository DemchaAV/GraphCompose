package com.demcha.compose.document.templates.data.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable email/contact payload for canonical CV and cover-letter templates.
 *
 * <p><b>Pipeline role:</b> carries author-provided email metadata from template
 * inputs into scene composers before any layout decisions are made.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class EmailYaml {
    private String to;
    private String subject;
    private String body;
    private String displayText;

    public static Builder builder() {
        return new Builder();
    }

    public static EmailYaml mailto(String to) {
        return builder()
                .to(to)
                .displayText(to)
                .build();
    }

    public static EmailYaml mailto(String to, String displayText) {
        return builder()
                .to(to)
                .displayText(displayText)
                .build();
    }

    public static final class Builder {
        private String to;
        private String subject;
        private String body;
        private String displayText;

        private Builder() {
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder displayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

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
