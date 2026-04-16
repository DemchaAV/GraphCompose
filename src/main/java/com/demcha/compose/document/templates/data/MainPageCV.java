package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable aggregate input for the canonical standard CV templates.
 *
 * <p><b>Pipeline role:</b> groups header, summary, and section modules into
 * one authoring payload consumed by canonical template composers.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class MainPageCV {
    private Header header;
    private ModuleSummary moduleSummary;
    private ModuleYml technicalSkills;
    private ModuleYml educationCertifications;
    private ModuleYml projects;
    private ModuleYml professionalExperience;
    private ModuleYml additional;
}
