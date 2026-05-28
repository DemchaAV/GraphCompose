package com.demcha.compose.document.templates.cv.spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * User-facing data record for a CV in Templates v2.
 *
 * <p>A {@code CvSpec} carries the subject's identity ({@link CvHeader})
 * plus an ordered list of named {@link CvModule}s. Presets reference
 * modules by name through their slot placements; the spec itself does
 * not pin module ordering — that is the preset's responsibility.</p>
 *
 * @param header  identity block (required)
 * @param modules ordered list of named modules; insertion order
 *                preserved
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard) — the layered model
 *             {@link com.demcha.compose.document.templates.cv.v2.data.CvDocument}
 *             plus the {@code cv.v2} presets. Kept for backward compatibility;
 *             scheduled for removal in a future major. See
 *             {@code docs/templates/v2-layered/}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public record CvSpec(CvHeader header, List<CvModule> modules) {

    /**
     * Compact constructor that defensively copies the modules list and
     * rejects duplicate module names.
     *
     * @throws NullPointerException     if either field is null
     * @throws IllegalArgumentException if two modules share a name
     */
    public CvSpec {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(modules, "modules");
        modules = List.copyOf(modules);
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (CvModule module : modules) {
            if (seen.put(module.name(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException(
                        "duplicate module name: " + module.name());
            }
        }
    }

    /**
     * Returns the module with the given name, if present.
     *
     * @param name lookup key (compared case-sensitively to
     *             {@link CvModule#name()})
     * @return module matching the name, or empty
     */
    public Optional<CvModule> findModule(String name) {
        Objects.requireNonNull(name, "name");
        for (CvModule module : modules) {
            if (module.name().equals(name)) {
                return Optional.of(module);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a fluent builder for assembling a {@code CvSpec}.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link CvSpec}.
     */
    public static final class Builder {
        private CvHeader header;
        private final java.util.List<CvModule> modules = new java.util.ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the document header.
         *
         * @param value non-null header
         * @return this builder
         */
        public Builder header(CvHeader value) {
            this.header = value;
            return this;
        }

        /**
         * Appends a module.
         *
         * @param module non-null module
         * @return this builder
         */
        public Builder module(CvModule module) {
            this.modules.add(module);
            return this;
        }

        /**
         * Builds an immutable {@link CvSpec}.
         *
         * @return new spec
         */
        public CvSpec build() {
            return new CvSpec(header, modules);
        }
    }
}
