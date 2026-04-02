package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.font_library.FontName;
import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.LayoutSystem;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfCanvas;
import com.demcha.compose.layout_core.system.implemented_systems.pdf_systems.PdfRenderingSystemECS;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockTextBuilderTest {

        private EntityManager entityManager;

        @BeforeEach
        void setUp() {
                entityManager = new EntityManager();
                PDDocument doc = new PDDocument();
                PdfCanvas canvas = new PdfCanvas(PDRectangle.A4, 0.0f);
                PdfRenderingSystemECS renderingSystemECS = new PdfRenderingSystemECS(doc, canvas);
                entityManager.getSystems().addSystem(new LayoutSystem(canvas, renderingSystemECS));
                entityManager.getSystems().addSystem(renderingSystemECS);
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

                assertTrue(headingHeight > plainHeight,
                                "Heading line should enlarge ContentSize before rendering so containers reserve more height");
        }
}
