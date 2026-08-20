package com.demcha.compose.document.templates.cv.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The runtime-assembled section and its item record: what they require,
 * what they normalise, and what they refuse.
 */
class ModuleSectionTest {

    @Test
    void anItemNeedsOnlyATitle() {
        CvItem item = CvItem.of("Mentor, Rails Girls");

        assertThat(item.title()).isEqualTo("Mentor, Rails Girls");
        assertThat(item.link()).isNull();
        assertThat(item.subtitle()).isEmpty();
        assertThat(item.period()).isEmpty();
        assertThat(item.location()).isEmpty();
        assertThat(item.body()).isEmpty();
        assertThat(item.bodyStyle()).isEqualTo(BodyStyle.PARAGRAPH);
        assertThat(item.url()).isEmpty();
    }

    @Test
    void anItemWithoutATitleIsRejected() {
        assertThatThrownBy(() -> CvItem.of("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(() -> CvItem.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("title");
    }

    @Test
    void theOptionalFieldsNormaliseNullToBlank() {
        // An import layer that has no value for a field passes null rather than
        // inventing a placeholder; every renderer downstream tests isBlank().
        CvItem item = new CvItem("Title", null, null, null, null, null, null);

        assertThat(item.subtitle()).isEmpty();
        assertThat(item.period()).isEmpty();
        assertThat(item.location()).isEmpty();
        assertThat(item.body()).isEmpty();
        assertThat(item.bodyStyle()).isEqualTo(BodyStyle.PARAGRAPH);
    }

    @Test
    void blankAndNullBodyLinesAreDropped() {
        CvItem item = CvItem.of("Role").bullets("Shipped it", "   ", null, "Measured it");

        assertThat(item.body()).containsExactly("Shipped it", "Measured it");
        assertThat(item.bodyStyle()).isEqualTo(BodyStyle.BULLETS);
    }

    @Test
    void theBodyListIsCopiedAndUnmodifiable() {
        List<String> source = new ArrayList<>(List.of("first"));
        CvItem item = CvItem.of("Role").body(source, BodyStyle.PARAGRAPH);
        source.add("added after the fact");

        assertThat(item.body()).containsExactly("first");
        assertThat(item.body().getClass().getName()).doesNotContain("ArrayList");
    }

    @Test
    void theWithStyleMethodsReadInRenderOrder() {
        CvItem item = CvItem.of("Senior Backend Engineer")
                .linkedTo("https://acme.example")
                .at("Acme GmbH")
                .in("Berlin, DE")
                .period("2021 - Present")
                .bullets("Cut p99 latency 40%");

        assertThat(item.title()).isEqualTo("Senior Backend Engineer");
        assertThat(item.url()).isEqualTo("https://acme.example");
        assertThat(item.link().label()).isEqualTo("Senior Backend Engineer");
        assertThat(item.subtitle()).isEqualTo("Acme GmbH");
        assertThat(item.location()).isEqualTo("Berlin, DE");
        assertThat(item.period()).isEqualTo("2021 - Present");
        assertThat(item.body()).containsExactly("Cut p99 latency 40%");
    }

    @Test
    void aBlankLinkTargetLeavesTheTitlePlain() {
        assertThat(CvItem.of("Role").linkedTo("").link()).isNull();
        assertThat(CvItem.of("Role").linkedTo(null).link()).isNull();
    }

    @Test
    void aModuleCarriesItsRoleAndKind() {
        ModuleSection module = ModuleSection.builder("Volunteering",
                        SectionRole.OTHER, CvKind.ENTRIES_DATED)
                .item(CvItem.of("Mentor").period("2019"))
                .build();

        assertThat(module.title()).isEqualTo("Volunteering");
        assertThat(module.role()).isEqualTo(SectionRole.OTHER);
        assertThat(module.kind()).isEqualTo(CvKind.ENTRIES_DATED);
        assertThat(module.items()).hasSize(1);
        assertThat(module).isInstanceOf(CvSection.class);
    }

    @Test
    void aModuleWithoutATitleIsRejected() {
        assertThatThrownBy(() -> ModuleSection.of(" ", SectionRole.OTHER, CvKind.BULLETS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void aModuleRejectsANullRoleOrKind() {
        assertThatThrownBy(() -> ModuleSection.of("Skills", null, CvKind.BULLETS))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("role");
        assertThatThrownBy(() -> ModuleSection.of("Skills", SectionRole.SKILLS, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("kind");
    }

    @Test
    void nullItemsAreDroppedAndTheListIsCopied() {
        List<CvItem> source = new ArrayList<>(
                Arrays.asList(CvItem.of("kept"), null, CvItem.of("also kept")));
        ModuleSection module = new ModuleSection("Interests", SectionRole.OTHER,
                CvKind.BULLETS, source);
        source.clear();

        assertThat(module.items()).extracting(CvItem::title)
                .containsExactly("kept", "also kept");
        // Both halves of "copied": the caller's list cannot reach in (above), and
        // the accessor hands out nothing a caller could reach in through.
        assertThatThrownBy(() -> module.items().add(CvItem.of("smuggled")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aSummaryModuleIsProseUnderTheSummaryRole() {
        ModuleSection module = ModuleSection.summary("Professional Summary",
                "Backend engineer.", "Ten years of it.");

        assertThat(module.role()).isEqualTo(SectionRole.SUMMARY);
        assertThat(module.kind()).isEqualTo(CvKind.PARAGRAPH);
        assertThat(module.items()).singleElement()
                .extracting(CvItem::body, org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .containsExactly("Backend engineer.", "Ten years of it.");
    }

    @Test
    void theBuilderShorthandsBuildTheSameItems() {
        ModuleSection module = ModuleSection.builder("Interests", SectionRole.OTHER,
                        CvKind.BULLETS)
                .item("Chess")
                .item("Cycling", "Long-distance, mostly.")
                .build();

        assertThat(module.items()).extracting(CvItem::title)
                .containsExactly("Chess", "Cycling");
        assertThat(module.items().get(0).body()).isEmpty();
        assertThat(module.items().get(1).body()).containsExactly("Long-distance, mostly.");
        assertThat(module.items().get(1).bodyStyle()).isEqualTo(BodyStyle.PARAGRAPH);
    }
}
