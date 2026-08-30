package com.demcha.compose.document.templates.cv.data;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the compatibility promise made when {@link CvIdentity} grew a
 * portrait: the constructors that predate the component are still there and
 * still mean what they meant, so a caller written against the four-argument
 * form keeps compiling and linking.
 */
class CvIdentityPortraitTest {

    private static final Contact CONTACT =
            new Contact("+44 20 7946 0000", "ada@example.com", "London, UK");

    @Test
    void theConstructorThatPredatesThePortraitLeavesItEmpty() {
        CvIdentity identity = new CvIdentity(
                CvName.of("Ada", "Lovelace"), "Analyst", CONTACT,
                List.of(new Link("site", "https://example.com")));
        assertThat(identity.portrait()).isEmpty();
        assertThat(identity.jobTitle()).isEqualTo("Analyst");
    }

    @Test
    void theConstructorThatPredatesTheJobTitleLeavesBothEmpty() {
        CvIdentity identity = new CvIdentity(
                CvName.of("Ada", "Lovelace"), CONTACT, List.of());
        assertThat(identity.portrait()).isEmpty();
        assertThat(identity.jobTitle()).isEmpty();
    }

    @Test
    void aNullPortraitNormalizesToAbsent() {
        CvIdentity identity = new CvIdentity(
                CvName.of("Ada", "Lovelace"), "Analyst", CONTACT, List.of(), null);
        assertThat(identity.portrait()).isEmpty();
    }

    @Test
    void theBuilderCarriesThePortraitThrough() {
        DocumentImageData image = DocumentImageData.fromBytes(onePixelPng());
        CvIdentity identity = CvIdentity.builder()
                .name("Ada", "Lovelace")
                .contact(CONTACT)
                .portrait(image)
                .build();
        assertThat(identity.portrait()).isEqualTo(Optional.of(image));
    }

    @Test
    void theBuilderTreatsANullPortraitAsNone() {
        CvIdentity identity = CvIdentity.builder()
                .name("Ada", "Lovelace")
                .contact(CONTACT)
                .portrait(null)
                .build();
        assertThat(identity.portrait()).isEmpty();
    }

    /** The smallest valid PNG: one opaque pixel. */
    private static byte[] onePixelPng() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmM"
                        + "IQAAAABJRU5ErkJggg==");
    }
}
