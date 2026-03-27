package com.demcha.compose.layout_core.components;

import com.demcha.templates.TemplateBuilder;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
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
}
