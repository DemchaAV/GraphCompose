package com.demcha.compose.document.templates.core.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactTest {

    @Test
    void rejects_blank_phone() {
        assertThatThrownBy(() -> new Contact("", "j@d.com", "London"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void rejects_blank_email() {
        assertThatThrownBy(() -> new Contact("+44 0", "  ", "London"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rejects_blank_address() {
        assertThatThrownBy(() -> new Contact("+44 0", "j@d.com", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("address");
    }

    @Test
    void rejects_null_field() {
        assertThatThrownBy(() -> new Contact(null, "j@d.com", "London"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void happy_path_keeps_fields() {
        Contact c = new Contact("+44 1234", "j@d.com", "London, UK");
        assertThat(c.phone()).isEqualTo("+44 1234");
        assertThat(c.email()).isEqualTo("j@d.com");
        assertThat(c.address()).isEqualTo("London, UK");
    }
}
