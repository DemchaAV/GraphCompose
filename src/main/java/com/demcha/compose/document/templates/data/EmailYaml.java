package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable email/contact DTO used by CV and cover-letter inputs.
 */
@Data
@NoArgsConstructor
public class EmailYaml {
    private String to;
    private String subject;
    private String body;
    private String displayText;
}
