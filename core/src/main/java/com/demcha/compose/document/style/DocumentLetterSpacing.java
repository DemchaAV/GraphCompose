package com.demcha.compose.document.style;

/**
 * Letter spacing (typographic <em>tracking</em>) for a
 * {@link DocumentTextStyle} &mdash; extra advance inserted after every rendered
 * code point, expressed either in absolute points or as a fraction of the font
 * size.
 *
 * <p>This is real tracking, not spaces: the string handed to the backend is the
 * author's string, so the PDF text layer, search, copy/paste, text extraction
 * and ATS parsing all still see {@code "JANE DOE"} for a headline that renders
 * as widely spaced caps. Padding the string with literal spaces &mdash; the
 * shape {@code "J A N E   D O E"} &mdash; achieves the same picture and breaks
 * every one of those.</p>
 *
 * <p>Prefer {@link #ofFontSize(double)}: expressed as a fraction, the tracking
 * scales with the type, so one style value reads the same at 9pt as at 24pt and
 * keeps its proportions under auto-size. {@link #points(double)} is there for
 * the cases that are specified in absolute points. The unit lives in the value,
 * so a call site says which one it meant &mdash; {@code ofFontSize(0.12)} and
 * {@code points(1.2)} are both plausible-looking numbers and a bare
 * {@code double} could not tell them apart.</p>
 *
 * <p>Negative tracking (tighter than normal) is allowed. {@link #NONE} is the
 * neutral value carried by every style that has not opted in; it resolves to
 * {@code 0} and leaves measurement and rendering exactly as they were.
 * Instances are immutable and thread-safe.</p>
 *
 * <h2>What survives into a file</h2>
 *
 * <p>This value keeps exactly what it was given: {@link #resolve(double)}
 * returns the amount asked for, to the last bit, and nothing rewrites it. The
 * <em>file formats</em> are what quantise, and they do it differently:</p>
 *
 * <ul>
 *   <li><strong>PDF and PPTX &mdash; 0.01pt.</strong> DrawingML states spacing
 *       in hundredths of a point, so that is the finest distinction the two
 *       fixed-layout formats can both make. The engine measures on that grid,
 *       which is what keeps the width it reserves and wraps against equal to
 *       the width the file draws. Ask for a third of a point and both get
 *       {@code 0.33}.</li>
 *   <li><strong>DOCX &mdash; 0.05pt.</strong> Word states spacing in twentieths
 *       of a point, and the export rounds this value to that grid on its own.
 *       Word owns its layout, so it is not held to the hundredth the fixed
 *       backends settled on.</li>
 * </ul>
 *
 * <p>So an arbitrary {@code double} does not survive all three formats exactly,
 * and no amount of care here would make it. What is guaranteed is that within
 * fixed layout there is one number: what was measured, what the PDF states and
 * what the deck states are the same value.</p>
 *
 * <p>Tracking larger than a format can state is refused when the document is
 * rendered, rather than wrapped into a negative &mdash; fixed layout tops out at
 * &plusmn;4000pt, DrawingML's own bound. The limits belong to the formats; this
 * value accepts any finite number.</p>
 *
 * <pre>{@code
 * DocumentTextStyle headline = DocumentTextStyle.builder()
 *         .size(24)
 *         .letterSpacing(DocumentLetterSpacing.ofFontSize(0.12)) // 12% of 24pt = 2.88pt
 *         .build();
 * }</pre>
 *
 * @param type  whether {@code value} is read as points or as a fraction of the
 *              font size
 * @param value the tracking amount, in the unit named by {@code type}
 * @author Artem Demchyshyn
 * @see DocumentTextStyle#letterSpacing()
 * @since 2.4.0
 */
public record DocumentLetterSpacing(Type type, double value) {

    /** The unit a tracking amount is expressed in. */
    public enum Type {
        /** {@code value} is an absolute amount in points. */
        POINTS,
        /** {@code value} is a fraction of the font size (an em share). */
        FONT_SIZE
    }

    /**
     * No tracking &mdash; the neutral value, and the default of every
     * {@link DocumentTextStyle}. Resolves to {@code 0} at any font size.
     */
    public static final DocumentLetterSpacing NONE = new DocumentLetterSpacing(Type.POINTS, 0.0);

    /**
     * Validates the unit and the amount.
     *
     * @param type  the unit; must not be {@code null}
     * @param value the amount; must be finite, may be negative
     */
    public DocumentLetterSpacing {
        if (type == null) {
            throw new IllegalArgumentException("Letter-spacing type cannot be null.");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Letter spacing must be a finite number, got: " + value);
        }
        // -0.0 renders identically to +0.0 but would compare unequal to it and
        // hash differently, so it is folded. Note this does not make zero a
        // single value: the factories return NONE for it, but
        // new DocumentLetterSpacing(FONT_SIZE, 0.0) is still constructible and
        // is not equal(NONE) — same behaviour, different unit, and the unit is
        // the caller's to state.
        value = value == 0.0 ? 0.0 : value;
    }

    /**
     * Tracking of an absolute size, in points.
     *
     * @param points extra advance after each code point, in points; negative
     *               tightens, {@code 0} is {@link #NONE}
     * @return a points-valued tracking
     */
    public static DocumentLetterSpacing points(double points) {
        return points == 0.0 ? NONE : new DocumentLetterSpacing(Type.POINTS, points);
    }

    /**
     * Tracking as a fraction of the font size, so it scales with the type.
     *
     * @param fraction share of the font size, e.g. {@code 0.12} for 12%;
     *                 negative tightens, {@code 0} is {@link #NONE}
     * @return a font-size-relative tracking
     */
    public static DocumentLetterSpacing ofFontSize(double fraction) {
        return fraction == 0.0 ? NONE : new DocumentLetterSpacing(Type.FONT_SIZE, fraction);
    }

    /**
     * Resolves this tracking to points against a concrete font size.
     *
     * <p>A non-finite {@code fontSize} makes the tracking contribution
     * {@code 0} instead of {@code NaN}. That bounds this term only — it says
     * nothing about the rest of the measurement, which still multiplies glyph
     * widths by that same font size. A non-finite font size remains a bad font
     * size, and it is not this type's job to make it finite.</p>
     *
     * @param fontSize the font size the text is set at, in points
     * @return the extra advance per code point, in points
     */
    public double resolve(double fontSize) {
        return switch (type) {
            case POINTS -> value;
            case FONT_SIZE -> Double.isFinite(fontSize) ? value * fontSize : 0.0;
        };
    }

    /**
     * Whether this is the neutral value, i.e. it resolves to {@code 0} at every
     * font size.
     *
     * @return {@code true} if no tracking is applied
     */
    public boolean isNone() {
        return value == 0.0;
    }
}
