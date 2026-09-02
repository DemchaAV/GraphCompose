package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.templates.data.proposal.ProposalGlance;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.IndigoFlow.layeredRow;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ABOUT_BODY_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ABOUT_BODY_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ABOUT_BODY_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ABOUT_HEADING_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ABOUT_MEASURE_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BAND_ARTWORK_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BAND_H_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BAND_LEFT_W;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BAND_TOP_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_GLYPH;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_LINE1_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_LINE2_CAP;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_PITCH_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_RADIUS;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_TILE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_TILE_AT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FEATURE_TILE_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.LABEL_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.MARGIN_L;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.MARGIN_R;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TILE_DARK;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.TINT;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.bold;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.boxBottomPx;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.plain;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.px;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.toPx;

/**
 * The tinted band across the middle of the sheet: a paragraph about the issuer,
 * a row of marked tiles under it, and the place the design fills with artwork.
 *
 * <h2>What this port leaves out</h2>
 *
 * <p>The design fills the band's right with a product photograph carrying its
 * own brand's mark. That is a brand asset, and the templates artifact carries
 * none, so the place it occupies is kept — the copy keeps the measure the design
 * solved it against, and the tiles keep their pitch — and left as flat tint.</p>
 */
final class IndigoBand {

    private IndigoBand() {
    }

    /**
     * The band.
     *
     * <p>{@code keepTogether} is a section's property and the band needs it: its
     * fill is painted behind its own content, so a band that split across a page
     * would leave its fill behind.</p>
     *
     * @param page   the page flow
     * @param glance the paragraph and the marked facts the band carries
     * @param flow   the sheet's cursor
     */
    static void render(PageFlowBuilder page, ProposalGlance glance, IndigoFlow flow) {
        if (glance.heading().isBlank() && glance.intro().isBlank()
                && glance.facts().isEmpty()) {
            // A band of tint three hundred pixels tall with nothing in it is not
            // a design decision; a document that says nothing about its issuer
            // simply has no band.
            return;
        }
        double top = flow.boxAt(BAND_TOP_PX, BAND_H_PX);
        page.addSection("AboutBand", band -> {
            band.spacing(0)
                    .keepTogether()
                    .fillColor(TINT)
                    // Negative on both sides, so the fill reaches both paper
                    // edges; the padding puts the content back inside the left
                    // margin, and the right stays open for the artwork the design
                    // runs off the page.
                    .margin(new DocumentInsets(top, -MARGIN_R, 0, -MARGIN_L))
                    .padding(new DocumentInsets(0, 0, 0, MARGIN_L));
            band.addRow("AboutRow", row -> {
                row.spacing(0)
                        .columns(DocumentRowColumn.weight(1),
                                DocumentRowColumn.fixed(BAND_ARTWORK_W));
                row.addSection("AboutLeft", left -> {
                    left.spacing(0);
                    IndigoFlow cell = new IndigoFlow(BAND_TOP_PX);
                    renderCopy(left, glance, cell);
                    renderFeatureTiles(left, glance.facts(), cell);
                });
                // The artwork's place, kept so the copy beside it keeps its
                // measure. The design's own picture is its brand's, but the
                // height is the band's: the artwork is what makes the band as
                // tall as it is, and without it the tint would close on the
                // copy and everything below would ride up.
                row.addSection("AboutArtwork", holder -> holder
                        .spacing(0)
                        .addSpacer(space -> space.name("AboutArtworkSpace")
                                .height(px(BAND_H_PX))));
            });
        });
    }

    private static void renderCopy(SectionBuilder left, ProposalGlance glance, IndigoFlow cell) {
        if (!glance.heading().isBlank()) {
            left.addParagraph(p -> p
                    .name("AboutHeading")
                    .text(glance.heading())
                    .textStyle(bold(LABEL_SIZE, ACCENT))
                    .margin(new DocumentInsets(
                            cell.capAt(ABOUT_HEADING_CAP, LABEL_SIZE, true), 0, 0, 0)));
        }
        if (glance.intro().isBlank()) {
            return;
        }
        double measureInset = Math.max(0, BAND_LEFT_W - px(ABOUT_MEASURE_PX));
        double gap = cell.capAt(ABOUT_BODY_CAP, ABOUT_BODY_SIZE, false);
        left.addParagraph(p -> p
                .name("AboutBody")
                .text(glance.intro())
                .textStyle(plain(ABOUT_BODY_SIZE, BODY))
                .lineSpacing(px(ABOUT_BODY_PITCH_PX) - ABOUT_BODY_SIZE)
                .margin(new DocumentInsets(gap, measureInset, 0, 0)));
        // The design sets it in four lines at this measure; the map states that
        // rather than asking the engine afterwards.
        cell.advanceTo(ABOUT_BODY_CAP + 3 * ABOUT_BODY_PITCH_PX + toPx(ABOUT_BODY_SIZE));
    }

    /**
     * The row of marked tiles.
     *
     * <p>The pitch is fixed for every tile but the last, which takes what is
     * left: four full pitches are wider than the band's left column, and the last
     * tile only ever needed its own width and its label's.</p>
     */
    private static void renderFeatureTiles(SectionBuilder left, List<ProposalGlance.Fact> facts,
                                           IndigoFlow cell) {
        if (facts.isEmpty()) {
            return;
        }
        double top = cell.boxAt(FEATURE_TILE_AT,
                boxBottomPx(FEATURE_LINE2_CAP, FEATURE_SIZE, false) - FEATURE_TILE_AT);
        layeredRow(left, "FeatureTiles", row -> {
            row.spacing(0).margin(new DocumentInsets(top, 0, 0, 0));
            DocumentRowColumn[] columns = new DocumentRowColumn[facts.size()];
            for (int i = 0; i < facts.size(); i++) {
                columns[i] = i == facts.size() - 1
                        ? DocumentRowColumn.weight(1)
                        : DocumentRowColumn.fixed(px(FEATURE_PITCH_PX));
            }
            row.columns(columns);
            for (int i = 0; i < facts.size(); i++) {
                ProposalGlance.Fact fact = facts.get(i);
                int index = i;
                row.addSection("FeatureCell_" + index, group -> {
                    group.spacing(0);
                    IndigoFlow stack = new IndigoFlow(FEATURE_TILE_AT);
                    group.add(IndigoWidgets.tile("FeatureTile_" + index, TILE_DARK,
                            fact.icon(), FEATURE_TILE, FEATURE_RADIUS, FEATURE_GLYPH));
                    stack.boxAt(FEATURE_TILE_AT, FEATURE_TILE_PX);
                    group.addParagraph(p -> p
                            .name("FeatureLine1_" + index)
                            .text(fact.label())
                            .textStyle(plain(FEATURE_SIZE, INK))
                            .margin(new DocumentInsets(
                                    stack.capAt(FEATURE_LINE1_CAP, FEATURE_SIZE, false), 0, 0, 0)));
                    group.addParagraph(p -> p
                            .name("FeatureLine2_" + index)
                            .text(fact.value())
                            .textStyle(plain(FEATURE_SIZE, INK))
                            .margin(new DocumentInsets(
                                    stack.capAt(FEATURE_LINE2_CAP, FEATURE_SIZE, false), 0, 0, 0)));
                });
            }
        });
    }
}
