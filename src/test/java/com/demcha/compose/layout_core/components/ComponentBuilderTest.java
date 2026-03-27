package com.demcha.compose.layout_core.components;

import com.demcha.templates.TemplateBuilder;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.renderable.Circle;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.core.PdfComposer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentBuilderTest {

    @Test
    void templateWithoutExplicitThemeShouldUseDefaultTemplateBuilderTheme() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf().create()) {
            var entity = TemplateBuilder.from(composer.componentBuilder())
                    .name("Jane Doe");

            assertThat(entity.getComponent(TextStyle.class))
                    .hasValueSatisfying(style -> assertThat(style.fontName()).isEqualTo(FontName.COURIER));
        }
    }

    @Test
    void shouldCreateCircleFromComponentBuilderFactory() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf().create()) {
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
}
