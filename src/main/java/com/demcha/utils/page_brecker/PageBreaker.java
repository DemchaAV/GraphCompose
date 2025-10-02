package com.demcha.utils.page_brecker;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.core.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class PageBreaker {
    private final EntityManager entityManager;
    private Canvas canvas;

    public void breakPages() {
        //TODO Break in to the pages
    }

    public void sortEntityInOrder() {
        Set<Map.Entry<UUID, Entity>> entries = entityManager.getEntities().entrySet();

        entries.stream()
                .sorted((e1, e2) -> {
                    double y1 = RenderingPosition.from(e1.getValue()).get().y();
                    double y2 = RenderingPosition.from(e2.getValue()).get().y();
                    return Double.compare(y2, y1); // сортировка от большего к меньшему
                })
                .forEach(entry -> {
                    RenderingPosition position = RenderingPosition.from(entry.getValue()).orElse(null);
                    log.debug("{} ->  {}\n", entry.getValue(), position);
                });
    }
}
