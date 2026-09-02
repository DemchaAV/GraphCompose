package com.demcha.compose.document.templates.data.rota;

import java.util.List;
import java.util.Objects;

/**
 * A band of the rota: the people who do one kind of job, under the name of it.
 *
 * @param label the band's name, as the rota prints it
 * @param icon  the mark the band opens with, from the preset's own vocabulary;
 *              blank for a band that opens with none
 * @param staff the people in it, in the order they are listed
 * @since 2.4.0
 */
public record RotaGroup(String label, String icon, List<RotaStaff> staff) {

    /**
     * Normalizes optional fields and freezes the staff list.
     */
    public RotaGroup {
        label = Objects.requireNonNullElse(label, "");
        icon = Objects.requireNonNullElse(icon, "");
        staff = List.copyOf(Objects.requireNonNullElse(staff, List.of()));
    }

    /**
     * A band that opens with no mark.
     *
     * @param label the band's name
     * @param staff the people in it
     */
    public RotaGroup(String label, List<RotaStaff> staff) {
        this(label, "", staff);
    }
}
