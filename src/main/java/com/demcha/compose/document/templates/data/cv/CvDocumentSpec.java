package com.demcha.compose.document.templates.data.cv;

import com.demcha.compose.document.templates.data.common.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Public compose-first CV input made of one header plus ordered semantic
 * modules.
 *
 * <p><b>Authoring role:</b> gives applications a stable, intuitive model for
 * building CV documents without depending on template-specific field names
 * such as {@code technicalSkills} or {@code professionalExperience}. Legacy
 * {@link MainPageCV} inputs still map into this model through
 * {@link #from(MainPageCV, MainPageCvDTO)}.</p>
 *
 * @param header optional header block rendered at the top of the document
 * @param modules ordered content modules rendered after the header
 * @author Artem Demchyshyn
 */
public record CvDocumentSpec(
        Header header,
        List<CvModule> modules
) {
    /**
     * Creates a normalized document spec.
     */
    public CvDocumentSpec {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    /**
     * Creates a document spec from a header plus ordered modules.
     *
     * @param header optional header block
     * @param modules ordered modules
     * @return immutable document spec
     */
    public static CvDocumentSpec of(Header header, CvModule... modules) {
        return new CvDocumentSpec(header, modules == null ? List.of() : Arrays.asList(modules));
    }

    /**
     * Starts a fluent document builder.
     *
     * @return document builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Maps the legacy standard-CV input into the new ordered module model.
     *
     * @param originalCv source CV input
     * @param rewrittenCv optional rewrite payload merged over the original CV
     * @return compose-first document spec
     */
    public static CvDocumentSpec from(MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        Objects.requireNonNull(originalCv, "originalCv");

        MainPageCV data = rewrittenCv == null ? originalCv : rewrittenCv.merge(originalCv);
        Builder builder = builder().header(data.getHeader());

        if (data.getModuleSummary() != null) {
            builder.addModule(CvModule.builder(data.getModuleSummary().getModuleName())
                    .name("Summary")
                    .paragraph(data.getModuleSummary().getBlockSummary())
                    .build());
        }

        addListModule(builder, "TechnicalSkills", data.getTechnicalSkills(), true);
        addListModule(builder, "Education", data.getEducationCertifications(), false);
        addListModule(builder, "Projects", data.getProjects(), false);
        addListModule(builder, "Experience", data.getProfessionalExperience(), false);
        addListModule(builder, "Additional", data.getAdditional(), false);

        return builder.build();
    }

    private static void addListModule(Builder builder, String moduleName, ModuleYml module, boolean bulletList) {
        if (module == null || module.getModulePoints() == null || module.getModulePoints().isEmpty()) {
            return;
        }

        List<String> points = sanitizeLines(module.getModulePoints());
        if (bulletList) {
            points = normalizeTechnicalSkillPoints(points);
        }
        if (points.isEmpty()) {
            return;
        }

        CvModule.Builder moduleBuilder = CvModule.builder(module.getName()).name(moduleName);
        if (bulletList) {
            moduleBuilder.list(points, list -> list.bullet());
        } else {
            moduleBuilder.rows(points);
        }
        builder.addModule(moduleBuilder.build());
    }

    private static List<String> sanitizeLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            String normalized = Objects.requireNonNullElse(value, "").trim();
            if (!normalized.isBlank()) {
                sanitized.add(normalized);
            }
        }
        return List.copyOf(sanitized);
    }

    private static List<String> normalizeTechnicalSkillPoints(List<String> points) {
        return points.stream()
                .map(CvDocumentSpec::stripLeadingListMarker)
                .filter(point -> !point.isBlank())
                .toList();
    }

    private static String stripLeadingListMarker(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.startsWith("\u2022")) {
            return normalized.substring(1).trim();
        }
        if (normalized.startsWith("- ")) {
            return normalized.substring(2).trim();
        }
        if (normalized.startsWith("* ") && !normalized.startsWith("**")) {
            return normalized.substring(2).trim();
        }
        return normalized;
    }

    /**
     * Builder for ordered compose-first CV document specs.
     */
    public static final class Builder {
        private Header header;
        private final List<CvModule> modules = new ArrayList<>();

        /**
         * Sets the top-level header.
         *
         * @param header header block
         * @return this builder
         */
        public Builder header(Header header) {
            this.header = header;
            return this;
        }

        /**
         * Appends one content module.
         *
         * @param module content module
         * @return this builder
         */
        public Builder addModule(CvModule module) {
            modules.add(Objects.requireNonNull(module, "module"));
            return this;
        }

        /**
         * Appends multiple content modules.
         *
         * @param modules content modules
         * @return this builder
         */
        public Builder modules(CvModule... modules) {
            if (modules != null) {
                this.modules.addAll(Arrays.asList(modules));
            }
            return this;
        }

        /**
         * Builds the immutable document spec.
         *
         * @return immutable document spec
         */
        public CvDocumentSpec build() {
            return new CvDocumentSpec(header, modules);
        }
    }
}
