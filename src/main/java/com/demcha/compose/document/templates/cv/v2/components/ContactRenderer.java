package com.demcha.compose.document.templates.cv.v2.components;

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
 * Draws the centred pipe-separated contact row under the headline:
 * phone → email (clickable mailto) → address → optional labelled
 * links in source order, each rendered as an active hyperlink.
 *
 * <p>Required contact fields are guaranteed non-blank by
 * {@link CvContact} so the helper never needs to skip-on-empty for
 * them. Optional {@link CvLink} entries are rendered if present and
 * silently omitted if not — that's the whole point of the
 * {@code .link(...)} builder method.</p>
 */
public final class ContactRenderer {

    private ContactRenderer() {
    }

    public static void render(SectionBuilder section, CvIdentity identity, CvTheme theme) {
        List<Part> parts = parts(identity);
        DocumentTextStyle textStyle = theme.contactStyle();
        DocumentTextStyle separatorStyle = theme.contactSeparatorStyle();

        section.spacing(0)
                .padding(theme.spacing().contactPadding())
                .addParagraph(p -> p
                        .textStyle(textStyle)
                        .align(TextAlign.CENTER)
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
                                    rich.style(theme.decoration().contactSeparator(),
                                            separatorStyle);
                                }
                            }
                        }));
    }

    private static List<Part> parts(CvIdentity identity) {
        CvContact contact = identity.contact();
        List<Part> parts = new ArrayList<>(4 + identity.links().size());
        parts.add(new Part(contact.phone(), null));
        parts.add(new Part(contact.email(),
                new DocumentLinkOptions("mailto:" + contact.email())));
        parts.add(new Part(contact.address(), null));
        for (CvLink link : identity.links()) {
            parts.add(new Part(link.label(), new DocumentLinkOptions(link.url())));
        }
        return parts;
    }

    private record Part(String text, DocumentLinkOptions link) {
    }
}
