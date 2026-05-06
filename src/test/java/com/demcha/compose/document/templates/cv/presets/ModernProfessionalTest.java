package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.cv.spec.CvHeader;
import com.demcha.compose.document.templates.cv.spec.CvModule;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModernProfessionalTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();

    private static CvSpec sampleSpec() {
        return CvSpec.builder()
                .header(CvHeader.builder()
                        .name("Alex Carter")
                        .address("EH3, Edinburgh, UK")
                        .phone("+44 7700 900123")
                        .email("alex.carter@example.dev")
                        .link("LinkedIn", "https://linkedin.com/in/alex")
                        .link("GitHub", "https://github.com/alexc")
                        .build())
                .module(CvModule.of("Professional Summary",
                        new ParagraphBlock("Backend Java Developer building Spring Boot microservices.")))
                .module(CvModule.of("Technical Skills",
                        new BulletListBlock(List.of("Java (21)", "SQL", "Kotlin"))))
                .module(CvModule.of("Education & Certifications",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item("MSc CS",
                                        "University of Manchester | 2021"),
                                new IndentedBlock.Item("BSc CS",
                                        "Northbridge | 2018")))))
                .module(CvModule.of("Projects",
                        new IndentedBlock(List.of(
                                new IndentedBlock.Item("TaskFlow Studio",
                                        "Spring Boot demo with JWT auth")))))
                .module(CvModule.of("Professional Experience",
                        new MultiParagraphBlock(List.of(
                                "Operations Coordinator | BrightWave | 2023-Present",
                                "Technical Team Lead | NovaBuild | 2019-2023"))))
                .module(CvModule.of("Additional Information",
                        new KeyValueBlock(List.of(
                                new KeyValueBlock.Entry("Languages", "English (Fluent)"),
                                new KeyValueBlock.Entry("Eligibility", "Eligible to work in the UK")))))
                .build();
    }

    @Test
    void exposesStableIdentityAndDisplayName() {
        DocumentTemplate<CvSpec> template = ModernProfessional.create(THEME);
        assertThat(template.id()).isEqualTo(ModernProfessional.ID).isEqualTo("modern-professional");
        assertThat(template.displayName()).isEqualTo(ModernProfessional.DISPLAY_NAME)
                .isEqualTo("Modern Professional");
    }

    @Test
    void composeAddsRootDocumentNodeToTheSession() throws Exception {
        DocumentTemplate<CvSpec> template = ModernProfessional.create(THEME);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {

            template.compose(session, sampleSpec());

            assertThat(session.roots()).isNotEmpty();
            // The single-column layout produces one ContainerNode root
            // wrapping the header plus all module nodes.
            assertThat(session.roots()).hasSize(1);
            assertThat(session.roots().get(0)).isInstanceOf(ContainerNode.class);
            ContainerNode root = (ContainerNode) session.roots().get(0);
            // Root contains: header + 6 modules = 7 children
            assertThat(root.children()).hasSize(7);
        }
    }

    @Test
    void composeFailsWhenSpecMissesAnExpectedModule() throws Exception {
        DocumentTemplate<CvSpec> template = ModernProfessional.create(THEME);
        CvSpec partial = CvSpec.builder()
                .header(CvHeader.builder().name("X").build())
                .module(CvModule.of("Professional Summary", new ParagraphBlock("x")))
                // missing the other 5 modules the preset declares
                .build();

        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {

            assertThatThrownBy(() -> template.compose(session, partial))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Technical Skills");
        }
    }

    @Test
    void rejectsNullTheme() {
        assertThatThrownBy(() -> ModernProfessional.create(null))
                .isInstanceOf(NullPointerException.class);
    }
}
