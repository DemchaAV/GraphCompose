package com.demcha.compose.document.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public API element as an <strong>Extension SPI</strong> or
 * <strong>Experimental</strong> surface — a deliberately-exposed seam that
 * library users can implement or call, but whose shape may still change
 * between minor releases.
 *
 * <p>Elements annotated {@code @Beta} carry the policy spelled out in
 * <a href="https://github.com/DemchaAV/GraphCompose/blob/develop/docs/api-stability.md">
 * the API stability policy</a> § 1 — broadly:</p>
 *
 * <ul>
 *   <li><strong>Extension SPI</strong> usage (the common case): the seam
 *       is intentionally public so users can implement or call it, but
 *       the shape may evolve in a minor release. When it does, the
 *       previous shape ships {@code @Deprecated} for at least one minor
 *       release first and the CHANGELOG entry calls out the migration
 *       under {@code ### Public API}.</li>
 *   <li><strong>Experimental</strong> usage: a brand-new public type
 *       shipping in its first minor release before the contract has
 *       stabilised. The contract is in active flux and may change or
 *       disappear in any minor release without a deprecation window;
 *       the docstring on the element says so explicitly.</li>
 * </ul>
 *
 * <p>The distinction between the two usages lives in the element's own
 * Javadoc, not in this annotation. The annotation itself just signals
 * "policy applies — read the page before depending on this".</p>
 *
 * <p>If your use case requires a stable contract over an
 * {@code @Beta}-marked element, please open an issue at
 * <a href="https://github.com/DemchaAV/GraphCompose/issues">the
 * GraphCompose tracker</a> so the stabilisation can be prioritised.</p>
 *
 * <p>Architecture guard tests may consume this annotation reflectively;
 * the runtime retention is intentional.</p>
 *
 * @see Internal
 * @since 1.6.6
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.CONSTRUCTOR,
        ElementType.PACKAGE,
})
public @interface Beta {
}
