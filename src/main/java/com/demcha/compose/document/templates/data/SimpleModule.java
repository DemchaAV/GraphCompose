package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable single-block module used by legacy integrations.
 */
@Data
@NoArgsConstructor
public class SimpleModule {
    private String name;
    private String blockText;
}
