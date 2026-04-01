package com.demcha.compose.layout_core.system.utils.containerUtils;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.geometry.ModuleWidthSeed;
import com.demcha.compose.layout_core.components.layout.ParentComponent;
import com.demcha.compose.layout_core.components.renderable.Module;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

/**
 * Resolves the stable content width contract for semantic modules before
 * container child alignment begins.
 */
@Slf4j
public final class ModuleWidthResolver {
    private ModuleWidthResolver() {
    }

    public static void process(EntityManager entityManager, Canvas canvas) {
        List<Entity> modules = entityManager.getEntities().values().stream()
                .filter(entity -> entity.has(Module.class))
                .sorted(Comparator.comparingInt(module -> depth(module, entityManager)))
                .toList();
        if (modules.isEmpty()) {
            return;
        }

        for (Entity module : modules) {
            resolveModuleWidth(module, entityManager, canvas);
        }
    }

    private static void resolveModuleWidth(Entity module, EntityManager entityManager, Canvas canvas) {
        Margin margin = module.getComponent(Margin.class).orElse(Margin.zero());
        ContentSize current = module.getComponent(ContentSize.class).orElse(new ContentSize(0, 0));
        double availableWidth = resolveAvailableWidth(module, entityManager, canvas);
        double resolvedWidth = Math.max(0, availableWidth - margin.horizontal());

        module.addComponent(new ContentSize(resolvedWidth, current.height()));
        log.debug("Resolved module width for {}: available={} margin={} resolved={}",
                module, availableWidth, margin.horizontal(), resolvedWidth);
    }

    private static double resolveAvailableWidth(Entity module, EntityManager entityManager, Canvas canvas) {
        ParentComponent parentComponent = module.getComponent(ParentComponent.class).orElse(null);
        if (parentComponent == null) {
            return module.getComponent(ModuleWidthSeed.class)
                    .map(ModuleWidthSeed::width)
                    .orElse(canvas.innerWidth());
        }

        return entityManager.getEntity(parentComponent.uuid())
                .flatMap(InnerBoxSize::from)
                .map(InnerBoxSize::width)
                .orElseGet(() -> {
                    log.warn("Module {} parent width could not be resolved; falling back to canvas inner width.", module);
                    return canvas.innerWidth();
                });
    }

    private static int depth(Entity entity, EntityManager entityManager) {
        int depth = 0;
        ParentComponent parent = entity.getComponent(ParentComponent.class).orElse(null);

        while (parent != null) {
            depth++;
            parent = entityManager.getEntity(parent.uuid())
                    .flatMap(parentEntity -> parentEntity.getComponent(ParentComponent.class))
                    .orElse(null);
        }

        return depth;
    }
}
