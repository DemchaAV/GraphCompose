package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_BODY_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_CIRCLE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_GAP;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_ICON_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ACHIEVEMENT_TITLE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ASIDE_JOIN_INK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.ASIDE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.BULLET_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CERT_GAP;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CERT_ISSUER_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CERT_TEXT_INSET;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CERT_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.CERT_TITLE_TO_ISSUER;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.DISPLAY_FONT;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.EDUCATION_CIRCLE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.EDUCATION_ICON_COLUMN;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.EDUCATION_LINE_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.EDUCATION_LINE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.EDUCATION_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.LATO;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.MUTED;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.RULE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.SKILL_PITCH;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.gap;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.leading;
import static com.demcha.compose.document.templates.cv.presets.OrangeOpsStyles.style;

/**
 * The narrow column: skills, achievements, education and certifications, each
 * under a text-width accent rule and separated from the next by a hairline.
 *
 * <h2>Why a card is a layer stack and not a table row</h2>
 *
 * <p>An achievement and the degree are both a disc beside a block of text, and
 * a table row can put the two side by side but cannot put the disc at the
 * <em>top</em> of the pair: a cell anchors its content to the foot of the row
 * whatever anchor it is given, which leaves every disc level with the middle of
 * its card. Two layers anchored {@link LayerAlign#TOP_LEFT} — the disc with a
 * right margin that ends its box at the split, the text with a matching left
 * margin — anchor where they are asked to.</p>
 */
final class OrangeOpsAside {

    private OrangeOpsAside() {
    }

    /**
     * Draws the column.
     *
     * @param side           the aside cell
     * @param skills         the skill list, or {@code null}
     * @param achievements   the achievement cards, or {@code null}
     * @param education      the degree, or {@code null}
     * @param certifications the certification list, or {@code null}
     */
    static void compose(SectionBuilder side, SkillsSection skills, EntriesSection achievements,
                        EntriesSection education, EntriesSection certifications) {
        List<OrangeOpsWidgets.Block> blocks = new ArrayList<>();
        if (SectionLookup.hasContent(skills)) {
            blocks.add(new OrangeOpsWidgets.Block("Skills",
                    column -> renderSkills(column, skills), LATO, BODY_SIZE, false));
        }
        if (SectionLookup.hasContent(achievements)) {
            blocks.add(new OrangeOpsWidgets.Block("Achievements",
                    column -> renderAchievements(column, achievements),
                    LATO, ACHIEVEMENT_BODY_SIZE, false));
        }
        if (SectionLookup.hasContent(education)) {
            blocks.add(new OrangeOpsWidgets.Block("Education",
                    column -> renderEducation(column, education),
                    LATO, EDUCATION_LINE_SIZE, false));
        }
        if (SectionLookup.hasContent(certifications)) {
            blocks.add(new OrangeOpsWidgets.Block("Certifications",
                    column -> renderCertifications(column, certifications),
                    LATO, CERT_ISSUER_SIZE, false));
        }
        OrangeOpsWidgets.stack(side, blocks, ASIDE_JOIN_INK);
    }

