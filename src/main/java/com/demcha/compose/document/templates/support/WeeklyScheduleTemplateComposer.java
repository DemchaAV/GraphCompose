package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.ScheduleAssignment;
import com.demcha.compose.document.templates.data.ScheduleCategory;
import com.demcha.compose.document.templates.data.ScheduleDay;
import com.demcha.compose.document.templates.data.ScheduleMetricRow;
import com.demcha.compose.document.templates.data.SchedulePerson;
import com.demcha.compose.document.templates.data.ScheduleSlot;
import com.demcha.compose.document.templates.data.WeeklyScheduleData;
import com.demcha.compose.document.templates.theme.WeeklyScheduleTheme;
import com.demcha.compose.layout_core.components.components_builders.TableCellSpec;
import com.demcha.compose.layout_core.components.components_builders.TableCellStyle;
import com.demcha.compose.layout_core.components.components_builders.TableColumnSpec;
import com.demcha.compose.layout_core.components.content.shape.Stroke;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared scene composer for the weekly schedule template.
 */
public final class WeeklyScheduleTemplateComposer {
    private final WeeklyScheduleTheme theme;

    public WeeklyScheduleTemplateComposer(WeeklyScheduleTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(TemplateComposeTarget target, WeeklyScheduleData data) {
        WeeklyScheduleData safe = Objects.requireNonNull(data, "data");
        if (safe.days().isEmpty()) {
            throw new IllegalArgumentException("Weekly schedule requires at least one day.");
        }

        GridSpec grid = gridSpec(target.pageWidth(), safe.days().size());
        Map<String, ScheduleCategory> categories = categoriesById(safe.categories());

        target.startDocument("WeeklyScheduleRoot", theme.rootSpacing());
        target.addParagraph(TemplateSceneSupport.paragraph(
                "WeeklyScheduleTitle",
                safe.title(),
                theme.titleStyle(),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.zero()));
        if (!safe.weekLabel().isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "WeeklyScheduleWeekLabel",
                    safe.weekLabel(),
                    theme.weekLabelStyle(),
                    TextAlign.RIGHT,
                    1.0,
                    Padding.zero(),
                    Margin.zero()));
        }
        target.addDivider(TemplateSceneSupport.divider(
                "WeeklyScheduleRule",
                target.pageWidth(),
                1.3,
                theme.accentColor(),
                Margin.zero()));
        target.addTable(dayLabelsBand(grid, safe.days()));

        TemplateTableSpec notesBand = dayNotesBand(grid, safe.days());
        if (notesBand != null) {
            target.addTable(notesBand);
        }

        TemplateTableSpec categoryBand = dayCategoryBand(grid, safe.days(), categories);
        if (categoryBand != null) {
            target.addTable(categoryBand);
        }

        for (ScheduleMetricRow metric : safe.headerMetrics()) {
            target.addTable(metricBand(grid, metric));
        }

        target.addTable(rosterTable(grid, safe, categories));

