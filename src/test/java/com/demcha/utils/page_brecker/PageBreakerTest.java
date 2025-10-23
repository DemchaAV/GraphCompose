package com.demcha.utils.page_brecker;

import com.demcha.components.core.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PageBreakerTest {

    @Test
    @ParameterizedTest(name = "[{index}] y={0}, h={1}, top={2} -> expected {3}")
    @DisplayName("boundingTopLine() with multiple scenarios")
    void boundingTopLineWithMultipleScenarios(double currentPositionY, double h, double topMargin) {

    }

}
