package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable top-level CV document input.
 */
@Data
@NoArgsConstructor
public class MainPageCV {
    private Header header;
    private ModuleSummary moduleSummary;
    private ModuleYml technicalSkills;
    private ModuleYml educationCertifications;
    private ModuleYml projects;
    private ModuleYml professionalExperience;
    private ModuleYml additional;
}
