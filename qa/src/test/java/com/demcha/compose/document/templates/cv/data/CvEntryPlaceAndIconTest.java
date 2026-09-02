package com.demcha.compose.document.templates.cv.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the compatibility promise made each time {@link CvEntry} grew: the
 * constructors that predate the location, the mark and the link are still
 * there and still mean what they meant, so a caller written against any of
 * them keeps compiling and linking.
 */
class CvEntryPlaceAndIconTest {

    @Test
    void theConstructorThatPredatesThemLeavesThemAllBlank() {
        CvEntry entry = new CvEntry("Engineer", "Acme", "2021", "Did the work.");
        assertThat(entry.place()).isEmpty();
        assertThat(entry.icon()).isEmpty();
        assertThat(entry.link()).isEmpty();
        assertThat(entry.subtitle()).isEqualTo("Acme");
    }

    @Test
    void theConstructorThatPredatesTheLinkLeavesItBlank() {
        CvEntry entry = new CvEntry("Engineer", "Acme", "2021", "Body", "Berlin", "cart");
        assertThat(entry.link()).isEmpty();
        assertThat(entry.place()).isEqualTo("Berlin");
        assertThat(entry.icon()).isEqualTo("cart");
    }

    @Test
    void nullsNormalizeToBlank() {
        CvEntry entry = new CvEntry("Engineer", "Acme", "2021", "Body", null, null, null);
        assertThat(entry.place()).isEmpty();
        assertThat(entry.icon()).isEmpty();
        assertThat(entry.link()).isEmpty();
    }

    @Test
    void theOriginalFieldsStillRejectNull() {
        assertThatThrownBy(() ->
                new CvEntry("Engineer", null, "2021", "Body", "Berlin", "cart", ""))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("subtitle");
    }

    @Test
    void aBlankTitleIsStillRejected() {
        assertThatThrownBy(() -> new CvEntry("  ", "Acme", "2021", "Body", "", "", ""))
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
                .link("https://example.com/ledgerkit")
                .build();
        assertThat(entry).isEqualTo(new CvEntry("Ledgerkit", "Java, PostgreSQL", "2024",
                "An open-source ledger.", "Remote", "cart",
                "https://example.com/ledgerkit"));
    }

    @Test
    void theBuilderLeavesWhatItIsNotToldBlank() {
        CvEntry entry = CvEntry.builder("Ledgerkit").build();
        assertThat(entry.subtitle()).isEmpty();
        assertThat(entry.date()).isEmpty();
        assertThat(entry.body()).isEmpty();
        assertThat(entry.place()).isEmpty();
        assertThat(entry.icon()).isEmpty();
        assertThat(entry.link()).isEmpty();
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
                .link(null)
                .build();
        assertThat(entry.subtitle()).isEmpty();
        assertThat(entry.body()).isEmpty();
        assertThat(entry.place()).isEmpty();
        assertThat(entry.link()).isEmpty();
    }
}
