package com.demcha.utils.page_brecker;

import com.demcha.components.core.Entity;
import com.demcha.components.layout.coordinator.RenderingPosition;
import com.demcha.system.utils.page_brecker.PageBreaker;
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
    @DisplayName("Sorts entries by y descending and preserves order for ties (stable sort)")
    void sortsByYDescending_andStableOnTies() {
        // Given: 4 entities with y: e1=10, e2=30, e3=20, e4=30 (e2 and e4 tie; input order is e2 then e4)
        Entity e1 = mock(Entity.class, "e1");
        Entity e2 = mock(Entity.class, "e2");
        Entity e3 = mock(Entity.class, "e3");
        Entity e4 = mock(Entity.class, "e4");

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        // LinkedHashMap to control original iteration order: e1 -> e2 -> e3 -> e4
        Map<UUID, Entity> input = new LinkedHashMap<>();
        input.put(id1, e1);
        input.put(id2, e2);
        input.put(id3, e3);
        input.put(id4, e4);

        try (MockedStatic<PageBreaker> yOfMock = mockStatic(PageBreaker.class, CALLS_REAL_METHODS);
             MockedStatic<RenderingPosition> rpMock = mockStatic(RenderingPosition.class)) {

            // Stub yOf(entity)

            // We don't care about actual value, just make from(...) return Optional present
            RenderingPosition dummy = mock(RenderingPosition.class);
            rpMock.when(() -> RenderingPosition.from(any(Entity.class)))
                    .thenReturn(Optional.of(dummy));

            // When
            Set<Map.Entry<UUID, Entity>> result = PageBreaker.sortByYPosition(input);

            // Then: it's a LinkedHashSet preserving the sorted order
            assertTrue(result instanceof LinkedHashSet, "Should return LinkedHashSet");

            // Extract order of entities from result
            List<Entity> ordered = result.stream().map(Map.Entry::getValue).collect(Collectors.toList());

            // Expected order by y desc: [e2(30), e4(30), e3(20), e1(10)]
            // tie between e2 and e4 should keep original relative order e2 before e4
            assertEquals(List.of(e2, e4, e3, e1), ordered, "Order should be by y desc and stable for ties");

            // Ensure size matches
            assertEquals(input.size(), result.size());

            // Verify RenderingPosition.from called for each entity (peek side-effect)
            rpMock.verify(() -> RenderingPosition.from(e1), times(1));
            rpMock.verify(() -> RenderingPosition.from(e2), times(1));
            rpMock.verify(() -> RenderingPosition.from(e3), times(1));
            rpMock.verify(() -> RenderingPosition.from(e4), times(1));

            // Verify yOf called for sorting
            yOfMock.verify(() ->  RenderingPosition.from (e1).orElseThrow().y(), atLeastOnce());
            yOfMock.verify(() ->  RenderingPosition.from (e1).orElseThrow().y(), atLeastOnce());
            yOfMock.verify(() ->  RenderingPosition.from (e1).orElseThrow().y(), atLeastOnce());
            yOfMock.verify(() ->  RenderingPosition.from (e1).orElseThrow().y(), atLeastOnce());
        }
    }

    @Test
    @DisplayName("Throws NPE when entities is null")
    void throwsOnNullMap() {
        assertThrows(NullPointerException.class, () -> PageBreaker.sortByYPosition(null));
    }

    @Test
    @DisplayName("Returns empty LinkedHashSet for empty input")
    void returnsEmptyForEmptyInput() {
        Map<UUID, Entity> empty = new LinkedHashMap<>();
        Set<Map.Entry<UUID, Entity>> result = PageBreaker.sortByYPosition(empty);

        assertTrue(result instanceof LinkedHashSet, "Should return LinkedHashSet even if empty");
        assertTrue(result.isEmpty());
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
        int result = PageBreaker.definePage(currentPositionY, pageHeight);
        assertEquals(expected, result);
    }

}

