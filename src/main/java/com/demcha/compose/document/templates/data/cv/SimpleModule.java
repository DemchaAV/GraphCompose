package com.demcha.compose.document.templates.data.cv;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable single-block template module kept for bridge-friendly canonical inputs.
 *
 * <p><b>Pipeline role:</b> represents one titled block of text that can be
 * mapped from legacy DTOs into canonical template composition.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class SimpleModule {
    private String name;
    private String blockText;
}
