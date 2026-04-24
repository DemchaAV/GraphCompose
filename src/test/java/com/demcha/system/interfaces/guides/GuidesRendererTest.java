package com.demcha.system.interfaces.guides;

import com.demcha.compose.engine.core.Canvas;
import com.demcha.compose.engine.components.layout.coordinator.RenderCoordinateContext;
import com.demcha.mock.FactoryClasses;
import com.demcha.mock.FactoryPresets;
import com.demcha.mock.data.CanvasData;
import com.demcha.mock.data.SizeData;
import com.demcha.compose.engine.render.pdf.PdfGuidesRenderer;
import com.demcha.compose.engine.render.pdf.PdfRenderingSystemECS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat; // Use standard AssertJ
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class GuidesRendererTest {

    private final PdfRenderingSystemECS renderingSystemMock = mock(PdfRenderingSystemECS.class);
    // Spy is used to test the real breakCoordinate logic while mocking dependencies
    private final PdfGuidesRenderer guidesRenderer = spy(new PdfGuidesRenderer(renderingSystemMock));

    @BeforeEach
    void setUp() {
        doReturn(renderingSystemMock).when(guidesRenderer).renderingSystem();
    }

    // Helper Record for assertions
    record ExpectedSegment(double y, double height, int page) {}

    static Stream<Arguments> provideBreakScenarios() {
        return Stream.of(

                Arguments.of(
                        "Split across 3 Pages (Complex Loop)",
                        FactoryPresets.OBJ_TALL_5_12,     // H=12
                        FactoryPresets.CANVAS_SPLIT_5_M1, // H=5, T=2, B=2
                        2.0, // Start Y on Page 2
                        3,    // pages
                        3,    // Expected Segments (40 + 80 + 30 = 150)
                        List.of(
                                // Segment 1: Page 2 (Tail) -> Space available: (100-10)-50 = 40
                                new ExpectedSegment(2.0, 3.0, 2),

                                // Segment 2: Page 1 (Full Page) -> Space available: 80.
                                // 110 remaining is > 80, so it takes full 80.
                                // Note: Y position depends on your logic:
                                // If logic uses margin.bottom() for next page start, check this value.
                                // Assuming standard top-down flow usually starts at margin.top().
                                // Based on your code: yOnPage = canvas.margin().bottom(); (Y=10)
                                new ExpectedSegment(0.0, 5.0, 1),

                                // Segment 3: Page 0 (Head) -> Remaining: 150 - 40 - 80 = 30.
                                new ExpectedSegment(0.0, 4.0, 0)
                        )
                ),
                Arguments.of(
                        "Split across 3 Pages (Complex Loop)",
                        FactoryPresets.OBJ_TALL_5_6,     // H=12
                        FactoryPresets.CANVAS_SPLIT_5_M1, // H=5, T=2, B=2
                        2.0, // Start Y on Page 2
                        2,    // pages
                        2,    // Expected Segments (40 + 80 + 30 = 150)
                        List.of(
                                // Segment 1: Page 2 (Tail) -> Space available: (100-10)-50 = 40
                                new ExpectedSegment(2.0, 3.0, 1),

                                // Segment 2: Page 1 (Full Page) -> Space available: 80.
                                // 110 remaining is > 80, so it takes full 80.
                                // Note: Y position depends on your logic:
                                // If logic uses margin.bottom() for next page start, check this value.
                                // Assuming standard top-down flow usually starts at margin.top().
                                // Based on your code: yOnPage = canvas.margin().bottom(); (Y=10)
                                new ExpectedSegment(0.0, 3.0, 0)

                        )
                ),

                // --- SCENARIO 3: Empty Optional ---
                Arguments.of(
                        "Empty Context Returns Empty List",
                        null,
                        FactoryPresets.CANVAS_A4_STANDARD,
                        0.0,
                        1,
                        0,
                        List.of()
                )
        );
    }

    @MethodSource("provideBreakScenarios")
    @ParameterizedTest(name = "{0}")
    void testBreakCoordinate(
            String name,
            SizeData sizeData,
            CanvasData canvasData,
            double startY,
            int pagesInput,
            int expectedListSize,
            List<ExpectedSegment> expectedSegments
    ) {
        // 1. Setup Mocks
        Canvas canvas = FactoryClasses.canvasMock(canvasData);
        when(renderingSystemMock.canvas()).thenReturn(canvas);

        Optional<RenderCoordinateContext> contextOpt;
        if (sizeData == null) {
            contextOpt = Optional.empty();
        } else {
            // We only care about Y and Height for this test
            var ctx = new RenderCoordinateContext(
                    0, startY, sizeData.width(), sizeData.height(),
                    0, 0, null, null
            );
            contextOpt = Optional.of(ctx);
        }

        // 2. Execution
        List<RenderCoordinateContext> result = guidesRenderer.breakCoordinate(contextOpt, pagesInput);

        // 3. Verification
        assertAll("Breaking Logic Verification",
                () -> assertThat(result)
                        .as("Result size mismatch")
                        .hasSize(expectedListSize),

                () -> {
                    // Check up to the smaller size to avoid IndexOutOfBounds if sizes differ
                    int checks = Math.min(result.size(), expectedSegments.size());
                    for (int i = 0; i < checks; i++) {
                        var actual = result.get(i);
                        var expected = expectedSegments.get(i);
                         final int index = i;

                        assertAll("Segment " + i,
                                () -> assertThat(actual.y())
                                        .as("Y Position (Seg %d)", index)
                                        .isEqualTo(expected.y()),
                                () -> assertThat(actual.height())
                                        .as("Height (Seg %d)", index)
                                        .isEqualTo(expected.height()),
                                () -> assertThat(expected.page())
                                        .as("Page Index (Seg %d)", index)
                                        .isEqualTo(actual.endPage())
                        );
                    }
                }
        );
    }
}
