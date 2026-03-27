package com.demcha.components.geometry;

import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.exceptions.ContentSizeNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class InnerBoxSizeTest {
    @Nested
    @DisplayName("from(ContentSize, Padding)")
    class FromContentAndPadding {

        @ParameterizedTest(name = "content=({0},{1}), padding=({2},{3},{4},{5}) → inner=({6},{7})")
        @CsvSource({
                // width, height, top, right, bottom, left, expectedInnerW, expectedInnerH
                "100, 200, 0, 0, 0, 0, 100, 200",
                "100, 200, 5, 10, 15, 20, 70, 180",     // W:100-(20+10)=70; H:200-(5+15)=180
                "50,  40,  10, 5,  10, 5,  40, 20",
                "50.5, 40.25, 1.5, 2.5, 3.5, 4.5, 43.5, 35.25",
                // padding larger than content → negative inner size (current behavior)
                "10,  10,  6, 6,  6, 6,  -2, -2"
        })
        void computesInnerSizeCorrectly(double w, double h,
                                        double top, double right, double bottom, double left,
                                        double expectedInnerW, double expectedInnerH) {
            ContentSize content = new ContentSize(w, h);
            Padding padding = new Padding(top, right, bottom, left);

            Optional<InnerBoxSize> result = InnerBoxSize.from(content, padding);

            assertThat(result).isPresent();
            InnerBoxSize box = result.get();
            assertThat(box.width()).isEqualTo(expectedInnerW);
            assertThat(box.height()).isEqualTo(expectedInnerH);
        }

        @Test
        void nullContent_throwsContentSizeNotFoundException() {
            Padding padding = Padding.zero();
            assertThatThrownBy(() -> InnerBoxSize.from(null, padding))
                    .isInstanceOf(ContentSizeNotFoundException.class);
        }

        @Test
        void nullPadding_throwsNullPointerWithMessage() {
            ContentSize content = new ContentSize(10, 20);
            assertThatThrownBy(() -> InnerBoxSize.from(content, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Padding cannot be null.");
        }
    }

    @Nested
    @DisplayName("from(Entity)")
    class FromEntity {

        @Test
        void returnsInnerSize_whenContentSizeAndPaddingPresent() {
            Entity entity = mock(Entity.class);

            ContentSize content = new ContentSize(100, 200);
            Padding padding = new Padding(5, 10, 15, 20); // top,right,bottom,left

            when(entity.getComponent(ContentSize.class)).thenReturn(Optional.of(content));
            when(entity.getComponent(Padding.class)).thenReturn(Optional.of(padding));

            Optional<InnerBoxSize> result = InnerBoxSize.from(entity);

            assertThat(result).isPresent();
            InnerBoxSize box = result.get();
            // W = 100 - (left+right) = 100 - (20+10) = 70
            // H = 200 - (top+bottom) = 200 - (5+15) = 180
            assertThat(box.width()).isEqualTo(70.0);
            assertThat(box.height()).isEqualTo(180.0);
        }

        @Test
        void usesZeroPadding_whenPaddingMissing() {
            Entity entity = mock(Entity.class);
            ContentSize content = new ContentSize(80, 60);

            when(entity.getComponent(ContentSize.class)).thenReturn(Optional.of(content));
            when(entity.getComponent(Padding.class)).thenReturn(Optional.empty()); // Padding.zero()

            Optional<InnerBoxSize> result = InnerBoxSize.from(entity);

            assertThat(result).isPresent();
            InnerBoxSize box = result.get();
            assertThat(box.width()).isEqualTo(80.0);
            assertThat(box.height()).isEqualTo(60.0);
        }

        @Test
        void throwsContentSizeNotFound_whenContentSizeMissing() {
            Entity entity = mock(Entity.class);

            when(entity.getComponent(ContentSize.class)).thenReturn(Optional.empty());
            when(entity.getComponent(Padding.class)).thenReturn(Optional.of(Padding.zero()));

            assertThatThrownBy(() -> InnerBoxSize.from(entity))
                    .isInstanceOf(ContentSizeNotFoundException.class);
        }


    }
}