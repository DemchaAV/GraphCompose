package com.demcha.compose.document.templates.data.cv;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
/**
 * Mutable named list section used across canonical CV template modules.
 *
 * <p><b>Pipeline role:</b> captures a section title plus ordered content lines
 * that shared template composers normalize into paragraphs, bullets, or tables.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class ModuleYml {
    private String name;
    private List<String> modulePoints = new ArrayList<>();
}
