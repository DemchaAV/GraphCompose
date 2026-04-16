package com.demcha.compose.document.templates.data;

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
}