    /**
     * The skills — one paragraph per name, opened by an inline accent dot.
     *
     * <p>Not a list: a list marker takes the list's own text colour, and here
     * the dots are accent against charcoal text. The design draws one flat run
     * of names, so a section that groups its skills has its groups flattened in
     * the order they were given; the group names are not drawn.</p>
     */
    private static void renderSkills(SectionBuilder side, SkillsSection skills) {
        side.addSection("Skills", block -> {
            double itemGap = gap(SKILL_PITCH, LATO.lineBox(BODY_SIZE));
            block.spacing(itemGap);
            OrangeOpsWidgets.asideHeading(block, "Skills", skills.title());
            List<String> names = new ArrayList<>();
            for (SkillGroup group : skills.groups()) {
                for (CvSkill skill : group.entries()) {
                    names.add(skill.name());
                }
            }
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                int index = i;
                // The section's own spacing is the item pitch, which is tighter
                // than the gap under the accent rule; the first item makes up
                // the difference rather than the rule being nudged.
                float lead = i == 0 ? (float) Math.max(0.0, RULE_TO_BODY - itemGap) : 0f;
                block.addParagraph(p -> p
                        .name("Skill" + index)
                        .lineSpacing(0)
                        .textStyle(style(BODY_FONT, BODY_SIZE, BODY, false))
                        .dot(BULLET_SIZE, ACCENT)
                        .inlineText("    ", style(BODY_FONT, BODY_SIZE, BODY, false))
                        .inlineText(name, style(BODY_FONT, BODY_SIZE, BODY, false))
                        .margin(lead, 0f, 0f, 0f));
            }
        });
    }

    /** The achievements — an accent disc beside a display-face title over its body. */
    private static void renderAchievements(SectionBuilder side, EntriesSection achievements) {
        side.addSection("Achievements", block -> {
            block.spacing(0);
            OrangeOpsWidgets.asideHeading(block, "Achievements", achievements.title());
            List<CvEntry> entries = achievements.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                int index = i;
                boolean first = i == 0;
                boolean last = i == entries.size() - 1;

                SectionBuilder text = new SectionBuilder();
                text.name("AchievementText" + index);
                text.spacing(0);
                text.margin(new DocumentInsets(0, ACHIEVEMENT_TEXT_INSET, 0,
                        ACHIEVEMENT_ICON_COLUMN));
                text.addParagraph(p -> {
                    p.name("AchievementTitle" + index);
                    p.lineSpacing(0);
                    p.textStyle(style(DISPLAY_FONT, ACHIEVEMENT_TITLE_SIZE, ACCENT, true));
                    if (entry.link().isBlank()) {
                        p.inlineText(entry.title(),
                                style(DISPLAY_FONT, ACHIEVEMENT_TITLE_SIZE, ACCENT, true));
                    } else {
                        p.inlineText(entry.title(),
                                style(DISPLAY_FONT, ACHIEVEMENT_TITLE_SIZE, ACCENT, true),
                                new DocumentLinkOptions(entry.link()));
                    }
                    p.margin(0f, 0f, (float) ACHIEVEMENT_TITLE_TO_BODY, 0f);
                });
                text.addParagraph(p -> p
                        .name("AchievementBody" + index)
                        .text(entry.body())
                        .lineSpacing(leading(ACHIEVEMENT_BODY_PITCH, ACHIEVEMENT_BODY_SIZE))
                        .textStyle(style(BODY_FONT, ACHIEVEMENT_BODY_SIZE, BODY, false)));

                block.addLayerStack(stack -> {
                    stack.name("AchievementCard" + index);
                    stack.margin(new DocumentInsets(
                            first ? RULE_TO_BODY : 0, 0, last ? 0 : ACHIEVEMENT_GAP, 0));
                    if (!entry.icon().isBlank()) {
                        stack.layer(badgeCell("AchievementBadge" + index, entry.icon(),
                                ACHIEVEMENT_CIRCLE, ACHIEVEMENT_ICON_COLUMN, ACCENT),
                                LayerAlign.TOP_LEFT, 0);
                    }
                    stack.layer(text.build(), LayerAlign.TOP_LEFT, 1);
                });
            }
        });
    }

    /**
     * The degree — the same disc-beside-text card, in ink rather than accent.
     *
     * <p>The design shows one degree, so the first entry is the one drawn. Its
     * body carries the institution, the place and the years, one to a line.</p>
     */
    private static void renderEducation(SectionBuilder side, EntriesSection education) {
        side.addSection("Education", block -> {
            block.spacing(0);
            OrangeOpsWidgets.asideHeading(block, "Education", education.title());
            CvEntry entry = education.entries().get(0);

            SectionBuilder text = new SectionBuilder();
            text.name("EducationText");
            text.spacing(0);
            text.margin(new DocumentInsets(0, 0, 0, EDUCATION_ICON_COLUMN));
            text.addParagraph(p -> {
                p.name("EducationDegree");
                p.lineSpacing(leading(EDUCATION_LINE_PITCH, EDUCATION_TITLE_SIZE));
                p.textStyle(style(BODY_FONT, EDUCATION_TITLE_SIZE, INK, true));
                if (entry.link().isBlank()) {
                    p.inlineText(entry.title(), style(BODY_FONT, EDUCATION_TITLE_SIZE, INK, true));
                } else {
                    p.inlineText(entry.title(), style(BODY_FONT, EDUCATION_TITLE_SIZE, INK, true),
                            new DocumentLinkOptions(entry.link()));
                }
            });
            List<String> detail = OrangeOpsWidgets.lines(entry.body());
            for (int i = 0; i < detail.size(); i++) {
                String line = detail.get(i);
                int index = i;
                text.addParagraph(p -> p
                        .name("EducationLine" + index)
                        .text(line)
                        .lineSpacing(0)
                        .textStyle(style(BODY_FONT, EDUCATION_LINE_SIZE, MUTED, false))
                        .margin((float) gap(EDUCATION_LINE_PITCH,
                                LATO.lineBox(EDUCATION_LINE_SIZE)), 0f, 0f, 0f));
            }

            block.addLayerStack(stack -> {
                stack.name("EducationEntry");
                stack.margin(new DocumentInsets(RULE_TO_BODY, 0, 0, 0));
                if (!entry.icon().isBlank()) {
                    stack.layer(badgeCell("EducationBadge", entry.icon(), EDUCATION_CIRCLE,
                            EDUCATION_ICON_COLUMN, INK), LayerAlign.TOP_LEFT, 0);
                }
                stack.layer(text.build(), LayerAlign.TOP_LEFT, 1);
            });
        });
    }

    /**
     * The certifications — an accent-dotted title over a muted issuer line.
     *
     * <p>The issuer is padded to the title's text left edge rather than to the
     * dot's, so it hangs under the title the way the design shows.</p>
     */
    private static void renderCertifications(SectionBuilder side, EntriesSection certifications) {
        side.addSection("Certifications", block -> {
            block.spacing(0);
            OrangeOpsWidgets.asideHeading(block, "Certifications", certifications.title());
            List<CvEntry> entries = certifications.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                int index = i;
                boolean first = i == 0;
                boolean last = i == entries.size() - 1;
                block.addParagraph(p -> {
                    p.name("CertTitle" + index);
                    p.lineSpacing(0);
                    p.textStyle(style(BODY_FONT, CERT_TITLE_SIZE, INK, true));
                    p.dot(BULLET_SIZE, ACCENT);
                    p.inlineText("    ", style(BODY_FONT, CERT_TITLE_SIZE, INK, true));
                    if (entry.link().isBlank()) {
                        p.inlineText(entry.title(), style(BODY_FONT, CERT_TITLE_SIZE, INK, true));
                    } else {
                        p.inlineText(entry.title(), style(BODY_FONT, CERT_TITLE_SIZE, INK, true),
                                new DocumentLinkOptions(entry.link()));
                    }
                    p.margin((float) (first ? RULE_TO_BODY : 0), 0f,
                            (float) CERT_TITLE_TO_ISSUER, 0f);
                });
                block.addParagraph(p -> p
                        .name("CertIssuer" + index)
                        .text(entry.subtitle())
                        .lineSpacing(0)
                        .textStyle(style(BODY_FONT, CERT_ISSUER_SIZE, MUTED, false))
                        .padding(0f, 0f, 0f, (float) CERT_TEXT_INSET)
                        .margin(0f, 0f, (float) (last ? 0 : CERT_GAP), 0f));
            }
        });
    }

    /**
     * A badge in a box that ends where the text column starts, so the two
     * layers of a card meet on the split rather than overlapping.
     */
    private static DocumentNode badgeCell(String name, String token, double diameter,
                                          double column, DocumentColor fill) {
        SectionBuilder cell = new SectionBuilder();
        cell.name(name + "Cell");
        cell.spacing(0);
        cell.margin(new DocumentInsets(0, ASIDE_WIDTH - column, 0, 0));
        cell.add(OrangeOpsWidgets.badge(name, token, diameter, fill));
        return cell.build();
    }
}
