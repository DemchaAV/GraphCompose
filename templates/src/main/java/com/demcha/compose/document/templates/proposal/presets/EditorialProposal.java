package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;

import java.util.List;
import java.util.Objects;

import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FOOTER_BAND;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FOOTER_RULE_Y;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FOOTER_TEXT_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.FOOTER_ZONE_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HAIRLINE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.MARGIN_SIDE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.MARGIN_TOP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PAGE_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PAGE_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PAGE_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.RULE;

/**
 * Editorial Proposal — the second structured proposal preset: a two-page
 * business proposal set in a serif display face with an orange accent.
 *
 * <p>Page one opens with the masthead — a drawn brand mark, the stacked
 * wordmark, the tracked document label — then the three title lines, the
 * meta line, the executive summary beside an at-a-glance fact card, the
 * goal cells, and the numbered scope list. Page two repeats the masthead
 * and carries the deliverable columns, the phase grid, the investment /
 * terms band, and the signing card.</p>
 *
 * <p>What sets it apart from {@link NorthlineProposal}, which renders the
 * same {@link StructuredProposalDocumentSpec}: section headings are set in
 * the display serif over a short accent rule rather than in the body sans
 * inside an icon badge, so the data's section icon tokens are not drawn;
 * the brand mark is drawn from vector paths rather than set as a monogram
 * letter; the fact card is untitled, so its heading is not drawn either;
 * the scope ordinal is plain accent text rather than a filled pill; and the
 * page foot is a hairline over the content width rather than a filled band.
 * Both presets set their titles in the same display face.</p>
 *
 * <p><strong>Moving a document between the two presets</strong> is a
 * one-line change in the code and two things to check in the data. Icon
 * tokens are preset-scoped vocabulary — each preset packages its own set,
 * and a token this one does not carry is reported as a data error; the four
 * {@code fact-*} tokens are named alike in both sets, the badge and goal
 * tokens are not. Heading text is drawn as authored, so a document written
 * for the sibling's tracked capitals keeps them here, where this design
 * sets its headings in title case.</p>
 *
 * <p>The preset owns its session geometry — A4, the margins published as
 * {@link #RECOMMENDED_MARGIN}, the near-white page fill — and draws the
 * brand line and page numbers as footer chrome, so they repeat on every
 * page. It issues no explicit page break: the page-two masthead is
 * {@code keepWithNext}, which is what stops it being orphaned at the foot
 * of page one, and a break issued after the compiler has already advanced
 * would leave a blank page between the two.</p>
 */
public final class EditorialProposal {

    /**
     * Stable template identifier.
     */
    public static final String ID = "proposal-editorial";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Editorial Proposal";

    /**
     * The side margin (in points) the preset sets. It is published so a
     * caller can measure against the same frame; the preset applies its own
     * margins inside {@code compose}, so callers leave the session
     * unconfigured.
     */
    public static final double RECOMMENDED_MARGIN = MARGIN_SIDE;

    private EditorialProposal() {
    }

    /**
     * Builds the preset.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<StructuredProposalDocumentSpec> create() {
        return new Template();
    }

    private record Template() implements DocumentTemplate<StructuredProposalDocumentSpec> {

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, StructuredProposalDocumentSpec spec) {
            Objects.requireNonNull(document, "document");
            StructuredProposalData data = Objects.requireNonNull(spec, "spec").proposal();

            document.pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                    // The bottom margin is the footer band: content stops
                    // above it, so nothing runs under the chrome.
                    .margin(new DocumentInsets(MARGIN_TOP, MARGIN_SIDE,
                            FOOTER_BAND, MARGIN_SIDE))
                    .pageBackground(PAGE_BACKGROUND);

            renderChrome(document, data.brand());

            document.pageFlow(page -> {
                // spacing(0): flow spacing survives a page break, so a band
                // opening a page would inherit a gap the first never had.
                page.name("Proposal").spacing(0);
                EditorialWidgets.renderMasthead(page, data.brand());
                EditorialPageOne.compose(page, data);
                EditorialWidgets.renderMasthead(page, data.brand());
                EditorialPageTwo.compose(page, data);
            });
        }

        /**
         * The page foot: a hairline across the content width as a page-ratio
         * background fill, and the brand line with the page number as
         * FOOTER-zone chrome, both repeating on every page. The number is
         * written {@code 0{page}} because the reference numbers its pages 01
         * and 02 and there is no zero-padded page-number style.
         */
        private static void renderChrome(DocumentSession document, ProposalBrand brand) {
            document.pageBackgrounds(List.of(new PageBackgroundFill(
                    MARGIN_SIDE / PAGE_WIDTH,
                    FOOTER_RULE_Y / PAGE_HEIGHT,
                    CONTENT_WIDTH / PAGE_WIDTH,
                    HAIRLINE / PAGE_HEIGHT,
                    RULE)));

            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    .height((float) FOOTER_ZONE_HEIGHT)
                    .leftText(brand.footerName() + "     |     " + brand.website())
                    .rightText("0{page}")
                    .fontSize((float) FOOTER_TEXT_SIZE)
                    .textColor(INK)
                    .build());
        }
    }
}
