package com.demcha.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MainPageCV {
    private Header header;                          // header
    private ModuleSummary moduleSummary;          // professionalSummary
    private ModuleYml technicalSkills;            // technicalSkills
    private ModuleYml educationCertifications;    // educationCertifications
    private ModuleYml projects;                   // projects
    private ModuleYml professionalExperience;     // professionalExperience
    private ModuleYml additional;              // additional
}
