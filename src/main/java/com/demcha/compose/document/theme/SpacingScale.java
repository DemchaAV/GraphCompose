package com.demcha.compose.document.theme;

import com.demcha.compose.document.style.DocumentInsets;

/**
 * Five-step spacing scale for business documents.
 *
 * <p>Values are sizes in points and are used both as gap/padding numbers and as
 * shorthand for symmetric {@link DocumentInsets} via {@link #insetsXs()} … {@link #insetsXl()}.</p>
 *
 * @param xs extra-small gap (typical: 4)
 * @param sm small gap (typical: 8)
 * @param md medium gap (typical: 12)
 * @param lg large gap (typical: 20)
 * @param xl extra-large gap (typical: 32)
 *
 * @author Artem Demchyshyn
 */
public record SpacingScale(double xs, double sm, double md, double lg, double xl) {
    /**
     * Validates the spacing scale (each step must be finite, non-negative and
     * non-decreasing across xs → xl so the scale stays monotonic).
     */
    public SpacingScale {
        validate("xs", xs);
        validate("sm", sm);
        validate("md", md);
        validate("lg", lg);
        validate("xl", xl);
        if (!(xs <= sm && sm <= md && md <= lg && lg <= xl)) {
            throw new IllegalArgumentException(
                    "Spacing scale must be non-decreasing: xs=" + xs + " sm=" + sm
                            + " md=" + md + " lg=" + lg + " xl=" + xl);
        }
    }

    /**
     * Returns the canonical default spacing scale (4 / 8 / 12 / 20 / 32).
     *
     * @return default spacing scale
     */
    public static SpacingScale defaultScale() {
        return new SpacingScale(4.0, 8.0, 12.0, 20.0, 32.0);
    }

    /**
     * Returns symmetric insets at the {@code xs} step.
     *
     * @return xs insets
     */
    public DocumentInsets insetsXs() {
        return DocumentInsets.of(xs);
    }

    /**
     * Returns symmetric insets at the {@code sm} step.
     *
     * @return sm insets
     */
    public DocumentInsets insetsSm() {
        return DocumentInsets.of(sm);
    }

    /**
     * Returns symmetric insets at the {@code md} step.
     *
     * @return md insets
     */
    public DocumentInsets insetsMd() {
        return DocumentInsets.of(md);
    }

    /**
     * Returns symmetric insets at the {@code lg} step.
     *
     * @return lg insets
     */
    public DocumentInsets insetsLg() {
        return DocumentInsets.of(lg);
    }

    /**
     * Returns symmetric insets at the {@code xl} step.
     *
     * @return xl insets
     */
    public DocumentInsets insetsXl() {
        return DocumentInsets.of(xl);
    }

    private static void validate(String label, double value) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Spacing token '" + label
                    + "' must be a finite, non-negative number: " + value);
        }
    }
}
