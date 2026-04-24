package com.demcha.compose.document.templates.support.common;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTemplateComposeTargetTest {

    @Test
    void addModuleShouldAttachSectionNodeInsteadOfFlatteningBlocks() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .create()) {
            SessionTemplateComposeTarget target = new SessionTemplateComposeTarget(session);

            target.startDocument("TemplateRoot", 8);
            target.addModule(new TemplateModuleSpec(
                    "TechnicalSkills",
                    TemplateSceneSupport.paragraph(
                            "TechnicalSkillsHeading",
                            "Technical Skills",
                            TextStyle.DEFAULT_STYLE,
                            TextAlign.LEFT,
                            0.0,
                            Padding.zero(),
                            Margin.bottom(4)),
                    List.of(
                            TemplateModuleBlock.paragraph(TemplateSceneSupport.paragraph(
                                    "TechnicalSkillsBody",
                                    "Canonical modules stay grouped in the document graph.",
                                    TextStyle.DEFAULT_STYLE,
                                    TextAlign.LEFT,
                                    0.0,
                                    Padding.zero(),
                                    Margin.zero())),
                            TemplateModuleBlock.list(TemplateSceneSupport.list(
                                    "TechnicalSkillsList",
                                    List.of("Java", "SQL", "Kotlin"),
                                    ListMarker.bullet(),
                                    TextStyle.DEFAULT_STYLE,
                                    TextAlign.LEFT,
                                    0.0,
                                    0.0,
                                    Padding.zero(),
                                    Margin.zero())))));
            target.finishDocument();

            assertThat(session.roots()).hasSize(1);
            DocumentNode rootNode = session.roots().getFirst();
            assertThat(rootNode).isInstanceOf(ContainerNode.class);

            ContainerNode root = (ContainerNode) rootNode;
            assertThat(root.children()).hasSize(1);
            assertThat(root.children().getFirst()).isInstanceOf(SectionNode.class);

            SectionNode module = (SectionNode) root.children().getFirst();
            assertThat(module.name()).isEqualTo("TechnicalSkills");
            assertThat(module.children()).hasSize(3);
            assertThat(module.children().get(0)).isInstanceOf(ParagraphNode.class);
            assertThat(((ParagraphNode) module.children().get(0)).text()).isEqualTo("Technical Skills");
            assertThat(module.children().get(1)).isInstanceOf(ParagraphNode.class);
            assertThat(module.children().get(2)).isInstanceOf(ListNode.class);
        }
    }

    @Test
    void customBlocksInsideModuleShouldRenderIntoTheModuleSection() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(new PDRectangle(240, 180))
                .margin(Margin.of(12))
                .create()) {
            SessionTemplateComposeTarget target = new SessionTemplateComposeTarget(session);

            target.startDocument("TemplateRoot", 8);
            target.addModule(new TemplateModuleSpec(
                    "Projects",
                    TemplateSceneSupport.paragraph(
                            "ProjectsHeading",
                            "Projects",
                            TextStyle.DEFAULT_STYLE,
                            TextAlign.LEFT,
                            0.0,
                            Padding.zero(),
                            Margin.zero()),
                    List.of(
                            TemplateModuleBlock.custom(moduleTarget -> moduleTarget.addParagraph(
                                    TemplateSceneSupport.paragraph(
                                            "ProjectsBody",
                                            "Custom blocks still emit inside the canonical module section.",
                                            TextStyle.DEFAULT_STYLE,
                                            TextAlign.LEFT,
                                            0.0,
                                            Padding.zero(),
                                            Margin.zero()))))));
            target.finishDocument();

            ContainerNode root = (ContainerNode) session.roots().getFirst();
            SectionNode module = (SectionNode) root.children().getFirst();

            assertThat(module.children()).hasSize(2);
            assertThat(module.children().get(1)).isInstanceOf(ParagraphNode.class);
            assertThat(((ParagraphNode) module.children().get(1)).text())
                    .isEqualTo("Custom blocks still emit inside the canonical module section.");
        }
    }
}
