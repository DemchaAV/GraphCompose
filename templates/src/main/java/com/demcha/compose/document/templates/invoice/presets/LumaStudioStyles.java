package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.table.DocumentTableTextAnchor;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.font.FontName;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * The measured geometry, palette and type scale of the Luma Studio invoice.
 *
 * <h2>Two left edges, and that is deliberate</h2>
 *
 * <p>The body text column starts further in than the line-item table, the two
 * rules that bracket the closing block and the notes disc. The table's own
 * first-cell padding brings its text back onto the text edge, so the text runs
 * straight down the page while the table and its rules sit one cell padding
 * wider. {@link #PAGE_MARGIN_LEFT} is therefore the <em>table's</em> edge, and
 * every text block carries {@link #TEXT_INSET} as its left padding — one
 * property per block rather than an inset restated on every paragraph.</p>
 *
 * <h2>Leading is authored as a pitch</h2>
 *
 * <p>{@link #leading} takes the distance the design draws between line tops
 * and subtracts the line box the type already fills. The ratio is per family,
 * because the three faces here set different line boxes at the same size —
 * one shared constant would be wrong by a line per block on two of them.</p>
 */
final class LumaStudioStyles {

    private LumaStudioStyles() {
    }

    // -- page ------------------------------------------------------------

    static final DocumentPageSize PAGE = DocumentPageSize.A4;

    static final double SIDEBAR_WIDTH_RATIO = 0.2218;
    static final double SIDEBAR_WIDTH = PAGE.width() * SIDEBAR_WIDTH_RATIO;
    static final double SIDEBAR_BRAND_HEIGHT_RATIO = 0.24614;
    static final double SIDEBAR_BRAND_HEIGHT = PAGE.height() * SIDEBAR_BRAND_HEIGHT_RATIO;

    static final double PAGE_MARGIN_LEFT = PAGE.width() * 0.25782;
    static final double PAGE_MARGIN_RIGHT = PAGE.width() * 0.0654;
    static final double PAGE_MARGIN_TOP = 46.3;
    static final double CONTENT_WIDTH = PAGE.width() - PAGE_MARGIN_LEFT - PAGE_MARGIN_RIGHT;
    static final double TEXT_INSET = PAGE.width() * 0.01232;
    static final double TEXT_WIDTH = CONTENT_WIDTH - TEXT_INSET;

    /** The stub the sidebar is drawn from: it carries art, not height. */
    static final double SIDEBAR_STUB_HEIGHT = 0.4;
    static final double BANNER_FLOW_HEIGHT = 0.3;
    static final double BANNER_HEIGHT_RATIO = 0.07445;
    static final double BANNER_HEIGHT = PAGE.height() * BANNER_HEIGHT_RATIO;
    static final double FOLIO_ZONE_HEIGHT = 30.5;

    // -- block rhythm ----------------------------------------------------

    static final double MASTHEAD_TO_RULE = 13.0;
    static final double RULE_TO_PARTIES = 12.0;
    static final double PARTIES_TO_TABLE = 12.9;
    static final double TABLE_TO_TOTALS = 4.4;
    static final double TOTALS_TO_RULE = 2.0;
    static final double RULE_TO_CLOSING = 12.0;
    static final double CLOSING_TO_BANNER = 8.6;

    // -- the column grid -------------------------------------------------

    static final double MASTHEAD_LEFT_WEIGHT = 0.5868;
    static final double MASTHEAD_RIGHT_WEIGHT = 1.0 - MASTHEAD_LEFT_WEIGHT;
    static final double SUPPLIER_WIDTH = CONTENT_WIDTH * MASTHEAD_LEFT_WEIGHT - TEXT_INSET;
    static final double META_WIDTH = CONTENT_WIDTH * MASTHEAD_RIGHT_WEIGHT;
    static final double PARTIES_LEFT_WEIGHT = 0.4748;
    static final double CLOSING_LEFT_WEIGHT = 0.4146;
    static final double[] TABLE_RATIOS = {0.0630, 0.3124, 0.0980, 0.2395, 0.0742, 0.2129};
    static final double TOTALS_WIDTH = CONTENT_WIDTH * 0.6204;

    // -- type ------------------------------------------------------------

    static final FontName SANS = FontName.CARLITO;
    static final FontName DISPLAY_SERIF = FontName.SPECTRAL;
    static final FontName TEXT_SERIF = FontName.TINOS;

    static final double BODY_SIZE = 9.4;
    static final double LINE_PITCH = 12.42;
    static final double SMALL_SIZE = BODY_SIZE * 0.851;
    static final double MICRO_SIZE = BODY_SIZE * 0.766;
    static final double SUPPLIER_NAME_SIZE = BODY_SIZE * 1.170;
    static final double TITLE_SIZE = BODY_SIZE * 3.883;
    static final double TOTAL_DUE_SIZE = BODY_SIZE * 1.968;
    static final double MONOGRAM_TOP_SIZE = BODY_SIZE * 6.170;
    static final double MONOGRAM_BOTTOM_SIZE = BODY_SIZE * 3.300;
    static final double WORDMARK_SIZE = BODY_SIZE * 0.830;
    static final double TAGLINE_SIZE = BODY_SIZE * 0.773;

    // -- band heights ----------------------------------------------------

    static final double CONTACT_ROW_HEIGHT = LINE_PITCH * 1.159;
    static final double META_ROW_HEIGHT = LINE_PITCH * 1.409;
    static final double TOTALS_ROW_HEIGHT = LINE_PITCH * 1.458;
    static final double TOTAL_DUE_HEIGHT = LINE_PITCH * 2.182;
    static final double BANK_ROW_HEIGHT = LINE_PITCH * 0.821;

    // -- ornaments and offsets -------------------------------------------

    static final double CONTACT_ICON_SIZE = 8.6;
    static final double CONTACT_TEXT_OFFSET = 21.0;
    static final double META_VALUE_SPLIT = 0.6134;
    static final double SECTION_DISC_SIZE = 24.8;
    static final double SECTION_DISC_ICON_SIZE = 12.0;
    static final double SECTION_HEADING_OFFSET = 35.5;
    static final double BANK_PIPE_OFFSET = 53.6;
    static final double BANK_VALUE_OFFSET = 63.8;
    static final double PARTIES_DIVIDER_INSET = 34.4;
    static final double CLOSING_DIVIDER_INSET = 24.3;
    static final double TITLE_RULE_WIDTH = 58.7;
    static final double TITLE_RULE_THICKNESS = 1.7;
    static final double TITLE_BLOCK_HEIGHT = 45.7;
    static final double TITLE_CAP_INSET = 3.4;
    static final double TITLE_RULE_TO_METADATA = 15.3;
    static final double TOTALS_LABEL_INSET = 41.8;
    static final double TOTALS_AMOUNT_INSET = 13.0;
    static final double RULE_THICKNESS = 0.56;
    static final double TABLE_RULE_THICKNESS = 0.62;

    static final double LOCKUP_LEFT = PAGE.width() * 0.05592;
    static final double LOCKUP_RULE_WIDTH = PAGE.width() * 0.03412;
    static final double LOCKUP_RULE_THICKNESS = 1.7;
    static final double MONOGRAM_TOP_Y = PAGE.height() * 0.04561;
    static final double MONOGRAM_BOTTOM_Y = PAGE.height() * 0.10597;
    static final double LOCKUP_RULE_Y = PAGE.height() * 0.16097;
    static final double WORDMARK_Y = PAGE.height() * 0.18645;
    static final double TAGLINE_Y = PAGE.height() * 0.20456;

    static final double ORNAMENT_DISC_RATIO = 0.34502;
    static final double ORNAMENT_DISC_LEFT_RATIO = 0.02938;
    static final double ORNAMENT_DISC_TOP_FACTOR = -0.48;
    static final double ORNAMENT_ARCH_WIDTH_RATIO = 0.19526;
    static final double ORNAMENT_ARCH_HEIGHT_RATIO = 0.28102;
    static final double ORNAMENT_ARCH_LEFT_RATIO = 0.02654;
    static final double ORNAMENT_SPRIG_WIDTH_RATIO = 0.16000;
    static final double ORNAMENT_SPRIG_LEFT_RATIO = 0.02180;
    static final double ORNAMENT_SPRIG_TOP_RATIO = 0.30500;

    static final double BANNER_CONTENT_INSET = PAGE.width() * 0.05498;
    static final double BANNER_DISC_SIZE = 33.9;
    static final double BANNER_DISC_ICON_SIZE = 16.0;
    static final double BANNER_TEXT_OFFSET = 47.9;

    // -- table padding ---------------------------------------------------

    static final double TABLE_CELL_PAD_X = 7.33;
    static final double TABLE_CELL_PAD_Y = 9.3;
    static final double TABLE_HEADER_PAD_Y = 7.2;
    static final double TABLE_TIGHT_PAD_X = 2.0;
    static final double TABLE_AMOUNT_PAD_RIGHT = 15.2;

    static final DocumentInsets PAD_INDEX = new DocumentInsets(
            TABLE_CELL_PAD_Y, TABLE_TIGHT_PAD_X, TABLE_CELL_PAD_Y, TABLE_CELL_PAD_X);
    static final DocumentInsets PAD_DESCRIPTION = new DocumentInsets(
            TABLE_CELL_PAD_Y, TABLE_TIGHT_PAD_X, TABLE_CELL_PAD_Y, TABLE_CELL_PAD_X * 0.93);
    static final DocumentInsets PAD_NUMERIC = DocumentInsets.symmetric(
            TABLE_CELL_PAD_Y, TABLE_TIGHT_PAD_X);
    static final DocumentInsets PAD_AMOUNT = new DocumentInsets(
            TABLE_CELL_PAD_Y, TABLE_AMOUNT_PAD_RIGHT, TABLE_CELL_PAD_Y, TABLE_TIGHT_PAD_X);
    static final DocumentInsets PAD_HEADER_LEFT = new DocumentInsets(
            TABLE_HEADER_PAD_Y, TABLE_TIGHT_PAD_X, TABLE_HEADER_PAD_Y, TABLE_CELL_PAD_X);
    static final DocumentInsets PAD_HEADER_CENTRE = DocumentInsets.symmetric(
            TABLE_HEADER_PAD_Y, TABLE_TIGHT_PAD_X);
    static final DocumentInsets PAD_HEADER_RIGHT = new DocumentInsets(
            TABLE_HEADER_PAD_Y, TABLE_AMOUNT_PAD_RIGHT, TABLE_HEADER_PAD_Y, TABLE_TIGHT_PAD_X);

    // -- palette ---------------------------------------------------------

    static final DocumentColor ACCENT = DocumentColor.rgb(163, 78, 51);
    static final DocumentColor INK_SURFACE = DocumentColor.rgb(46, 46, 46);
    static final DocumentColor INK = DocumentColor.rgb(51, 51, 51);
    static final DocumentColor MUTED = DocumentColor.rgb(90, 87, 80);
    static final DocumentColor SIDEBAR_SURFACE = DocumentColor.rgb(243, 236, 224);
    static final DocumentColor ORNAMENT_TINT = DocumentColor.rgb(219, 199, 182);
    static final DocumentColor PAPER = DocumentColor.rgb(251, 250, 249);
    static final DocumentColor PANEL_HIGHLIGHT = DocumentColor.rgb(232, 222, 213);
    static final DocumentColor HAIRLINE = DocumentColor.rgb(221, 212, 202);
    static final DocumentColor HAIRLINE_QUIET = DocumentColor.rgb(211, 202, 189);
    static final DocumentColor ON_DARK = DocumentColor.WHITE;
    static final DocumentColor ON_ACCENT = DocumentColor.rgb(247, 240, 231);

    // -- text styles -----------------------------------------------------

    static final DocumentTextStyle BODY = sans(BODY_SIZE, DocumentTextDecoration.DEFAULT, INK);
    static final DocumentTextStyle BODY_BOLD = sans(BODY_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SMALL_BOLD =
            sans(SMALL_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle MICRO_MUTED =
            sans(MICRO_SIZE, DocumentTextDecoration.DEFAULT, MUTED);
    static final DocumentTextStyle SUPPLIER_NAME =
            sans(SUPPLIER_NAME_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle META_LABEL =
            sans(SMALL_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle SECTION_HEADING =
            sans(SMALL_SIZE, DocumentTextDecoration.BOLD, ACCENT);
    static final DocumentTextStyle TABLE_HEADER =
            sans(SMALL_SIZE, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle ITEM_INDEX =
            sans(BODY_SIZE * 1.15, DocumentTextDecoration.DEFAULT, ACCENT);
    static final DocumentTextStyle ITEM_TITLE =
            sans(BODY_SIZE * 0.875, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle ITEM_DESC =
            serif(BODY_SIZE * 0.780, DocumentTextDecoration.ITALIC, MUTED);
    static final DocumentTextStyle TOTAL_LABEL =
            sans(BODY_SIZE * 1.02, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle TOTAL_AMOUNT =
            sans(BODY_SIZE * 1.02, DocumentTextDecoration.DEFAULT, INK);
    static final DocumentTextStyle TOTAL_DUE_LABEL =
            sans(BODY_SIZE * 1.09, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle TOTAL_DUE_AMOUNT =
            sans(TOTAL_DUE_SIZE, DocumentTextDecoration.BOLD, ACCENT);
    static final DocumentTextStyle NOTE_TEXT =
            serif(BODY_SIZE * 0.766, DocumentTextDecoration.ITALIC, INK);
    static final DocumentTextStyle BANK_LABEL =
            sans(MICRO_SIZE, DocumentTextDecoration.BOLD, INK);
    static final DocumentTextStyle BANK_VALUE =
            sans(MICRO_SIZE, DocumentTextDecoration.DEFAULT, MUTED);
    static final DocumentTextStyle INVOICE_TITLE =
            display(TITLE_SIZE, DocumentTextDecoration.DEFAULT, INK_SURFACE);
    static final DocumentTextStyle MONOGRAM_TOP =
            display(MONOGRAM_TOP_SIZE, DocumentTextDecoration.DEFAULT, ON_ACCENT);
    static final DocumentTextStyle MONOGRAM_BOTTOM =
            display(MONOGRAM_BOTTOM_SIZE, DocumentTextDecoration.DEFAULT, ON_ACCENT);
    static final DocumentTextStyle WORDMARK =
            sans(WORDMARK_SIZE, DocumentTextDecoration.BOLD, ON_ACCENT);
    static final DocumentTextStyle TAGLINE =
            serif(TAGLINE_SIZE, DocumentTextDecoration.ITALIC, ON_ACCENT);
    static final DocumentTextStyle BANNER_SIGN_OFF =
            sans(BODY_SIZE * 1.02, DocumentTextDecoration.BOLD, ON_DARK);
    static final DocumentTextStyle BANNER_DUE =
            serif(BODY_SIZE * 1.09, DocumentTextDecoration.ITALIC, ON_DARK);

    // -- tracking --------------------------------------------------------

    /**
     * The letter-spacing each tracked run is set with, in points.
     *
     * <p>A text style carries no tracking, so a tracked run is written letter
     * by letter with an invisible inline rectangle between the pairs, and
     * these are its widths. They are frozen constants rather than a
     * measurement: the design derived each by measuring the face and dividing
     * the difference from a target width across the gaps, which would tie the
     * preset to the font artifact's own resource layout at compose time. The
     * numbers are what that measurement produced, and the pixel-parity gate
     * is what proves they are still the right ones.</p>
     */
    static final double TRACK_TITLE = 0.059021;
    static final double TRACK_META_LABEL = 1.462532;
    static final double TRACK_SECTION_HEADING = 1.462576;
    static final double TRACK_TABLE_HEADER = 1.525822;
    static final double TRACK_TOTAL_LABEL = 0.612126;
    static final double TRACK_WORDMARK = 0.990497;

    // -- helpers ---------------------------------------------------------

    static DocumentTextStyle sans(double size, DocumentTextDecoration decoration,
                                  DocumentColor color) {
        return TextStyles.of(SANS, size, decoration, color);
    }

    static DocumentTextStyle display(double size, DocumentTextDecoration decoration,
                                     DocumentColor color) {
        return TextStyles.of(DISPLAY_SERIF, size, decoration, color);
    }

    static DocumentTextStyle serif(double size, DocumentTextDecoration decoration,
                                   DocumentColor color) {
        return TextStyles.of(TEXT_SERIF, size, decoration, color);
    }

    /**
     * The leading to author so lines land on a given pitch.
     *
     * @param targetPitch the distance the design draws between line tops
     * @param style       the style of the lines
     * @return the extra leading, never negative
     */
    static double leading(double targetPitch, DocumentTextStyle style) {
        return Math.max(0, targetPitch - style.size() * lineBoxRatio(style.fontName()));
    }

    /**
     * How tall a line box is as a share of its type size, per family. The
     * three faces differ enough that one shared ratio would be wrong by a
     * line per block on two of them.
     */
    private static double lineBoxRatio(FontName fontName) {
        if (fontName.sameFamily(DISPLAY_SERIF)) {
            return 1.522;
        }
        if (fontName.sameFamily(TEXT_SERIF)) {
            return 1.150;
        }
        return 1.221;
    }

    static DocumentTableStyle cellStyle(DocumentTextStyle textStyle,
                                        DocumentTableTextAnchor anchor,
                                        DocumentInsets padding) {
        return DocumentTableStyle.builder()
                .padding(padding)
                .fillColor(PAPER)
                .stroke(DocumentStroke.of(PAPER, 0))
                .textStyle(textStyle)
                .textAnchor(anchor)
                .lineSpacing(0)
                .build();
    }

    static DocumentTableStyle headerStyle(DocumentTableTextAnchor anchor,
                                          DocumentInsets padding) {
        return DocumentTableStyle.builder()
                .padding(padding)
                .fillColor(INK_SURFACE)
                .stroke(DocumentStroke.of(INK_SURFACE, 0))
                .textStyle(TABLE_HEADER)
                .textAnchor(anchor)
                .lineSpacing(0)
                .build();
    }

    /**
     * A figure written the way this sheet writes money: the currency mark,
     * then grouped digits to two places.
     *
     * <p>The formatter is built per call rather than shared: {@link NumberFormat}
     * is not thread-safe, and a document is cheap to format beside a static
     * monitor every render on every thread would queue behind.</p>
     *
     * @param currencySymbol the mark to prefix
     * @param value          the figure
     * @return the formatted amount
     */
    static String money(String currencySymbol, BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return currencySymbol + format.format(value);
    }
}
