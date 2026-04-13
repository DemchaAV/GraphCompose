package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableBuilder;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.components_builders.TextBuilder;
import com.demcha.compose.layout_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.DocumentComposer;
import com.demcha.templates.WeeklyScheduleTheme;
import com.demcha.templates.data.ScheduleAssignment;
import com.demcha.templates.data.ScheduleCategory;
import com.demcha.templates.data.ScheduleDay;
import com.demcha.templates.data.ScheduleMetricRow;
import com.demcha.templates.data.SchedulePerson;
import com.demcha.templates.data.ScheduleSlot;
import com.demcha.templates.data.WeeklyScheduleData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal composition builder for the weekly schedule scene.
 * <p>
 * This class assembles a backend-neutral entity tree against a
 * {@link DocumentComposer}. Format-specific document creation stays in the
 * template adapter.
 * </p>
 */
final class WeeklyScheduleSceneBuilder {
    private static final String ROOT_NAME = "WeeklyScheduleRoot";
    private static final double HEADER_GAP = 18;
    private static final double RULE_HEIGHT = 5;
    private static final double RULE_STROKE = 1.3;

    private final WeeklyScheduleTheme theme;

    WeeklyScheduleSceneBuilder(WeeklyScheduleTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    void compose(DocumentComposer composer, WeeklyScheduleData data) {
        WeeklyScheduleData safeData = safeData(data);
        validateData(safeData);

        ComponentBuilder cb = composer.componentBuilder();
        double width = composer.canvas().innerWidth();
        GridSpec grid = gridSpec(width, safeData.days().size());
        Map<String, ScheduleCategory> categoriesById = categoriesById(safeData.categories());
        Map<CellKey, ScheduleAssignment> assignments = mergeAssignments(safeData.assignments());

        VContainerBuilder root = cb.vContainer(Align.left(theme.rootSpacing()))
                .entityName(ROOT_NAME)
                .size(width, 0)
                .anchor(Anchor.topLeft());

        root.addChild(createHeader(cb, safeData, width));
        root.addChild(createDayLabelsBand(cb, safeData.days(), grid));

        Entity noteBand = createDayNotesBand(cb, safeData.days(), grid);
        if (noteBand != null) {
            root.addChild(noteBand);
        }

        Entity categoryBand = createDayCategoryBand(cb, safeData.days(), categoriesById, grid);
        if (categoryBand != null) {
            root.addChild(categoryBand);
        }

        for (ScheduleMetricRow metric : safeData.headerMetrics()) {
            root.addChild(createMetricBand(cb, metric, grid));
        }

        root.addChild(createRosterTable(cb, safeData, categoriesById, assignments, grid));

        Entity footer = createFooterNotes(cb, safeData.footerNotes(), width);
        if (footer != null) {
            root.addChild(footer);
        }

        root.build();
    }

    private Entity createHeader(ComponentBuilder cb, WeeklyScheduleData data, double width) {
        double leftWidth = Math.max(260, width - 220);
        double rightWidth = width - leftWidth - HEADER_GAP;

        VContainerBuilder container = cb.vContainer(Align.left(4))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        HContainerBuilder row = cb.hContainer(Align.left(0))
                .size(width, 0)
                .anchor(Anchor.topLeft());

        row.addChild(createAlignedCell(cb, createText(cb, data.title(), theme.titleStyle(), Anchor.topLeft(), Margin.zero()), leftWidth, Anchor.topLeft()));
        row.addChild(createSpacer(cb, HEADER_GAP, 1));
        row.addChild(createAlignedCell(cb, createText(cb, data.weekLabel(), theme.weekLabelStyle(), Anchor.topRight(), Margin.top(8)), rightWidth, Anchor.topRight()));

        container.addChild(row.build());
        container.addChild(createRule(cb, width, Margin.zero()));
        return container.build();
    }

    private Entity createDayLabelsBand(ComponentBuilder cb, List<ScheduleDay> days, GridSpec grid) {
        List<TableCellSpec> cells = new ArrayList<>(days.size());
        for (ScheduleDay day : days) {
            List<String> labelLines = splitMultiline(day.label());
            cells.add(TableCellSpec.of(labelLines.isEmpty() ? List.of(day.label()) : labelLines));
        }
        return createBandTable(
                cb,
                "WeeklyScheduleDayLabels",
                grid,
                bandLeadingStyle(),
                dayLabelCellStyle(),
                "",
                cells);
    }

    private Entity createDayNotesBand(ComponentBuilder cb, List<ScheduleDay> days, GridSpec grid) {
        boolean hasNotes = days.stream().anyMatch(day -> !day.headerNote().isBlank());
        if (!hasNotes) {
            return null;
        }

        List<TableCellSpec> cells = new ArrayList<>(days.size());
        for (ScheduleDay day : days) {
            cells.add(TableCellSpec.of(splitMultiline(day.headerNote())));
        }
        return createBandTable(
                cb,
                "WeeklyScheduleDayNotes",
                grid,
                bandLeadingStyle(),
                dayNoteCellStyle(),
                "",
                cells);
    }

    private Entity createDayCategoryBand(ComponentBuilder cb,
                                         List<ScheduleDay> days,
                                         Map<String, ScheduleCategory> categoriesById,
                                         GridSpec grid) {
        boolean hasCategories = days.stream().anyMatch(day -> !day.headerCategoryId().isBlank());
        if (!hasCategories) {
            return null;
        }

        List<TableCellSpec> cells = new ArrayList<>(days.size());
        for (ScheduleDay day : days) {
            ScheduleCategory category = categoriesById.get(day.headerCategoryId());
            if (category == null) {
                cells.add(TableCellSpec.text(""));
                continue;
            }
            cells.add(TableCellSpec.text(category.label()).withStyle(categoryBandCellOverride(category)));
        }

        return createBandTable(
                cb,
                "WeeklyScheduleDayCategories",
                grid,
                bandLeadingStyle(),
                dayCategoryDefaultStyle(),
                "",
                cells);
    }

    private Entity createMetricBand(ComponentBuilder cb, ScheduleMetricRow metric, GridSpec grid) {
        List<TableCellSpec> cells = new ArrayList<>(grid.dayCount());
        for (int i = 0; i < grid.dayCount(); i++) {
            String value = i < metric.dayValues().size() ? metric.dayValues().get(i) : "";
            cells.add(TableCellSpec.text(value));
        }

        return createBandTable(
                cb,
                "WeeklyScheduleMetric" + sanitizeEntityPart(metric.label()),
                grid,
                metricLeadingStyle(),
                metricCellStyle(),
                metric.label(),
                cells);
    }

    private Entity createRosterTable(ComponentBuilder cb,
                                     WeeklyScheduleData data,
                                     Map<String, ScheduleCategory> categoriesById,
                                     Map<CellKey, ScheduleAssignment> assignments,
                                     GridSpec grid) {
        TableBuilder table = baseGridTable(cb, "WeeklyScheduleRoster", grid)
                .defaultCellStyle(rosterDefaultCellStyle())
                .columnStyle(0, rosterNameColumnStyle());

        List<SchedulePerson> people = sortedPeople(data.people());
        for (SchedulePerson person : people) {
            List<TableCellSpec> dayCells = new ArrayList<>(grid.dayCount());
            for (ScheduleDay day : data.days()) {
                dayCells.add(assignmentCellSpec(assignments.get(new CellKey(person.id(), day.id())), categoriesById));
            }
            table.row(buildGridRow(person.displayName(), dayCells, grid.dayCount()));
        }

        return table.build();
    }

    private Entity createFooterNotes(ComponentBuilder cb, List<String> footerNotes, double width) {
        List<String> sanitized = sanitizeLines(footerNotes);
        if (sanitized.isEmpty()) {
            return null;
        }

        VContainerBuilder container = cb.vContainer(Align.left(theme.sectionSpacing()))
                .size(width, 0)
                .anchor(Anchor.topLeft())
                .margin(Margin.top(2));

        container.addChild(createRule(cb, width, Margin.zero()));
        container.addChild(createText(cb, "Notes", theme.metricStyle(), Anchor.topLeft(), Margin.top(2)));
        container.addChild(createBulletParagraph(cb, sanitized, width, "WeeklyScheduleFooterNotes"));
        return container.build();
    }

    private Entity createBandTable(ComponentBuilder cb,
                                   String entityName,
                                   GridSpec grid,
                                   TableCellStyle leadingStyle,
                                   TableCellStyle defaultStyle,
                                   String leadingLabel,
                                   List<TableCellSpec> dayCells) {
        TableBuilder table = baseGridTable(cb, entityName, grid)
                .defaultCellStyle(defaultStyle)
                .columnStyle(0, leadingStyle);

        table.row(buildGridRow(leadingLabel, dayCells, grid.dayCount()));
        return table.build();
    }

    private TableBuilder baseGridTable(ComponentBuilder cb, String entityName, GridSpec grid) {
        return cb.table()
                .entityName(entityName)
                .anchor(Anchor.topLeft())
                .columns(grid.columns())
                .width(grid.totalWidth());
    }

    private TableCellSpec[] buildGridRow(String leadingLabel, List<TableCellSpec> dayCells, int dayCount) {
        TableCellSpec[] row = new TableCellSpec[dayCount + 1];
        row[0] = TableCellSpec.text(leadingLabel);
        for (int i = 0; i < dayCount; i++) {
            row[i + 1] = i < dayCells.size() ? dayCells.get(i) : TableCellSpec.text("");
        }
        return row;
    }

    private TableCellSpec assignmentCellSpec(ScheduleAssignment assignment,
                                             Map<String, ScheduleCategory> categoriesById) {
        if (assignment == null) {
            return TableCellSpec.text("").withStyle(emptyRosterCellStyle());
        }

        List<String> lines = new ArrayList<>();
        for (ScheduleSlot slot : assignment.slots()) {
            String display = slot.displayText();
            if (!display.isBlank()) {
                lines.add(display);
            }
        }
        lines.addAll(splitMultiline(assignment.note()));

        ScheduleCategory category = categoriesById.get(assignment.categoryId());
        if (lines.isEmpty() && category != null && !category.label().isBlank()) {
            lines.add(category.label());
        }

        return TableCellSpec.of(lines.isEmpty() ? List.of("") : lines,
                category == null ? emptyRosterCellStyle() : categoryRosterCellStyle(category));
    }

    private TableCellStyle bandLeadingStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(theme.nameColumnFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.metricStyle())
                .textAnchor(Anchor.centerLeft())
                .build();
    }

