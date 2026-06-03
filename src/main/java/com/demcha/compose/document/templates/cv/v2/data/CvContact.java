package com.demcha.compose.document.templates.cv.v2.data;

import java.util.Objects;

/**
 * Required contact triple — phone, email, address — that every CV in
 * the v2 model carries.
 *
 * <p>All three fields are non-blank by construction so renderers
 * never have to skip-on-empty for the canonical contact row.</p>
 *
 * @param phone   non-blank phone number, formatted by the author
 * @param email   non-blank email (rendered as a clickable mailto link)
 * @param address non-blank location / postal address line
 */
public record CvContact(String phone, String email, String address) {

    /** Validates that every field is non-null and non-blank. */
    public CvContact {
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(address, "address");
        if (phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
    }
}
