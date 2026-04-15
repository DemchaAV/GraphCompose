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
        target.addTable(scheduleTable(target, safe));

        if (!safe.footerNotes().isEmpty()) {
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

    private TemplateTableSpec scheduleTable(TemplateComposeTarget target, WeeklyScheduleData data) {
        GridSpec grid = gridSpec(target.pageWidth(), data.days().size());
        Map<String, ScheduleCategory> categories = categoriesById(data.categories());
        Map<CellKey, ScheduleAssignment> assignments = mergeAssignments(data.assignments());

        List<List<TableCellSpec>> rows = new ArrayList<>();
        Map<Integer, TableCellStyle> rowStyles = new LinkedHashMap<>();

        rows.add(buildHeaderRow(data.days()));
        rowStyles.put(0, dayLabelCellStyle());

        for (ScheduleMetricRow metric : data.headerMetrics()) {
            rows.add(buildMetricRow(metric, data.days().size()));
            rowStyles.put(rows.size() - 1, metricRowStyle());
        }

        List<SchedulePerson> people = data.people().stream()
                .sorted(Comparator.comparingInt(SchedulePerson::sortOrder)
                        .thenComparing(SchedulePerson::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (SchedulePerson person : people) {
            rows.add(buildPersonRow(person, data.days(), categories, assignments));
        }

        Map<Integer, TableCellStyle> columnStyles = Map.of(0, nameColumnStyle());
        return new TemplateTableSpec(
                "WeeklyScheduleRoster",
                grid.columns(),
                rows,
                rosterDefaultStyle(),
                rowStyles,
                columnStyles,
                grid.totalWidth(),
                Padding.zero(),
                Margin.top(4));
    }

    private List<TableCellSpec> buildHeaderRow(List<ScheduleDay> days) {
        List<TableCellSpec> row = new ArrayList<>();
        row.add(TableCellSpec.text(""));
        for (ScheduleDay day : days) {
            List<String> lines = new ArrayList<>();
            lines.add(day.label());
            if (!day.headerNote().isBlank()) {
                lines.add(day.headerNote());
            }
            row.add(TableCellSpec.of(lines));
        }
        return row;
    }

    private List<TableCellSpec> buildMetricRow(ScheduleMetricRow metric, int dayCount) {
        List<TableCellSpec> row = new ArrayList<>();
        row.add(TableCellSpec.text(metric.label()));
        for (int index = 0; index < dayCount; index++) {
            String value = index < metric.dayValues().size() ? metric.dayValues().get(index) : "";
            row.add(TableCellSpec.text(value));
        }
        return row;
    }

    private List<TableCellSpec> buildPersonRow(SchedulePerson person,
                                               List<ScheduleDay> days,
                                               Map<String, ScheduleCategory> categories,
                                               Map<CellKey, ScheduleAssignment> assignments) {
        List<TableCellSpec> row = new ArrayList<>();
        row.add(TableCellSpec.text(person.displayName()));
        for (ScheduleDay day : days) {
            ScheduleAssignment assignment = assignments.get(new CellKey(person.id(), day.id()));
            row.add(assignmentCell(assignment, categories));
        }
        return row;
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

    private GridSpec gridSpec(double totalWidth, int dayCount) {
        double nameColumnWidth = Math.min(theme.nameColumnWidth(), Math.max(96, totalWidth * 0.2));
        double dayColumnWidth = (totalWidth - nameColumnWidth) / dayCount;
        List<TableColumnSpec> columns = new ArrayList<>();
        columns.add(TableColumnSpec.fixed(nameColumnWidth));
        for (int index = 0; index < dayCount; index++) {
            columns.add(TableColumnSpec.fixed(dayColumnWidth));
        }
        return new GridSpec(totalWidth, columns);
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

    private TableCellStyle dayLabelCellStyle() {
        return TableCellStyle.builder()
                .padding(new Padding(theme.bandPaddingVertical(), theme.bandPaddingHorizontal(), theme.bandPaddingVertical(), theme.bandPaddingHorizontal()))
                .fillColor(theme.bandFillColor())
                .stroke(new Stroke(theme.gridBorderColor(), 1.0))
                .textStyle(theme.dayLabelStyle())
                .textAnchor(Anchor.center())
                .build();
    }

    private TableCellStyle metricRowStyle() {
        return TableCellStyle.builder()
                .fillColor(Color.WHITE)
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

    private record GridSpec(double totalWidth, List<TableColumnSpec> columns) {
    }

    private record CellKey(String personId, String dayId) {
    }
}
