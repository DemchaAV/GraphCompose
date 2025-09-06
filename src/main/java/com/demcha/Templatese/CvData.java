package com.demcha.Templatese;

import com.demcha.Templatese.data.Heder;
import com.demcha.Templatese.data.ModuleSummary;
import com.demcha.Templatese.data.ModuleYml;
import com.demcha.Templatese.data.SimpleModule;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CvData {
    private Heder heder;                          // header
    private ModuleSummary moduleSummary;          // professionalSummary
    private ModuleYml technicalSkills;            // technicalSkills
    private ModuleYml educationCertifications;    // educationCertifications
    private ModuleYml projects;                   // projects
    private ModuleYml professionalExperience;     // professionalExperience
    private ModuleYml additional;              // additional
}
