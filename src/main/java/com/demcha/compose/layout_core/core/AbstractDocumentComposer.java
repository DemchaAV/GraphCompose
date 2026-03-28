package com.demcha.compose.layout_core.core;

import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;

import java.util.Objects;

/**
 * Shared base for backend-specific document composers.
 */
public abstract class AbstractDocumentComposer implements DocumentComposer {
    private final EntityManager entityManager;
    private final ComponentBuilder componentBuilder;
    private final Canvas canvas;

    protected AbstractDocumentComposer(EntityManager entityManager, Canvas canvas) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.componentBuilder = ComponentBuilder.builder(entityManager);
    }

    @Override
    public ComponentBuilder componentBuilder() {
        return componentBuilder;
    }

    @Override
    public EntityManager entityManager() {
        return entityManager;
    }

    @Override
    public Canvas canvas() {
        return canvas;
    }

    @Override
    public void markdown(boolean enabled) {
        entityManager.setMarkdown(enabled);
    }

    @Override
    public void guideLines(boolean enabled) {
        entityManager.setGuideLines(enabled);
    }

    @Override
    public final void build() throws Exception {
        buildComponents();
        buildDocument();
    }

    @Override
    public final byte[] toBytes() throws Exception {
        buildComponents();
        return exportBytes();
    }

    protected final void buildComponents() {
        componentBuilder.buildsComponents();
    }

    protected abstract void buildDocument() throws Exception;

    protected abstract byte[] exportBytes() throws Exception;
}
