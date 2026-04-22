package com.demcha.compose.document.templates.data.cv;

import com.demcha.compose.document.templates.data.common.Header;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable override payload for patching a source CV before canonical composition.
 *
 * <p><b>Pipeline role:</b> lets callers override only selected CV sections,
 * then merges those overrides into a resolved {@link MainPageCV} instance for
 * downstream template scene building.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
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
