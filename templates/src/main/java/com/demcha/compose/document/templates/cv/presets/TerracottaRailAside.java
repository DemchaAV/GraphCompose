package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.ACCENT_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.COLUMN_PAD_BOTTOM;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.CONTACT_ROW_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INFO_BLOCK_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INFO_LINE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INFO_WEIGHTS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MONOGRAM_RULE_TO_CONTACT_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MONOGRAM_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MONOGRAM_SIZE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.MONOGRAM_TO_RULE_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_DASH_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_DIVIDER_GAP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_HEADING_SPACER;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.SIDEBAR_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.serif;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailStyles.text;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.bulletLine;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.divider;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.heading;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.headingWithDash;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.inlineIcon;
import static com.demcha.compose.document.templates.cv.presets.TerracottaRailWidgets.layeredRow;

/**
 * The narrow column: the monogram, the contact channels, the two bulleted
 * lists, the credentials and the closing facts.
 */
final class TerracottaRailAside {

    private TerracottaRailAside() {
    }

    static void compose(SectionBuilder side, CvIdentity identity, SkillsSection competencies,
                        SkillsSection software, EntriesSection certifications,
                        EntriesSection facts) {
        side.name("Sidebar");
        side.spacing(0);
        side.accentRight(RULE, RULE_THICKNESS);
        side.padding((float) SIDEBAR_PAD_TOP, (float) SIDEBAR_PAD_RIGHT,
                (float) COLUMN_PAD_BOTTOM, (float) SIDEBAR_PAD_LEFT);

        renderMonogram(side, identity);
        renderContact(side, identity);

        if (hasSkills(competencies)) {
            divider(side, "AfterContact", SIDEBAR_DIVIDER_GAP, SIDEBAR_DIVIDER_GAP);
            renderBulletedSkills(side, competencies, "CoreCompetencies", "Competency", false);
        }
        if (hasSkills(software)) {
            divider(side, "AfterCompetencies", SIDEBAR_DIVIDER_GAP, SIDEBAR_DIVIDER_GAP);
            renderBulletedSkills(side, software, "Software", "Software", true);
        }
        if (hasEntries(certifications)) {
            divider(side, "AfterSoftware", SIDEBAR_DIVIDER_GAP, SIDEBAR_DIVIDER_GAP);
            renderCertifications(side, certifications);
        }
        if (hasEntries(facts)) {
            divider(side, "AfterCertifications", SIDEBAR_DIVIDER_GAP, SIDEBAR_DIVIDER_GAP);
            renderFacts(side, facts);
        }
    }

    // -- the lockup --------------------------------------------------------

    /**
     * The monogram over its terracotta rule. It is drawn from the name's own
     * initials rather than a field of its own — a document states the name
     * once, and a monogram that could disagree with it would be a second
     * place to keep true.
     */
    private static void renderMonogram(SectionBuilder side, CvIdentity identity) {
        side.addParagraph(p -> p
                .name("Monogram")
                .text(monogram(identity))
                .textStyle(serif(MONOGRAM_SIZE, ACCENT))
                .margin(0f, 0f, (float) MONOGRAM_TO_RULE_GAP, 0f));
        side.addLine(line -> line
                .name("MonogramRule")
                .horizontal(MONOGRAM_RULE_WIDTH)
                .thickness(ACCENT_RULE_THICKNESS)
                .color(ACCENT)
                .margin(new DocumentInsets(0, 0, MONOGRAM_RULE_TO_CONTACT_GAP, 0)));
    }

    /** The initials of the given and the family name, in capitals. */
    private static String monogram(CvIdentity identity) {
        StringBuilder out = new StringBuilder(2);
        appendInitial(out, identity.name().first());
        appendInitial(out, identity.name().last());
        return out.toString();
    }

    private static void appendInitial(StringBuilder out, String part) {
        if (!part.isBlank()) {
            out.append(Character.toUpperCase(part.charAt(0)));
        }
    }

    // -- the channels ------------------------------------------------------

    /**
     * The contact block: the three channels and a row per link, all on one
     * axis behind their marks.
     *
     * <p>A link shows its own label and carries the address behind it, which
     * is what keeps the rows the same. Writing the URL out would make that row
     * as wide as whatever the reader's profile happens to be called — long
     * enough to need setting smaller than the rows above it, and a different
     * width for every document.</p>
     */
    private static void renderContact(SectionBuilder side, CvIdentity identity) {
        side.addSection("Contact", block -> {
            block.spacing(0);
            // The contact triple is non-blank by construction, so the three
            // channels are always drawn; only the links are optional.
            Contact contact = identity.contact();
            channel(block, 0, TerracottaRailIcons.EMAIL, contact.email(),
                    "mailto:" + contact.email());
            channel(block, 1, TerracottaRailIcons.PHONE, contact.phone(),
                    telUri(contact.phone()));
            channel(block, 2, TerracottaRailIcons.LOCATION, contact.address(), null);
            int index = 3;
            for (Link link : identity.links()) {
                channel(block, index++, markFor(link), link.label(), link.url());
            }
        });
    }

