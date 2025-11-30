package com.demcha.system.utils.page_breaker;

import com.demcha.core.EntityManager;
import com.demcha.exceptions.BigSizeElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;


@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PageBreakerTest {
    EntityManager entityManager = mock(EntityManager.class);
    PageLayoutCalculator pageLayoutCalculator = new PageLayoutCalculator(entityManager);

    @ParameterizedTest(name = "yPos={0}, height={1}, marginT={2}, marginB={3}, canvasH={4}, canvasMarginT={5}, canvasMarginB={6} -> Expected Shift: {7}")
    @CsvSource({
            // 1. yPosition < canvasMarginBottom -> Returns 0.0 (Early Exit)
            "10.0, 50.0, 10.0, 10.0, 100.0, 20.0, 20.0, -100.0",

            // 2. Normal case where it fits (goes to else block -> 0.0)
            "50.0, 20.0, 5.0, 5.0, 200.0, 20.0, 20.0, 0.0",

            // 3. Edge Case: Element exactly fits the inner height
            "30.0, 60.0, 10.0, 10.0, 100.0, 10.0, 10.0, -120.0",

            "5.0, 40.0, 10.0, 10.0, 100.0, 20.0, 20.0, -85.0",

            "45.0, 40.0, 10.0, 10.0, 90.0, 20.0, 20.0, -25.0",

            "40.0, 35.0, 10.0, 10.0, 90.0, 20.0, 20.0, -15.0"
    })
    @DisplayName("Test downShift method")
    void testDownShift(double yPosition, double elementHeight, double elementMarginTop, double elementMarginBottom,
                       double canvasHeight, double canvasMarginTop, double canvasMarginBottom, double expectedShift) throws BigSizeElementException {
        double actualShift = PageLayoutCalculator.downShift(yPosition, elementHeight, elementMarginBottom, elementMarginTop,
                canvasHeight, canvasMarginBottom, canvasMarginTop);
        assertEquals(expectedShift, actualShift, 0.001);
    }

}