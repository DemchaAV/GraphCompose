package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable CV rewrite DTO used to override subsets of a source CV before
 * template composition.
 */
@Data
@NoArgsConstructor
public class MainPageCvDTO {
    private Header header;
    private ModuleSummary moduleSummary;
    private ModuleYml technicalSkills;
    private ModuleYml educationCertifications;
    private ModuleYml projects;
    private ModuleYml professionalExperience;
    private ModuleYml additional;

    /**
     * Merges this rewrite DTO over the supplied original CV.
     *
     * @param originalCv original CV data
     * @return merged CV data ready for composition
     */
    public MainPageCV merge(MainPageCV originalCv) {
        if (originalCv == null) {
            throw new IllegalArgumentException("originalCv cannot be null");
        }

        MainPageCV merged = new MainPageCV();
        merged.setHeader(header != null ? header : originalCv.getHeader());
        merged.setModuleSummary(moduleSummary != null ? moduleSummary : originalCv.getModuleSummary());
        merged.setTechnicalSkills(technicalSkills != null ? technicalSkills : originalCv.getTechnicalSkills());
        merged.setEducationCertifications(
                educationCertifications != null ? educationCertifications : originalCv.getEducationCertifications());
        merged.setProjects(projects != null ? projects : originalCv.getProjects());
        merged.setProfessionalExperience(
                professionalExperience != null ? professionalExperience : originalCv.getProfessionalExperience());
        merged.setAdditional(additional != null ? additional : originalCv.getAdditional());
        return merged;
    }

    /**
     * Creates a rewrite DTO that mirrors the supplied CV.
     *
     * @param cv source CV data
     * @return DTO copy of the supplied CV
     */
    public static MainPageCvDTO from(MainPageCV cv) {
        MainPageCvDTO dto = new MainPageCvDTO();
        dto.setHeader(cv.getHeader());
        dto.setModuleSummary(cv.getModuleSummary());
        dto.setTechnicalSkills(cv.getTechnicalSkills());
        dto.setEducationCertifications(cv.getEducationCertifications());
        dto.setProjects(cv.getProjects());
        dto.setProfessionalExperience(cv.getProfessionalExperience());
        dto.setAdditional(cv.getAdditional());
        return dto;
    }
}
