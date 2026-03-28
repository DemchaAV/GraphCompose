package com.demcha.compose.layout_core.components;

import com.demcha.templates.TemplateBuilder;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.components_builders.CircleBuilder;
import com.demcha.compose.layout_core.components.components_builders.LineBuilder;
import com.demcha.compose.layout_core.components.content.shape.LinePath;
import com.demcha.compose.layout_core.components.renderable.Circle;
import com.demcha.compose.layout_core.components.renderable.Line;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.core.DocumentComposer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentBuilderTest {

    @Test
    void templateWithoutExplicitThemeShouldUseDefaultTemplateBuilderTheme() throws Exception {
        try (DocumentComposer composer = GraphCompose.pdf().create()) {
            var entity = TemplateBuilder.from(composer.componentBuilder())
                    .name("Jane Doe");

            assertThat(entity.getComponent(TextStyle.class))
                    .hasValueSatisfying(style -> assertThat(style.fontName()).isEqualTo(FontName.COURIER));
        }
    }

    @Test
    void shouldCreateCircleFromComponentBuilderFactory() throws Exception {
        try (DocumentComposer composer = GraphCompose.pdf().create()) {
            var entity = composer.componentBuilder()
                    .circle()
                    .size(40, 30)
                    .build();

            assertThat(entity.hasAssignable(Circle.class)).isTrue();
            assertThat(entity.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(40);
                        assertThat(size.height()).isEqualTo(30);
                    });
        }
    }

    @Test
    void shouldCreateLineFromComponentBuilderFactory() throws Exception {
        try (DocumentComposer composer = GraphCompose.pdf().create()) {
            var entity = composer.componentBuilder()
                    .line()
                    .size(120, 8)
                    .build();

            assertThat(entity.hasAssignable(Line.class)).isTrue();
            assertThat(entity.getComponent(LinePath.class)).hasValue(LinePath.horizontal());
            assertThat(entity.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(120);
                        assertThat(size.height()).isEqualTo(8);
                    });
        }
    }

    @Test
    void circleBuilderShouldStayLeafLikeAndNotExposeRectangleOnlyShapeMethods() {
        var methodNames = Arrays.stream(CircleBuilder.class.getMethods())
                .map(method -> method.getName())
                .toList();

        assertThat(methodNames)
                .contains("fillColor", "stroke", "size", "padding", "margin", "anchor")
                .doesNotContain("cornerRadius", "rectangle");
    }

    @Test
    void lineBuilderShouldStayLeafLikeAndExposeLineSpecificPathMethods() {
        var methodNames = Arrays.stream(LineBuilder.class.getMethods())
                .map(method -> method.getName())
                .toList();

        assertThat(methodNames)
                .contains("stroke", "path", "horizontal", "vertical", "diagonalAscending", "diagonalDescending",
                        "size", "padding", "margin", "anchor")
                .doesNotContain("fillColor", "cornerRadius", "rectangle");
    }
}
