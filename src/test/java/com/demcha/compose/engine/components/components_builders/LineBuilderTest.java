package com.demcha.compose.engine.components.components_builders;

import com.demcha.compose.engine.components.content.shape.LinePath;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.core.EntityManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineBuilderTest {

    @Test
    void shouldProvideDefaultHorizontalPathAndStroke() {
        var entity = new LineBuilder(new EntityManager())
                .size(140, 10)
                .build();

        assertThat(entity.getComponent(LinePath.class)).hasValue(LinePath.horizontal());
        assertThat(entity.getComponent(Stroke.class))
                .hasValueSatisfying(stroke -> {
                    assertThat(stroke.width()).isEqualTo(Stroke.DEFAULT_WIDTH);
                    assertThat(stroke.strokeColor().color()).isEqualTo(ComponentColor.BLACK);
                });
    }

    @Test
    void shouldAllowOverridingPathAndStroke() {
        LinePath customPath = new LinePath(0.25, 1.0, 0.75, 0.0);
        Stroke customStroke = new Stroke(ComponentColor.ROYAL_BLUE, 4.0);

        var entity = new LineBuilder(new EntityManager())
                .size(120, 40)
                .vertical()
                .path(customPath)
                .stroke(customStroke)
                .build();

        assertThat(entity.getComponent(LinePath.class)).hasValue(customPath);
        assertThat(entity.getComponent(Stroke.class)).hasValue(customStroke);
    }
}
