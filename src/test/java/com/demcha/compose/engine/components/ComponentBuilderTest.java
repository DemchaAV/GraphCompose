package com.demcha.compose.engine.components;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.engine.components.components_builders.CircleBuilder;
import com.demcha.compose.engine.components.components_builders.LineBuilder;
import com.demcha.compose.engine.components.content.shape.LinePath;
import com.demcha.compose.engine.components.renderable.Circle;
import com.demcha.compose.engine.components.renderable.Line;
import com.demcha.compose.engine.components.renderable.Module;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.testsupport.EngineComposerHarness;
import com.demcha.compose.testsupport.EngineComposerHarness;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentBuilderTest {

    @Test
    void shouldCreateCircleFromComponentBuilderFactory() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
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
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
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
    void shouldCreateModuleUsingCanvasOverload() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            var entity = composer.componentBuilder()
                    .moduleBuilder(Align.middle(4), composer.canvas())
                    .build();

            assertThat(entity.hasAssignable(Module.class)).isTrue();
            assertThat(entity.getComponent(ContentSize.class)).isPresent();
        }
    }

    @Test
    void shouldCreateModuleUsingContentSizeOverload() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            var entity = composer.componentBuilder()
                    .moduleBuilder(Align.middle(4), new ContentSize(240, 120))
                    .build();

            assertThat(entity.hasAssignable(Module.class)).isTrue();
            assertThat(entity.getComponent(ContentSize.class)).isPresent();
        }
    }

    @Test
    void rootModuleShouldUseCanvasWidthMinusOwnHorizontalMargin() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf()
                .margin(24, 24, 24, 24)
                .create()) {
            var cb = composer.componentBuilder();
            var module = cb.moduleBuilder(Align.middle(4))
                    .anchor(Anchor.topLeft())
                    .margin(new Margin(0, 12, 0, 8))
                    .addChild(cb.rectangle().size(60, 20).build())
                    .build();

            composer.toBytes();

            assertThat(module.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(composer.canvas().innerWidth() - 20);
                        assertThat(size.height()).isGreaterThan(0);
                    });
        }
    }

    @Test
    void nestedModuleShouldUseParentInnerWidthInsteadOfWidestChild() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            var cb = composer.componentBuilder();
            var module = cb.moduleBuilder(Align.left(4))
                    .anchor(Anchor.topLeft())
                    .margin(new Margin(0, 10, 0, 10))
                    .addChild(cb.rectangle().size(220, 20).build())
                    .build();

            cb.vContainer(Align.left(0))
                    .size(200, 0)
                    .anchor(Anchor.topLeft())
                    .addChild(module)
                    .build();

            composer.toBytes();

            assertThat(module.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(180);
                        assertThat(size.height()).isGreaterThan(0);
                    });
        }
    }

    @Test
    void moduleShouldGrowVerticallyWithoutGrowingHorizontally() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            var cb = composer.componentBuilder();
            var module = cb.moduleBuilder(Align.middle(4), new ContentSize(240, 10))
                    .anchor(Anchor.topLeft())
                    .addChild(cb.rectangle().size(100, 30).build())
                    .addChild(cb.rectangle().size(120, 40).build())
                    .build();

            composer.toBytes();

            assertThat(module.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> {
                        assertThat(size.width()).isEqualTo(240);
                        assertThat(size.height()).isEqualTo(74);
                    });
        }
    }

    @Test
    void oversizedChildShouldNotForceModuleToExpandHorizontally() throws Exception {
        try (EngineComposerHarness composer = com.demcha.compose.testsupport.EngineComposerHarness.pdf().create()) {
            var cb = composer.componentBuilder();
            var module = cb.moduleBuilder(Align.middle(4), new ContentSize(180, 0))
                    .anchor(Anchor.topLeft())
                    .addChild(cb.rectangle().size(320, 20).build())
                    .build();

            composer.toBytes();

            assertThat(module.getComponent(ContentSize.class))
                    .hasValueSatisfying(size -> assertThat(size.width()).isEqualTo(180));
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
