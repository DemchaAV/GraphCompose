package com.demcha.system.utils.page_breaker;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.renderable.BlockText;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.BigSizeElementException;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PageLayoutCalculator {
    private final  EntityManager entityManager;

    /**
     * Calculates the necessary vertical offset for an element to ensure it does not go outside
     * the canvas rendering bounds ({@code canvas.boundingTopLine()} and {@code canvas.boundingBottomLine()}).
     *
     * @param e      The entity to check.
     * @param startY The element's current Y-position.
     * @param canvas The canvas with boundary information.
     * @return The offset value. Returns 0.0 if no offset is required.
     */
    public   double shift(Entity e, float startY, Canvas canvas) throws BigSizeElementException {
        var margin = e.getComponent(Margin.class).orElse(Margin.zero());
        if (e.hasAssignable(BlockText.class)) {
            var style = e.getComponent(TextStyle.class).orElseThrow(() -> new IllegalStateException("TextComponent has no TextStyle"));

            try {
                return shift(startY, style.getLineHeight(), 0.0, 0.0, canvas);
            } catch (BigSizeElementException ex) {
                throw new BigSizeElementException(startY, style.getLineHeight(), canvas.innerHeigh(), e.printInfo());
            }
        } else {
            if (e.getRender() != null) {

                if (!Breakable.class.isAssignableFrom(e.getRender().getClass())) {
                    var high = e.getComponent(ContentSize.class).orElseThrow();
                    try {
                        return shift(startY, high.height(), margin.top(), margin.bottom(), canvas);
                    } catch (BigSizeElementException ex) {
                        throw new BigSizeElementException(startY, high.height(), canvas.innerHeigh(), e.printInfo());
                    }

                }
            } else {
                log.error("Render is null {}", e);
            }
        }
        return 0;
    }

    /**
     * Core logic for calculating the shift. Checks if the element fits on the page
     * and does not exceed the top or bottom boundaries.
     *
     * @param yPosition     The top Y-coordinate of the element.
     * @param elementHeight The height of the element.
     * @param marginTop     The top margin.
     * @param marginBottom  The bottom margin.
     * @param canvas        The canvas.
     * @return The calculated offset. A positive value means shift up, negative means shift down.
     * @throws BigSizeElementException if a non-breakable element is too large to fit
     *                                 in the available page space.
     *
     */
    public  double shift(
            double yPosition,
            double elementHeight,
            double marginTop,
            double marginBottom,
            Canvas canvas
    ) throws BigSizeElementException {
        return shift(yPosition, elementHeight, marginTop, marginBottom, canvas.boundingBottomLine(), canvas.boundingTopLine());

    }

    private static double shift(
            double yPosition,
            double elementHeight,
            double marginTop,
            double marginBottom,
            double canvasBottomBondingLine,
            double canvasTopBondingLine
    ) throws BigSizeElementException {

        // 1) Can it ever fit (including margins)?
        final double requiredHeight = elementHeight + marginTop + marginBottom;
        final double availableHeight = canvasTopBondingLine - canvasBottomBondingLine;
        if (requiredHeight > availableHeight) {
            log.error("Element is too large and non-breakable — it cannot fit between the bounding lines with margins.\n" +
                      "requiredHeight: {} availableHeight: {}", requiredHeight, availableHeight);

            throw new BigSizeElementException(yPosition, elementHeight, availableHeight);
        }

        // 2) Check top overflow
        if (yPosition + elementHeight + marginTop > canvasTopBondingLine) {
            double delta = canvasTopBondingLine - (yPosition + elementHeight + marginTop); // negative => move down
            log.info("Element shifted down by {} from top bound {}", delta, canvasTopBondingLine);
            return delta;
        }

        // 3) Check bottom overflow
        if (yPosition - marginBottom < canvasBottomBondingLine) {
            double delta = (canvasBottomBondingLine + marginBottom)-yPosition; // positive => move up
            // Re-check (defensive): after moving up, we still must not exceed top
            if (yPosition + delta + elementHeight + marginTop > canvasTopBondingLine) {
                throw new BigSizeElementException(
                        "Element is too large and non-breakable — shifting up would exceed the top bound."
                );
            }
            log.info("Element shifted up by {} from bottom bound {}", delta, canvasBottomBondingLine);
            return delta;
        }

        // 4) Already within bounds
        return 0.0;
    }
    /**
     * Determines an object's position on a page based on its current Y-coordinate and dimensions.
     * This method calculates the page number and the new Y-coordinate within that page.
     * It also handles situations where an object crosses the bottom boundary of a page,
     * applying an offset to move it.
     *
     * @param currentPositionY   The object's current absolute Y-coordinate.
     * @param objectHeight       The height of the object.
     * @param objectMarginTop    The object's top margin.
     * @param objectMarginBottom The object's bottom margin.
     * @param currentPageNumber  The current page number (used as a base for calculating the new page).
     * @param canvas             The canvas object, providing page dimensions.
     * @param yOffset            An object for tracking and applying vertical offsets.
     * @param isBreakable        A flag indicating whether the object can be broken.
     * @return A {@link YPositionOnPage} containing the new Y-position and page number.
     */
    public YPositionOnPage definePositionOnPage(double currentPositionY,
                                                double objectHeight, double objectMarginTop, double objectMarginBottom,
                                                int currentPageNumber, Canvas canvas,
                                                Offset yOffset, boolean isBreakable, Entity entity) throws BigSizeElementException, PageOutOfBoundException {
        return calculatePageCoordinates(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvas.boundingTopLine(), canvas.margin().top(), canvas.margin().bottom(), yOffset, isBreakable, entity);
    }

    /**
     * Internal delegate method for {@link #definePositionOnPage}.
     */
    private YPositionOnPage definePositionOnPage(double currentPositionY,
                                                 @NonNull Entity entity,
                                                 int currentPageNumber, double canvasHigh, double canvasMarginTop, double canvasMarginBottom,  //Canvas Settings
                                                 @NonNull Offset yOffset,
                                                 boolean isBreakable) {
        var size = entity.getComponent(ContentSize.class).orElseThrow();
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());

        double objectHeight = size.height();
        double objectMarginTop = margin.top();
        double objectMarginBottom = margin.bottom();
        try {
            return calculatePageCoordinates(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvasHigh, canvasMarginTop, canvasMarginBottom, yOffset, isBreakable, entity);
        } catch (BigSizeElementException | PageOutOfBoundException e) {
            throw new RuntimeException(entity.printInfo(), e);
        }

    }

    /**
     * Internal delegate method for {@link #definePositionOnPage}.
     */
    public YPositionOnPage definePositionOnPage(double currentPositionY,
                                                Entity entity,
                                                int currentPageNumber, Canvas canvas,  //Canvas Settings
                                                Offset yOffset,
                                                boolean isBreakable) {
        log.debug("Defining position on page for {}", entity);
        log.debug("currentPositionY: {}, currentPageNumber: {} , yOffset: {}, isBreakable: {}", currentPositionY, currentPageNumber, yOffset, isBreakable);

        var size = entity.getComponent(ContentSize.class).orElseThrow();
        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());

        double objectHeight = size.height();
        double objectMarginTop = margin.top();
        double objectMarginBottom = margin.bottom();
        try {
            return definePositionOnPage(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvas, yOffset, isBreakable,entity);
        } catch (BigSizeElementException e) {
            log.error("{}{}", entity.printInfo(), e.getMessage());
            throw new RuntimeException(entity.printInfo(), e);
        } catch (PageOutOfBoundException e) {

            throw new RuntimeException(e);
        }

    }
    /**
     * Calculates the page offset relative to a starting page.
     * <p>
     * Given an absolute Y-coordinate and a fixed {@code pageHeight} ($H$), this
     * determines how many pages away the coordinate is from the page that starts at Y=0.
     * The offset is defined as follows (with the Y-axis pointing down):
     * <ul>
     * <li>$Y \in [0, H)$ $\rightarrow$ 0 (same page)</li>
     * <li>$Y \in [H, 2H)$ $\rightarrow$ -1 (one page down)</li>
     * <li>$Y \in [-H, 0)$ $\rightarrow$ 1 (one page up)</li>
     * </ul>
     * The calculation formula is: $- \lfloor \frac{Y}{H} \rfloor$.
     *
     * @param currentPositionY The entity's absolute Y-coordinate.
     * @param pageHeight       The height of a single page (must be a finite positive number).
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
    /**
     * Core logic for calculating the position on a page.
     */
    private YPositionOnPage calculatePageCoordinates(double currentPositionY,
                                                     double objectHeight, double objectMarginTop, double objectMarginBottom, //object Settings
                                                     int currentPageNumber, double canvasTopBondingLine, double canvasMarginTop, double canvasMarginBottom,  //Canvas Settings
                                                     Offset yOffset, boolean isBreakable, Entity entity) throws BigSizeElementException, PageOutOfBoundException {

        if (!(canvasTopBondingLine > 0.0) || Double.isInfinite(canvasTopBondingLine)) {
            throw new IllegalArgumentException("canvasTopBondingLine must be a finite positive number; was " + canvasTopBondingLine);
        }

        log.debug("""
                        --- Defining Position On Page ---
                        Object Settings:
                          - currentPositionY:   {}
                          - objectHeight:       {}
                          - objectMargin (T/B): {}/{}
                        Canvas Settings:
                          - canvasTopBondingLine: {}
                          - canvasMargin (T/B):   {}/{}
                        State:
                          - currentPageNumber:  {}
                          - isBreakable:        {}
                          - yOffset (before):     {}
                        """,
                currentPositionY, objectHeight, objectMarginTop, objectMarginBottom,
                canvasTopBondingLine, canvasMarginTop, canvasMarginBottom,
                currentPageNumber, isBreakable, yOffset);

        int pageOffset = definePage(currentPositionY, canvasTopBondingLine); // may be negative, zero, or positive
        int startPage = Math.addExact(currentPageNumber, pageOffset); // detect overflow
        if (startPage < 0) {
            log.error("Invalid page number {}", startPage);
            throw new IllegalArgumentException("Page number is less than zero after pageOffset: " + startPage);
        }


        // Normalize y into [0, pageHeight)
        double yInPage = positiveModulo(currentPositionY, canvasTopBondingLine);
        double shift;
        int endPage = startPage;
        if (!isBreakable) {
            shift = shift(yInPage, objectHeight, objectMarginTop, objectMarginBottom, canvasMarginBottom, canvasTopBondingLine);
            log.debug("Shift: {}", shift);
            if (entity != null) {
                entity.updateParent(entityManager, shift);
            }
            if (shift < 0) {


                yInPage += shift;
                startPage++;
                yOffset.incrementY(shift);
                entity.updateParent(entityManager,  + shift);

            }
            if (yInPage < canvasMarginBottom) {

                //ShiftTo nextPage
                double currentOffset = yInPage + objectHeight;

                yInPage = canvasTopBondingLine - objectHeight;
                if (entity != null) {
                    entity.updateParent(entityManager, currentOffset + canvasMarginTop * -1);
                }
                startPage++;
                yOffset.incrementY(currentOffset * -1);

            }
            endPage = startPage;
        } else {
            double requireSpace = yInPage + objectHeight + canvasMarginTop;
            double currentSize = canvasTopBondingLine;
            if (currentSize < requireSpace) {
                log.debug("Defining a new endPage: {}", endPage);

                while (requireSpace > currentSize) {
                    currentSize += canvasMarginTop + canvasTopBondingLine;
                    endPage--;
                    if (endPage < 0) {
                        throw new PageOutOfBoundException("Page out of bound, Current PageNumber" + endPage);
                    }
                }
            }
        }


        YPositionOnPage result = new YPositionOnPage(yInPage, startPage, endPage);


        log.debug("Defined position on page: {}", result);
        return result;

    }

    /**
     * True mathematical modulo (always positive).
     */
    private static double positiveModulo(double a, double m) {
        double r = a % m;
        return (r < 0) ? r + m : r;
    }



}