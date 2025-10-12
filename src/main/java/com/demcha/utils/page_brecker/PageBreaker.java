package com.demcha.utils.page_brecker;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderingPosition;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
/**
 * Utility class for sorting graphical entities based on their Y-position
 * and for calculating their resulting page and in-page Y coordinates
 * based on a fixed page height.
 * <p>
 * This class is designed to help determine where entities should fall
 * when dividing a continuous coordinate system into discrete pages.
 */
@Slf4j
public class PageBreaker {
    private PageBreaker() {}


    // === Sorting =============================================================

    /**
     * Sorts the given map of entities based on their Y-position in **descending** order.
     * <p>
     * The sorting is based on the {@code RenderingPosition} of each {@link Entity}.
     * The result is a {@link Set} of {@link Map.Entry} objects, preserved in sorted order
     * by being collected into a {@link LinkedHashSet}.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects
     * to be sorted. Must not be null.
     * @return A {@code Set} of map entries sorted by Y-position, descending (top-to-bottom on a page).
     * @throws NullPointerException if the input map is null.
     * @throws IllegalStateException if any entity does not have a defined {@code RenderingPosition}.
     */
    public static Set<Map.Entry<UUID, Entity>> sortByYPosition(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .peek(entry -> {
                    Optional<RenderingPosition> pos = RenderingPosition.from(entry.getValue());
                    log.debug("{} -> {}", entry.getValue(), pos.orElse(null));
                })

                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Sorts the given map of entities based on their Y-position in **descending** order
     * and returns the result as a new {@link LinkedHashMap}.
     * <p>
     * This provides a convenient map-based view where the iteration order is the sorted order.
     *
     * @param entities A map where keys are {@link UUID}s and values are {@link Entity} objects
     * to be sorted. Must not be null.
     * @return A {@code LinkedHashMap} with the entities, ordered by Y-position, descending.
     * @throws NullPointerException if the input map is null.
     * @throws IllegalStateException if any entity does not have a defined {@code RenderingPosition}.
     */
    public static LinkedHashMap<UUID, Entity> sortByYPositionToMap(Map<UUID, Entity> entities) {
        Objects.requireNonNull(entities, "entities must not be null");

        return entities.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Entity> e) -> yOf(e.getValue()))
                        .reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private static double yOf(Entity e) {
        return RenderingPosition.from(e)
                .map(RenderingPosition::y)
                .orElseThrow(() -> new IllegalStateException(
                        "Entity " + e + " has no RenderingPosition"));
    }

    // === Paging math =========================================================



    /**
     * Calculates a new Y position on the page and the resulting page number.
     * <p>
     * This method normalizes the {@code currentPositionY} into the range
     * {@code [0, pageHeight)} and calculates the page offset relative to
     * {@code currentPageNumber}.
     *
     * @param currentPositionY The entity's current absolute Y-coordinate.
     * @param pageHeight The height of a single page (must be positive and finite).
     * @param currentPageNumber The current page number (e.g., the page the entity is currently thought to be on).
     * @return A {@link PositionOnPage} object containing the normalized Y-position and the final page number.
     * @throws IllegalArgumentException if {@code pageHeight} is not a finite positive number, or if the
     * resulting page number is less than zero.
     */
    public static PositionOnPage definePositionOnPage(double currentPositionY, double pageHeight, int currentPageNumber) {
        if (!(pageHeight > 0.0) || Double.isNaN(pageHeight) || Double.isInfinite(pageHeight)) {
            throw new IllegalArgumentException("pageHeight must be a finite positive number; was " + pageHeight);
        }

        log.debug("Input: y={}, pageHeight={}, currentPage={}", currentPositionY, pageHeight, currentPageNumber);

        int offset = definePage(currentPositionY, pageHeight); // may be negative, zero, or positive
        int finalPage = Math.addExact(currentPageNumber, offset); // detect overflow
        if (finalPage < 0) {
            throw new IllegalArgumentException("Page number is less than zero after offset: " + finalPage);
        }

        // Normalize y into [0, pageHeight)
        double yInPage = positiveModulo(currentPositionY, pageHeight);

        PositionOnPage result = new PositionOnPage(yInPage, finalPage);
        log.debug("Result: {}", result);
        return result;
    }

    /**
     * Calculates the page offset relative to a starting page.
     * <p>
     * Given an absolute Y-coordinate and a fixed {@code pageHeight} ($H$), this
     * determines how many pages away the coordinate is from the page that
     * starts at Y=0. The resulting offset is defined such that:
     * <ul>
     * <li>$Y$ in $[0, H)$ $\rightarrow$ 0 (Same page)</li>
     * <li>$Y$ in $[H, 2H)$ $\rightarrow$ -1 (One page down, assuming a coordinate system where positive Y moves down)</li>
     * <li>$Y$ in $[-H, 0)$ $\rightarrow$ 1 (One page up)</li>
     * </ul>
     * The calculation is $- \lfloor \frac{Y}{H} \rfloor$.
     *
     * @param currentPositionY The entity's current absolute Y-coordinate.
     * @param pageHeight The height of a single page (must be positive and finite).
     * @return The integer page offset.
     * @throws IllegalArgumentException if {@code pageHeight} is not a finite positive number.
     */
    public static int definePage(double currentPositionY, double pageHeight) {
        if (!(pageHeight > 0.0) || Double.isNaN(pageHeight) || Double.isInfinite(pageHeight)) {
            throw new IllegalArgumentException("pageHeight must be a finite positive number; was " + pageHeight);
        }
        log.debug("definePage: pageHeight={}, y={}", pageHeight, currentPositionY);

        // floor(y / H) is ... -2, -1, 0, 1, 2 ...
        // We want offset = -floor(y/H)
        int definedPage = (int) Math.floor(currentPositionY / pageHeight) * -1;

        log.debug("definePage -> offset={}", definedPage);
        return definedPage;
    }

    /** Proper positive modulo for doubles: result in [0, m). */
    private static double positiveModulo(double a, double m) {
        double r = a % m;
        return (r < 0) ? r + m : r;
    }
    /**
     * Placeholder method for the main page-breaking logic.
     * <p>
     * **TODO:** The implementation should likely involve sorting entities
     * (using {@link #sortByYPositionToMap(Map)}) and then iterating through
     * them to calculate and potentially adjust their page and in-page
     * coordinates (using {@link #definePositionOnPage(double, double, int)}).
     */
    public void breakPages() {
        // TODO: implement page breaking using the helpers above
    }
}
