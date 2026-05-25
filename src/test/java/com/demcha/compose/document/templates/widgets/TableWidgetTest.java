package com.demcha.compose.document.templates.widgets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableWidgetTest {

    @Test
    void fixed_table_renders_with_custom_border_and_fill() throws Exception {
        render(section -> TableWidget.fixed(section,
                List.of(
                        List.of("Name", "Role"),
                        List.of("Jordan", "Engineer")),
                240,
                TableWidget.Style.builder()
                        .name("SharedWidgetTable")
                        .columns(2)
                        .cellPadding(new DocumentInsets(3, 4, 3, 4))
                        .border(DocumentColor.rgb(80, 120, 180), 0.5)
                        .cellFillColor(DocumentColor.rgb(248, 250, 252))
                        .textStyle(bodyStyle())
                        .build()));
    }

    @Test
    void grid_table_renders_with_zebra_rows() throws Exception {
        render(section -> TableWidget.grid(section,
                List.of("Java", "Kotlin", "SQL", "PDFBox", "Maven"),
                240,
                TableWidget.Style.builder()
                        .name("SharedWidgetGrid")
                        .columns(3)
                        .cellPadding(new DocumentInsets(2, 3, 2, 3))
                        .border(DocumentColor.rgb(180, 190, 205), 0.5)
                        .zebraFillColor(DocumentColor.rgb(245, 247, 250))
                        .textStyle(bodyStyle())
                        .widthAdjustment(1.0)
                        .build()));
    }

    private static void render(SectionAction action) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(320, 420)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.dsl().pageFlow()
                    .name("SharedTableWidgetRoot")
                    .addSection("SharedTableWidgetSlot", action::run)
                    .build();
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static DocumentTextStyle bodyStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .decoration(DocumentTextDecoration.DEFAULT)
                .color(DocumentColor.rgb(30, 40, 55))
                .build();
    }

    @FunctionalInterface
    private interface SectionAction {
        void run(com.demcha.compose.document.dsl.SectionBuilder section);
    }
}
