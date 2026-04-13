package com.demcha.compose.layout_core.components.content.header_footer;

import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import lombok.Builder;
import lombok.Getter;

import java.awt.Color;

/**
 * Configuration for a repeating page header or footer.
 *
 * <p>Headers and footers are document-level constructs, not regular ECS
 * entities. They are attached to the {@code PdfComposer} and rendered once
 * per page after the main layout/pagination pass.</p>
 *
 * <p>Text content may include placeholders:</p>
 * <ul>
 *   <li>{@code {page}} — current page number</li>
 *   <li>{@code {pages}} — total number of pages</li>
 *   <li>{@code {date}} — current date (ISO format)</li>
 * </ul>
 *
 * @author Artem Demchyshyn
 */
@Getter
@Builder
public final class HeaderFooterConfig implements Component {

    /** Whether this is a HEADER or FOOTER zone. */
    private final HeaderFooterZone zone;

    /** Height reserved for this zone in points. */
    @Builder.Default
    private final float height = 30f;

    /** Left-aligned text (may contain placeholders). */
    private final String leftText;

    /** Center-aligned text (may contain placeholders). */
    private final String centerText;

    /** Right-aligned text (may contain placeholders). */
    private final String rightText;

    /** Font size for the header/footer text. */
    @Builder.Default
    private final float fontSize = 9f;

    /** Text color. */
    @Builder.Default
    private final Color textColor = new Color(128, 128, 128);

    /** Whether to draw a separator line between the zone and the content. */
    @Builder.Default
    private final boolean showSeparator = false;

    /** Separator line color. */
    @Builder.Default
    private final Color separatorColor = new Color(200, 200, 200);

    /** Separator line thickness. */
    @Builder.Default
    private final float separatorThickness = 0.5f;

    /**
     * Resolves placeholder tokens in the given text for a specific page.
     *
     * @param text          raw text with placeholders
     * @param currentPage   1-based page number
     * @param totalPages    total number of pages
     * @return resolved text
     */
    public static String resolvePlaceholders(String text, int currentPage, int totalPages) {
        if (text == null || text.isEmpty()) return text;
        return text
                .replace("{page}", String.valueOf(currentPage))
                .replace("{pages}", String.valueOf(totalPages))
                .replace("{date}", java.time.LocalDate.now().toString());
    }
}
