package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.components.SectionLookup;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ACCENT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.DATE_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.DETAIL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.EMPLOYER_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.EMPLOYER_TO_HIGHLIGHTS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ENTRY_GAP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ENTRY_INDENT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ENTRY_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.EXPERIENCE_TO_ENTRIES;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.HIGHLIGHT_ITEM_GAP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.HIGHLIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MAIN_PAD_LEFT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MAIN_PAD_RIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MAIN_PAD_TOP;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MARKER_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MARKER_WEIGHT;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MASTHEAD_RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MASTHEAD_RULE_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.MASTHEAD_TO_TITLE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.NAME_FAMILY_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.NAME_GIVEN_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.JOB_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ROLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.ROLE_TO_EMPLOYER;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE_THICKNESS;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.RULE_TO_SUMMARY;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SMALL_SIZE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SUMMARY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.SUMMARY_TO_EXPERIENCE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.TITLE_TO_RULE;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.textStyle;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldStyles.tracked;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.layeredRow;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.mainHeading;
import static com.demcha.compose.document.templates.cv.presets.CharcoalGoldWidgets.marker;

/**
 * The paper right column: the two-tone name over its tracked role, the
 * summary, and the roles held on a dated rail.
 */
final class CharcoalGoldMain {

    private CharcoalGoldMain() {
    }

    static void compose(SectionBuilder main,
                        CvIdentity identity,
                        ParagraphSection summary,
                        EntriesSection experience) {
        main.name("MainColumn");
        main.spacing(0);
        main.padding((float) MAIN_PAD_TOP, (float) MAIN_PAD_RIGHT, 0f, (float) MAIN_PAD_LEFT);
        renderMasthead(main, identity);
        if (SectionLookup.hasContent(summary)) {
            renderSummary(main, summary);
        }
        if (SectionLookup.hasContent(experience)) {
            renderExperience(main, experience);
        }
    }

    // -- masthead --------------------------------------------------------

    /**
     * The name in two tones and two sizes — the given name in ink, the
     * family name larger and in gold — then the role tracked out, and the
     * short gold rule that closes the block.
     *
     * <p>The two halves come from {@code CvName}, which is why this preset
     * wants a structured name rather than one string: nothing else could
     * tell it where to change colour.</p>
     */
    private static void renderMasthead(SectionBuilder main, CvIdentity identity) {
        main.addSection("Masthead", block -> {
            block.spacing(0);
            block.addParagraph(p -> p
                    .name("GivenName")
                    .text(identity.name().first())
                    .textStyle(textStyle(NAME_GIVEN_SIZE, INK, false))
                    .lineSpacing(1.0));
            block.addParagraph(p -> p
                    .name("FamilyName")
                    .text(identity.name().last())
                    .textStyle(textStyle(NAME_FAMILY_SIZE, ACCENT, false))
                    .lineSpacing(1.0)
                    .margin(0f, 0f, (float) MASTHEAD_TO_TITLE, 0f));
            block.addParagraph(p -> p
                    .name("JobTitle")
                    .text(tracked(identity.jobTitle()))
                    .textStyle(textStyle(JOB_TITLE_SIZE, INK, false))
                    .margin(0f, 0f, (float) TITLE_TO_RULE, 0f));
            block.addLine(line -> line
                    .name("MastheadRule")
                    .horizontal(MASTHEAD_RULE_WIDTH)
                    .thickness(MASTHEAD_RULE_THICKNESS)
                    .color(ACCENT));
        });
    }

    // -- summary ---------------------------------------------------------

    /**
     * The opening block, one paragraph per line of the section's body. This
     * design gives it no heading, so the section's title is not drawn.
     */
    private static void renderSummary(SectionBuilder main, ParagraphSection summary) {
        main.addSection("Summary", block -> {
            block.spacing(0);
            block.margin((float) RULE_TO_SUMMARY, 0f, 0f, 0f);
            List<String> paragraphs = CharcoalGoldText.lines(summary.body());
            for (int i = 0; i < paragraphs.size(); i++) {
                int index = i;
                block.addParagraph(p -> p
                        .name("Summary_" + index)
                        .text(paragraphs.get(index))
                        .textStyle(textStyle(BODY_SIZE, INK, false))
                        .lineSpacing(SUMMARY_LEADING));
            }
        });
    }

