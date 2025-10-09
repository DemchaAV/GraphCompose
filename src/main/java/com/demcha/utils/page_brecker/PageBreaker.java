package com.demcha.utils.page_brecker;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderingPosition;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class PageBreaker {
    private PageBreaker() {
    }

    public static Set<Map.Entry<UUID, Entity>> sortEntityInOrder(Map<UUID, Entity> entities) {
        // Create a LinkedHashSet to preserve the order after sorting
        return entities.entrySet()
                .stream()
                .sorted((e1, e2) -> {
                    double y1 = RenderingPosition.from(e1.getValue()).get().y();
                    double y2 = RenderingPosition.from(e2.getValue()).get().y();
                    return Double.compare(y2, y1); // sort from largest → smallest (descending)
                })
                .peek(entry -> {
                    RenderingPosition position = RenderingPosition.from(entry.getValue()).orElse(null);
                    log.debug("{} -> {}\n", entry.getValue(), position);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Calculates a new Y position for an object and determines its corresponding page number,
     * represented by the {@code PositionOnPage} class.
     * <p>
     * This method adjusts the Y coordinate if the object overflows the current page height,
     * assigning it to the correct position on the next page.
     *
     * @param currentPositionY   the current Y coordinate of the object
     * @param pageHeight         the height of the page (must be greater than zero)
     * @param currentPageNumber  the current page number (must not be negative)
     * @return a new {@code PositionOnPage} instance containing the updated Y position and page number
     * @throws IllegalArgumentException if {@code currentPageNumber} is less than zero
     *                                  or {@code pageHeight} is less than or equal to zero
     */

    public static PositionOnPage defainPositionOnPage(double currentPositionY, double pageHeight, int currentPageNumber) {
        log.debug("Current Page Number: {}", currentPageNumber);
        int i;
        double y;

        if (currentPositionY >= 0) {
            i = definePage(currentPositionY, pageHeight);
            y = currentPositionY - (i * -1 * pageHeight);
        } else {
            i = definePage(currentPositionY, pageHeight);
            y = i * pageHeight + currentPositionY;
        }

        i = currentPageNumber + i;
        if (i < 0) {
            throw new IllegalArgumentException("Page number is less than zero");
        }
        if (y<0){
            throw new IllegalArgumentException("Page height is less than zero");
        }

        return new PositionOnPage(y, i);
    }

    /**
     * Determines whether a new page should be added based on the current Y position.
     * <p>
     * Returns a positive number if a new page should be added (the element goes to the next page),
     * or a negative number if the element should remain on the previous page.
     *
     * @param currentPositionY the current vertical position (Y coordinate)
     * @param pageHeight       the height of the page
     * @return a positive value if a new page is needed, or a negative value if it fits on the previous page
     */
    public static int definePage(double currentPositionY, double pageHeight) {
        int definedPage = 0;
        if (currentPositionY >= 0) {
            definedPage = (int) Math.floor(currentPositionY / pageHeight) * -1;
        } else {
            definedPage = (int) Math.floor(currentPositionY / pageHeight) * -1;
        }
        return definedPage;
    }

    public void breakPages() {
        //TODO Break in to the pages
    }
}

class test {
    public static void main(String[] args) {
        int i = PageBreaker.definePage(-22.0, 10.0);
        System.out.println(i);
    }
}

class test2 {
    public static void main(String[] args) {
        PositionOnPage test1 = PageBreaker.defainPositionOnPage(-2, 10, 0);
        PositionOnPage test1PredictedResult = new PositionOnPage(8, 1);

        PositionOnPage test2 = PageBreaker.defainPositionOnPage(-13, 10, 2);
        PositionOnPage test2PredictedResult = new PositionOnPage(7, 4);

        PositionOnPage test3 = PageBreaker.defainPositionOnPage(-2, 10, 0);
        PositionOnPage test3PredictedResult = new PositionOnPage(8, 1);

        System.out.println(test1 + " = " + test1PredictedResult + " " + test1PredictedResult.equals(test1));
        System.out.println(test2 + " = " + test2PredictedResult + " " + test2PredictedResult.equals(test2));
        System.out.println(test3 + " = " + test3PredictedResult + " " + test3PredictedResult.equals(test3));
    }
}

class test3 {
}
