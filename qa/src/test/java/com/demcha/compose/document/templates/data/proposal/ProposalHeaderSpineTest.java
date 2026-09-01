package com.demcha.compose.document.templates.data.proposal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The header spine the one-page sales proposals share.
 *
 * <p>Each case is one of the seven designs the spine was measured against, so a
 * field that stops carrying what a design states fails here rather than in that
 * design's own render.</p>
 */
class ProposalHeaderSpineTest {

    // -- the addressed block --------------------------------------------

    @Test
    void aRecipientCarriesTheCaptionTheNameAndTheAddressSeparately() {
        ProposalRecipient recipient = new ProposalRecipient("PREPARED FOR", "Bright Future Ltd.",
                List.of("45 King Street", "Manchester, M2 4WU", "United Kingdom"));
        assertThat(recipient.label()).isEqualTo("PREPARED FOR");
        assertThat(recipient.name()).isEqualTo("Bright Future Ltd.");
        assertThat(recipient.addressLines()).hasSize(3);
        assertThat(recipient.isPresent()).isTrue();
    }

    @Test
    void aRecipientThatNamesNothingIsNotPresent() {
        assertThat(new ProposalRecipient(null, null, null).isPresent()).isFalse();
        assertThat(new ProposalRecipient("PREPARED FOR", "", List.of()).isPresent()).isFalse();
    }

    // -- the attention line ---------------------------------------------

    @Test
    void anAttentionLineCarriesTheRoleAndBothChannels() {
        ProposalAttention attention = new ProposalAttention("ATTN", "A. Mitchell", "Founder",
                "a@brightfuture.example", "+44 7700 900123");
        assertThat(attention.role()).isEqualTo("Founder");
        assertThat(attention.email()).isEqualTo("a@brightfuture.example");
        assertThat(attention.phone()).isEqualTo("+44 7700 900123");
        assertThat(attention.isPresent()).isTrue();
    }

    @Test
    void aDesignThatFoldsTheNameIntoItsCaptionStillReads() {
        // One of the seven prints "Attn: A. Mitchell" as the caption and gives
        // the name no line of its own, which is why the caption is the design's
        // own string rather than a fixed word.
        ProposalAttention attention = new ProposalAttention("Attn: A. Mitchell", "", "Founder",
                "a@brightfuture.example", "+44 7700 900123");
        assertThat(attention.label()).contains("A. Mitchell");
        assertThat(attention.name()).isEmpty();
        assertThat(attention.isPresent()).isTrue();
    }

    // -- the header tiles -----------------------------------------------

    @Test
    void theTilesAreAListBecauseEveryOneOfTheDesignsSetsFour() {
        ProposalMetaLine meta = new ProposalMetaLine(List.of(
                new ProposalMetaLine.Entry("date", "DATE", "27 May 2026"),
                new ProposalMetaLine.Entry("id", "PROPOSAL ID", "REV-2026-0527-01"),
                new ProposalMetaLine.Entry("valid", "VALID UNTIL", "26 June 2026"),
                new ProposalMetaLine.Entry("by", "PREPARED BY", "The Business Team")));
        assertThat(meta.entries()).hasSize(4);
        assertThat(meta.entries().get(1).label()).isEqualTo("PROPOSAL ID");
        // The running-header trio is a different statement, and stays empty.
        assertThat(meta.preparedFor()).isEmpty();
    }

    @Test
    void theRunningHeaderTrioIsUntouchedByTheTiles() {
        ProposalMetaLine meta = new ProposalMetaLine("Bright Future Ltd.", "The Team", "27 May");
        assertThat(meta.preparedFor()).isEqualTo("Bright Future Ltd.");
        assertThat(meta.entries()).isEmpty();
    }

    // -- the title -------------------------------------------------------

    @Test
    void aTitleOfFourLinesKeepsAllFourAndStillReadsAsThree() {
        ProposalTitleLines title = ProposalTitleLines.of("", List.of(
                "Proposal for", "Financial Infrastructure",
                "& Embedded Finance", "Partnership"), List.of("Empowering your business."));
        assertThat(title.lines()).hasSize(4);
        // The three named lines are the first three of the list, so a preset
        // written against them reads the same title.
        assertThat(title.lead()).isEqualTo("Proposal for");
        assertThat(title.second()).isEqualTo("Financial Infrastructure");
        assertThat(title.third()).isEqualTo("& Embedded Finance");
        assertThat(title.standfirst()).containsExactly("Empowering your business.");
    }

    @Test
    void aTitleStatedAsThreeLinesReadsBackAsAListOfThree() {
        ProposalTitleLines title = new ProposalTitleLines("Proposal for", "Cloud Services", "");
        // The blank third line is not a line of the title.
        assertThat(title.lines()).containsExactly("Proposal for", "Cloud Services");
        assertThat(title.eyebrow()).isEmpty();
    }

    @Test
    void aTitleCarriesTheEyebrowSomeDesignsSetAboveIt() {
        ProposalTitleLines title = ProposalTitleLines.of("Proposal for",
                List.of("Payments", "& Financial Infrastructure"), List.of());
        assertThat(title.eyebrow()).isEqualTo("Proposal for");
        assertThat(title.lead()).isEqualTo("Payments");
    }

    // -- the foot --------------------------------------------------------

    @Test
    void aFooterCarriesTheEntityTheAddressTheChannelsAndTheNotice() {
        ProposalFooter footer = new ProposalFooter("Northwind UK Limited",
                List.of("110 Bishopsgate, London EC2N 4AY, United Kingdom"),
                List.of("www.northwind.example", "+44 800 092 1223"),
                "This proposal is confidential and valid for 30 days.");
        assertThat(footer.name()).isEqualTo("Northwind UK Limited");
        assertThat(footer.contacts()).hasSize(2);
        assertThat(footer.confidentiality()).contains("confidential");
        assertThat(footer.isPresent()).isTrue();
    }

    @Test
    void aFootThatCarriesOnlyANoticeIsStillPresent() {
        // One of the seven states nothing else in its foot.
        ProposalFooter footer = new ProposalFooter(null, null, null,
                "This proposal is confidential and valid for 30 days.");
        assertThat(footer.name()).isEmpty();
        assertThat(footer.isPresent()).isTrue();
    }

    @Test
    void aFootThatStatesNothingIsNotPresent() {
        assertThat(new ProposalFooter(null, null, null, null).isPresent()).isFalse();
    }

    // -- the aggregate ---------------------------------------------------

    @Test
    void theConstructorThatPredatesTheSpineLeavesItEmptyRatherThanNull() {
        StructuredProposalData data = new StructuredProposalData(
                null, null, null, null, null, null, null, null, null, null, null, null);
        assertThat(data.recipient().isPresent()).isFalse();
        assertThat(data.attention().isPresent()).isFalse();
        assertThat(data.footer().isPresent()).isFalse();
    }

    @Test
    void theBuilderCarriesTheSpineThrough() {
        StructuredProposalData data = StructuredProposalData.builder()
                .recipient(new ProposalRecipient("PREPARED FOR", "Bright Future Ltd.",
                        List.of("45 King Street")))
                .attention(new ProposalAttention("ATTN", "A. Mitchell", "Founder", "", ""))
                .footer(new ProposalFooter("Northwind UK Limited", List.of(), List.of(), ""))
                .build();
        assertThat(data.recipient().name()).isEqualTo("Bright Future Ltd.");
        assertThat(data.attention().role()).isEqualTo("Founder");
        assertThat(data.footer().name()).isEqualTo("Northwind UK Limited");
    }
}
