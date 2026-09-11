package com.demcha.compose.document.templates.receipt.components;

import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.TextStyles;
import com.demcha.compose.document.templates.core.theme.BrandTheme;

/**
 * The text styles a receipt is built from, each composed once from the
 * {@link BrandTheme}.
 *
 * <p>A receipt has few kinds of text but uses each of them many times: the
 * same grey label appears on every field row, in both party cards, and on
 * every timeline entry. Naming them once here is what keeps the widgets free
 * of style plumbing, and what makes a change to the type scale a change to
 * one file rather than to eight.</p>
 *
 * <p>Every value reads from the theme — the receipt themes'
 * {@link com.demcha.compose.document.templates.core.theme.Typography} slots
 * are documented against these names.</p>
 */
public final class ReceiptStyles {

    private ReceiptStyles() {
    }

    /**
     * Small spaced-caps caption above a block — {@code AMOUNT SENT}.
     *
     * @param theme active theme
     * @return the eyebrow style
     */
    public static DocumentTextStyle eyebrow(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeBanner(),
                DocumentTextDecoration.DEFAULT, theme.palette().muted());
    }

    /**
     * Spaced-caps heading over a field group — {@code TRANSFER DETAILS}.
     *
     * @param theme active theme
     * @return the group-heading style
     */
    public static DocumentTextStyle groupTitle(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeBanner(),
                DocumentTextDecoration.BOLD, theme.palette().ink());
    }

    /**
     * The document heading — {@code Transfer confirmation}.
     *
     * @param theme active theme
     * @return the document-title style
     */
    public static DocumentTextStyle title(BrandTheme theme) {
        return TextStyles.of(theme.typography().headlineFont(), theme.typography().sizeEntryTitle(),
                DocumentTextDecoration.BOLD, theme.palette().ink());
    }

    /**
     * Quiet one-liner under a title or an amount.
     *
     * @param theme active theme
     * @return the caption style
     */
    public static DocumentTextStyle caption(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeEntrySubtitle(),
                DocumentTextDecoration.DEFAULT, theme.palette().muted());
    }

    /**
     * Left column of a field row.
     *
     * @param theme active theme
     * @return the field-label style
     */
    public static DocumentTextStyle label(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeEntryDate(),
                DocumentTextDecoration.DEFAULT, theme.palette().muted());
    }

    /**
     * Right column of a field row.
     *
     * @param theme active theme
     * @return the field-value style
     */
    public static DocumentTextStyle value(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeEntryDate(),
                DocumentTextDecoration.DEFAULT, theme.palette().ink());
    }

    /**
     * Right column of the one field row per group a reader looks for first.
     *
     * @param theme active theme
     * @return the emphasised field-value style
     */
    public static DocumentTextStyle valueStrong(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeEntryDate(),
                DocumentTextDecoration.BOLD, theme.palette().ink());
    }

    /**
     * The hero amount — the largest thing on the page.
     *
     * @param theme active theme
     * @return the amount style
     */
    public static DocumentTextStyle amount(BrandTheme theme) {
        return TextStyles.of(theme.typography().headlineFont(), theme.typography().sizeHeadline(),
                DocumentTextDecoration.BOLD, theme.palette().ink());
    }

    /**
     * Account holder or beneficiary name inside a party card.
     *
     * @param theme active theme
     * @return the party-name style
     */
    public static DocumentTextStyle partyName(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeBody(),
                DocumentTextDecoration.BOLD, theme.palette().ink());
    }

    /**
     * Body prose — notes and address lines.
     *
     * @param theme active theme
     * @return the body style
     */
    public static DocumentTextStyle body(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeBody(),
                DocumentTextDecoration.DEFAULT, theme.palette().ink());
    }

    /**
     * Footer small print and support lines.
     *
     * @param theme active theme
     * @return the small-print style
     */
    public static DocumentTextStyle smallPrint(BrandTheme theme) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeContact(),
                DocumentTextDecoration.DEFAULT, theme.palette().muted());
    }

    /**
     * Text inside the status pill, coloured by the caller.
     *
     * @param theme active theme
     * @param color pill foreground, chosen from the status tone
     * @return the status-pill text style
     */
    public static DocumentTextStyle pill(BrandTheme theme, DocumentColor color) {
        return TextStyles.of(theme.typography().bodyFont(), theme.typography().sizeBanner(),
                DocumentTextDecoration.BOLD, color);
    }
}
