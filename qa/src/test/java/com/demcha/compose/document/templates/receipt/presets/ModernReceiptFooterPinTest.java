package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.snapshot.LayoutNodeSnapshot;
import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The footer pin — {@link ModernReceipt} measures what the body left on the
 * last page and spends it, so the verification code and the small print land on
 * the bottom margin instead of floating under the notes.
 *
 * <p>The mechanism re-composes, which clears the session, so the cases that
 * matter are the ones where it must <em>not</em> run: a session that already
 * carries content, and a receipt with no footer to hold down.</p>
 */
class ModernReceiptFooterPinTest {

    /** How close to the bottom margin the footer has to land to count as seated. */
    private static final double SEATED_TOLERANCE = 2.0;

    private static final String PIN_NODE = "ReceiptFooterPin";
    private static final String FOOTER_NODE = "ReceiptFooter";

    private static DocumentSession session() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(ModernReceipt.RECOMMENDED_MARGIN))
                .create();
    }

    private static ReceiptDocumentSpec receipt(Consumer<com.demcha.compose.document.templates
            .data.receipt.ReceiptData.Builder> extra) {
        return ReceiptDocumentSpec.of(builder -> {
            builder.documentTitle("Transfer confirmation")
                    .issuerName("Northwind Pay")
                    .generatedOn("09 August 2026")
                    .amount("Amount collected", "£66.62")
                    .status(ReceiptStatus.settled("Completed"))
                    .payer("Paid from", party -> party.name("Alex Sample"))
                    .beneficiary("Paid to", party -> party.name("Harbour Finance Ltd"));
            extra.accept(builder);
        });
    }

    private static Optional<LayoutNodeSnapshot> node(LayoutSnapshot snapshot, String entityName) {
        return snapshot.nodes().stream()
                .filter(n -> entityName.equals(n.entityName()))
                .findFirst();
    }

    @Test
    void seatsTheFooterOnTheBottomMargin() throws Exception {
        ReceiptDocumentSpec spec = receipt(builder -> builder
                .verification("https://example.com/v/1", "Scan to check it.")
                .legalNote("Every name on this page is invented."));

        try (DocumentSession document = session()) {
            ModernReceipt.create().compose(document, spec);
            LayoutSnapshot snapshot = document.layoutSnapshot();

            assertThat(node(snapshot, PIN_NODE))
                    .describedAs("a short receipt leaves most of the page free, so it pins")
                    .isPresent();
            LayoutNodeSnapshot footer = node(snapshot, FOOTER_NODE).orElseThrow();
            assertThat(footer.placementY())
                    .describedAs("footer bottom against the page's bottom margin")
                    .isCloseTo(snapshot.canvas().margin().bottom(), within(SEATED_TOLERANCE));
        }
    }

    @Test
    void leavesContentThatWasAlreadyComposed() throws Exception {
        // The pin re-composes by clearing the session. A caller who put a cover
        // page in first must get it back, footer pinned or not.
        ReceiptDocumentSpec spec = receipt(builder -> builder
                .verification("https://example.com/v/1", "Scan to check it."));

        try (DocumentSession document = session()) {
            document.dsl().pageFlow().name("CallerCover").addParagraph("Statement pack").build();
            ModernReceipt.create().compose(document, spec);

            assertThat(document.roots()).hasSizeGreaterThan(1);
            LayoutSnapshot snapshot = document.layoutSnapshot();
            assertThat(node(snapshot, "CallerCover"))
                    .describedAs("the caller's own content survives compose()")
                    .isPresent();
            assertThat(node(snapshot, PIN_NODE))
                    .describedAs("a shared session is not the receipt's to re-compose")
                    .isEmpty();
        }
    }

    @Test
    void pinsNothingWhenThereIsNoFooter() throws Exception {
        // No QR, no support lines, no small print: a spacer here would be a
        // page-tall hole holding nothing down.
        try (DocumentSession document = session()) {
            ModernReceipt.create().compose(document, receipt(builder -> { }));

            assertThat(node(document.layoutSnapshot(), PIN_NODE)).isEmpty();
        }
    }

    @Test
    void seatsTheFooterOnTheLastPageOfALongReceipt() throws Exception {
        // Placement coordinates resolve on the page a node starts on, so a body
        // spanning a page break must be measured from nodes that live entirely on
        // the last page — otherwise the gap measured is page one's.
        ReceiptDocumentSpec spec = receipt(builder -> {
            for (int group = 0; group < 12; group++) {
                int index = group;
                builder.detailGroup("Block " + index, rows -> {
                    for (int row = 0; row < 8; row++) {
                        rows.field("Field " + row, "Value " + row);
                    }
                });
            }
            builder.verification("https://example.com/v/1", "Scan to check it.")
                    .legalNote("Every name on this page is invented.");
        });

        try (DocumentSession document = session()) {
            ModernReceipt.create().compose(document, spec);
            LayoutSnapshot snapshot = document.layoutSnapshot();

            assertThat(snapshot.totalPages())
                    .describedAs("the fixture is sized to overflow one page")
                    .isGreaterThan(1);
            LayoutNodeSnapshot footer = node(snapshot, FOOTER_NODE).orElseThrow();
            assertThat(footer.startPage()).isEqualTo(snapshot.totalPages() - 1);
            assertThat(footer.placementY())
                    .describedAs("seated on the last page's bottom margin, not page one's")
                    .isCloseTo(snapshot.canvas().margin().bottom(), within(SEATED_TOLERANCE));
            assertThat(document.toPdfBytes()).isNotEmpty();
        }
    }
}
