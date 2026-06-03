package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;
import com.demcha.compose.document.templates.widgets.TableWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders CV skill groups as a flat table/grid without exposing the
 * preset to table plumbing or category parsing.
 */
public final class SkillTableRenderer {

    private SkillTableRenderer() {
    }

    /**
     * Renders all skills across the groups as a flat grid, optionally
     * prefixing each cell with a bullet.
     *
     * @param host         host section receiving the grid
     * @param groups       the skill groups whose skills are flattened
     * @param width        available grid width in points
     * @param style        visual table options
     * @param bulletPrefix text prepended to each skill cell; null means none
     */
    public static void grid(SectionBuilder host,
                            List<SkillGroup> groups,
                            double width,
                            TableWidget.Style style,
                            String bulletPrefix) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        TableWidget.grid(host, flatten(groups, bulletPrefix), width, style);
    }

    private static List<String> flatten(List<SkillGroup> groups,
                                        String bulletPrefix) {
        String prefix = bulletPrefix == null ? "" : bulletPrefix;
        List<String> out = new ArrayList<>();
        for (SkillGroup group : groups) {
            for (String skill : group.skills()) {
                if (!skill.isBlank()) {
                    out.add(prefix + skill);
                }
            }
        }
        return out;
    }
}
