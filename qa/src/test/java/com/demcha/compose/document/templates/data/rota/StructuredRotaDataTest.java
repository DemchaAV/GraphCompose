package com.demcha.compose.document.templates.data.rota;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the rota model's two contracts: a partial document composes without null
 * checks in preset code, and nothing a caller keeps a reference to can change
 * the document afterwards.
 */
class StructuredRotaDataTest {

    @Test
    void anEmptyRotaHasEveryComponentRatherThanNulls() {
        // A preset reads six components and drawing guards on each of them is
        // six chances to forget one. The model is what makes that unnecessary.
        StructuredRotaData rota = StructuredRotaData.builder().build();
        assertThat(rota.venue()).isNotNull();
        assertThat(rota.week()).isNotNull();
        assertThat(rota.days()).isEmpty();
        assertThat(rota.legend()).isNotNull();
        assertThat(rota.groups()).isEmpty();
        assertThat(rota.footer()).isNotNull();
        assertThat(rota.venue().isPresent()).isFalse();
        assertThat(rota.week().isPresent()).isFalse();
        assertThat(rota.legend().isPresent()).isFalse();
        assertThat(rota.footer().isPresent()).isFalse();
        assertThat(StructuredRotaDocumentSpec.from(null).rota()).isNotNull();
    }

    @Test
    void everyBuilderSetterReachesTheDocumentItBuilds() {
        // Without this, a setter that dropped its argument passes the suite:
        // the empty-rota test above asserts only the absent half of each block.
        RotaVenue venue = new RotaVenue("HARBOUR", "Quayside", "Harbour Quayside");
        RotaWeek week = new RotaWeek("WEEKLY ROTA", "31 Aug - 6 Sep");
        RotaLegend legend = new RotaLegend("STATUS", "COVERS", "L", "D",
                List.of(new RotaLegend.Entry("OFF", ShiftStatus.OFF)));
        RotaFooter footer = new RotaFooter("Swaps to the duty manager by Thursday.");
        RotaDay day = new RotaDay("MONDAY", "31", "ST", "Pianist 18:30",
                new RotaCovers("104", "50"));
        RotaGroup group = new RotaGroup("BAR", "glass",
                List.of(new RotaStaff("Priya", List.of(List.of(RotaShift.hours("09:00-17:00"))))));

        StructuredRotaData rota = StructuredRotaData.builder()
                .venue(venue).week(week).days(List.of(day))
                .legend(legend).groups(List.of(group)).footer(footer)
                .build();

        assertThat(rota.venue()).isEqualTo(venue);
        assertThat(rota.week()).isEqualTo(week);
        assertThat(rota.days()).containsExactly(day);
        assertThat(rota.legend()).isEqualTo(legend);
        assertThat(rota.groups()).containsExactly(group);
        assertThat(rota.footer()).isEqualTo(footer);
        assertThat(StructuredRotaDocumentSpec.from(rota).rota()).isEqualTo(rota);
    }

    @Test
    void aStatedBlockReportsItselfPresent() {
        // The other half of every isPresent(): all three would pass the empty
        // case if they simply always answered false.
        assertThat(new RotaVenue("HARBOUR", "", "").isPresent()).isTrue();
        assertThat(new RotaVenue("", "Quayside", "").isPresent()).isTrue();
        assertThat(new RotaVenue("", "", "Harbour Quayside").isPresent()).isTrue();
        assertThat(new RotaWeek("WEEKLY ROTA", "").isPresent()).isTrue();
        assertThat(new RotaWeek("", "31 Aug - 6 Sep").isPresent()).isTrue();
        assertThat(new RotaFooter("Swaps to the duty manager.").isPresent()).isTrue();
        assertThat(new RotaLegend("STATUS", List.of()).isPresent()).isTrue();
        assertThat(new RotaLegend("", List.of(
                new RotaLegend.Entry("OFF", ShiftStatus.OFF))).isPresent()).isTrue();
    }

