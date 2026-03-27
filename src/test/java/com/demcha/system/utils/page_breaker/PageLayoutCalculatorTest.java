package com.demcha.system.utils.page_breaker;

import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.exceptions.BigSizeElementException;
import com.demcha.compose.layout_core.system.utils.page_breaker.Offset;
import com.demcha.compose.layout_core.system.utils.page_breaker.PageLayoutCalculator;
import com.demcha.compose.layout_core.system.utils.page_breaker.YPositionOnPage;
import com.demcha.mock.FactoryClasses;
import com.demcha.mock.FactoryPresets;
import com.demcha.mock.data.CanvasData;
import com.demcha.mock.data.MarginData;
import com.demcha.mock.data.OffsetData;
import com.demcha.mock.data.SizeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PageLayoutCalculatorTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PageLayoutCalculator pageLayoutCalculator;

    static Stream<Arguments> provideScenarios() {

        return Stream.of(
                // Сценарий 1: Стандартный А4, маленький объект
                Arguments.of(
                        "Standard A4 Flow",             // Название
                        100.0, //Current positionY
                        0,// Current page
                        FactoryPresets.OBJ_SMALL_BOX_100_50,      // <--- БЕРЕМ ГОТОВЫЙ ПРЕСЕТ
                        FactoryPresets.MARGIN_STANDARD_20,
                        FactoryPresets.CANVAS_A4_STANDARD, // <--- БЕРЕМ ГОТОВЫЙ ПРЕСЕТ
                        FactoryPresets.OFFSET_DATA_ZERO, // Отступы объекта
                        false,
                        0.0,
                        new YPositionOnPage(100, 0, 0)                          // Ожидаемый результат
                ),
                Arguments.of(
                        "Full Width No Margins",             // Название
                        100.0,
                        1,// Current Y
                        FactoryPresets.OBJ_FULL_WIDTH_500_100,      // <--- БЕРЕМ ГОТОВЫЙ ПРЕСЕТ
                        FactoryPresets.MARGIN_ZERO,
                        FactoryPresets.CANVAS_A4_NO_MARGINS, // <--- БЕРЕМ ГОТОВЫЙ ПРЕСЕТ
                        FactoryPresets.OFFSET_DATA_ALL_2, // Отступы объекта
                        false,
                        2,
                        new YPositionOnPage(100, 1, 1)                          // Ожидаемый результат
                ),
                Arguments.of(
                        "Full Width No Margins",             // Название
                        5.0,
                        1,// Current Y
                        FactoryPresets.OBJ_SMALL_BOX_100_50,      // <--- БЕРЕМ ГОТОВЫЙ ПРЕСЕТ
                        FactoryPresets.MARGIN_ZERO,
                        FactoryPresets.CANVAS_A4_NO_MARGINS, // <--- БЕРЕМ ГОТОВЫЙ ПРЕСЕТ
                        FactoryPresets.OFFSET_DATA_ZERO, // Отступы объекта
                        false,
                        0,
                        new YPositionOnPage(5, 1, 1)                          // Ожидаемый результат
                ),
                Arguments.of(
                        "Next page Position under Margin",             // Название
                        10.0,
                        0,
                        FactoryPresets.OBJ_SMALL_BOX_100_50, // H=50
                        FactoryPresets.MARGIN_VERT_10,       // T=10, B=10
                        FactoryPresets.CANVAS_TEST_100_100_M_T10_B20,
                        FactoryPresets.OFFSET_DATA_ZERO, // Отступы объекта
                        false,
                        -80.0,
                        new YPositionOnPage(30, 1, 1)                          // Ожидаемый результат
                ),
                Arguments.of(
                        "Next page Position under Margin 2",             // Название
                        10.0,
                        0,
                        FactoryPresets.OBJ_SMALL_BOX_100_50, // H=50
                        FactoryPresets.MARGIN_VERT_10,       // T=10, B=10
                        FactoryPresets.CANVAS_TEST_100_100_M_T10_B20,
                        FactoryPresets.OFFSET_DATA_ZERO, // Отступы объекта
                        false,
                        -80,
                        new YPositionOnPage(30, 1, 1)                          // Ожидаемый результат
                ),

                Arguments.of(
                        "Next page negative position",             // Название
                        -30,
                        0,
                        FactoryPresets.OBJ_SMALL_BOX_100_50, // H=50
                        FactoryPresets.MARGIN_VERT_10,       // T=10, B=10
                        FactoryPresets.CANVAS_TEST_100_100_M_T10_B20,
                        FactoryPresets.OFFSET_DATA_ZERO, // Отступы объекта
                        false,
                        -40,
                        new YPositionOnPage(30, 1, 1)                          // Ожидаемый результат
                )



        );
    }

    // --- 1. Data Provider (Сценарии) ---
    static Stream<Arguments> provideDownShiftScenarios() {
        return Stream.of(
                // 1. Early Exit (yPos < MarginBottom)
                Arguments.of(
                        "Early Exit",
                        10.0,
                        FactoryPresets.OBJ_SMALL_BOX_100_50, // H=50
                        FactoryPresets.MARGIN_VERT_10,       // T=10, B=10
                        FactoryPresets.CANVAS_TEST_100_100_M_T10_B20,
                        -80.0
                ),

                // 2. Normal Fit
                Arguments.of(
                        "Normal Fit",
                        50.0,
                        FactoryPresets.OBJ_W100_H20,         // H=20
                        FactoryPresets.MARGIN_VERT_5,        // T=5, B=5
                        FactoryPresets.CANVAS_TEST_100_200_M_T10_B20,
                        0.0
                ),

                // 3. Edge Case: Fits exactly
                Arguments.of(
                        "Fits Exactly",
                        30.0,
                        FactoryPresets.OBJ_W100_H60,         // H=60
                        FactoryPresets.MARGIN_VERT_10,
                        FactoryPresets.CANVAS_TEST_100_100_M_10,
                        -10.0
                ),

                // 4. Overflow with small Y
                Arguments.of(
                        "Overflow Small Y",
                        5.0,
                        FactoryPresets.OBJ_W100_H40,         // H=40
                        FactoryPresets.MARGIN_VERT_10,
                        FactoryPresets.CANVAS_TEST_100_100_M_T10_B20,
                        -65.0
                ),

                // 5. Standard A4
                Arguments.of(
                        "Standard A4 Zero Shift",
                        100.0,
                        FactoryPresets.OBJ_SMALL_BOX_100_50,
                        FactoryPresets.MARGIN_STANDARD_20,
                        FactoryPresets.CANVAS_A4_STANDARD,
                        0.0
                )
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest(name = "{0}") // Использует первый аргумент (String) как имя
    @MethodSource("provideDownShiftScenarios")
    void testDownShift_Refactored(
            String description,
            double yPosition,
            SizeData elementSize,
            MarginData elementMargin,
            CanvasData canvas,
            double expectedResult
    ) throws BigSizeElementException {
        // Мы "распаковываем" объекты обратно в примитивы для вызова старого метода
        // Если ваш метод downShift принимает объекты, код станет еще чище!

        double result = PageLayoutCalculator.downShift(
                yPosition,
                elementSize.height(),
                elementMargin.bottom(),
                elementMargin.top(),
                canvas.height(),
                canvas.margin().bottom(),
                canvas.margin().top()
        );

        assertThat(result)
                .as("Scenario: " + description)
                .isEqualTo(expectedResult);
    }

    // 💡 Exception Handling Test
    @Test
    @DisplayName("Should throw BigSizeElementException when element is taller than canvas inner height")
    void testDownShift_ThrowsException_WhenElementTooLarge() {
        // Given
        double elementHeight = 100.0;
        double elementMarginBottom = 10.0;
        double elementMarginTop = 10.0;
        // Total Element = 120.0

        double canvasHeight = 100.0;
        double canvasMarginBottom = 10.0;
        double canvasMarginTop = 10.0;
        // Canvas Inner = 80.0

        // When & Then

        assertThatThrownBy(() -> PageLayoutCalculator.downShift(
                50.0, elementHeight, elementMarginBottom, elementMarginTop,
                canvasHeight, canvasMarginBottom, canvasMarginTop
        ))
                .isInstanceOf(BigSizeElementException.class)
                .hasMessageContaining("Element is too large");
    }

    @MethodSource("provideScenarios")
    @ParameterizedTest(name = "{0}")
    void testLayoutCalculation(
            String name,
            double currentY,
            int currentPage,
            SizeData objectSizeData, // Принимаем Record
            MarginData objectMarginData,
            CanvasData canvasData,   // Принимаем Record
            OffsetData offsetData,
            boolean isBreakable,
            double expectedOffsetY,
            YPositionOnPage expectedY
    ) {
        // 1. Создаем Моки через Фабрику, передавая Records
        Canvas canvas = FactoryClasses.canvasMock(canvasData);
        ContentSize size = FactoryClasses.mockContentSize(objectSizeData);
        Margin objMargin = FactoryClasses.margin(objectMarginData);

        // Остальная подготовка...
//        Entity entity = FactoryClasses.entityMock(size, objMargin);
        Entity entity = FactoryClasses.entityMock(size, objMargin);
        Offset offset = FactoryClasses.offsetReal(offsetData);


        // 2. Выполняем тест
        var result = pageLayoutCalculator.definePositionOnPage(currentY, entity, currentPage, canvas, offset, isBreakable);

        // 3. Проверка
        assertAll("Проверка позиции на странице", // Заголовок
                () -> assertThat(result.yPosition())
                        .as("Y Position")
                        .isEqualTo(expectedY.yPosition()),

                () -> assertThat(result.startPage())
                        .as("Start Page")
                        .isEqualTo(expectedY.startPage()),

                () -> assertThat(result.endPage())
                        .as("End Page")
                        .isEqualTo(expectedY.endPage())
                ,() -> assertThat(expectedOffsetY)
                        .as("Offset")
                        .isEqualTo(offset.y())

        );

    }


}
