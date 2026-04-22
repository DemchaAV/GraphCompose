package com.demcha.compose.document.templates.data.cv;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable summary section payload for canonical CV templates.
 *
 * <p><b>Pipeline role:</b> carries the summary heading and body block that
 * shared scene composers place near the top of a CV.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class ModuleSummary {
    private String moduleName;
    private String blockSummary;
}
