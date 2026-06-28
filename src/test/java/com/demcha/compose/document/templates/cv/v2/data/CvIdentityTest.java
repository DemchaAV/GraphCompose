package com.demcha.compose.document.templates.cv.v2.data;

import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.core.identity.PartyIdentity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the {@link PartyIdentity} mapping that {@link CvIdentity} adds — the
 * neutral header widgets read a CV through this contract.
 */
class CvIdentityTest {

    private static CvIdentity sample(String jobTitle) {
        return new CvIdentity(
                CvName.of("Jane", "Doe"),
                jobTitle,
                new Contact("+44 1234", "jane@demo.dev", "London"),
                List.of(new Link("GitHub", "https://example.dev")));
    }

    @Test
    void isAPartyIdentity() {
        assertThat(sample("Engineer")).isInstanceOf(PartyIdentity.class);
    }

    @Test
    void displayNameIsTheFullName() {
        CvIdentity id = sample("Engineer");
        assertThat(id.displayName()).isEqualTo(id.name().full());
    }

    @Test
    void taglineMirrorsJobTitle() {
        CvIdentity id = sample("Platform Engineer");
        assertThat(id.tagline()).isEqualTo(id.jobTitle()).isEqualTo("Platform Engineer");
    }

    @Test
    void taglineIsBlankWhenJobTitleAbsent() {
        // the back-compat three-arg constructor leaves the title blank
        PartyIdentity id = new CvIdentity(CvName.of("Jane", "Doe"),
                new Contact("+44 1234", "jane@demo.dev", "London"), List.of());
        assertThat(id.tagline()).isEmpty();
    }

    @Test
    void contactAndLinksAreExposedThroughTheContract() {
        PartyIdentity id = sample("Engineer");
        assertThat(id.contact().email()).isEqualTo("jane@demo.dev");
        assertThat(id.links()).hasSize(1);
    }
}
