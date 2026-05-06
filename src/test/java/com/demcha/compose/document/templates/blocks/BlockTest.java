package com.demcha.compose.document.templates.blocks;

import com.demcha.compose.document.node.TextAlign;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockTest {

    // ParagraphBlock --------------------------------------------------

    @Test
    void paragraphDefaultsAlignmentToLeft() {
        ParagraphBlock block = new ParagraphBlock("hello");
        assertThat(block.text()).isEqualTo("hello");
        assertThat(block.align()).isEqualTo(TextAlign.LEFT);
    }

    @Test
    void paragraphRetainsExplicitAlignment() {
        ParagraphBlock block = new ParagraphBlock("hello", TextAlign.CENTER);
        assertThat(block.align()).isEqualTo(TextAlign.CENTER);
    }

    @Test
    void paragraphNormalisesNullAlignmentToLeft() {
        ParagraphBlock block = new ParagraphBlock("hello", null);
        assertThat(block.align()).isEqualTo(TextAlign.LEFT);
    }

    @Test
    void paragraphRejectsNullText() {
        assertThatThrownBy(() -> new ParagraphBlock(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("text");
    }

    @Test
    void paragraphImplementsBlock() {
        Block block = new ParagraphBlock("x");
        assertThat(block).isInstanceOf(ParagraphBlock.class);
    }

    // BulletListBlock -------------------------------------------------

    @Test
    void bulletListPreservesOrder() {
        BulletListBlock block = new BulletListBlock(List.of("a", "b", "c"));
        assertThat(block.items()).containsExactly("a", "b", "c");
    }

    @Test
    void bulletListAcceptsEmpty() {
        BulletListBlock block = new BulletListBlock(List.of());
        assertThat(block.items()).isEmpty();
    }

    @Test
    void bulletListIsImmutable() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        BulletListBlock block = new BulletListBlock(source);
        source.add("c");
        assertThat(block.items()).containsExactly("a", "b");
        assertThatThrownBy(() -> block.items().add("d"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void bulletListRejectsNullList() {
        assertThatThrownBy(() -> new BulletListBlock(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("items");
    }

    @Test
    void bulletListRejectsNullItem() {
        List<String> withNull = new ArrayList<>();
        withNull.add("a");
        withNull.add(null);
        assertThatThrownBy(() -> new BulletListBlock(withNull))
                .isInstanceOf(NullPointerException.class);
    }

    // NumberedListBlock -----------------------------------------------

    @Test
    void numberedListPreservesOrder() {
        NumberedListBlock block = new NumberedListBlock(List.of("first", "second"));
        assertThat(block.items()).containsExactly("first", "second");
    }

    @Test
    void numberedListIsImmutable() {
        List<String> source = new ArrayList<>(List.of("a"));
        NumberedListBlock block = new NumberedListBlock(source);
        source.add("b");
        assertThat(block.items()).containsExactly("a");
    }

    @Test
    void numberedListRejectsNullList() {
        assertThatThrownBy(() -> new NumberedListBlock(null))
                .isInstanceOf(NullPointerException.class);
    }

    // MultiParagraphBlock ---------------------------------------------

    @Test
    void multiParagraphPreservesOrder() {
        MultiParagraphBlock block = new MultiParagraphBlock(List.of("p1", "p2", "p3"));
        assertThat(block.paragraphs()).containsExactly("p1", "p2", "p3");
    }

    @Test
    void multiParagraphIsImmutable() {
        List<String> source = new ArrayList<>(List.of("p1"));
        MultiParagraphBlock block = new MultiParagraphBlock(source);
        source.add("p2");
        assertThat(block.paragraphs()).containsExactly("p1");
    }

    @Test
    void multiParagraphRejectsNullList() {
        assertThatThrownBy(() -> new MultiParagraphBlock(null))
                .isInstanceOf(NullPointerException.class);
    }

    // IndentedBlock ---------------------------------------------------

    @Test
    void indentedItemPreservesFields() {
        IndentedBlock.Item item = new IndentedBlock.Item("MSc CS", "University of Manchester | 2021");
        assertThat(item.title()).isEqualTo("MSc CS");
        assertThat(item.body()).isEqualTo("University of Manchester | 2021");
    }

    @Test
    void indentedItemRejectsNullTitle() {
        assertThatThrownBy(() -> new IndentedBlock.Item(null, "body"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("title");
    }

    @Test
    void indentedItemRejectsNullBody() {
        assertThatThrownBy(() -> new IndentedBlock.Item("title", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("body");
    }

    @Test
    void indentedBlockIsImmutable() {
        List<IndentedBlock.Item> source = new ArrayList<>();
        source.add(new IndentedBlock.Item("a", "b"));
        IndentedBlock block = new IndentedBlock(source);
        source.add(new IndentedBlock.Item("c", "d"));
        assertThat(block.items()).hasSize(1);
    }

    @Test
    void indentedBlockRejectsNullList() {
        assertThatThrownBy(() -> new IndentedBlock(null))
                .isInstanceOf(NullPointerException.class);
    }

    // KeyValueBlock ---------------------------------------------------

    @Test
    void keyValueEntryPreservesFields() {
        KeyValueBlock.Entry entry = new KeyValueBlock.Entry("Languages", "English (Fluent)");
        assertThat(entry.key()).isEqualTo("Languages");
        assertThat(entry.value()).isEqualTo("English (Fluent)");
    }

    @Test
    void keyValueEntryRejectsNullKey() {
        assertThatThrownBy(() -> new KeyValueBlock.Entry(null, "v"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key");
    }

    @Test
    void keyValueEntryRejectsNullValue() {
        assertThatThrownBy(() -> new KeyValueBlock.Entry("k", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
    }

    @Test
    void keyValueFromMapPreservesInsertionOrder() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("Languages", "English (Fluent)");
        source.put("Eligibility", "Eligible to work in the UK");
        KeyValueBlock block = KeyValueBlock.fromMap(source);
        assertThat(block.entries())
                .extracting(KeyValueBlock.Entry::key)
                .containsExactly("Languages", "Eligibility");
    }

    @Test
    void keyValueFromMapRejectsNullMap() {
        assertThatThrownBy(() -> KeyValueBlock.fromMap(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void keyValueBlockIsImmutable() {
        List<KeyValueBlock.Entry> source = new ArrayList<>();
        source.add(new KeyValueBlock.Entry("a", "b"));
        KeyValueBlock block = new KeyValueBlock(source);
        source.add(new KeyValueBlock.Entry("c", "d"));
        assertThat(block.entries()).hasSize(1);
    }

    // Sealing ---------------------------------------------------------

    @Test
    void blockSealingPermitsAllSixVariants() {
        // The sealed permit list of Block must include all six concrete
        // block kinds. This is essentially a compile-time guarantee, but
        // the test fails fast if the permit list ever drifts from the
        // record set in this package.
        Class<?>[] permitted = Block.class.getPermittedSubclasses();
        assertThat(permitted)
                .containsExactlyInAnyOrder(
                        ParagraphBlock.class,
                        BulletListBlock.class,
                        NumberedListBlock.class,
                        IndentedBlock.class,
                        KeyValueBlock.class,
                        MultiParagraphBlock.class);
    }
}
