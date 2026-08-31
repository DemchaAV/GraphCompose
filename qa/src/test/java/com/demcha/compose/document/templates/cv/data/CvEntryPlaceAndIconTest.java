package com.demcha.compose.document.templates.cv.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the compatibility promise made when {@link CvEntry} grew a location
 * and a mark: the four-argument constructor that predates them is still
 * there and still means what it meant, so a caller written against it keeps
 * compiling and linking.
 */
class CvEntryPlaceAndIconTest {

    @Test
    void theConstructorThatPredatesThemLeavesBothBlank() {
        CvEntry entry = new CvEntry("Engineer", "Acme", "2021", "Did the work.");
        assertThat(entry.place()).isEmpty();
        assertThat(entry.icon()).isEmpty();
        assertThat(entry.subtitle()).isEqualTo("Acme");
    }

    @Test
    void nullsNormalizeToBlank() {
        CvEntry entry = new CvEntry("Engineer", "Acme", "2021", "Body", null, null);
        assertThat(entry.place()).isEmpty();
        assertThat(entry.icon()).isEmpty();
    }

    @Test
    void theOriginalFieldsStillRejectNull() {
        assertThatThrownBy(() -> new CvEntry("Engineer", null, "2021", "Body", "Berlin", "cart"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("subtitle");
    }

    @Test
    void aBlankTitleIsStillRejected() {
        assertThatThrownBy(() -> new CvEntry("  ", "Acme", "2021", "Body", "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void theBuilderCarriesEveryFieldThrough() {
        CvEntry entry = CvEntry.builder("Ledgerkit")
                .subtitle("Java, PostgreSQL")
                .date("2024")
                .body("An open-source ledger.")
                .place("Remote")
                .icon("cart")
                .build();
        assertThat(entry).isEqualTo(new CvEntry("Ledgerkit", "Java, PostgreSQL", "2024",
                "An open-source ledger.", "Remote", "cart"));
    }

    @Test
    void theBuilderLeavesWhatItIsNotToldBlank() {
        CvEntry entry = CvEntry.builder("Ledgerkit").build();
        assertThat(entry.subtitle()).isEmpty();
        assertThat(entry.date()).isEmpty();
        assertThat(entry.body()).isEmpty();
        assertThat(entry.place()).isEmpty();
        assertThat(entry.icon()).isEmpty();
    }

    @Test
    void theBuilderJoinsABodyGivenAsLines() {
        // The presets that draw a bulleted entry read one bullet per line, so
        // the list form saves every caller the same String.join.
        CvEntry entry = CvEntry.builder("Engineer")
                .body(List.of("Shipped a thing.", "Shipped another."))
                .build();
        assertThat(entry.body()).isEqualTo("Shipped a thing.\nShipped another.");
    }

    @Test
    void theBuilderTreatsNullsAsBlank() {
        CvEntry entry = CvEntry.builder("Engineer")
                .subtitle(null)
                .date(null)
                .body((String) null)
                .place(null)
                .icon(null)
                .build();
        assertThat(entry.subtitle()).isEmpty();
        assertThat(entry.body()).isEmpty();
        assertThat(entry.place()).isEmpty();
    }
}
