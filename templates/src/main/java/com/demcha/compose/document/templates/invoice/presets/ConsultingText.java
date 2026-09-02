package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.node.DocumentLinkOptions;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.DESCRIPTION_CONTENT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL_SIZE;

/**
 * The text and number utilities the Consulting Invoice regions share:
 * money and quantity formatting, link URIs, and the measurement behind the
 * line-item title breaker.
 */
final class ConsultingText {

    /**
     * The face the line-item title is measured in.
     *
     * <p><strong>The templates artifact does not carry it.</strong> The
     * face ships in {@code graph-compose-fonts}, which this module does not
     * depend on, so a caller that adds only the templates artifact measures
     * in the platform's {@code SansSerif} instead — a different width, and
     * one that differs between operating systems. That caller also renders
     * the document in a substituted face, so the page is already not the
     * designed one; what the fallback adds is that the description column's
     * break lands differently per platform. Put the font artifact on the
     * classpath to get the designed render.</p>
     */
    private static final Font DESCRIPTION_BOLD_FONT = loadDescriptionBoldFont();

    private static final FontRenderContext FONT_RENDER_CONTEXT =
            new FontRenderContext(null, true, true);

    private ConsultingText() {
    }

    /**
     * Width of the invisible breaker that pushes a line item's description
     * onto its own line: what is left of the description column once the
     * title has been set.
     *
     * <p>A rich run cannot carry a line break — the engine strips a newline
     * inside one — so the description column ends its title run with a
     * zero-height rectangle wide enough to fill the line instead. Sizing
     * that rectangle needs the rendered title width, which is measured here
     * rather than laid out.</p>
     *
     * @param title the line-item title
     * @return the breaker width, never below one point
     */
    static double titleBreakerWidth(String title) {
        double titleWidth = DESCRIPTION_BOLD_FONT
                .deriveFont((float) SMALL_SIZE)
                .getStringBounds(Objects.requireNonNullElse(title, ""), FONT_RENDER_CONTEXT)
                .getWidth();
        return Math.max(1.0, DESCRIPTION_CONTENT_WIDTH - titleWidth - 4.0);
    }

    private static Font loadDescriptionBoldFont() {
        try (InputStream stream = ConsultingText.class.getClassLoader()
                .getResourceAsStream("fonts/google/poppins/Poppins-Bold.ttf")) {
            if (stream != null) {
                return Font.createFont(Font.TRUETYPE_FONT, stream);
            }
        } catch (Exception cause) {
            // A present-but-unreadable font is worth saying out loud; a
            // missing one is the documented fallback below.
            System.getLogger(ConsultingText.class.getName())
                    .log(System.Logger.Level.WARNING,
                            "Falling back to the platform face: the packaged Poppins Bold "
                                    + "could not be read",
                            cause);
        }
        return new Font("SansSerif", Font.BOLD, 1);
    }

    /**
     * Spaces a value out into tracked capitals (the heading treatment).
     *
     * @param value the text, already in the case it is set in
     * @return the tracked text
     */
    static String tracked(String value) {
        StringBuilder tracked = new StringBuilder(value.length() * 2);
        for (int index = 0; index < value.length(); index++) {
            if (index > 0) {
                tracked.append(' ');
            }
            tracked.append(value.charAt(index));
        }
        return tracked.toString();
    }

    /**
     * Formats a quantity at two decimal places, without grouping.
     *
     * @param value the quantity
     * @return the formatted quantity
     */
    static String decimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Formats an amount with thousands grouping and two decimal places. The
     * currency itself is printed in the column header, not per figure.
     *
     * @param value the amount
     * @return the formatted amount
     */
    static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    /**
     * Wraps a URI as link options.
     *
     * @param uri the target
     * @return the link options
     */
    static DocumentLinkOptions link(String uri) {
        return new DocumentLinkOptions(uri);
    }

    /**
     * Builds the {@code tel:} URI of a printed phone number.
     *
     * @param phone the printed number
     * @return the dial URI
     */
    static String telUri(String phone) {
        return "tel:" + phone.replace(" ", "");
    }

    /**
     * Builds the browsable URI of a printed website, adding the scheme when
     * the printed form omits it.
     *
     * @param website the printed website
     * @return the browsable URI
     */
    static String websiteUri(String website) {
        return website.startsWith("http://") || website.startsWith("https://")
                ? website
                : "https://" + website;
    }

    /**
     * Keeps a printed phone number on one line.
     *
     * @param phone the printed number
     * @return the number with non-breaking spaces
     */
    static String nonBreakingPhone(String phone) {
        return phone.replace(' ', '\u00A0');
    }
}