    private TableCellStyle metricLeadingStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(theme.nameColumnFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.metricStyle())
                .textAnchor(Anchor.center())
                .build();
    }

    private TableCellStyle dayLabelCellStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(theme.bandFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.dayLabelStyle())
                .textAnchor(Anchor.center())
                .build();
    }

    private TableCellStyle dayNoteCellStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(Color.WHITE)
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.noteStyle())
                .textAnchor(Anchor.centerLeft())
                .build();
    }

    private TableCellStyle dayCategoryDefaultStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(theme.bandFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.categoryLabelStyle(theme.titleColor()))
                .textAnchor(Anchor.center())
                .build();
    }

    private TableCellStyle categoryBandCellOverride(ScheduleCategory category) {
        return TableCellStyle.builder()
                .fillColor(category.fillColor())
                .stroke(new Stroke(category.borderColor(), 1.0))
                .textStyle(theme.categoryLabelStyle(category.textColor()))
                .textAnchor(Anchor.center())
                .build();
    }

    private TableCellStyle metricCellStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(Color.WHITE)
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.metricStyle())
                .textAnchor(Anchor.center())
                .build();
    }

    private TableCellStyle rosterNameColumnStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bodyPaddingVertical(), theme.bodyPaddingHorizontal(), theme.bodyPaddingVertical(), theme.bodyPaddingHorizontal()))
                .fillColor(theme.nameColumnFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.personNameStyle())
                .textAnchor(Anchor.centerLeft())
                .build();
    }

    private TableCellStyle rosterDefaultCellStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bodyPaddingVertical(), theme.bodyPaddingHorizontal(), theme.bodyPaddingVertical(), theme.bodyPaddingHorizontal()))
                .fillColor(theme.emptyCellFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.cellTextStyle(theme.bodyColor()))
                .textAnchor(Anchor.topLeft())
                .build();
    }

    private TableCellStyle emptyRosterCellStyle() {
        return TableCellStyle.builder()
                .fillColor(theme.emptyCellFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.cellTextStyle(theme.bodyColor()))
                .textAnchor(Anchor.topLeft())
                .build();
    }

    private TableCellStyle categoryRosterCellStyle(ScheduleCategory category) {
        return TableCellStyle.builder()
                .fillColor(category.fillColor())
                .stroke(new Stroke(category.borderColor(), 1.0))
                .textStyle(theme.cellTextStyle(category.textColor()))
                .textAnchor(Anchor.topLeft())
                .build();
    }

    private Entity createRule(ComponentBuilder cb, double width, Margin margin) {
        return cb.line()
                .horizontal()
                .size(width, RULE_HEIGHT)
                .padding(Padding.of(1))
                .stroke(new Stroke(theme.accentColor(), RULE_STROKE))
                .anchor(Anchor.topLeft())
                .margin(margin)
                .build();
    }

    private Entity createBulletParagraph(ComponentBuilder cb, List<String> lines, double width, String entityName) {
        BlockTextBuilder builder = cb.blockText(Align.left(2), theme.footerStyle())
                .entityName(entityName)
                .size(width, 2)
                .strategy(BlockIndentStrategy.FROM_SECOND_LINE)
                .bulletOffset("•")
                .anchor(Anchor.topLeft())
                .padding(Padding.zero())
                .text(lines, theme.footerStyle(), Padding.zero(), Margin.zero());
        return builder.build();
    }

    private Entity createText(ComponentBuilder cb, String value, TextStyle style, Anchor anchor, Margin margin) {
        TextBuilder builder = cb.text()
                .textWithAutoSize(Objects.requireNonNullElse(value, ""))
                .textStyle(style)
                .anchor(anchor)
                .margin(margin);
        return builder.build();
    }

    private Entity createAlignedCell(ComponentBuilder cb, Entity child, double width, Anchor anchor) {
        VContainerBuilder cell = cb.vContainer(Align.left(0))
                .size(width, 0)
                .anchor(anchor);
        cell.addChild(child);
        return cell.build();
    }

    private Entity createSpacer(ComponentBuilder cb, double width, double height) {
        return cb.vContainer(Align.left(0))
                .size(width, height)
                .anchor(Anchor.topLeft())
                .build();
    }

    private GridSpec gridSpec(double totalWidth, int dayCount) {
        double nameColumnWidth = Math.min(theme.nameColumnWidth(), Math.max(96, totalWidth * 0.2));
        double dayColumnWidth = (totalWidth - nameColumnWidth) / dayCount;
        TableColumnSpec[] columns = new TableColumnSpec[dayCount + 1];
        columns[0] = TableColumnSpec.fixed(nameColumnWidth);
        for (int i = 0; i < dayCount; i++) {
            columns[i + 1] = TableColumnSpec.fixed(dayColumnWidth);
        }
        return new GridSpec(totalWidth, dayCount, columns);
    }

    private Map<String, ScheduleCategory> categoriesById(List<ScheduleCategory> categories) {
        Map<String, ScheduleCategory> map = new LinkedHashMap<>();
        for (ScheduleCategory category : categories) {
            if (!category.id().isBlank()) {
                map.put(category.id(), category);
            }
        }
        return map;
    }

    private List<SchedulePerson> sortedPeople(List<SchedulePerson> people) {
        return people.stream()
                .sorted(Comparator.comparingInt(SchedulePerson::sortOrder)
                        .thenComparing(SchedulePerson::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<CellKey, ScheduleAssignment> mergeAssignments(List<ScheduleAssignment> assignments) {
        Map<CellKey, ScheduleAssignment> merged = new LinkedHashMap<>();
        for (ScheduleAssignment assignment : assignments) {
            CellKey key = new CellKey(assignment.personId(), assignment.dayId());
            ScheduleAssignment existing = merged.get(key);
            if (existing == null) {
                merged.put(key, assignment);
                continue;
            }

            if (!existing.categoryId().isBlank()
                    && !assignment.categoryId().isBlank()
                    && !existing.categoryId().equals(assignment.categoryId())) {
                throw new IllegalArgumentException("Conflicting categories for person/day cell: "
                        + assignment.personId() + " / " + assignment.dayId());
            }

            List<ScheduleSlot> slots = new ArrayList<>(existing.slots());
            slots.addAll(assignment.slots());
            String note = joinNonBlank(" / ", existing.note(), assignment.note());
            String categoryId = existing.categoryId().isBlank() ? assignment.categoryId() : existing.categoryId();
            merged.put(key, new ScheduleAssignment(existing.personId(), existing.dayId(), categoryId, slots, note));
        }
        return merged;
    }

    private List<String> splitMultiline(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        String[] raw = value.split("\\r?\\n");
        List<String> lines = new ArrayList<>(raw.length);
        for (String line : raw) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return List.copyOf(lines);
    }

    private List<String> sanitizeLines(List<String> lines) {
        List<String> sanitized = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                sanitized.add(line.trim());
            }
        }
        return List.copyOf(sanitized);
    }

    private String joinNonBlank(String delimiter, String first, String second) {
        List<String> values = new ArrayList<>(2);
        if (first != null && !first.isBlank()) {
            values.add(first);
        }
        if (second != null && !second.isBlank()) {
            values.add(second);
        }
        return String.join(delimiter, values);
    }

    private String sanitizeEntityPart(String value) {
        return Objects.requireNonNullElse(value, "Metric").replaceAll("[^A-Za-z0-9]+", "");
    }

    private WeeklyScheduleData safeData(WeeklyScheduleData data) {
        return data == null
                ? new WeeklyScheduleData("Weekly Schedule", "", List.of(), List.of(), List.of(), List.of(), List.of(), List.of())
                : data;
    }

    private void validateData(WeeklyScheduleData data) {
        if (data.days().isEmpty()) {
            throw new IllegalArgumentException("Weekly schedule requires at least one day.");
        }
    }

    private record GridSpec(double totalWidth, int dayCount, TableColumnSpec[] columns) {
    }

    private record CellKey(String personId, String dayId) {
    }
}
