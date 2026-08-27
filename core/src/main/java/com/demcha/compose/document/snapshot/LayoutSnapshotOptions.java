package com.demcha.compose.document.snapshot;

import java.util.Objects;

/**
 * Which optional diagnostic sections a layout snapshot should carry.
 *
 * <p>The default snapshot is the one every committed baseline was recorded
 * against, and it stays that way: {@link #defaults()} enables nothing, so
 * upgrading GraphCompose never adds a key to a baseline a consumer already has
 * on disk. A diagnostic section is something you ask for:</p>
 *
 * <pre>{@code
 * LayoutSnapshot snapshot = document.layoutSnapshot(
 *         LayoutSnapshotOptions.builder()
 *                 .typography(true)
 *                 .build());
 * }</pre>
 *
 * <p>This is a builder rather than a record so a later section — links, paint,
 * accessibility — is one more builder method rather than one more
 * {@code layoutSnapshot(...)} overload, and so adding one cannot change the
 * meaning of an existing call.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.2.2
 */
public final class LayoutSnapshotOptions {

    private static final LayoutSnapshotOptions DEFAULTS = builder().build();

    private final boolean typography;

    private LayoutSnapshotOptions(Builder builder) {
        this.typography = builder.typography;
    }

    /**
     * Returns the options every {@code layoutSnapshot()} call uses: no optional
     * section enabled, and therefore the snapshot shape baselines were recorded
     * against.
     *
     * @return the default, all-sections-off options
     */
    public static LayoutSnapshotOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Creates a builder with every optional section disabled.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Indicates whether the snapshot should carry the {@code typography} section.
     *
     * @return {@code true} when typography was requested
     */
    public boolean typography() {
        return typography;
    }

    /**
     * Indicates whether every optional section is disabled, i.e. whether this is
     * the legacy snapshot shape.
     *
     * @return {@code true} when no optional section is enabled
     */
    public boolean isDefault() {
        return !typography;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LayoutSnapshotOptions options && typography == options.typography;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typography);
    }

    @Override
    public String toString() {
        return "LayoutSnapshotOptions[typography=" + typography + "]";
    }

    /**
     * Builds a {@link LayoutSnapshotOptions}.
     *
     * @author Artem Demchyshyn
     * @since 2.2.2
     */
    public static final class Builder {

        private boolean typography;

        private Builder() {
        }

        /**
         * Requests the {@code typography} section: one entry per resolved
         * paragraph fragment describing the font it was laid out in and where
         * its lines ended up.
         *
         * @param enabled whether to emit the section
         * @return this builder
         */
        public Builder typography(boolean enabled) {
            this.typography = enabled;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return immutable options
         */
        public LayoutSnapshotOptions build() {
            return new LayoutSnapshotOptions(this);
        }
    }
}
