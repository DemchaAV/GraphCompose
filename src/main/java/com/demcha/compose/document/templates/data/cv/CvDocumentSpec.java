package com.demcha.compose.document.templates.data.cv;

import com.demcha.compose.document.templates.data.common.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public compose-first CV input made of one header plus ordered semantic
 * modules.
 *
 * <p><b>Authoring role:</b> gives applications a stable, intuitive model for
 * building CV documents without depending on template-specific field names
 * such as {@code technicalSkills} or {@code professionalExperience}.</p>
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
     * Builder for ordered compose-first CV document specs.
     */
    public static final class Builder {
        private Header header;
        private final List<CvModule> modules = new ArrayList<>();

        private Builder() {
        }

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
         * Appends a configurable semantic module.
         *
         * @param title visible module title
         * @param spec module configuration
         * @return this builder
         */
        public Builder module(String title, Consumer<CvModule.Builder> spec) {
            CvModule.Builder builder = CvModule.builder(title);
            if (spec != null) {
                spec.accept(builder);
            }
            return addModule(builder.build());
        }

        /**
         * Appends a paragraph module.
         *
         * @param title visible module title
         * @param text paragraph body
         * @return this builder
         */
        public Builder paragraph(String title, String text) {
            return addModule(CvModule.paragraph(title, text));
        }

        /**
         * Appends a default bullet-list module.
         *
         * @param title visible module title
         * @param items ordered item texts
         * @return this builder
         */
        public Builder bullets(String title, List<String> items) {
            return module(title, module -> module.list(items, list -> list.bullet()));
        }

        /**
         * Appends a default bullet-list module.
         *
         * @param title visible module title
         * @param items ordered item texts
         * @return this builder
         */
        public Builder bullets(String title, String... items) {
            return bullets(title, items == null ? List.of() : Arrays.asList(items));
        }

        /**
         * Appends a dash-list module.
         *
         * @param title visible module title
         * @param items ordered item texts
         * @return this builder
         */
        public Builder dashList(String title, List<String> items) {
            return module(title, module -> module.list(items, list -> list.dash()));
        }

        /**
         * Appends a dash-list module.
         *
         * @param title visible module title
         * @param items ordered item texts
         * @return this builder
         */
        public Builder dashList(String title, String... items) {
            return dashList(title, items == null ? List.of() : Arrays.asList(items));
        }

        /**
         * Appends a markerless row module with aligned wrapped continuations.
         *
         * @param title visible module title
         * @param rows ordered row texts
         * @return this builder
         */
        public Builder rows(String title, List<String> rows) {
            return addModule(CvModule.rows(title, rows));
        }

        /**
         * Appends a markerless row module with aligned wrapped continuations.
         *
         * @param title visible module title
         * @param rows ordered row texts
         * @return this builder
         */
        public Builder rows(String title, String... rows) {
            return rows(title, rows == null ? List.of() : Arrays.asList(rows));
        }

        /**
         * Appends the common professional-summary module.
         *
         * @param text summary body
         * @return this builder
         */
        public Builder summary(String text) {
            return addModule(CvModule.builder("Professional Summary")
                    .name("Summary")
                    .paragraph(text)
                    .build());
        }

        /**
         * Appends the common technical-skills bullet module.
         *
         * @param items ordered skill rows
         * @return this builder
         */
        public Builder technicalSkills(List<String> items) {
            return addModule(CvModule.builder("Technical Skills")
                    .name("TechnicalSkills")
                    .list(items, list -> list.bullet())
                    .build());
        }

        /**
         * Appends the common technical-skills bullet module.
         *
         * @param items ordered skill rows
         * @return this builder
         */
        public Builder technicalSkills(String... items) {
            return technicalSkills(items == null ? List.of() : Arrays.asList(items));
        }

        /**
         * Appends the common projects markerless row module.
         *
         * @param rows ordered project rows
         * @return this builder
         */
        public Builder projects(String... rows) {
            return addModule(CvModule.builder("Projects")
                    .name("Projects")
                    .rows(rows)
                    .build());
        }

        /**
         * Appends the common professional-experience markerless row module.
         *
         * @param rows ordered experience rows
         * @return this builder
         */
        public Builder experience(String... rows) {
            return addModule(CvModule.builder("Professional Experience")
                    .name("Experience")
                    .rows(rows)
                    .build());
        }

        /**
         * Appends the common education/certifications markerless row module.
         *
         * @param rows ordered education rows
         * @return this builder
         */
        public Builder education(String... rows) {
            return addModule(CvModule.builder("Education & Certifications")
                    .name("Education")
                    .rows(rows)
                    .build());
        }

        /**
         * Appends the common additional-information markerless row module.
         *
         * @param rows ordered rows
         * @return this builder
         */
        public Builder additional(String... rows) {
            return addModule(CvModule.builder("Additional Information")
                    .name("Additional")
                    .rows(rows)
                    .build());
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
