package com.demcha.compose.document.templates.cv.presets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one rule {@link ContactUri} exists for: a printed number and the
 * number a device dials are not the same string.
 *
 * <p>Every preset that draws a telephone number now routes through it, so the
 * rule is asserted here once rather than through each of their renders.</p>
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
}
