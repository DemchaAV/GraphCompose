package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.proposal.builder.ProposalBuilder;
import com.demcha.compose.document.templates.proposal.spec.ProposalSpec;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import com.demcha.compose.font.FontName;

/**
 * Templates v2 "Modern Proposal" preset — minimal v2 proposal surface.
 *
 * <p>Provides a clean, single-column proposal rendering through the
 * new {@link ProposalBuilder} pipeline. The legacy
 * {@code ProposalTemplateV2} continues to ship the cinematic proposal
 * experience; this v2 preset is the canonical seam for builder-driven
 * custom proposals.</p>
 */
public final class ModernProposal {

    /**
     * Stable template identifier.
     */
    public static final String ID = "modern-proposal";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Modern Proposal";

    private ModernProposal() {
    }

    /**
     * Builds a fresh {@code Modern Proposal} template configured for
     * the given business theme.
     *
     * @param theme active business theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
     */
    public static DocumentTemplate<ProposalSpec> create(BusinessTheme theme) {
        Spacing spacing = Spacing.airy();

        DocumentTextStyle titleStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(24.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(41, 128, 185))
                .build();

        DocumentTextStyle headingStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(14.0)
                .decoration(DocumentTextDecoration.BOLD)
                .color(DocumentColor.rgb(44, 62, 80))
                .build();

        DocumentTextStyle bodyStyle = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(10.5)
                .color(DocumentColor.rgb(40, 50, 70))
                .build();

        return ProposalBuilder.builder()
                .id(ID)
                .displayName(DISPLAY_NAME)
                .titleStyle(titleStyle)
                .headingStyle(headingStyle)
                .bodyStyle(bodyStyle)
                .spacing(spacing)
                .build();
    }
}
