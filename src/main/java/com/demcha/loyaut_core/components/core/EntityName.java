package com.demcha.loyaut_core.components.core;

/**
 * Human-readable name for an entity.
 *
 * <p>Attach this component to make logs, debugging, and inspector views clearer.
 * The name is <strong>not</strong> an identifier and does not have to be unique.</p>
 *
 * <h2>Constraints</h2>
 * <ul>
 *   <li>Non-null and non-blank.</li>
 *   <li>Trimmed; recommended max length is 100 characters.</li>
 * </ul>
 *
 * <p><b>Examples</b>:
 * <pre>{@code
 * addComponent(entityId, new EntityName("Header → ContactRow"));
 * addComponent(entityId, new EntityName("Button[primary]/#submit"));
 * }</pre>
 * </p>
 *
 * @param value the human-readable name of the entity; must be non-blank
 * @since 1.0
 */
public record EntityName(String value) implements Component {
}