    // -- experience ------------------------------------------------------

    /**
     * The roles held: the dates in gold at the left, and the entry itself
     * behind a hairline rail with a ringed marker capping each role.
     */
    private static void renderExperience(SectionBuilder main, EntriesSection experience) {
        main.addSection("Experience", block -> {
            block.spacing(0);
            block.margin((float) SUMMARY_TO_EXPERIENCE, 0f, 0f, 0f);
            block.keepTogether();
            mainHeading(block, experience.title());
            List<CvEntry> entries = experience.entries();
            for (int i = 0; i < entries.size(); i++) {
                CvEntry entry = entries.get(i);
                boolean first = i == 0;
                boolean last = i == entries.size() - 1;
                int index = i;
                layeredRow(block, "ExperienceEntry_" + index,
                        first ? EXPERIENCE_TO_ENTRIES : 0.0, 0.0,
                        row -> {
                            row.spacing(0);
                            // Two cells, not three. Giving the marker a column
                            // of its own would put it in a different node from
                            // the rail, leaving the rail free to paint straight
                            // through it; the date column carries that width
                            // instead, so the body's left border lands in
                            // exactly the same place.
                            row.weights(DATE_WEIGHT + MARKER_WEIGHT, ENTRY_WEIGHT);
                            row.addParagraph(p -> p
                                    .name("Period_" + index)
                                    .text(entry.date())
                                    .textStyle(textStyle(DETAIL_SIZE, ACCENT, false)));
                            row.addSection("EntryBody_" + index, body ->
                                    renderEntryBody(body, entry, index, last));
                        });
            }
        });
    }

    private static void renderEntryBody(SectionBuilder body, CvEntry entry,
                                        int index, boolean last) {
        body.spacing(0);
        body.accentLeft(RULE, RULE_THICKNESS);
        body.padding(0f, 0f, last ? 0f : (float) ENTRY_GAP, (float) ENTRY_INDENT);
        renderRoleWithMarker(body, entry, index);
        body.addParagraph(p -> p
                .name("Employer_" + index)
                .text(employerLine(entry))
                .textStyle(textStyle(EMPLOYER_SIZE, INK, true))
                .margin(0f, 0f, (float) EMPLOYER_TO_HIGHLIGHTS, 0f));
        List<String> highlights = CharcoalGoldText.lines(entry.body());
        if (!highlights.isEmpty()) {
            body.addList(list -> list
                    .name("Highlights_" + index)
                    .items(highlights)
                    .marker(ListMarker.bullet())
                    .textStyle(textStyle(SMALL_SIZE, INK, false))
                    .itemSpacing(HIGHLIGHT_ITEM_GAP)
                    .lineSpacing(HIGHLIGHT_LEADING));
        }
    }

    /** The employer, and the place beside it behind a raised dot. */
    private static String employerLine(CvEntry entry) {
        return entry.place().isBlank()
                ? entry.subtitle()
                : entry.subtitle() + "   ·   " + entry.place();
    }

    /**
     * The role, with the ring positioned back over the rail beside it.
     *
     * <p>The marker is placed from the title's own layer stack rather than
     * from the row, so it sits on the role's first line whatever the title
     * measures — and half its diameter left of the body's border, which puts
     * the ring centred on the rail.</p>
     */
    private static void renderRoleWithMarker(SectionBuilder body, CvEntry entry, int index) {
        SectionBuilder titleLayer = new SectionBuilder();
        titleLayer.name("RoleLayer_" + index);
        titleLayer.spacing(0);
        titleLayer.addParagraph(p -> {
            p.name("Role_" + index).textStyle(textStyle(ROLE_SIZE, INK, true));
            CharcoalGoldText.title(p, entry, textStyle(ROLE_SIZE, INK, true));
        });
        body.addLayerStack(stack -> stack
                .name("MarkerCap_" + index)
                .margin(new DocumentInsets(0, 0, ROLE_TO_EMPLOYER, 0))
                .layer(titleLayer.build(), LayerAlign.TOP_LEFT, 0)
                .position(marker(index),
                        -(ENTRY_INDENT + MARKER_DIAMETER / 2.0), 0.0,
                        LayerAlign.TOP_LEFT, 1));
    }
}
