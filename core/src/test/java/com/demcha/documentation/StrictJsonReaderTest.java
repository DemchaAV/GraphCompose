package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrictJsonReaderTest {

    @Test
    void readsEveryKindOfValue() {
        Object document = StrictJsonReader.read(
                " {\"id\": \"a\\u0041\\n\", \"size\": -1.5e2, \"badge\": true, \"deck\": null,"
                        + " \"tags\": [\"x\", []], \"meta\": {}}\r\n");

        assertThat(document).isInstanceOf(Map.class);
        Map<?, ?> card = (Map<?, ?>) document;
        assertThat(card.get("id")).isEqualTo("aA\n");
        assertThat(card.get("size")).isEqualTo(-150.0);
        assertThat(card.get("badge")).isEqualTo(true);
        assertThat(card.containsKey("deck")).isTrue();
        assertThat(card.get("deck")).isNull();
        assertThat(card.get("tags")).isEqualTo(List.of("x", List.of()));
        assertThat(card.get("meta")).isEqualTo(Map.of());
    }

    @Test
    void refusesWhatABrowserRefuses() {
        List<String> invalid = List.of(
                "{\"id\": \"a\",}",
                "{\"id\": \"a\"} {",
                "{\"id\": \"a\fb\"}",
                "[+1]",
                "[01]",
                "[.5]",
                "[1.]",
                (char) 0x0B + "[]",
                "{\"id\": \"\\u00G1\"}",
                "{\"id\": \"a");

        for (String document : invalid) {
            assertThatThrownBy(() -> StrictJsonReader.read(document))
                    .as("%s", document)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not valid JSON");
        }
    }
}
