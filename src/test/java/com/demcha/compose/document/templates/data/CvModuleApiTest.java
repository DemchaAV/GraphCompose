package com.demcha.compose.document.templates.data;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.templates.support.TemplateDividerSpec;
import com.demcha.compose.document.templates.support.TemplateTableSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CvModuleApiTest {

    @Test
    void factoriesShouldCreateParagraphBulletAndRowModules() {
        CvModule paragraph = CvModule.paragraph("Professional Summary", "Compose-first backend engineer.");
        CvModule bullets = CvModule.list("Technical Skills", List.of("Java", "SQL", "Kotlin"));
        CvModule rows = CvModule.rows("Projects", List.of(
                "GraphCompose - Canonical V2 migration and list abstraction work.",
                "CvRewriter - Resume rewriting service with Spring Boot."));

        assertThat(paragraph.name()).isEqualTo("ProfessionalSummary");
        assertThat(paragraph.title()).isEqualTo("Professional Summary");
        assertThat(paragraph.bodyBlocks()).hasSize(1);
        assertThat(paragraph.bodyBlocks().get(0).kind()).isEqualTo(CvModule.BodyKind.PARAGRAPH);
        assertThat(paragraph.bodyBlocks().get(0).text()).isEqualTo("Compose-first backend engineer.");
        assertThat(paragraph.bodyBlocks().get(0).firstLineIndent()).isEqualTo("    ");

        assertThat(bullets.name()).isEqualTo("TechnicalSkills");
        assertThat(bullets.bodyBlocks()).hasSize(1);
        assertThat(bullets.bodyBlocks().get(0).kind()).isEqualTo(CvModule.BodyKind.LIST);
        assertThat(bullets.bodyBlocks().get(0).marker()).isEqualTo(ListMarker.bullet());
        assertThat(bullets.bodyBlocks().get(0).items()).containsExactly("Java", "SQL", "Kotlin");

        assertThat(rows.name()).isEqualTo("Projects");
        assertThat(rows.bodyBlocks()).hasSize(1);
        assertThat(rows.bodyBlocks().get(0).marker()).isEqualTo(ListMarker.none());
        assertThat(rows.bodyBlocks().get(0).continuationIndent()).isEqualTo("  ");
    }

    @Test
    void listBuilderShouldSupportDashMarkerCustomNamesAndNormalization() {
        CvModule module = CvModule.builder("Additional")
                .list(List.of("Architecture decisions", "Mentoring"), list -> list
                        .name("AdditionalRows")
                        .dash()
                        .continuationIndent(">>> ")
                        .normalizeMarkers(false))
                .build();

        CvModule.BodyBlock block = module.bodyBlocks().get(0);

        assertThat(block.kind()).isEqualTo(CvModule.BodyKind.LIST);
        assertThat(block.name()).isEqualTo("AdditionalRows");
        assertThat(block.marker()).isEqualTo(ListMarker.dash());
        assertThat(block.continuationIndent()).isEqualTo(">>> ");
        assertThat(block.normalizeMarkers()).isFalse();
    }

    @Test
    void builderShouldSupportTableDividerPageBreakAndCustomBlocks() {
        AtomicBoolean customInvoked = new AtomicBoolean(false);
        TemplateTableSpec table = new TemplateTableSpec(
                "ProjectsTable",
                List.of(TableColumnSpec.auto()),
                List.of(List.of(TableCellSpec.text("GraphCompose"))),
                TableCellStyle.DEFAULT,
                Map.of(),
                Map.of(),
                120.0,
                Padding.zero(),
                Margin.zero());
        TemplateDividerSpec divider = new TemplateDividerSpec(
                "ProjectsRule",
                120.0,
                1.0,
                Color.GRAY,
                Margin.zero());

        CvModule module = CvModule.builder("Projects")
                .table(table)
                .divider(divider)
                .pageBreak("ProjectsBreak")
                .custom(target -> customInvoked.set(true))
                .build();

        assertThat(module.bodyBlocks()).extracting(CvModule.BodyBlock::kind)
                .containsExactly(
                        CvModule.BodyKind.TABLE,
                        CvModule.BodyKind.DIVIDER,
                        CvModule.BodyKind.PAGE_BREAK,
                        CvModule.BodyKind.CUSTOM);
        assertThat(module.bodyBlocks().get(0).table()).isSameAs(table);
        assertThat(module.bodyBlocks().get(1).divider()).isSameAs(divider);
        assertThat(module.bodyBlocks().get(2).pageBreakName()).isEqualTo("ProjectsBreak");

        module.bodyBlocks().get(3).customRenderer().accept(null);
        assertThat(customInvoked).isTrue();
    }
}
