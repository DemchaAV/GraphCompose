package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;

import java.util.Objects;

import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.FLOW_ORIGIN_PX;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.PAGE;
import static com.demcha.compose.document.templates.proposal.presets.IndigoStyles.PAGE_MARGIN;

/**
 * Indigo Proposal — a one-page sales proposal: an addressed head, a tinted band
 * about the issuer, a numbered plan beside its price, and a foot that says who
 * sent it.
 *
 * <p>The sheet is a single page by design and not by accident: every block's
 * position is a cap top in one vertical map ({@link IndigoStyles}), and the
 * cursor that turns those into margins ({@link IndigoFlow}) walks the page once,
 * top to bottom. Content heavier than the design's own — a sixth step, a fifth
 * priced row, an address that wraps — pushes the blocks below it down and can
 * carry the foot onto a second page. The preset does not cap what it is given:
 * a document that overruns paginates rather than being truncated, and never
 * prints one block over another.</p>
 *
 * <p>Two places the design fills with its own brand assets are kept as places
 * rather than filled: the band's artwork column (see {@link IndigoBand}) and the
 * foot's logotype (see {@link IndigoClosing}). The templates artifact carries no
 * brand assets, so the copy keeps the measure it was solved against and the foot
 * sets whatever monogram the document names.</p>
 *
 * <p>The preset consumes the structured proposal model
 * ({@link StructuredProposalDocumentSpec}); the icon tokens the data carries
 * resolve against the packaged Indigo set ({@link IndigoIcons}). It owns its
 * page geometry — A4 and the design's own ink margins — so a session margin set
 * by the caller is overwritten; see {@link #RECOMMENDED_MARGIN}.</p>
 *
 * <h2>Fonts</h2>
 *
 * <p>The sheet is set in Gothic A1, the family every size was solved against.
 * The templates artifact carries no fonts: it arrives with
 * {@code graph-compose-fonts} on the classpath, or a caller registers a family
 * of that name on the session itself. With neither, the engine substitutes and
 * every size is solved for type that is not there.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * DocumentTemplate<StructuredProposalDocumentSpec> template = IndigoProposal.create();
 * template.compose(session, spec);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class IndigoProposal {

    /**
     * Stable template identifier.
     */
    public static final String ID = "proposal-indigo";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Indigo Proposal";

    /**
     * Recommended session margin (in points). The preset sets the design's own
     * ink margins inside {@code compose}, so callers leave the session margin at
     * zero and let the preset own the page frame.
     */
    public static final double RECOMMENDED_MARGIN = 0.0;

    private IndigoProposal() {
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

            document.pageSize(PAGE).margin(PAGE_MARGIN);

            document.pageFlow(page -> {
                // spacing(0): every gap on this sheet is a margin the map
                // solved, so a flow spacing would be added on top of all of
                // them.
                page.name("Proposal").spacing(0);
                IndigoFlow flow = new IndigoFlow(FLOW_ORIGIN_PX);
                IndigoMasthead.renderMasthead(page, data.brand(), flow);
                IndigoMasthead.renderMastheadRule(page, flow);
                IndigoMasthead.renderIntroRow(page, data.recipient(), data.attention(),
                        data.title(), data.meta(), flow);
                IndigoBand.render(page, data.glance(), flow);
                IndigoLower.render(page, data.scope(), data.investment(), data.terms(), flow);
                IndigoClosing.renderFooterRule(page, flow);
                IndigoClosing.renderFooter(page, data.brand(), data.footer(), flow);
            });
        }
    }
}