    private static void channel(SectionBuilder block, int index, String token, String value,
                                String href) {
        block.addParagraph(p -> {
            p.name("Contact_" + index);
            inlineIcon(p, token, TerracottaRailIcons.CONTACT_SIZE);
            p.inlineText("  ");
            if (href == null || href.isBlank()) {
                p.inlineText(value, text(DETAIL_SIZE, INK, false));
            } else {
                p.inlineText(value, text(DETAIL_SIZE, INK, false),
                        new DocumentLinkOptions(href));
            }
            p.margin(0f, 0f, (float) CONTACT_ROW_GAP, 0f);
        });
    }

    /** A link takes the mark of the network it points at, or a globe. */
    private static String markFor(Link link) {
        String target = (link.url() + " " + link.label()).toLowerCase(Locale.ROOT);
        return target.contains("linkedin")
                ? TerracottaRailIcons.LINKEDIN
                : TerracottaRailIcons.GLOBE;
    }

    /**
     * The dial target for a phone number: its digits, keeping a leading
     * {@code +} so an international number stays international, and dropping
     * a parenthesised trunk prefix the way a caller dialling from abroad
     * drops it.
     */
    private static String telUri(String phone) {
        String dialled = phone.replaceAll("\\(0+\\)", "");
        String digits = dialled.replaceAll("[^0-9]", "");
        return digits.isEmpty()
                ? null
                : "tel:" + (phone.trim().startsWith("+") ? "+" : "") + digits;
    }

    // -- the lists ---------------------------------------------------------

    /**
     * A bulleted list of skills. The competencies take a terracotta square
     * and no dash under their heading; the software list takes a disc and a
     * dash — the design's own way of telling two lists of one-liners apart.
     */
    private static void renderBulletedSkills(SectionBuilder side, SkillsSection section,
                                             String blockName, String prefix,
                                             boolean discBullet) {
        side.addSection(blockName, block -> {
            block.spacing(0);
            if (discBullet) {
                headingWithDash(block, section.title(), SIDEBAR_HEADING_SPACER,
                        SIDEBAR_DASH_WIDTH);
            } else {
                heading(block, section.title(), SIDEBAR_HEADING_SPACER);
            }
            int index = 0;
            for (SkillGroup group : section.groups()) {
                for (CvSkill skill : group.entries()) {
                    String name = prefix + "_" + index++;
                    if (discBullet) {
                        bulletLine(block, name, p -> p.dot(3.0, ACCENT), skill.name(),
                                INK, DETAIL_SIZE, null);
                    } else {
                        bulletLine(block, name,
                                p -> inlineIcon(p, TerracottaRailIcons.SQUARE,
                                        TerracottaRailIcons.BULLET_SIZE),
                                skill.name(), INK, DETAIL_SIZE, null);
                    }
                }
            }
        });
    }

    /** The credentials: one disc-bulleted line each. */
    private static void renderCertifications(SectionBuilder side, EntriesSection section) {
        side.addSection("Certifications", block -> {
            block.spacing(0);
            headingWithDash(block, section.title(), SIDEBAR_HEADING_SPACER, SIDEBAR_DASH_WIDTH);
            List<CvEntry> entries = section.entries();
            for (int index = 0; index < entries.size(); index++) {
                CvEntry entry = entries.get(index);
                bulletLine(block, "Certification_" + index, p -> p.dot(3.0, ACCENT),
                        entry.title(), INK, DETAIL_SIZE, entry.link());
            }
        });
    }

    /**
     * The closing facts: a mark beside a label and the lines under it.
     *
     * <p>The mark is an inline run inside a paragraph rather than a block
     * icon node: a block icon in a row cell makes the whole row lay its cells
     * out vertically.</p>
     */
    private static void renderFacts(SectionBuilder side, EntriesSection section) {
        side.addSection("AdditionalInformation", block -> {
            block.spacing(0);
            heading(block, section.title(), SIDEBAR_HEADING_SPACER);
            List<CvEntry> entries = section.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                int index = i;
                layeredRow(block, "AddInfo_" + index, 0.0, INFO_BLOCK_GAP, row -> {
                    row.weights(INFO_WEIGHTS[0], INFO_WEIGHTS[1]);
                    row.addParagraph(p -> {
                        p.name("Icon_" + index);
                        if (!entry.icon().isBlank()) {
                            inlineIcon(p, entry.icon(), TerracottaRailIcons.FACT_SIZE);
                        }
                    });
                    row.addSection("Text_" + index, cell -> {
                        cell.spacing(0);
                        cell.addParagraph(p -> p
                                .name("Label_" + index)
                                .text(entry.title())
                                .textStyle(text(DETAIL_SIZE, INK, true))
                                .margin(0f, 0f, (float) INFO_LINE_GAP, 0f));
                        for (String line : lines(entry.body())) {
                            cell.addParagraph(p -> p
                                    .name("Val_" + index)
                                    .text(line)
                                    .textStyle(text(DETAIL_SIZE, INK, false))
                                    .margin(0f, 0f, (float) INFO_LINE_GAP, 0f));
                        }
                    });
                });
            }
        });
    }

    /** A fact's body, one line per line the document wrote. */
    private static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String line : body.split(String.valueOf((char) 10))) {
            if (!line.isBlank()) {
                out.add(line.strip());
            }
        }
        return out;
    }

    // -- presence ----------------------------------------------------------

    private static boolean hasSkills(SkillsSection section) {
        if (section == null) {
            return false;
        }
        return section.groups().stream().anyMatch(group -> !group.entries().isEmpty());
    }

    private static boolean hasEntries(EntriesSection section) {
        return section != null && !section.entries().isEmpty();
    }
}
