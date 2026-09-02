package com.demcha.compose.document.templates.data.rota;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One person, and what they are down for on each day of the rota.
 *
 * <p>{@code days} is a list of days, and each day is a list of what that person
 * does on it — which is not the same as one shift each. A day with nothing in it
 * is a legal blank cell rather than a hole in the data, and a day with two
 * entries is a split shift. Modelling a day as a single shift would make the
 * split the exception a preset has to invent a representation for.</p>
 *
 * @param name the person, as the rota prints them
 * @param days one list of entries per day, in the order the rota's days run
 * @since 2.4.0
 */
public record RotaStaff(String name, List<List<RotaShift>> days) {

    /**
     * Normalizes optional fields and freezes the day lists, including the inner
     * ones — a frozen outer list of mutable days is not a frozen rota.
     */
    public RotaStaff {
        name = Objects.requireNonNullElse(name, "");
        List<List<RotaShift>> stated = Objects.requireNonNullElse(days, List.of());
        List<List<RotaShift>> copied = new ArrayList<>(stated.size());
        for (List<RotaShift> day : stated) {
            copied.add(List.copyOf(Objects.requireNonNullElse(day, List.of())));
        }
        days = List.copyOf(copied);
    }

    /**
     * What this person is down for on one day.
     *
     * @param index the day's position in the rota's own day list
     * @return the entries, or nothing when the rota does not reach that day
     */
    public List<RotaShift> day(int index) {
        return index >= 0 && index < days.size() ? days.get(index) : List.of();
    }
}
