package com.demcha.components.geometry;


import com.demcha.compose.loyaut_core.components.core.Entity;
import com.demcha.compose.loyaut_core.components.geometry.ContentSize;
import com.demcha.compose.loyaut_core.components.geometry.OuterBoxSize;
import com.demcha.compose.loyaut_core.components.style.Margin;
import com.demcha.compose.loyaut_core.exceptions.ContentSizeNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OuterBoxSizeTest {

    @Nested
    @DisplayName("from(ContentSize, Margin)")
    class FromContentAndMargin {

        @ParameterizedTest(name = "content=({0},{1}), margin=({2},{3},{4},{5}) → outer=({6},{7})")
        @CsvSource({
                // width, height, top, right, bottom, left, expectedW, expectedH
                "100, 200, 0, 0, 0, 0, 100, 200",
                "100, 200, 5, 10, 15, 20, 130, 220",   // w + (left+right) = 100 + (20+10) = 130; h + (top+bottom) = 200 + (5+15) = 220
                "0,   0,   1, 2,  3,  4,  6,   4",
                "50.5, 75.25, 0.5, 1.5, 2.5, 3.5, 55.5, 78.25"
        })
        void sumsContentAndMarginCorrectly(double w, double h,
                                           double top, double right, double bottom, double left,
                                           double expectedW, double expectedH) {
            ContentSize content = new ContentSize(w, h);
            Margin margin = new Margin(top, right, bottom, left);

            Optional<OuterBoxSize> result = OuterBoxSize.from(content, margin);

            assertThat(result)
                    .isPresent()
                    .get()
                    .satisfies(box -> {
                        assertThat(box.width()).isEqualTo(expectedW);
                        assertThat(box.height()).isEqualTo(expectedH);
                    });
        }

        @Test
        void nullContent_throwsNullPointerBecauseOfLombokNonNull() {
            Margin margin = Margin.zero();
            assertThatThrownBy(() -> OuterBoxSize.from(null, margin))
                    .isInstanceOf(ContentSizeNotFoundException.class);
        }

        @Test
        void nullMargin_throwsNullPointerBecauseOfLombokNonNull() {
            ContentSize content = new ContentSize(10, 20);
            assertThatThrownBy(() -> OuterBoxSize.from(content, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("from(Entity)")
    class FromEntity {

        @Test
        void returnsOuterSize_whenContentSizeAndMarginPresent() {
            // Arrange
            Entity entity = mock(Entity.class);

            ContentSize content = new ContentSize(100, 200);
            Margin margin = new Margin(5, 10, 15, 20); // top,right,bottom,left

            when(entity.getComponent(ContentSize.class)).thenReturn(Optional.of(content));
            when(entity.getComponent(Margin.class)).thenReturn(Optional.of(margin));

            // Act
            Optional<OuterBoxSize> result = OuterBoxSize.from(entity);

            // Assert
            assertThat(result).isPresent();
            OuterBoxSize box = result.get();
            // width = 100 + (left+right) = 100 + (20+10) = 130
            // height = 200 + (top+bottom) = 200 + (5+15) = 220
            assertThat(box.width()).isEqualTo(130.0);
            assertThat(box.height()).isEqualTo(220.0);
        }

        @Test
        void usesZeroMargin_whenMarginMissing() {
            Entity entity = mock(Entity.class);
            ContentSize content = new ContentSize(80, 60);

            when(entity.getComponent(ContentSize.class)).thenReturn(Optional.of(content));
            when(entity.getComponent(Margin.class)).thenReturn(Optional.empty()); // Margin.zero()

            Optional<OuterBoxSize> result = OuterBoxSize.from(entity);

            assertThat(result).isPresent();
            OuterBoxSize box = result.get();
            assertThat(box.width()).isEqualTo(80.0);
            assertThat(box.height()).isEqualTo(60.0);
        }

        @Test
        void throwsContentSizeNotFound_whenContentSizeMissing() {
            Entity entity = mock(Entity.class);

            when(entity.getComponent(ContentSize.class)).thenReturn(Optional.empty());
            when(entity.getComponent(Margin.class)).thenReturn(Optional.of(Margin.zero()));

            assertThatThrownBy(() -> OuterBoxSize.from(entity))
                    .isInstanceOf(ContentSizeNotFoundException.class);
        }
    }

}