        if (!safe.footerNotes().isEmpty()) {
            target.addDivider(TemplateSceneSupport.divider(
                    "WeeklyScheduleFooterRule",
                    target.pageWidth(),
                    1.0,
                    theme.accentColor(),
                    Margin.top(2)));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "WeeklyScheduleFooterLabel",
                    "Notes",
                    theme.metricStyle(),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(2)));
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "WeeklyScheduleFooter",
                    TemplateSceneSupport.bulletText(safe.footerNotes()),
                    theme.footerStyle(),
                    TextAlign.LEFT,
                    2.0,
                    Padding.zero(),
                    Margin.top(4)));
        }
        target.finishDocument();
    }

    private TemplateTableSpec dayLabelsBand(GridSpec grid, List<ScheduleDay> days) {
        List<TableCellSpec> cells = new ArrayList<>();
        for (ScheduleDay day : days) {
            List<String> labelLines = splitMultiline(day.label());
            cells.add(TableCellSpec.of(labelLines.isEmpty() ? List.of(day.label()) : labelLines));
        }
        return bandTable("WeeklyScheduleDayLabels", grid, bandLeadingStyle(), dayLabelCellStyle(), "", cells);
    }

    private TemplateTableSpec dayNotesBand(GridSpec grid, List<ScheduleDay> days) {
        boolean hasNotes = days.stream().anyMatch(day -> !day.headerNote().isBlank());
        if (!hasNotes) {
            return null;
        }
        List<TableCellSpec> cells = new ArrayList<>();
        for (ScheduleDay day : days) {
            cells.add(TableCellSpec.of(splitMultiline(day.headerNote())));
        }
        return bandTable("WeeklyScheduleDayNotes", grid, bandLeadingStyle(), dayNoteCellStyle(), "", cells);
    }

    private TemplateTableSpec dayCategoryBand(GridSpec grid,
                                              List<ScheduleDay> days,
                                              Map<String, ScheduleCategory> categories) {
        boolean hasCategories = days.stream().anyMatch(day -> !day.headerCategoryId().isBlank());
        if (!hasCategories) {
            return null;
        }
        List<TableCellSpec> cells = new ArrayList<>();
        for (ScheduleDay day : days) {
            ScheduleCategory category = categories.get(day.headerCategoryId());
            if (category == null) {
                cells.add(TableCellSpec.text(""));
                continue;
            }
            cells.add(TableCellSpec.text(category.label()).withStyle(categoryBandCellStyle(category)));
        }
        return bandTable("WeeklyScheduleDayCategories", grid, bandLeadingStyle(), dayCategoryDefaultStyle(), "", cells);
    }

    private TemplateTableSpec metricBand(GridSpec grid, ScheduleMetricRow metric) {
        List<TableCellSpec> cells = new ArrayList<>();
        for (int index = 0; index < grid.dayCount(); index++) {
            String value = index < metric.dayValues().size() ? metric.dayValues().get(index) : "";
            cells.add(TableCellSpec.text(value));
        }
        return bandTable(
                "WeeklyScheduleMetric" + sanitizeEntityPart(metric.label()),
                grid,
                metricLeadingStyle(),
                metricCellStyle(),
                metric.label(),
                cells);
    }

    private TemplateTableSpec rosterTable(GridSpec grid,
                                          WeeklyScheduleData data,
                                          Map<String, ScheduleCategory> categories) {
        Map<CellKey, ScheduleAssignment> assignments = mergeAssignments(data.assignments());
        List<List<TableCellSpec>> rows = new ArrayList<>();

        List<SchedulePerson> people = data.people().stream()
                .sorted(Comparator.comparingInt(SchedulePerson::sortOrder)
                        .thenComparing(SchedulePerson::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (SchedulePerson person : people) {
            List<TableCellSpec> dayCells = new ArrayList<>();
            for (ScheduleDay day : data.days()) {
                ScheduleAssignment assignment = assignments.get(new CellKey(person.id(), day.id()));
                dayCells.add(assignmentCell(assignment, categories));
            }
            rows.add(buildGridRow(person.displayName(), dayCells, grid.dayCount()));
        }

        Map<Integer, TableCellStyle> columnStyles = Map.of(0, nameColumnStyle());
        return new TemplateTableSpec(
                "WeeklyScheduleRoster",
                grid.columns(),
                rows,
                rosterDefaultStyle(),
                Map.of(),
                columnStyles,
                grid.totalWidth(),
                Padding.zero(),
                Margin.top(4));
    }

    private TableCellSpec assignmentCell(ScheduleAssignment assignment, Map<String, ScheduleCategory> categories) {
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
        if (!assignment.note().isBlank()) {
            lines.add(assignment.note());
        }

        ScheduleCategory category = categories.get(assignment.categoryId());
        if (lines.isEmpty() && category != null && !category.label().isBlank()) {
            lines.add(category.label());
        }
        return TableCellSpec.of(lines.isEmpty() ? List.of("") : lines,
                category == null ? emptyRosterCellStyle() : categoryCellStyle(category));
    }

    private TemplateTableSpec bandTable(String name,
                                        GridSpec grid,
                                        TableCellStyle leadingStyle,
                                        TableCellStyle defaultStyle,
                                        String leadingLabel,
                                        List<TableCellSpec> dayCells) {
        return new TemplateTableSpec(
                name,
                grid.columns(),
                List.of(buildGridRow(leadingLabel, dayCells, grid.dayCount())),
                defaultStyle,
                Map.of(),
                Map.of(0, leadingStyle),
                grid.totalWidth(),
                Padding.zero(),
                Margin.zero());
    }

    private List<TableCellSpec> buildGridRow(String leadingLabel, List<TableCellSpec> dayCells, int dayCount) {
        List<TableCellSpec> row = new ArrayList<>(dayCount + 1);
        row.add(TableCellSpec.text(leadingLabel));
        for (int index = 0; index < dayCount; index++) {
            row.add(index < dayCells.size() ? dayCells.get(index) : TableCellSpec.text(""));
        }
        return row;
    }

    private GridSpec gridSpec(double totalWidth, int dayCount) {
        double nameColumnWidth = Math.min(theme.nameColumnWidth(), Math.max(96, totalWidth * 0.2));
        double dayColumnWidth = (totalWidth - nameColumnWidth) / dayCount;
        List<TableColumnSpec> columns = new ArrayList<>();
        columns.add(TableColumnSpec.fixed(nameColumnWidth));
        for (int index = 0; index < dayCount; index++) {
            columns.add(TableColumnSpec.fixed(dayColumnWidth));
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

    private Map<CellKey, ScheduleAssignment> mergeAssignments(List<ScheduleAssignment> assignments) {
        Map<CellKey, ScheduleAssignment> merged = new LinkedHashMap<>();
        for (ScheduleAssignment assignment : assignments) {
            CellKey key = new CellKey(assignment.personId(), assignment.dayId());
            ScheduleAssignment existing = merged.get(key);
            if (existing == null) {
                merged.put(key, assignment);
                continue;
            }
            List<ScheduleSlot> slots = new ArrayList<>(existing.slots());
            slots.addAll(assignment.slots());
            String note = TemplateSceneSupport.joinNonBlank(" / ", existing.note(), assignment.note());
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

    private String sanitizeEntityPart(String value) {
        return Objects.requireNonNullElse(value, "Metric").replaceAll("[^A-Za-z0-9]+", "");
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

    private TableCellStyle categoryBandCellStyle(ScheduleCategory category) {
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

    private TableCellStyle nameColumnStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bodyPaddingVertical(), theme.bodyPaddingHorizontal(), theme.bodyPaddingVertical(), theme.bodyPaddingHorizontal()))
                .fillColor(theme.nameColumnFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.personNameStyle())
                .textAnchor(Anchor.centerLeft())
                .build();
    }

    private TableCellStyle rosterDefaultStyle() {
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

    private TableCellStyle categoryCellStyle(ScheduleCategory category) {
        return TableCellStyle.builder()
                .fillColor(category.fillColor())
                .stroke(new Stroke(category.borderColor(), 1.0))
                .textStyle(theme.cellTextStyle(category.textColor()))
                .textAnchor(Anchor.topLeft())
                .build();
    }

    private record GridSpec(double totalWidth, int dayCount, List<TableColumnSpec> columns) {
    }

    private record CellKey(String personId, String dayId) {
    }
}
