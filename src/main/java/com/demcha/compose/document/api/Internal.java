package com.demcha.compose.document.api;

import java.lang.annotation.*;

/**
 * Marks an API element as internal to GraphCompose.
 *
 * <p>Types, methods, fields, constructors, and packages annotated with
 * {@code @Internal} are implementation details and
 * <strong>may change in any release without notice</strong>.
 * They are technically reachable because Java visibility cannot fully
 * encapsulate cross-package collaborators in a non-modular library, but
 * they are not part of the public API contract and library users should
 * not depend on them.</p>
 *
 * <p>If your use case requires importing an {@code @Internal}-marked
 * element, please open an issue at
 * <a href="https://github.com/DemchaAV/GraphCompose/issues">the
 * GraphCompose tracker</a> so a stable extension point can be considered.</p>
 *
 * <p>Architecture guard tests may consume this annotation reflectively;
 * the runtime retention is intentional.</p>
 *
 * @since 1.6.0
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
public @interface Internal {
}