    @Test
    void aDayCarriesItsNoteAndItsCovers() {
        RotaDay day = new RotaDay("MONDAY", "31", "ST", "Pianist 18:30",
                new RotaCovers("104", "50"));
        assertThat(day.note()).isEqualTo("Pianist 18:30");
        assertThat(day.covers().lunch()).isEqualTo("104");
        assertThat(day.covers().dinner()).isEqualTo("50");
    }

    @Test
    void theCoversRowCarriesItsOwnLabels() {
        // The covers row is a label and two marks, and all three are the
        // document's words rather than the preset's.
        RotaLegend legend = new RotaLegend("STATUS LEGEND", "COVERS", "L", "D", List.of());
        assertThat(legend.coversLabel()).isEqualTo("COVERS");
        assertThat(legend.coversLunchLabel()).isEqualTo("L");
        assertThat(legend.coversDinnerLabel()).isEqualTo("D");
    }

    @Test
    void aDayIsAListSoAnEmptyDayAndASplitDayAreBothOrdinary() {
        // The reason days() is a list of lists: a blank cell and a split shift
        // are the two shapes a rota actually has, and neither is an exception.
        RotaStaff staff = new RotaStaff("Priya", List.of(
                List.of(),
                List.of(RotaShift.hours("09:00-18:00")),
                List.of(RotaShift.hours("12:00-16:00"),
                        new RotaShift("16:00-22:00", ShiftStatus.NONE, ShiftEmphasis.SOFT))));
        assertThat(staff.day(0)).isEmpty();
        assertThat(staff.day(1)).hasSize(1);
        assertThat(staff.day(2)).hasSize(2);
    }

    @Test
    void askingForADayTheRotaDoesNotReachIsNotAnError() {
        // A preset walks the rota's own day columns, and a person listed with
        // fewer days than the sheet has is a short row, not a broken document.
        RotaStaff staff = new RotaStaff("Priya", List.of(List.of(RotaShift.hours("09:00-17:00"))));
        assertThat(staff.day(6)).isEmpty();
        assertThat(staff.day(-1)).isEmpty();
    }

