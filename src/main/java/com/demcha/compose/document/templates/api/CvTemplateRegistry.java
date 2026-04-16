package com.demcha.compose.document.templates.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for canonical CV templates.
 *
 * <p>The registry is intentionally caller-owned: integrations provide the
 * template instances they want to expose, and the registry offers stable lookup
 * by template id.</p>
 *
 * <p><b>Mutability/thread-safety:</b> instances are populated at construction
 * time and then behave as immutable read-mostly registries. Concurrent reads
 * are safe as long as registered template implementations are themselves safe
 * to reuse.</p>
 *
 * <pre>{@code
 * CvTemplateRegistry registry = new CvTemplateRegistry(List.of(
 *         new CvTemplateV1(),
 *         new EditorialBlueCvTemplate()));
 *
 * CvTemplate template = registry.getTemplateOrDefault(requestedId, registry.getDefaultTemplateId());
 * }</pre>
 */
public final class CvTemplateRegistry {
    private final Map<String, CvTemplate> templates = new HashMap<>();

    /**
     * Registers the supplied template instances.
     *
     * @param templateList templates to register
     * @throws NullPointerException if {@code templateList} or one of its elements is {@code null}
     */
    public CvTemplateRegistry(List<CvTemplate> templateList) {
        for (CvTemplate template : templateList) {
            templates.put(template.getTemplateId(), template);
        }
    }

    /**
     * Returns the template registered for the supplied id.
     *
     * @param templateId public template identifier
     * @return registered template instance
     * @throws IllegalArgumentException when the id is not registered
     */
    public CvTemplate getTemplate(String templateId) {
        return Optional.ofNullable(templates.get(templateId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Template not found: " + templateId + ". Available templates: " + templates.keySet()));
    }

    /**
     * Looks up a template and falls back to the supplied default id when needed.
     *
     * @param templateId requested template id
     * @param defaultTemplateId fallback template id
     * @return requested template or fallback template, or {@code null} when neither id is registered
     */
    public CvTemplate getTemplateOrDefault(String templateId, String defaultTemplateId) {
        CvTemplate template = templates.get(templateId);
        return template != null ? template : templates.get(defaultTemplateId);
    }

    /**
     * Lists all registered templates.
     *
     * @return immutable template list
     */
    public List<CvTemplate> getAllTemplates() {
        return List.copyOf(templates.values());
    }

    /**
     * Default built-in template id used by GraphCompose examples.
     *
     * @return default template id
     */
    public String getDefaultTemplateId() {
        return "modern-professional";
    }

    /**
     * Returns whether a template id is currently registered.
     *
     * @param templateId template id to check
     * @return {@code true} when the id is present
     */
    public boolean hasTemplate(String templateId) {
        return templates.containsKey(templateId);
    }
}
