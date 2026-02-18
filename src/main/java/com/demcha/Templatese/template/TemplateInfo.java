package com.demcha.Templatese.template;

/**
 * DTO for template information returned by the API.
 */
public record TemplateInfo(
        String id,
        String name,
        String description,
        boolean isDefault) {
    public static TemplateInfo from(CvTemplate template, String defaultTemplateId) {
        return new TemplateInfo(
                template.getTemplateId(),
                template.getTemplateName(),
                template.getDescription(),
                template.getTemplateId().equals(defaultTemplateId));
    }
}