    @Test
    void theInnerDayListsAreFrozenAndNotOnlyTheOuterOne() {
        // A frozen outer list of mutable days is not a frozen rota: the cell a
        // preset reads twice has to be the cell the caller handed over.
        List<RotaShift> monday = new ArrayList<>(List.of(RotaShift.hours("09:00-17:00")));
        List<List<RotaShift>> days = new ArrayList<>(List.of(monday));
        RotaStaff staff = new RotaStaff("Priya", days);

        monday.add(RotaShift.strong("OFF", ShiftStatus.OFF));
        days.add(List.of());

        assertThat(staff.days()).hasSize(1);
        assertThat(staff.day(0)).hasSize(1);
        assertThatThrownBy(() -> staff.day(0).add(RotaShift.hours("18:00-23:00")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aShiftSaysWhatItMeansSeparatelyFromWhatItPrints() {
        // Two sites print different words for the same thing; a preset colours
        // by the meaning, so both colour alike.
        RotaShift annualLeave = RotaShift.strong("A/L", ShiftStatus.HOLIDAY);
        RotaShift holiday = RotaShift.strong("HOL", ShiftStatus.HOLIDAY);
        assertThat(annualLeave.status()).isEqualTo(holiday.status());
        assertThat(annualLeave.text()).isNotEqualTo(holiday.text());
        assertThat(annualLeave.emphasis()).isEqualTo(ShiftEmphasis.STRONG);
    }

    @Test
    void anEntryStatesItsOwnEmphasisBecauseNoFactoryCanGuessIt() {
        // A split day is drawn loud then quiet, and sometimes the other way,
        // and sometimes loud twice — so there is no marked(text, status) that
        // picks for the caller. Both halves are named.
        assertThat(RotaShift.strong("09:00-16:00", ShiftStatus.STOCK).emphasis())
                .isEqualTo(ShiftEmphasis.STRONG);
        assertThat(RotaShift.soft("16:00-22:00", ShiftStatus.STOCK).emphasis())
                .isEqualTo(ShiftEmphasis.SOFT);
        assertThat(RotaShift.soft("16:00-22:00", ShiftStatus.STOCK).status())
                .isEqualTo(ShiftStatus.STOCK);
    }

    @Test
    void aShiftThatStatesNothingIsPlainAndUnmarked() {
        RotaShift shift = new RotaShift(null, null, null);
        assertThat(shift.text()).isEmpty();
        assertThat(shift.status()).isEqualTo(ShiftStatus.NONE);
        assertThat(shift.emphasis()).isEqualTo(ShiftEmphasis.PLAIN);
        assertThat(RotaShift.hours("09:00-17:00").emphasis()).isEqualTo(ShiftEmphasis.PLAIN);
    }

    @Test
    void aBandStatesItsOwnMarkOrNone() {
        assertThat(new RotaGroup("MANAGEMENT", List.of()).icon()).isEmpty();
        assertThat(new RotaGroup("BAR", "glass", List.of()).icon()).isEqualTo("glass");
    }

    @Test
    void aDaysDateIsThreeFieldsSoASuffixCanBeSetApart() {
        // A design that raises the suffix cannot split a string that arrived
        // joined, so the day never joins them itself.
        RotaDay day = new RotaDay("MONDAY", "31", "ST");
        assertThat(day.name()).isEqualTo("MONDAY");
        assertThat(day.ordinal()).isEqualTo("31");
        assertThat(day.ordinalSuffix()).isEqualTo("ST");
        assertThat(day.note()).isEmpty();
        assertThat(day.covers().isPresent()).isFalse();
    }

    @Test
    void coversAreTwoCountsBecauseOneStringLosesWhichIsWhich() {
        RotaCovers covers = new RotaCovers("104", "50");
        assertThat(covers.isPresent()).isTrue();
        assertThat(covers.lunch()).isEqualTo("104");
        assertThat(covers.dinner()).isEqualTo("50");
        assertThat(new RotaCovers(null, null).isPresent()).isFalse();
    }

    @Test
    void aLegendEntryPairsThePrintedWordWithTheStatusItStandsFor() {
        RotaLegend legend = new RotaLegend("STATUS", List.of(
                new RotaLegend.Entry("A/L", ShiftStatus.HOLIDAY),
                new RotaLegend.Entry("OFF", ShiftStatus.OFF)));
        assertThat(legend.isPresent()).isTrue();
        assertThat(legend.coversLabel()).isEmpty();
        assertThat(legend.entries()).extracting(RotaLegend.Entry::status)
                .containsExactly(ShiftStatus.HOLIDAY, ShiftStatus.OFF);
        assertThat(new RotaLegend.Entry("?", null).status()).isEqualTo(ShiftStatus.NONE);
    }

    @Test
    void theBuildersListsAreCopiedRatherThanKept() {
        List<RotaDay> days = new ArrayList<>(List.of(new RotaDay("MONDAY", "31", "st")));
        List<RotaGroup> groups = new ArrayList<>(List.of(new RotaGroup("BAR", List.of())));
        StructuredRotaData rota = StructuredRotaData.builder()
                .days(days)
                .groups(groups)
                .build();

        days.add(new RotaDay("TUESDAY", "1", "st"));
        groups.add(new RotaGroup("KITCHEN", List.of()));

        assertThat(rota.days()).hasSize(1);
        assertThat(rota.groups()).hasSize(1);
        assertThatThrownBy(() -> rota.days().add(new RotaDay("TUESDAY", "1", "ST")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rota.groups().add(new RotaGroup("KITCHEN", List.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void staffAndTheirGroupsAreReadInTheOrderTheyArePrinted() {
        StructuredRotaData rota = StructuredRotaData.builder()
                .groups(List.of(
                        new RotaGroup("MANAGEMENT", List.of(new RotaStaff("Priya", List.of()))),
                        new RotaGroup("BAR", "glass", List.of(
                                new RotaStaff("Tomas", List.of()),
                                new RotaStaff("Lena", List.of())))))
                .build();
        assertThat(rota.groups()).extracting(RotaGroup::label)
                .containsExactly("MANAGEMENT", "BAR");
        assertThat(rota.groups().get(1).staff()).extracting(RotaStaff::name)
                .containsExactly("Tomas", "Lena");
    }
}
