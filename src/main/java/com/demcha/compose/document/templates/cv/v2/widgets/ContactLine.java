package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvContact;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Contact-line widget — the pipe-separated phone / email / address /
 * optional links row that sits under the {@link Headline}.
 *
 * <h2>Variants</h2>
 *
 * <ul>
 *   <li>{@link #centered} — centred line, phone → email → address →
 *       links. Used by classic / editorial presets that center the
 *       whole header block.</li>
 *   <li>{@link #rightAligned} — right-aligned line, address → phone
 *       → email → links. Used by modern presets where the header
 *       sits flush right next to the name.</li>
 * </ul>
 *
 * <p>Email is always rendered as a clickable {@code mailto:} link;
 * each optional {@link CvLink} becomes a clickable hyperlink with
 * the {@code label} as the visible text. The separator glyph comes
 * from {@code theme.decoration().contactSeparator()}.</p>
 */
public final class ContactLine {

    private ContactLine() {
    }

    /**
     * Centred pipe-separated contact row. Order: phone → email →
     * address → links. Visual signature of {@code BoxedSections},
     * {@code MinimalUnderlined}.
     */
    public static void centered(SectionBuilder host, CvIdentity identity, CvTheme theme) {
        render(host, identity, theme, TextAlign.CENTER, Order.PHONE_FIRST);
    }

    /**
     * Right-aligned pipe-separated contact row. Order: address →
     * phone → email → links — address-first reads as the location
     * label authors usually put first in this style. Visual
     * signature of {@code ModernProfessional}.
     */
    public static void rightAligned(SectionBuilder host, CvIdentity identity, CvTheme theme) {
        render(host, identity, theme, TextAlign.RIGHT, Order.ADDRESS_FIRST);
    }

    /**
     * Lower-level entry. Pick the alignment and the field order
     * explicitly.
     */
    public static void render(SectionBuilder host, CvIdentity identity, CvTheme theme,
                              TextAlign alignment, Order order) {
        List<Part> parts = parts(identity, order);
        DocumentTextStyle textStyle = theme.contactStyle();
        DocumentTextStyle separatorStyle = theme.contactSeparatorStyle();
        String separator = theme.decoration().contactSeparator();

        host.spacing(0)
                .padding(theme.spacing().contactPadding())
                .addParagraph(p -> p
                        .textStyle(textStyle)
                        .align(alignment)
                        .margin(DocumentInsets.zero())
                        .rich(rich -> {
                            for (int i = 0; i < parts.size(); i++) {
                                Part part = parts.get(i);
                                if (part.link() != null) {
                                    rich.link(part.text(), part.link());
                                } else {
                                    rich.style(part.text(), textStyle);
                                }
                                if (i < parts.size() - 1) {
                                    rich.style(separator, separatorStyle);
                                }
                            }
                        }));
    }

    /** Field order in the rendered line. */
    public enum Order {
        /** phone → email → address → links. */
        PHONE_FIRST,
        /** address → phone → email → links. */
        ADDRESS_FIRST
    }

    private static List<Part> parts(CvIdentity identity, Order order) {
        CvContact c = identity.contact();
        List<Part> parts = new ArrayList<>(4 + identity.links().size());
        DocumentLinkOptions email = new DocumentLinkOptions("mailto:" + c.email());
        switch (order) {
            case PHONE_FIRST -> {
                parts.add(new Part(c.phone(), null));
                parts.add(new Part(c.email(), email));
                parts.add(new Part(c.address(), null));
            }
            case ADDRESS_FIRST -> {
                parts.add(new Part(c.address(), null));
                parts.add(new Part(c.phone(), null));
                parts.add(new Part(c.email(), email));
            }
        }
        for (CvLink link : identity.links()) {
            parts.add(new Part(link.label(), new DocumentLinkOptions(link.url())));
        }
        return parts;
    }

    private record Part(String text, DocumentLinkOptions link) {
    }
}
