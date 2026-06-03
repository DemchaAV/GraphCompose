package com.demcha.compose.document.templates.cv.v2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Root of the v2 CV data model — required {@link CvIdentity} block
 * plus an ordered list of section {@link Placement}s.
 *
 * <p>Each {@link Placement} pairs a {@link CvSection} with a
 * {@link Slot} so multi-column presets can decide which sections go
 * into the sidebar vs. the main column without having to guess from
 * section titles. Sections built without an explicit slot default to
 * {@link Slot#MAIN}.</p>
 *
 * <p>Single-column presets read {@link #sectionsIn(Slot)
 * sectionsIn(Slot.MAIN)} and ignore the rest; multi-column presets
 * call {@code sectionsIn} once per slot they support.</p>
 *
 * @param identity   required identity / contact block
 * @param placements ordered placements; render order matches source
 *                   order within each slot
 */
public record CvDocument(CvIdentity identity, List<Placement> placements) {

    /**
     * Validates that {@code identity} and {@code placements} are
     * non-null and defensively copies the placement list.
     */
    public CvDocument {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(placements, "placements");
        placements = List.copyOf(placements);
    }

    /**
     * Backward-compatible factory that wraps every supplied section
     * in a {@link Slot#MAIN} placement. Provided so callers
     * migrating from the pre-slot {@code new CvDocument(identity,
     * sections)} pattern have a 1:1 replacement without rewriting
     * their construction code.
     *
     * <p>Equivalent to building each section through
     * {@link Builder#section(CvSection)} (which also defaults to
     * {@link Slot#MAIN}). Prefer the {@link Builder} in new code —
     * multi-column presets need explicit slot placement that the
     * builder makes obvious at the call site.</p>
     *
     * @param identity required identity / contact block
     * @param sections sections to wrap, each in a {@link Slot#MAIN}
     *                 placement, in source order
     * @return a {@code CvDocument} with every section placed in
     *         {@link Slot#MAIN}
     * @deprecated since the slot model — prefer the {@link Builder}
     *             so non-MAIN slots are visible at the call site.
     */
    @Deprecated
    public static CvDocument ofMainSections(CvIdentity identity,
                                            List<CvSection> sections) {
        return new CvDocument(identity, toMainPlacements(sections));
    }

    /**
     * One section together with the slot it should render in.
     *
     * @param slot    placement region (never null)
     * @param section section body (never null)
     */
    public record Placement(Slot slot, CvSection section) {

        /** Validates that both fields are non-null. */
        public Placement {
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(section, "section");
        }
    }

    // -- accessors -------------------------------------------------------

    /**
     * Returns every section in this document in source order,
     * regardless of slot. Useful for debugging, indexing, or
     * iterating all sections without caring where they render.
     *
     * <p>Presets should usually call {@link #sectionsIn(Slot)}
     * instead — a single-column preset that iterates this flat list
     * will accidentally render sidebar content inline with the main
     * flow.</p>
     *
     * @return flat list of all sections, source order
     */
    public List<CvSection> sections() {
        List<CvSection> out = new ArrayList<>(placements.size());
        for (Placement p : placements) {
            out.add(p.section());
        }
        return List.copyOf(out);
    }

    /**
     * Returns the sections placed in {@code slot}, in source order.
     * The standard call from a preset's {@code compose()} method.
     *
     * @param slot non-null slot
     * @return sections placed in that slot; empty list if none
     */
    public List<CvSection> sectionsIn(Slot slot) {
        Objects.requireNonNull(slot, "slot");
        List<CvSection> out = new ArrayList<>();
        for (Placement p : placements) {
            if (p.slot() == slot) {
                out.add(p.section());
            }
        }
        return List.copyOf(out);
    }

    /**
     * Returns the slot a given section was placed in. Uses identity
     * comparison ({@code ==}) to avoid surprises when two sections
     * happen to be record-equal but were added separately.
     *
     * @param section section instance to look up
     * @return its slot, or {@link Slot#MAIN} if the section is not
     *         in this document
     */
    public Slot slotOf(CvSection section) {
        Objects.requireNonNull(section, "section");
        for (Placement p : placements) {
            if (p.section() == section) {
                return p.slot();
            }
        }
        return Slot.MAIN;
    }

    // -- builder ---------------------------------------------------------

    /**
     * Creates a fluent builder for {@code CvDocument}.
     *
     * @return new fluent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link CvDocument}.
     *
     * <p>The {@code section}-family methods append to the placement
     * list in call order. Pass a {@link Slot} as the first argument
     * to place into a non-main slot; omit it to default to
     * {@link Slot#MAIN}.</p>
     */
    public static final class Builder {
        private CvIdentity identity;
        private final List<Placement> placements = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the required identity block.
         *
         * @param value the identity block to use
         * @return this builder for chaining
         */
        public Builder identity(CvIdentity value) {
            this.identity = value;
            return this;
        }

        /**
         * Appends one section into {@link Slot#MAIN}.
         *
         * @param section section to append
         * @return this builder for chaining
         */
        public Builder section(CvSection section) {
            return section(Slot.MAIN, section);
        }

        /**
         * Appends one section into a chosen slot.
         *
         * @param slot    placement region
         * @param section section to append
         * @return this builder for chaining
         */
        public Builder section(Slot slot, CvSection section) {
            this.placements.add(new Placement(slot, section));
            return this;
        }

        /**
         * Varargs convenience — all sections placed in
         * {@link Slot#MAIN}.
         *
         * @param values sections to append, in source order
         * @return this builder for chaining
         */
        public Builder sections(CvSection... values) {
            Objects.requireNonNull(values, "values");
            for (CvSection s : values) {
                section(Slot.MAIN, s);
            }
            return this;
        }

        /**
         * Varargs convenience — all supplied sections placed in
         * {@code slot}.
         *
         * @param slot   placement region for every supplied section
         * @param values sections to append, in source order
         * @return this builder for chaining
         */
        public Builder sections(Slot slot, CvSection... values) {
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(values, "values");
            for (CvSection s : values) {
                section(slot, s);
            }
            return this;
        }

        /**
         * List variant — all sections placed in {@link Slot#MAIN}.
         *
         * @param values sections to append, in source order
         * @return this builder for chaining
         */
        public Builder sections(List<CvSection> values) {
            Objects.requireNonNull(values, "values");
            for (CvSection s : values) {
                section(Slot.MAIN, s);
            }
            return this;
        }

        /**
         * Appends a pre-built {@link Placement}.
         *
         * @param placement the placement to append (non-null)
         * @return this builder for chaining
         */
        public Builder placement(Placement placement) {
            this.placements.add(Objects.requireNonNull(placement, "placement"));
            return this;
        }

        /**
         * Builds the immutable {@link CvDocument}.
         *
         * @return the assembled document
         */
        public CvDocument build() {
            return new CvDocument(identity, placements);
        }
    }

    private static List<Placement> toMainPlacements(List<CvSection> sections) {
        Objects.requireNonNull(sections, "sections");
        List<Placement> out = new ArrayList<>(sections.size());
        for (CvSection s : sections) {
            out.add(new Placement(Slot.MAIN, s));
        }
        return out;
    }
}
