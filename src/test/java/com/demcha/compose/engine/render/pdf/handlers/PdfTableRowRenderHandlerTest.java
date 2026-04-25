package com.demcha.compose.engine.render.pdf.handlers;

import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.engine.components.content.table.TableCellLayoutStyle;
import com.demcha.compose.engine.components.content.shape.Side;
import com.demcha.compose.engine.components.content.shape.Stroke;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.components.content.table.TableResolvedCell;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.Anchor;
import com.demcha.compose.engine.components.layout.ParentComponent;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.components.style.ComponentColor;
import com.demcha.compose.engine.components.style.Padding;
import com.demcha.compose.engine.core.EntityManager;
import com.demcha.compose.engine.render.pdf.PdfFont;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTableRowRenderHandlerTest {

    private final PdfTableRowRenderHandler handler = new PdfTableRowRenderHandler();

    @Test
    void shouldTreatFirstRowOnNewPageAsNewFragment() {
        EntityManager manager = new EntityManager();
        Entity parent = new Entity();
        Entity previousRow = new Entity();
        Entity currentRow = new Entity();

        parent.getChildren().add(previousRow.getUuid());
        parent.getChildren().add(currentRow.getUuid());

        previousRow.addComponent(new ParentComponent(parent));
        currentRow.addComponent(new ParentComponent(parent));

        previousRow.addComponent(new Placement(0, 0, 200, 20, 0, 0));
        currentRow.addComponent(new Placement(0, 0, 200, 20, 1, 1));

        manager.putEntity(parent);
        manager.putEntity(previousRow);
        manager.putEntity(currentRow);

        TableResolvedCell cell = new TableResolvedCell(
                "cell",
                0,
                100,
                20,
                List.of("value"),
                TableCellLayoutStyle.builder()
                        .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                        .build(),
                new Padding(3.0, 0.5, 0.5, 0.5),
                Set.of(Side.RIGHT, Side.BOTTOM)
        );

        assertThat(handler.startsPageFragment(manager, currentRow)).isTrue();
        assertThat(handler.effectiveBorderSides(cell, true)).containsExactlyInAnyOrder(Side.TOP, Side.RIGHT, Side.BOTTOM);
        assertThat(handler.effectiveFillInsets(cell, true))
                .isEqualTo(new Padding(1.0, 0.5, 0.5, 0.5));
    }

    @Test
    void shouldKeepSingleSeparatorWhenRowsStayOnSamePage() {
        EntityManager manager = new EntityManager();
        Entity parent = new Entity();
        Entity previousRow = new Entity();
        Entity currentRow = new Entity();

        parent.getChildren().add(previousRow.getUuid());
        parent.getChildren().add(currentRow.getUuid());

        previousRow.addComponent(new ParentComponent(parent));
        currentRow.addComponent(new ParentComponent(parent));

        previousRow.addComponent(new Placement(0, 40, 200, 20, 0, 0));
        currentRow.addComponent(new Placement(0, 20, 200, 20, 0, 0));

        manager.putEntity(parent);
        manager.putEntity(previousRow);
        manager.putEntity(currentRow);

        TableResolvedCell cell = new TableResolvedCell(
                "cell",
                0,
                100,
                20,
                List.of("value"),
                TableCellLayoutStyle.builder()
                        .stroke(new Stroke(ComponentColor.BLACK, 2.0))
                        .build(),
                new Padding(3.0, 0.5, 0.5, 0.5),
                Set.of(Side.RIGHT, Side.BOTTOM)
        );

        assertThat(handler.startsPageFragment(manager, currentRow)).isFalse();
        assertThat(handler.effectiveBorderSides(cell, false)).containsExactlyInAnyOrder(Side.RIGHT, Side.BOTTOM);
        assertThat(handler.effectiveFillInsets(cell, false))
                .isEqualTo(new Padding(3.0, 0.5, 0.5, 0.5));
    }

    @Test
    void shouldResolveTopAndMiddleAnchorsForMultilineContent() {
        PdfFont font = DefaultFonts.standardLibrary()
                .getFont(TextStyle.DEFAULT_STYLE.fontName(), PdfFont.class)
                .orElseThrow();

        TableResolvedCell topCell = new TableResolvedCell(
                "top",
                0,
                120,
                60,
                List.of("09:00 17:00", "Stock take"),
                TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, TableCellLayoutStyle.builder()
                        .textAnchor(Anchor.topLeft())
                        .build()),
                Padding.zero(),
                Set.of(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM)
        );

        TableResolvedCell middleCell = new TableResolvedCell(
                "middle",
                0,
                120,
                60,
                List.of("09:00 17:00", "Stock take"),
                TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, TableCellLayoutStyle.builder()
                        .textAnchor(Anchor.centerLeft())
                        .build()),
                Padding.zero(),
                Set.of(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM)
        );

        var topLines = handler.resolveTextLines(font, topCell, 0, 0);
        var middleLines = handler.resolveTextLines(font, middleCell, 0, 0);
        double lineHeight = font.getLineHeight(topCell.style().textStyle());

        assertThat(topLines).hasSize(2);
        assertThat(middleLines).hasSize(2);
        assertThat(topLines.getFirst().baselineY()).isGreaterThan(middleLines.getFirst().baselineY());
        assertThat(topLines.getFirst().baselineY() - topLines.get(1).baselineY()).isEqualTo(lineHeight);
        assertThat(middleLines.getFirst().baselineY() - middleLines.get(1).baselineY()).isEqualTo(lineHeight);
    }

    @Test
    void shouldApplyCellLineSpacingWhenResolvingMultilineContent() {
        PdfFont font = DefaultFonts.standardLibrary()
                .getFont(TextStyle.DEFAULT_STYLE.fontName(), PdfFont.class)
                .orElseThrow();
        double lineSpacing = 2.5;

        TableResolvedCell cell = new TableResolvedCell(
                "spaced",
                0,
                120,
                80,
                List.of("First", "Second"),
                TableCellLayoutStyle.merge(TableCellLayoutStyle.DEFAULT, TableCellLayoutStyle.builder()
                        .textAnchor(Anchor.topLeft())
                        .lineSpacing(lineSpacing)
                        .build()),
                Padding.zero(),
                Set.of(Side.TOP, Side.LEFT, Side.RIGHT, Side.BOTTOM)
        );

        var lines = handler.resolveTextLines(font, cell, 0, 0);
        double lineHeight = font.getLineHeight(cell.style().textStyle());

        assertThat(lines).hasSize(2);
        assertThat(lines.getFirst().baselineY() - lines.get(1).baselineY()).isEqualTo(lineHeight + lineSpacing);
    }
}
