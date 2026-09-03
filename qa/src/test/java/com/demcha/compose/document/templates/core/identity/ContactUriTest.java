package com.demcha.compose.document.templates.core.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rules {@link ContactUri} exists for, starting with the one it was
 * written for: a printed number and the number a device dials are not the same
 * string.
 *
 * <p>Every preset in every family that draws a contact routes through it, so
 * the rules are asserted here once rather than through each of their renders.</p>
 */
class ContactUriTest {

    @Test
    void aTrunkPrefixIsDroppedBecauseAnInternationalCallerOmitsIt() {
        assertThat(ContactUri.tel("+44 (0)20 7946 0832")).isEqualTo("tel:+442079460832");
        assertThat(ContactUri.tel("+44 (0) 20 3966 1900")).isEqualTo("tel:+442039661900");
    }

    @Test
    void aParenthesisedAreaCodeIsNotATrunkPrefixAndStays() {
        // Only an all-zero group goes. A US area code in brackets is part of
        // the number, and dropping it would dial somewhere else entirely.
        assertThat(ContactUri.tel("+1 (415) 555 7842")).isEqualTo("tel:+14155557842");
    }

    @Test
    void aNumberWithoutAPlusKeepsItsLocalForm() {
        assertThat(ContactUri.tel("020 7946 0832")).isEqualTo("tel:02079460832");
    }

    @Test
    void separatorsAndSpacingDoNotReachTheTarget() {
        assertThat(ContactUri.tel("+61 402-938-209")).isEqualTo("tel:+61402938209");
    }

    @Test
    void textWithNoDigitsIsNotDialable() {
        assertThat(ContactUri.tel("ask reception")).isNull();
        assertThat(ContactUri.tel("")).isNull();
        assertThat(ContactUri.tel("   ")).isNull();
        assertThat(ContactUri.tel(null)).isNull();
    }

    @Test
    void theLinkFormMirrorsTheTargetForm() {
        assertThat(ContactUri.telLink("+44 (0)20 7946 0832")).isNotNull();
        assertThat(ContactUri.telLink("ask reception")).isNull();
        assertThat(ContactUri.telLink(null)).isNull();
    }

    @Test
    void anAddressIsMailedAndASiteIsOpened() {
        assertThat(ContactUri.mailLink("  billing@example.com ").uri())
                .isEqualTo("mailto:billing@example.com");
        assertThat(ContactUri.webLink("example.com/business").uri())
                .isEqualTo("https://example.com/business");
        assertThat(ContactUri.webLink("http://example.com").uri())
                .isEqualTo("http://example.com");
        assertThat(ContactUri.mailLink(" ")).isNull();
        assertThat(ContactUri.webLink(null)).isNull();
    }

    @Test
    void aChannelIsReadFromTheShapeOfWhatItPrints() {
        // A foot lists its channels without saying which is which, because a
        // reader can see it. This reads the same thing back.
        assertThat(ContactUri.channelLink("business@example.com").uri())
                .isEqualTo("mailto:business@example.com");
        assertThat(ContactUri.channelLink("+44 20 3322 8352").uri())
                .isEqualTo("tel:+442033228352");
        assertThat(ContactUri.channelLink("example.com/business").uri())
                .isEqualTo("https://example.com/business");
        assertThat(ContactUri.channelLink("  ")).isNull();
    }

    @Test
    void aSiteCarryingDigitsIsStillASite() {
        // The dialable test asks for nine digits AND a mostly-numeric string,
        // so a domain with a year in it does not become a phone number.
        assertThat(ContactUri.channelLink("example2026.com").uri())
                .isEqualTo("https://example2026.com");
    }

    @Test
    void proseWhereAChannelWasExpectedLosesItsLinkRatherThanThePage() {
        // A notice, a name, a line of prose can all reach a channel field.
        // DocumentLinkOptions refuses a string that is not a URI, and a contact
        // line is not the place to find that out.
        assertThat(ContactUri.channelLink("Confidential — for the addressee only")).isNull();
        assertThat(ContactUri.webLink("ask at the bar")).isNull();
        assertThat(ContactUri.mailLink("Jane Doe <jane@example.com>")).isNull();
    }
}
