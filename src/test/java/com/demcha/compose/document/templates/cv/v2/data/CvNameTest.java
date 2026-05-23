package com.demcha.compose.document.templates.cv.v2.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvNameTest {

    @Test
    void rejects_blank_first() {
        assertThatThrownBy(() -> new CvName("", "", "Doe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first");
    }

    @Test
    void rejects_blank_last() {
        assertThatThrownBy(() -> new CvName("Jane", "", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last");
    }

    @Test
    void rejects_null_first() {
        assertThatThrownBy(() -> new CvName(null, "", "Doe"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void null_middle_becomes_empty() {
        CvName name = new CvName("Jane", null, "Doe");
        assertThat(name.middle()).isEqualTo("");
        assertThat(name.middleName()).isEmpty();
        assertThat(name.full()).isEqualTo("Jane Doe");
    }

    @Test
    void full_joins_with_middle() {
        CvName name = new CvName("Jane", "Q", "Doe");
        assertThat(name.full()).isEqualTo("Jane Q Doe");
        assertThat(name.middleName()).contains("Q");
    }

    @Test
    void of_factory_builds_two_part_name() {
        CvName name = CvName.of("Jane", "Doe");
        assertThat(name.full()).isEqualTo("Jane Doe");
        assertThat(name.middle()).isEqualTo("");
    }
}
