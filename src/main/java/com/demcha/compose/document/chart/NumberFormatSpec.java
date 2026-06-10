package com.demcha.compose.document.chart;

import java.util.Locale;

/**
 * Declarative axis / value number format.
 *
 * <p>Deliberately a serializable value type rather than a {@code Function} so a
 * chart survives a round trip through the JSON document model. A
 * {@link java.text.DecimalFormat} pattern plus locale, optional prefix/suffix,
 * and an optional scale divisor cover currency, percentages, and SI-style
 * "k / M" abbreviation without an escape hatch. Java-only callers that truly
 * need arbitrary formatting can still pre-format their {@link ChartData} labels.</p>
 *
 * @param pattern {@link java.text.DecimalFormat} pattern, e.g. {@code "#,##0.0"}
 * @param locale grouping/decimal-separator locale; defaults to {@link Locale#ROOT}
 * @param prefix text prepended to every formatted value (e.g. {@code "$"})
 * @param suffix text appended to every formatted value (e.g. {@code " k"})
 * @param scaleDivisor value is divided by this before formatting; must be finite and non-zero
 *
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record NumberFormatSpec(
        String pattern,
        Locale locale,
        String prefix,
        String suffix,
        double scaleDivisor
) {
    /** Normalizes nullable fields and rejects a zero / non-finite divisor. */
    public NumberFormatSpec {
        pattern = (pattern == null || pattern.isBlank()) ? "#,##0.##" : pattern;
        locale = locale == null ? Locale.ROOT : locale;
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        if (scaleDivisor == 0 || Double.isNaN(scaleDivisor) || Double.isInfinite(scaleDivisor)) {
            throw new IllegalArgumentException("scaleDivisor must be finite and non-zero: " + scaleDivisor);
        }
    }

    /**
     * Plain pattern, root locale, no affixes, no scaling.
     *
     * @param pattern decimal-format pattern
     * @return format spec
     */
    public static NumberFormatSpec pattern(String pattern) {
        return new NumberFormatSpec(pattern, Locale.ROOT, "", "", 1.0);
    }

    /**
     * Default integer-friendly format ({@code #,##0.##}).
     *
     * @return format spec
     */
    public static NumberFormatSpec defaults() {
        return pattern("#,##0.##");
    }

    /**
     * Returns a copy with a different locale.
     *
     * @param locale grouping/decimal-separator locale
     * @return updated format spec
     */
    public NumberFormatSpec withLocale(Locale locale) {
        return new NumberFormatSpec(pattern, locale, prefix, suffix, scaleDivisor);
    }

    /**
     * Returns a copy with a different prefix.
     *
     * @param prefix value prefix
     * @return updated format spec
     */
    public NumberFormatSpec withPrefix(String prefix) {
        return new NumberFormatSpec(pattern, locale, prefix, suffix, scaleDivisor);
    }

    /**
     * Returns a copy with a different suffix.
     *
     * @param suffix value suffix
     * @return updated format spec
     */
    public NumberFormatSpec withSuffix(String suffix) {
        return new NumberFormatSpec(pattern, locale, prefix, suffix, scaleDivisor);
    }

    /**
     * Returns a copy with a different scale divisor.
     *
     * @param scaleDivisor divisor applied before formatting
     * @return updated format spec
     */
    public NumberFormatSpec scaledBy(double scaleDivisor) {
        return new NumberFormatSpec(pattern, locale, prefix, suffix, scaleDivisor);
    }

    /**
     * Formats one value. A {@link java.text.DecimalFormat} is constructed per
     * call (it is not thread-safe); callers that format in tight loops should
     * cache via {@link #formatter()}.
     *
     * @param value raw data value
     * @return formatted string with prefix/suffix applied
     */
    public String format(double value) {
        return prefix + formatter().format(value / scaleDivisor) + suffix;
    }

    /**
     * Builds a fresh, configured {@link java.text.DecimalFormat} for this spec.
     *
     * @return decimal format
     */
    public java.text.DecimalFormat formatter() {
        var symbols = new java.text.DecimalFormatSymbols(locale);
        return new java.text.DecimalFormat(pattern, symbols);
    }
}
