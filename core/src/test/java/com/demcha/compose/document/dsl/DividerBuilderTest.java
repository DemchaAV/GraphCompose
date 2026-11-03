package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
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
    void aDividerCarriesWhatARuleCanHoldAndDropsWhatWouldStopItBeingOne() {
        // A rule written as Word's own is a bordered paragraph, and a paragraph holds a bookmark,
        // so an anchor reaches the node and has something to point at; its link and its outline
        // entry reach it too. A corner radius, a transform and a gradient paint are each a reason
        // the box would not be a rule at all, so the builder drops them rather than quietly
        // turning its divider into a drawing; it drops a paint whole, a solid one too, and keeps
        // the colour color(...) set.
        ShapeNode divider = (ShapeNode) new SectionBuilder()
                .addDivider(d -> {
                    d.name("Seam").width(200).thickness(2).color(INK);
                    d.anchor("seam");
                    d.linkTo("elsewhere");
                    d.bookmark(new DocumentBookmarkOptions("Seam"));
                    d.cornerRadius(DocumentCornerRadius.of(3));
                    d.fill(DocumentPaint.linear(DocumentColor.WHITE, INK));
                    d.transform(DocumentTransform.rotate(10));
                })
                .build().children().get(0);

        assertThat(divider.divider()).isTrue();
        assertThat(divider.anchor()).isEqualTo("seam");
        assertThat(divider.linkTarget()).isNotNull();
        assertThat(divider.bookmarkOptions()).isEqualTo(new DocumentBookmarkOptions("Seam"));
        assertThat(divider.cornerRadius()).as("dropped: a rounded box is no rule")
                .isEqualTo(DocumentCornerRadius.ZERO);
        assertThat(divider.fillPaint()).as("dropped: a gradient box is no rule").isNull();
        assertThat(divider.fillColor().color()).as("the colour it was given stands")
                .isEqualTo(INK.color());
        assertThat(divider.transform()).as("dropped: a transformed box is no rule")
                .isEqualTo(DocumentTransform.NONE);
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
