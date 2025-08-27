package com.demcha.components.content.components_builders;

import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.core.EntityName;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.OuterBoxSize;
import com.demcha.components.layout.Anchor;
import com.demcha.components.layout.Layer;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.layout.coordinator.Position;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.PdfDocument;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

/**
 * Base class for ECS-style builders that assemble a set of {@link Component}s
 * (one component per type) and optionally create/populate an entity in a {@link PdfDocument}.
 *
 * <p><strong>Responsibilities</strong>:</p>
 * <ul>
 *   <li>Hold exactly one component instance per component type (class) via an internal map.</li>
 *   <li>Provide a fluent API (CRTP) so subclass methods can chain and return the concrete builder type.</li>
 *   <li>Expose convenience methods for common components (e.g., {@link Position}, {@link OuterBoxSize}, {@link Margin}).</li>
 *   <li>Build the final, de-duplicated set of components and push them into a {@link PdfDocument}.</li>
 * </ul>
 *
 * <p><strong>Design</strong>:</p>
 * <ul>
 *   <li><em>ECS rule</em>: one component instance per type; adding a component of the same type replaces the previous one.</li>
 *   <li><em>CRTP</em>: the type parameter {@code B} is the concrete builder type to keep the fluent API.</li>
 *   <li><em>Deterministic order</em>: {@link LinkedHashMap} preserves insertion order for debugging or predictable iteration.</li>
 * </ul>
 *
 * @param <B> the concrete builder type (for fluent chaining via CRTP)
 * @see ComponentBuilder
 * @see PdfDocument
 * @see Component
 */
@Slf4j
public abstract class ComponentBoxBuilder<B extends ComponentBoxBuilder<B>>
        implements ComponentBuilder {

    /**
     * Holds exactly one component per runtime type (class).
     */
    protected final Entity entity = new Entity();
    protected boolean filledHorizontally;

    /**
     * @return {@code this} as the concrete builder type (CRTP).
     */
    protected abstract B self();

    /**
     * Attach a human-readable {@link EntityName} to the entity (useful for logging/debugging).
     *
     * @param name the entity tag/name component
     * @return this builder
     */
    public B entityName(EntityName name) {
        return addComponent(name);
    }

    public B layer(Layer layer) {
        return addComponent(layer);
    }

    public B anchor(Anchor anchor) {
        return addComponent(anchor);
    }

    public B padding(Padding padding) {
        return addComponent(padding);
    }

    public B parentComponent(ParentComponent parentComponent) {
        return addComponent(parentComponent);
    }

    public B parentComponent(@NonNull Entity parrentEntity) {
        return addComponent(new ParentComponent(parrentEntity));
    }

    /**
     * Get the stored entity name value (if present).
     *
     * <p><strong>Note:</strong> returns {@code null} and logs an error if {@link EntityName}
     * was never set. Consider guarding for this in callers, or provide a "required" accessor
     * in subclasses if the name is mandatory.</p>
     *
     * @return the entity name, or {@code null} if missing
     */
    @Override
    public String entityName() {
        return entity.name();
    }

    /**
     * Add or replace a {@link Position} component.
     *
     * @param position the position component
     * @return this builder
     */
    public B position(Position position) {
        return addComponent(position);
    }

    /**
     * Add or replace a {@link OuterBoxSize} component.
     *
     * @return this builder
     */
    public B size(ContentSize contentSize) {
        return addComponent(contentSize);
    }


    /**
     * Add or replace a {@link Margin} component.
     *
     * @param margin the margin component
     * @return this builder
     */
    public B margin(Margin margin) {
        return addComponent(margin);
    }

    /**
     * Put (add or replace) a component by its runtime class.
     * The last call for a given type wins.
     *
     * @param component the component to store
     * @param <T>       the component type
     * @return this builder
     */
    protected <T extends Component> B addComponent(T component) {
        entity.addComponent(component);
        log.debug("Component {} has been added", component);
        return self();
    }

    /**
     * Build the set of components accumulated in this builder.
     * Maintains insertion order due to {@link LinkedHashMap}.
     *
     * @return a de-duplicated set of components (one per type)
     */
    @Override
    public Entity buildComponents() {
        return this.entity;
    }

    /**
     * Create an entity inside the given {@link PdfDocument} and populate it with this builder's components.
     *
     * <p>Delegates to {@code pdfDocument.createAndPopulateEntity(this)}.
     * Ensure that your {@link PdfDocument} has a compatible method.</p>
     *
     * @param pdfDocument the document to create/populate an entity in
     * @return the created entity's UUID
     */
    public Entity buildInto(PdfDocument pdfDocument) {
        log.info("Put  {} in to the PdfDocument", entity);
        if (filledHorizontally) {
            var component = this.entity.getComponent(ContentSize.class).orElseThrow();
            var margin = this.entity.getComponent(Margin.class).orElse(Margin.zero());
            var newComponentSize = new ContentSize(component.width() - margin.horizontal(), component.height());
            this.entity.addComponent(newComponentSize);
        }
        pdfDocument.putEntity(this.entity);
        return this.entity;
    }
}
