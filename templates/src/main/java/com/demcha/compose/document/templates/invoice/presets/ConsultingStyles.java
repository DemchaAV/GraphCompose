package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.font.FontName;

/**
 * The Consulting Invoice look: page geometry, the colour roles, the Poppins
 * type scale, and the derived widths every region measures against.
 *
 * <p>Preset-local rather than a {@code BrandTheme}: every value below was
 * measured against the ported template's reference render, and the type
 * scale derives from a single {@link #BODY_SIZE} step that no other family
 * shares. A theme slot would add indirection without a second consumer.</p>
 */
final class ConsultingStyles {

    private ConsultingStyles() {
    }

    // --- page ---------------------------------------------------------------
    static final DocumentPageSize PAGE = DocumentPageSize.A4;
    static final double PAGE_MARGIN = 30.5;
    /** The denominator of every derived width below. */
    static final double CONTENT_WIDTH = PAGE.width() - 2.0 * PAGE_MARGIN;

    // --- band splits, measured from the reference ---------------------------
    static final double HEADER_LEFT_WEIGHT = 0.51;
    static final double HEADER_RIGHT_WEIGHT = 1.0 - HEADER_LEFT_WEIGHT;
    static final double HEADER_GAP = 25.0;
    static final double SUPPLIER_WIDTH = (CONTENT_WIDTH - HEADER_GAP) * HEADER_LEFT_WEIGHT;
    static final double INVOICE_WIDTH = (CONTENT_WIDTH - HEADER_GAP) * HEADER_RIGHT_WEIGHT;
    static final double PARTIES_LEFT_WEIGHT = 0.55;
    static final double LOWER_LEFT_WEIGHT = 0.54;
    static final double LOWER_GAP = 22.0;
    static final double PAYMENT_WIDTH = (CONTENT_WIDTH - LOWER_GAP) * LOWER_LEFT_WEIGHT;
    static final double NOTES_WIDTH = (CONTENT_WIDTH - LOWER_GAP) * (1.0 - LOWER_LEFT_WEIGHT);
    static final double TOTALS_WIDTH = CONTENT_WIDTH * 0.42;

    /** #, description, service period, qty, unit price, amount. */
    static final double[] TABLE_RATIOS = {0.050, 0.341, 0.218, 0.110, 0.154, 0.127};

    static final double TABLE_CELL_HORIZONTAL_PADDING = 5.0;
    static final double DESCRIPTION_CONTENT_WIDTH =
            CONTENT_WIDTH * TABLE_RATIOS[1] - 2.0 * TABLE_CELL_HORIZONTAL_PADDING;

    // --- independent dimensions ---------------------------------------------
    static final double CONTACT_ROW_HEIGHT = 13.0;
    static final double METADATA_ROW_HEIGHT = 18.0;
    static final double FOOTER_BAND_RATIO = 0.039;
    static final double FOOTER_ZONE_HEIGHT = 22.0;
    static final double FOOTER_TEXT_SIZE = 7.0;
    static final double BRAND_LOGO_BOX_HEIGHT = 64.0;
    static final double TOTAL_LABEL_LEFT_INSET = 13.0;
    static final double TOTAL_AMOUNT_RIGHT_INSET = 10.0;

    // --- type scale, all derived from one step ------------------------------
    static final double BODY_SIZE = 7.6;
    static final double SMALL_SIZE = BODY_SIZE * 0.90;
    static final double SECTION_SIZE = BODY_SIZE * 1.15;
    static final double TITLE_SIZE = BODY_SIZE * 3.10;
    static final double TOTAL_SIZE = BODY_SIZE * 1.80;

    // --- colour roles: named for the job, not the hue -----------------------
    static final DocumentColor PAGE_BACKGROUND = DocumentColor.rgb(254, 254, 254);
    static final DocumentColor ACCENT_PRIMARY = DocumentColor.rgb(7, 86, 86);
    static final DocumentColor INK = DocumentColor.rgb(35, 43, 54);
    static final DocumentColor MUTED = DocumentColor.rgb(83, 91, 100);
    static final DocumentColor ROW_ALT = DocumentColor.rgb(248, 248, 248);
    static final DocumentColor EMPHASIS_FILL = DocumentColor.rgb(237, 246, 245);
    static final DocumentColor FOOTER_FILL = DocumentColor.rgb(238, 246, 246);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(225, 229, 229);

    static final FontName TEXT_FONT = FontName.POPPINS;

    // --- text roles ----------------------------------------------------------
    static final DocumentTextStyle BODY = style(BODY_SIZE, DocumentTextDecoration.DEFAULT, INK);
    static final DocumentTextStyle BODY_BOLD = style(BODY_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle BODY_ACCENT =
            style(BODY_SIZE, DocumentTextDecoration.BOLD, ACCENT_PRIMARY);
    static final DocumentTextStyle BODY_LINK =
            style(BODY_SIZE, DocumentTextDecoration.DEFAULT, ACCENT_PRIMARY);
    static final DocumentTextStyle SMALL = style(SMALL_SIZE, DocumentTextDecoration.DEFAULT, INK);
    static final DocumentTextStyle SMALL_BOLD = style(SMALL_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SMALL_ITALIC =
            style(SMALL_SIZE, DocumentTextDecoration.ITALIC, INK);
    static final DocumentTextStyle NOTICE_BOLD =
            style(BODY_SIZE * 0.76, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle NOTICE_ACCENT =
            style(BODY_SIZE * 0.76, DocumentTextDecoration.BOLD, ACCENT_PRIMARY);
    static final DocumentTextStyle SECTION_HEADING =
            style(SECTION_SIZE, DocumentTextDecoration.BOLD, ACCENT_PRIMARY);
    static final DocumentTextStyle INVOICE_TITLE =
            style(TITLE_SIZE, DocumentTextDecoration.BOLD, ACCENT_PRIMARY);
    static final DocumentTextStyle BRAND_NAME =
            style(BODY_SIZE * 2.45, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle BRAND_MARK =
            style(BODY_SIZE * 3.25, DocumentTextDecoration.BOLD, ACCENT_PRIMARY);
    static final DocumentTextStyle BRAND_QUALIFIER =
            style(BODY_SIZE * 1.20, DocumentTextDecoration.DEFAULT, INK);
    static final DocumentTextStyle TABLE_HEADER =
            style(SMALL_SIZE, DocumentTextDecoration.BOLD, DocumentColor.WHITE);
    static final DocumentTextStyle TOTAL_AMOUNT =
            style(TOTAL_SIZE, DocumentTextDecoration.BOLD, ACCENT_PRIMARY);

    /**
     * One line-item cell style: the zebra fill it sits on and where its value
     * is anchored.
     *
     * @param fill   the row fill
     * @param anchor where the value sits in the cell
     * @return the cell style
     */
    static DocumentTableStyle tableStyle(DocumentColor fill, DocumentTableTextAnchor anchor) {
        return DocumentTableStyle.builder()
                .padding(DocumentInsets.symmetric(4.2, TABLE_CELL_HORIZONTAL_PADDING))
                .fillColor(fill)
                .stroke(DocumentStroke.of(HAIRLINE, 0.55))
                .textStyle(BODY)
                .textAnchor(anchor)
                .lineSpacing(1.15)
                .build();
    }

    /**
     * Composes a text style in the preset's face.
     *
     * @param size       type size in points
     * @param decoration weight or slant
     * @param color      ink colour
     * @return the composed style
     */
    static DocumentTextStyle style(double size,
                                   DocumentTextDecoration decoration,
                                   DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(TEXT_FONT)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
