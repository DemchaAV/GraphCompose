package com.demcha.compose.testsupport.engine.assembly;

import com.demcha.compose.font.FontName;
import com.demcha.compose.engine.components.content.text.BlockTextData;
import com.demcha.compose.engine.components.content.text.BlockTextLineMetrics;
import com.demcha.compose.engine.components.content.text.LineTextData;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.geometry.ContentSize;
import com.demcha.compose.engine.components.layout.Align;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.measurement.TextMeasurementSystem;
import com.demcha.compose.engine.measurement.FontLibraryTextMeasurementSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockTextBuilderTest {

        private EntityManager entityManager;

        @BeforeEach
        void setUp() {
                entityManager = new EntityManager();
                entityManager.getSystems().addSystem(new FontLibraryTextMeasurementSystem(entityManager.getFonts(), PdfFont.class));
                entityManager.setMarkdown(true);
        }

        @Test
        void testComplexMarkdownText() {
                String text = "**Lead Engineer**, Nikoplast, Odessa, Ukraine | *Sep 2016 – Feb 2022*\n" +
                                "- Developed strong leadership and mentoring skills, guiding a team of 8 and acting as a key point of contact for management, effectively **communicating with senior non-technical staff**.\n"
                                +
                                "- Championed process improvements and maintained rigorous quality protocols, demonstrating a pragmatic approach to problem-solving and delivery excellence.";

                TextStyle textStyle = TextStyle.builder()
                                .size(10)
                                .color(ComponentColor.BLACK)
                                .fontName(FontName.HELVETICA)
                                .decoration(TextDecoration.DEFAULT)
                                .build();

                BlockTextBuilder builder = new BlockTextBuilder(entityManager, Align.left(1.0), textStyle);
                builder.size(new ContentSize(500, 100)); // Set a reasonable width
                builder.padding(Padding.zero());
                builder.margin(Margin.zero());
                builder.anchor(Anchor.topLeft());

                // Use breakLinesFromList directly or via text() method if available for list
                // Since the input is a single string with newlines, we can split it or pass as
                // list
                List<String> textList = List.of(text);

                builder.text(textList, textStyle, Padding.zero(), Margin.zero());
                Entity entity = builder.build();

                assertTrue(entity.has(BlockTextData.class));
                BlockTextData blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();

                assertFalse(blockTextData.lines().isEmpty(), "BlockTextData should contain lines");

                // Basic validation of content structure
                // We expect multiple lines due to wrapping and newlines in source text
                assertTrue(blockTextData.lines().size() > 1, "Should have multiple lines");

                // Verify that bold/italic parsing happened (checking if we have different
                // styles in the first line)
                // The first line starts with "**Lead Engineer**", so the first segment should
                // be bold.
                var firstLine = blockTextData.lines().get(0);
                assertFalse(firstLine.bodies().isEmpty());

                // Note: Exact verification depends on how MarkDownParser splits tokens and how
                // line breaking works with the font.
                // But we can check if we have at least some bold text.
                boolean hasBold = blockTextData.lines().stream()
                                .flatMap(line -> line.bodies().stream())
                                .anyMatch(body -> body.textStyle().decoration() == TextDecoration.BOLD
                                                || body.textStyle().decoration() == TextDecoration.BOLD_ITALIC);

                assertTrue(hasBold, "Should contain bold text segments");

                boolean hasItalic = blockTextData.lines().stream()
                                .flatMap(line -> line.bodies().stream())
                                .anyMatch(body -> body.textStyle().decoration() == TextDecoration.ITALIC
                                                || body.textStyle().decoration() == TextDecoration.BOLD_ITALIC);

                assertTrue(hasItalic, "Should contain italic text segments");
        }

        @Test
        void shouldIncreasePrecomputedHeightWhenMarkdownContainsHeadingLine() {
                TextStyle textStyle = TextStyle.builder()
                                .size(10)
                                .color(ComponentColor.BLACK)
                                .fontName(FontName.HELVETICA)
                                .decoration(TextDecoration.DEFAULT)
                                .build();

                BlockTextBuilder plainBuilder = new BlockTextBuilder(entityManager, Align.left(2.0), textStyle);
                plainBuilder.size(new ContentSize(320, 100));
                plainBuilder.padding(Padding.zero());
                plainBuilder.margin(Margin.zero());
                plainBuilder.anchor(Anchor.topLeft());
                plainBuilder.text(List.of("First line\nSecond line\nThird line"), textStyle, Padding.zero(), Margin.zero());
                Entity plainEntity = plainBuilder.build();

                BlockTextBuilder headingBuilder = new BlockTextBuilder(entityManager, Align.left(2.0), textStyle);
                headingBuilder.size(new ContentSize(320, 100));
                headingBuilder.padding(Padding.zero());
                headingBuilder.margin(Margin.zero());
                headingBuilder.anchor(Anchor.topLeft());
                headingBuilder.text(List.of("First line\n# Section heading\nThird line"), textStyle, Padding.zero(), Margin.zero());
                Entity headingEntity = headingBuilder.build();

                double plainHeight = plainEntity.getComponent(ContentSize.class).orElseThrow().height();
                double headingHeight = headingEntity.getComponent(ContentSize.class).orElseThrow().height();
                BlockTextData headingBlockTextData = headingEntity.getComponent(BlockTextData.class).orElseThrow();

                assertTrue(headingHeight > plainHeight,
                                "Heading line should enlarge ContentSize before rendering so containers reserve more height");
                assertCachedMeasurementsPresent(headingBlockTextData);
                assertTrue(headingBlockTextData.lines().get(1).lineMetrics().lineHeight()
                                > headingBlockTextData.lines().get(0).lineMetrics().lineHeight(),
                                "Heading line should carry taller cached metrics than the body lines");
                assertContentSizeMatchesCachedMeasurements(headingEntity, headingBlockTextData);
        }

        @Test
        void shouldBuildBlockTextWithoutLayoutSystemWhenMeasurementSystemIsRegistered() {
                TextStyle textStyle = TextStyle.builder()
                                .size(10)
                                .color(ComponentColor.BLACK)
                                .fontName(FontName.HELVETICA)
                                .decoration(TextDecoration.DEFAULT)
                                .build();

                Entity entity = new BlockTextBuilder(entityManager, Align.left(2.0), textStyle)
                                .size(new ContentSize(320, 100))
                                .padding(Padding.zero())
                                .margin(Margin.zero())
                                .anchor(Anchor.topLeft())
                                .text(List.of("Line one\n# Heading\nLine three"), textStyle, Padding.zero(), Margin.zero())
                                .build();

                assertTrue(entity.getComponent(BlockTextData.class).isPresent());
                assertTrue(entity.getComponent(ContentSize.class).isPresent());

                BlockTextData blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();
                assertCachedMeasurementsPresent(blockTextData);
                assertContentSizeMatchesCachedMeasurements(entity, blockTextData);
        }

        private void assertCachedMeasurementsPresent(BlockTextData blockTextData) {
                assertFalse(blockTextData.lines().isEmpty(), "Expected cached lines to be present");

                for (LineTextData line : blockTextData.lines()) {
                        assertTrue(line.hasCachedLineWidth(), "Line width cache should be populated");
                        assertTrue(line.hasCachedLineMetrics(), "Line metrics cache should be populated");
                        assertTrue(line.hasCachedBaselineOffset(), "Baseline cache should be populated");
                        assertEquals(line.lineMetrics().baselineOffsetFromBottom(), line.baselineOffset(), 0.0001,
                                        "Cached baseline should match the cached line metrics");
                }
        }

        private void assertContentSizeMatchesCachedMeasurements(Entity entity, BlockTextData blockTextData) {
                ContentSize contentSize = entity.getComponent(ContentSize.class).orElseThrow();
                Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
                Align align = entity.getComponent(Align.class).orElse(Align.defaultAlign(0.0));
                TextStyle style = entity.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE);

                double expectedWidth = blockTextData.lines().stream()
                                .mapToDouble(LineTextData::lineWidth)
                                .max()
                                .orElse(0.0) + padding.horizontal();

                TextMeasurementSystem.LineMetrics baseMetrics =
                                BlockTextLineMetrics.resolveStyleMetrics(entityManager, style);

                double expectedHeight = padding.vertical();
                List<LineTextData> lines = blockTextData.lines();
                for (int i = 0; i < lines.size(); i++) {
                        TextMeasurementSystem.LineMetrics metrics = lines.get(i).lineMetrics();
                        expectedHeight += metrics.lineHeight();
                        if (i < lines.size() - 1) {
                                expectedHeight += BlockTextLineMetrics.interLineGap(
                                                metrics,
                                                lines.get(i + 1).lineMetrics(),
                                                baseMetrics,
                                                align.spacing());
                        }
                }

                assertEquals(expectedWidth, contentSize.width(), 0.001,
                                "Content width should be derived from cached line widths");
                assertEquals(expectedHeight, contentSize.height(), 0.001,
                                "Content height should be derived from cached line metrics");
        }
}
