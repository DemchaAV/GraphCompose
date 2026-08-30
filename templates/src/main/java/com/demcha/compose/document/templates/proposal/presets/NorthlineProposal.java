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

import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FOOTER_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FOOTER_TEXT_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.FOOTER_ZONE_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.MARGIN_SIDE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.MARGIN_TOP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ON_DARK;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PAGE_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PAGE_BLOCK_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PAGE_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.PAGE_WIDTH;

/**
 * Northline Proposal — the first structured proposal preset: a two-page
 * teal-and-navy business proposal on the Spectral/Lato pair.
 *
 * <p>Page one carries the brand header, the three stacked title lines, the
 * meta line, the executive summary beside the at-a-glance card, the goal
 * cells, and the numbered scope list; page two repeats the header and
 * carries the deliverable columns, the phase grid, the investment / terms
 * band, and the signing card. Like the ported template, the preset issues
 * no explicit page break: page one's bands are sized so reference-volume
 * content ends just short of the content box and the page-two header opens
 * the second page (a break issued after that natural advance would leave a
 * blank page between them). The two-page distribution is therefore a
 * property of the content volume — noticeably lighter content lets the
 * page-two bands climb onto page one. A section whose data is empty keeps
 * its badge disc and heading; only structures that cannot be composed
 * empty — the goal cells, the phase grid, the signature row — are
 * skipped.</p>
 *
 * <p>The page chrome is not flow content: the navy footer band and the
 * teal page-number block are {@link PageBackgroundFill}s (their geometry
 * comes from the page and they repeat on every page), the brand line and
 * the {@code 0{page}} number are a {@link DocumentHeaderFooter} in the
 * FOOTER zone, and the bottom page margin equals the band height so
 * content stops above the chrome. The preset therefore owns its session
 * geometry — page size, margins, page background — and a session margin
 * set by the caller would be overwritten; see {@link #RECOMMENDED_MARGIN}.</p>
 *
 * <p>The preset consumes the structured proposal model
 * ({@link StructuredProposalDocumentSpec}); the icon tokens the data
 * carries resolve against the packaged Northline icon set. The colours and
 * the measured geometry are preset-local ({@link NorthlineStyles}) — the
 * look is not shared with any other family today.</p>
 */
public final class NorthlineProposal {

    /**
     * Stable template identifier.
     */
    public static final String ID = "proposal-northline";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Northline Proposal";

    /**
     * Recommended session margin (in points). The preset sets its own page
     * geometry — side margins, the top margin, and a bottom margin equal to
     * the footer band — inside {@code compose}, so callers leave the
     * session margin at zero and let the preset own the page frame.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private NorthlineProposal() {
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
                    // above it, so nothing runs underneath the chrome.
                    .margin(new DocumentInsets(MARGIN_TOP, MARGIN_SIDE,
                            FOOTER_HEIGHT, MARGIN_SIDE))
                    .pageBackground(PAGE_BACKGROUND);

            renderChrome(document, data.brand());

            document.pageFlow(page -> {
                // spacing(0): flow spacing survives a page break and would
                // indent the page-two header; the rhythm lives on the bands.
                page.name("Proposal").spacing(0);
                page.addSection("Header",
                        header -> NorthlineWidgets.renderHeader(header, data.brand()));
                NorthlinePageOne.compose(page, data);
                page.addSection("Header",
                        header -> NorthlineWidgets.renderHeader(header, data.brand()));
                NorthlinePageTwo.compose(page, data);
            });
        }

        /**
         * The band that closes every page: the navy band and the teal
         * corner block as page-ratio background fills, and the brand line +
         * page number as FOOTER-zone chrome with {@code {page}} resolved
         * per page ({@code 0{page}} because the reference numbers its pages
         * 01 and 02 and there is no zero-padded page-number style).
         */
        private static void renderChrome(DocumentSession document, ProposalBrand brand) {
            document.pageBackgrounds(List.of(
                    PageBackgroundFill.bottomBand(FOOTER_HEIGHT / PAGE_HEIGHT, INK),
                    new PageBackgroundFill(
                            1.0 - PAGE_BLOCK_WIDTH / PAGE_WIDTH,
                            1.0 - FOOTER_HEIGHT / PAGE_HEIGHT,
                            PAGE_BLOCK_WIDTH / PAGE_WIDTH,
                            FOOTER_HEIGHT / PAGE_HEIGHT,
                            ACCENT)));

            document.footer(DocumentHeaderFooter.builder()
                    .zone(DocumentHeaderFooterZone.FOOTER)
                    // The backend places the baseline at (height - fontSize)
                    // above the page edge; this height centres the cap band
                    // inside the footer band.
                    .height((float) FOOTER_ZONE_HEIGHT)
                    .leftText(brand.footerName() + "     |     " + brand.website())
                    .rightText("0{page}")
                    .fontSize((float) FOOTER_TEXT_SIZE)
                    .textColor(ON_DARK)
                    .build());
        }
    }
}
