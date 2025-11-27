package com.demcha.utils.page_brecker;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.system.utils.page_breaker.PageBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PageBreakerTest {


    @ParameterizedTest(name = "[{index}] p={0}, height={1} -> expected {2}")
    @CsvSource({
            // currentPosition, canvas height, expected page
            "20,        12,         -1",
            "-8,        12,         1",
            "-4.1,      2,          3",
            "-4,        2,          2",
            "1,         2,          0",
            "20.2,      40.5,       0",

    })
    void sortByYPosition() {
        Map < UUID, Entity> map = new HashMap();


    }





    @Test
    void sortByYPositionToMap() {
    }

    @Test
    void testSortByYPositionToMap() {
    }

    @Test
    void testSortByYPositionToMap1() {
    }

    @Test
    void testSortByYPositionToMap2() {
    }

    @Test
    void breakPages() {
    }

    @Test
    void testBreakPages() {
    }

    @Test
    void definePositionOnPage() {
    }

    @ParameterizedTest(name = "[{index}] p={0}, height={1} -> expected {2}")
    @CsvSource({
            // currentPosition, canvas height, expected page
            "20,        12,         -1",
            "-8,        12,         1",
            "-4.1,      2,          3",
            "-4,        2,          2",
            "1,         2,          0",
            "20.2,      40.5,       0",

    })
    void definePage(double currentPositionY, double pageHeight, int expected) {
    }

}

