package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvRow;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.RowsSection;

import java.util.List;

import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_BODY_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_GLYPH_BOX;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_GLYPH_HANG;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_GLYPH_TO_TEXT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_SEPARATOR_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_TEXT_DROP;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ACHIEVEMENT_TITLE_TO_BODY;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CERTIFICATIONS_TO_ACHIEVEMENTS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CERT_GLYPH_TO_TEXT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CERT_LINE_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CERT_PLATE_DIAMETER;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CERT_SEPARATOR_HEIGHT;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.CONTENT_WIDTH;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.GLYPH_CLEARANCE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_TO_ACHIEVEMENTS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.HEADING_TO_CERTIFICATIONS;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.INK;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.ITEM_TITLE_SIZE;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.MARGIN;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.PLATE_HANG;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.TIGHT_LEADING;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.compact;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.small;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineStyles.style;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.BandColumn;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.columnBand;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.plate;
import static com.demcha.compose.document.templates.cv.presets.SerifHeadlineWidgets.sectionHeading;

/**
 * The two full-width bands that close the sheet: the certifications behind
 * their medals, and the achievements behind their own marks.
 */
final class SerifHeadlineClosing {

    private SerifHeadlineClosing() {
    }

    /**
     * The certifications: a plate, then the qualification over who issued it.
     *
     * <p>Every plate takes the same medal. It is the one mark on this sheet
     * that is chrome rather than data — a certification is a certification —
     * so the rows are {@link CvRow}s and carry no icon token.</p>
     */
    static void renderCertifications(PageFlowBuilder page, RowsSection certifications,
                                     boolean achievementsFollow) {
        List<CvRow> rows = certifications.rows();
        page.addSection("Certifications", section -> {
            section.spacing(0).padding(DocumentInsets.zero())
                    .margin(new DocumentInsets(0, 0,
                            achievementsFollow ? CERTIFICATIONS_TO_ACHIEVEMENTS : 0, 0));
            sectionHeading(section, certifications.title(), CONTENT_WIDTH, true, 0.0, MARGIN);
            columnBand(section, "Certifications", CONTENT_WIDTH, rows.size(),
                    HEADING_TO_CERTIFICATIONS, CERT_SEPARATOR_HEIGHT,
                    index -> certificationColumn(rows.get(index), index));
        });
    }

    private static BandColumn certificationColumn(CvRow certification, int index) {
        double lead = index == 0 ? PLATE_HANG : GLYPH_CLEARANCE;
        SectionBuilder column = new SectionBuilder();
        column.name("Certification_" + compact(certification.label()))
                .spacing(0)
                .padding(new DocumentInsets(0, 0, 0, lead + CERT_GLYPH_TO_TEXT));
        column.addParagraph(p -> p
                .name("CertificationTitle_" + compact(certification.label()))
                .text(certification.label())
                .textStyle(small())
                .lineSpacing(CERT_LINE_LEADING)
                .margin(DocumentInsets.zero()));
        column.addParagraph(p -> p
                .name("CertificationIssuer_" + compact(certification.label()))
                .text(certification.body())
                .textStyle(small())
                .lineSpacing(CERT_LINE_LEADING)
                .margin(DocumentInsets.zero()));
        return new BandColumn(column,
                plate("CertificationPlate_" + compact(certification.label()),
                        SerifHeadlineIcons.MEDAL, CERT_PLATE_DIAMETER),
                lead);
    }

    /**
     * The achievements: the entry's own mark, drawn bare rather than on a
     * plate, beside the title and what it was for.
     */
    static void renderAchievements(PageFlowBuilder page, EntriesSection achievements) {
        List<CvEntry> entries = achievements.entries();
        page.addSection("Achievements", section -> {
            section.spacing(0).padding(DocumentInsets.zero());
            sectionHeading(section, achievements.title(), CONTENT_WIDTH, true, 0.0, MARGIN);
            columnBand(section, "Achievements", CONTENT_WIDTH, entries.size(),
                    HEADING_TO_ACHIEVEMENTS, ACHIEVEMENT_SEPARATOR_HEIGHT,
                    index -> achievementColumn(entries.get(index), index));
        });
    }

    private static BandColumn achievementColumn(CvEntry achievement, int index) {
        double lead = index == 0 ? ACHIEVEMENT_GLYPH_HANG : GLYPH_CLEARANCE;
        SectionBuilder column = new SectionBuilder();
        column.name("Achievement_" + compact(achievement.title()))
                .spacing(0)
                // The text drops a little, because the mark is taller than the
                // title it stands beside and the two centre on each other.
                .padding(new DocumentInsets(
                        ACHIEVEMENT_TEXT_DROP, 0, 0, lead + ACHIEVEMENT_GLYPH_TO_TEXT));
        column.addParagraph(p -> {
            p.name("AchievementTitle_" + compact(achievement.title()))
                    .textStyle(style(ITEM_TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                    .lineSpacing(TIGHT_LEADING);
            SerifHeadlineText.title(p, achievement,
                    style(ITEM_TITLE_SIZE, INK, DocumentTextDecoration.BOLD));
            p.margin(new DocumentInsets(0, 0, ACHIEVEMENT_TITLE_TO_BODY, 0));
        });
        column.addParagraph(p -> p
                .name("AchievementBody_" + compact(achievement.title()))
                .text(achievement.body())
                .textStyle(small())
                .lineSpacing(ACHIEVEMENT_BODY_LEADING)
                .margin(DocumentInsets.zero()));
        DocumentNode mark = achievement.icon().isBlank()
                ? null
                : new ImageBuilder()
                        .name("AchievementGlyph_" + compact(achievement.title()))
                        .source(SerifHeadlineIcons.image(achievement.icon()))
                        .fitToBounds(ACHIEVEMENT_GLYPH_BOX, ACHIEVEMENT_GLYPH_BOX)
                        .margin(DocumentInsets.zero())
                        .build();
        return new BandColumn(column, mark, lead);
    }
}
