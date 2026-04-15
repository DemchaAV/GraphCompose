package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable summary module used by CV templates.
 */
@Data
@NoArgsConstructor
public class ModuleSummary {
    private String moduleName;
    private String blockSummary;
}
