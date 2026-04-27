package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SectionPresetTest {

    private static final double EPS = 1e-6;
    private static final DocumentColor ACCENT = DocumentColor.of(new Color(40, 90, 180));
    private static final DocumentColor PANEL = DocumentColor.of(new Color(245, 247, 250));

    @Test
    void bandSetsFillColorOnly() {
        SectionNode section = new SectionBuilder()
                .name("Band")
                .band(ACCENT)
                .addParagraph("Hello")
                .build();

        assertThat(section.fillColor()).isEqualTo(ACCENT);
        assertThat(section.cornerRadius().radius()).isEqualTo(0.0, within(EPS));
        assertThat(section.borders()).isEqualTo(DocumentBorders.NONE);
    }

    @Test
    void softPanelSetsFillRadiusAndPadding() {
        SectionNode section = new SectionBuilder()
                .name("Panel")
                .softPanel(PANEL, 12.0, 16.0)
                .addParagraph("Inside")
                .build();

        assertThat(section.fillColor()).isEqualTo(PANEL);
        assertThat(section.cornerRadius().radius()).isEqualTo(12.0, within(EPS));
        assertThat(section.padding().top()).isEqualTo(16.0, within(EPS));
        assertThat(section.padding().right()).isEqualTo(16.0, within(EPS));
        assertThat(section.padding().bottom()).isEqualTo(16.0, within(EPS));
        assertThat(section.padding().left()).isEqualTo(16.0, within(EPS));
    }

    @Test
    void softPanelDefaultUsesEightPointRadiusAndTwelvePointPadding() {
        SectionNode section = new SectionBuilder()
                .name("DefaultPanel")
                .softPanel(PANEL)
                .addParagraph("Inside")
                .build();

        assertThat(section.fillColor()).isEqualTo(PANEL);
        assertThat(section.cornerRadius().radius()).isEqualTo(8.0, within(EPS));
        assertThat(section.padding().top()).isEqualTo(12.0, within(EPS));
    }

    @Test
    void accentLeftSetsLeftBorderStrokeOnly() {
        SectionNode section = new SectionBuilder()
                .name("Quote")
                .accentLeft(ACCENT, 4.0)
                .addParagraph("Quoted")
                .build();

        DocumentBorders borders = section.borders();
        assertThat(borders.left()).isNotNull();
        assertThat(borders.left().color()).isEqualTo(ACCENT);
        assertThat(borders.left().width()).isEqualTo(4.0, within(EPS));
        assertThat(borders.right()).isNull();
        assertThat(borders.top()).isNull();
        assertThat(borders.bottom()).isNull();
    }

    @Test
    void accentBottomSetsBottomBorderStrokeOnly() {
        SectionNode section = new SectionBuilder()
                .name("Header")
                .accentBottom(ACCENT, 2.0)
                .addParagraph("Title")
                .build();

        DocumentBorders borders = section.borders();
        assertThat(borders.bottom()).isNotNull();
        assertThat(borders.bottom().width()).isEqualTo(2.0, within(EPS));
        assertThat(borders.top()).isNull();
        assertThat(borders.left()).isNull();
        assertThat(borders.right()).isNull();
    }

    @Test
    void accentTopAndAccentRightAreSymmetric() {
        SectionNode top = new SectionBuilder().name("Top")
                .accentTop(ACCENT, 3.0).addParagraph("x").build();
        SectionNode right = new SectionBuilder().name("Right")
                .accentRight(ACCENT, 3.0).addParagraph("x").build();

        assertThat(top.borders().top()).isNotNull();
        assertThat(top.borders().right()).isNull();
        assertThat(right.borders().right()).isNotNull();
        assertThat(right.borders().top()).isNull();
    }
}
