package com.demcha.templates.api;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for available CV templates.
 * Templates are provided explicitly by the caller and looked up by template ID.
 *
 * @deprecated Use {@link com.demcha.compose.document.templates.api.CvTemplateRegistry}
 *             instead.
 */
@Deprecated(forRemoval = false)
public class CvTemplateRegistry {

    private final Map<String, CvTemplate> templates = new HashMap<>();

    /**
     * Constructor that registers the provided template instances.
     */
    public CvTemplateRegistry(List<CvTemplate> templateList) {
        for (CvTemplate template : templateList) {
            templates.put(template.getTemplateId(), template);
        }
    }

    /**
     * Gets a template by its ID.
     * 
     * @param templateId The template identifier
     * @return The template, or throws if not found
     */
    public CvTemplate getTemplate(String templateId) {
        return Optional.ofNullable(templates.get(templateId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Template not found: " + templateId + ". Available templates: " + templates.keySet()));
    }

    /**
     * Gets a template by ID, with a fallback to default template.
     */
    public CvTemplate getTemplateOrDefault(String templateId, String defaultTemplateId) {
        CvTemplate template = templates.get(templateId);
        if (template != null) {
            return template;
        }
        return templates.get(defaultTemplateId);
    }

    /**
     * Lists all available templates.
     */
    public List<CvTemplate> getAllTemplates() {
        return List.copyOf(templates.values());
    }

    /**
     * Gets the default template ID.
     */
    public String getDefaultTemplateId() {
        return "modern-professional";
    }

    /**
     * Checks if a template exists.
     */
    public boolean hasTemplate(String templateId) {
        return templates.containsKey(templateId);
    }
}
