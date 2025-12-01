package com.demcha.system.utils.page_breaker;

import com.demcha.components.components_builders.Canvas;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.style.Margin;
import com.demcha.core.EntityManager;
import com.demcha.exceptions.BigSizeElementException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PageLayoutCalculator {
    private final EntityManager entityManager;

//    private static double shift(
//            double yPosition,
//            double elementHeight,
//            double marginTop,
//            double marginBottom,
//            double canvasBottomBondingLine,
//            double canvasTopBondingLine
//    ) throws BigSizeElementException {
//
//        // 1) Can it ever fit (including margins)?
//        final double requiredHeight = elementHeight + marginTop + marginBottom;
//        final double availableHeight = canvasTopBondingLine - canvasBottomBondingLine;
//        if (requiredHeight > availableHeight) {
//            log.error("Element is too large and non-breakable — it cannot fit between the bounding lines with margins.\n" +
//                      "requiredHeight: {} availableHeight: {}", requiredHeight, availableHeight);
//
//            throw new BigSizeElementException(yPosition, elementHeight, availableHeight);
//        }
//
//        // 2) Check top overflow
//        if (yPosition + elementHeight + marginTop > canvasTopBondingLine) {
//            double delta = canvasTopBondingLine - (yPosition + elementHeight + marginTop); // negative => move down
//            log.info("Element shifted down by {} from top bound {}", delta, canvasTopBondingLine);
//            return delta;
//        }
//
//        // 3) Check bottom overflow
//        if (yPosition - marginBottom < canvasBottomBondingLine) {
//            double delta = (canvasBottomBondingLine + marginBottom) - yPosition; // positive => move up
//            // Re-check (defensive): after moving up, we still must not exceed top
//            if (yPosition + delta + elementHeight + marginTop > canvasTopBondingLine) {
//                throw new BigSizeElementException(
//                        "Element is too large and non-breakable — shifting up would exceed the top bound."
//                );
//            }
//            log.info("Element shifted up by {} from bottom bound {}", delta, canvasBottomBondingLine);
//            return delta;
//        }
//
//        // 4) Already within bounds
//        return 0.0;
//    }

    public static double downShift(double yPosition,
                                   double elementHeight,
                                   double elementMarginBottom,
                                   double elementMarginTop,
                                   double canvasHeight,
                                   double canvasMarginBottom,
                                   double canvasMarginTop
    ) throws BigSizeElementException {
        double elementOuterHeight = elementHeight + elementMarginBottom + elementMarginTop;
        double canvasInnerHeight = canvasHeight - canvasMarginBottom - canvasMarginTop;
        if (elementOuterHeight > canvasInnerHeight) {
            log.error("Element is too large and non-breakable — it cannot fit between the bounding lines with margins.\n" +
                      "requiredHeight: {} availableHeight: {}", elementOuterHeight, canvasInnerHeight);

            throw new BigSizeElementException(yPosition, elementHeight, canvasInnerHeight);
        }
        if (yPosition - elementMarginBottom > canvasMarginBottom) {
            if (yPosition + elementHeight + elementMarginTop > canvasHeight - canvasMarginTop) {
                return ((yPosition + elementHeight + elementMarginTop) - (canvasHeight - canvasMarginTop)) * -1;
            } else {
                return 0.0;
            }
        }


        if (yPosition - elementMarginBottom < canvasMarginBottom) {
            return (yPosition + elementHeight + elementMarginTop + canvasMarginTop) * -1;

        } else {
            return 0.0;
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

        double pageInnerHeight;
        double pageBottomMargin;
        double pageTopMargin;

        double objectPosition;
        double objectHeight;
        double objectMarginTop;
        double objectMarginBottom;

        log.debug("definePage: pageHeight={}, y={}", pageHeight, currentPositionY);

        // floor(y / H) is ... -2, -1, 0, 1, 2 ...
        // We want offset = -floor(y/H)
        int definedPage = (int) Math.floor(currentPositionY / pageHeight) * -1;

        log.debug("definePage -> offset={}", definedPage);
        return definedPage;
    }

    /**
     * True mathematical modulo (always positive).
     * a -  element height
     * m -  canvas height
     */
    private static double positiveModulo(double a, double m) {
        double r = a % m;
        return (r < 0) ? r + m : r;
    }

    private static void debugCalculation(double currentPositionY, double objectHeight, double objectMarginTop, double objectMarginBottom, int currentPageNumber, double canvasHeight, double canvasMarginTop, double canvasMarginBottom, Offset yOffset, boolean isBreakable) {
        log.debug("""
                        --- Defining Position On Page ---
                        Object Settings:
                          - currentPositionY:   {}
                          - objectHeight:       {}
                          - objectMargin (T/B): {}/{}
                        Canvas Settings:
                          - canvasHeight: {}
                          - canvasMargin (T/B):   {}/{}
                        State:
                          - currentPageNumber:  {}
                          - isBreakable:        {}
                          - yOffset (before):     {}
                        """,
                currentPositionY, objectHeight, objectMarginTop, objectMarginBottom,
                canvasHeight, canvasMarginTop, canvasMarginBottom,
                currentPageNumber, isBreakable, yOffset);
    }

    public static double downShift(Entity e, float startY, Canvas canvas) throws BigSizeElementException {
        var size = e.getComponent(ContentSize.class).orElseThrow();
        var margin = e.getComponent(Margin.class).orElse(Margin.zero());


        return downShift(startY, size.height(), margin.bottom(), margin.top(), canvas.height(), canvas.margin().bottom(), canvas.margin().top());
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
        //canvas
        double canvasHeight = canvas.height();
        double canvasMarginTop = canvas.margin().top();
        double canvasMarginBottom = canvas.margin().bottom();

        try {

            return calculatePageCoordinates(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvasHeight, canvasMarginTop, canvasMarginBottom, yOffset, isBreakable, entity);

        } catch (BigSizeElementException e) {
            log.error("{}{}", entity.printInfo(), e.getMessage());
            throw new RuntimeException(entity.printInfo(), e);
        } catch (PageOutOfBoundException e) {

            throw new RuntimeException(e);
        }

    }

    /**
     * Core logic for calculating the position on a page.
     */
    public YPositionOnPage calculatePageCoordinates(double currentPositionY,
                                                    double objectHeight, double objectMarginTop, double objectMarginBottom, //object Settings
                                                    int currentPageNumber, double canvasHeight, double canvasMarginTop, double canvasMarginBottom,  //Canvas Settings
                                                    Offset yOffset, boolean isBreakable, Entity entity) throws BigSizeElementException, PageOutOfBoundException {

        if (!(canvasHeight > 0.0) || Double.isInfinite(canvasHeight)) {
            throw new IllegalArgumentException("canvasHeight must be a finite positive number; was " + canvasHeight);
        }

        if (log.isDebugEnabled()) {
            debugCalculation(currentPositionY, objectHeight, objectMarginTop, objectMarginBottom, currentPageNumber, canvasHeight, canvasMarginTop, canvasMarginBottom, yOffset, isBreakable);
        }

        int pageOffset = definePage(currentPositionY, canvasHeight); // may be negative, zero, or positive
        int startPage = Math.addExact(currentPageNumber, pageOffset);
        log.debug("startPage: {}, pageOffset: {}", startPage, pageOffset);
        if (startPage < 0) {
            log.error("Invalid page number {}", startPage);
            throw new IllegalArgumentException("Page number is less than zero after pageOffset: " + startPage);
        }


        // Normalize y into [0, pageHeight)
        double yInPage = positiveModulo(currentPositionY, canvasHeight);
        double shift = 0.0;


        int endPage = startPage;
        if (!isBreakable) {
            log.trace("Element is not breakable {}", entity);
            shift = downShift(yInPage, objectHeight, objectMarginBottom, objectMarginTop, canvasHeight, canvasMarginBottom, canvasMarginTop);
            if (shift == 0) {
                return new YPositionOnPage(yInPage, startPage, endPage);
            }
            yInPage += shift;
            pageOffset = definePage(yInPage, canvasHeight);
            yInPage = positiveModulo(yInPage, canvasHeight);
            startPage += pageOffset;
            log.debug("Shift: {}", shift);
            endPage = startPage;

        } else {
            double requireSpace = yInPage + objectHeight + objectMarginTop;
            double currentSize = Math.min(yInPage + requireSpace, canvasHeight - canvasMarginTop);
            if (currentSize < requireSpace) {
                log.debug("Defining a new endPage: {}", endPage);

                while (requireSpace >= currentSize) {
                    currentSize += canvasHeight;
                    endPage--;
                    if (endPage < 0) {
                        endPage=0;
                        log.error("Page out of bound, Current PageNumber {}, {}", endPage, entity.printInfo());
                        //TODO посмотреть почему делает меньше 0
//                        throw new PageOutOfBoundException( String.format("Page out of bound, Current PageNumber %d, %s", endPage, entity.printInfo()));
                    }
                }

            }
        }
        if (entity != null) {
            entity.updateEntitySize(entityManager,shift);
        }
        yOffset.incrementY(shift);
        YPositionOnPage result = new YPositionOnPage(yInPage, startPage, endPage);


        log.debug("Defined position on page: {}", result);
        return result;

    }


}