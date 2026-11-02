package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What tells a divider from a box of a colour, which is nothing in the box itself.
 *
 * <p>A divider and a swatch of the same thickness are the same {@link ShapeNode}: a fill, no
 * stroke, and a height that is the thickness. Only the signal the builder sets says which asked
 * to be a rule, and a backend with a rule of its own has nothing else to read.</p>
 */
class DividerBuilderTest {

    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);

    @Test
    void aDividerCarriesTheSignal() {
        ShapeNode divider = (ShapeNode) new SectionBuilder()
                .addDivider(d -> d.name("Rule").width(200).thickness(2).color(INK))
                .build().children().get(0);

        assertThat(divider.divider()).isTrue();
    }

    @Test
    void aDividerKeepsEverythingElseItWasGiven() {
        // The signal is passed through the canonical constructor, where padding and margin stand
        // next to each other: anything shuffled there would move a divider on the page silently.
        DocumentInsets padding = new DocumentInsets(1, 2, 3, 4);
        DocumentInsets margin = new DocumentInsets(5, 6, 7, 8);
        ShapeNode divider = (ShapeNode) new SectionBuilder()
                .addDivider(d -> d.name("Rule").width(200).thickness(2).color(INK)
                        .padding(padding).margin(margin))
                .build().children().get(0);

        assertThat(divider.name()).isEqualTo("Rule");
        assertThat(divider.width()).isEqualTo(200);
        assertThat(divider.height()).as("thickness is the height").isEqualTo(2);
        assertThat(divider.fillColor().color()).isEqualTo(INK.color());
        assertThat(divider.padding()).isEqualTo(padding);
        assertThat(divider.margin()).isEqualTo(margin);
        assertThat(divider.stroke()).isNull();
        assertThat(divider.cornerRadius()).isEqualTo(DocumentCornerRadius.ZERO);
        assertThat(divider.transform()).isEqualTo(DocumentTransform.NONE);
        assertThat(divider.fillPaint()).isNull();
        assertThat(divider.anchor()).isNull();
    }

    @Test
    void aBoxOfAColourOfTheSameShapeCarriesNoSignal() {
        ShapeNode swatch = new ShapeBuilder().name("Swatch").size(200, 2).fillColor(INK).build();

        assertThat(swatch.divider()).isFalse();
        assertThat(swatch.height()).as("the same box but for the signal").isEqualTo(2);
        assertThat(swatch.stroke()).isNull();
    }

    @Test
    void theConstructorThatStatesNoSignalBuildsNoDivider() {
        // Every caller written before the signal existed, inside this tree and outside it.
        ShapeNode node = new ShapeNode("Plate", 10, 10, INK, null,
                DocumentCornerRadius.ZERO, null, null, DocumentInsets.zero(), DocumentInsets.zero(),
                DocumentTransform.NONE, null, null);

        assertThat(node.divider()).isFalse();
    }
}
