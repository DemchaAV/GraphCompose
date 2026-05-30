package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.components.SectionLookup;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvEntry;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.CvSkill;
import com.demcha.compose.document.templates.cv.v2.data.EntriesSection;
import com.demcha.compose.document.templates.cv.v2.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.SkillGroup;
import com.demcha.compose.document.templates.cv.v2.data.SkillsSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.IconTextRow;
import com.demcha.compose.document.templates.cv.v2.widgets.SkillBar;
import com.demcha.compose.document.templates.cv.v2.widgets.Subheadline;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v2 "Mint Editorial" CV preset — a two-page, two-column editorial resume
 * ported from the flat canonical {@code MintEditorialCvTemplate}.
 *
 * <p>Visual signature: a centred spaced-caps name with a soft mint accent
 * tagline, a full-width 6pt mint accent rule, then two pages that each lay
 * out a narrow left sidebar (weight {@value #SIDEBAR_WEIGHT}) beside a wide
 * main column. Page 1 sidebar carries Contact (icon rows), Interests and
 * Education; page 1 main carries Profile and the first slice of Experience.
 * Page 2 sidebar carries Expertise (badge + label list), level-driven Skill
 * bars and Social rows; page 2 main carries the rest of Experience plus
 * Awards and References two-column grids. Poppins throughout; the mint
 * accent lives in {@code theme.palette().banner()}.</p>
 *
 * <h2>Atomic-row partitioning</h2>
 *
 * <p>Each page's sidebar+main grid is a single {@code addRow}, which the
 * canonical paginator treats as <strong>atomic</strong> — the whole row must
 * fit on one page or it raises {@code AtomicNodeTooLargeException} (rows do
 * not page-break, and a row cannot host a table directly, so the Awards /
 * References tables live inside the main <em>section</em> column, not the row).
 * To keep each page's row inside the page bound the preset slices Experience
 * across the two pages ({@value #EXPERIENCE_PAGE_ONE} entries on page 1, the
 * remainder on page 2), mirroring the blueprint's
 * {@code experiencePage1 / experiencePage2} split.</p>
 *
 * <p>The two rows reach two pages by <strong>natural atomic overflow</strong>,
 * not an explicit page break: the page-1 row fills page 1, so the page-2 row —
 * being atomic — moves whole onto page 2. An explicit {@code addPageBreak} is
 * deliberately avoided because a break landing on an already-full page-1
 * emits a blank intermediate page.</p>
 *
 * <h2>Section mapping &amp; graceful degradation</h2>
 *
 * <p>Sections are resolved from {@link Slot#MAIN} by title keyword via
 * {@link SectionLookup}; the canonical sample places everything in MAIN.
 * Mappings:</p>
 *
 * <ul>
 *   <li><b>Contact</b> ← {@link CvIdentity#contact()} (phone / email /
 *       address) + {@link CvIdentity#links()} as icon rows.</li>
 *   <li><b>Interests</b> ← a {@link RowsSection} titled "interests" (absent
 *       in the canonical sample → skipped).</li>
 *   <li><b>Education</b> ← the Education {@link EntriesSection}.</li>
 *   <li><b>Expertise</b> ← the {@link SkillsSection} group categories (the
 *       category labels read as an expertise list), beneath the badge image.</li>
 *   <li><b>Skills</b> ← the {@link SkillsSection} entries flattened into
 *       {@link SkillBar}s; entries with a level draw a bar, name-only entries
 *       render as a label.</li>
 *   <li><b>Social</b> ← {@link CvIdentity#links()} as icon rows.</li>
 *   <li><b>Profile</b> ← the summary / profile {@link ParagraphSection}.</li>
 *   <li><b>Experience</b> ← the Experience {@link EntriesSection}, split
 *       across the two pages.</li>
 *   <li><b>Awards</b> ← a {@link RowsSection} titled "awards" (name = row
 *       label, meta = row body); absent → skipped.</li>
 *   <li><b>References</b> ← a {@link RowsSection} titled "references"; absent
 *       → skipped.</li>
 * </ul>
 *
 * <p>Absent sidebar/main sections are skipped rather than crashing, so the
 * preset renders cleanly against documents that omit Interests, Awards,
 * References, or Social.</p>
 *
 * <h2>Page chrome</h2>
 *
 * <p>Unlike the sidebar presets, Mint draws no {@code pageBackgrounds}: the
 * page is white and the two-column structure comes purely from the per-page
 * weighted {@code addRow}. The reusable drawing — icon rows, skill bars — is
 * delegated to {@link IconTextRow} and {@link SkillBar}; the preset only
 * orchestrates page composition, section mapping, and the Awards / References
 * grids (which are page-composition concerns local to this layout).</p>
 */
public final class MintEditorial {

    /** Stable template identifier. */
    public static final String ID = "mint-editorial";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Mint Editorial";

    /** Recommended symmetric page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /** Sidebar column weight; main column gets {@code 1 - SIDEBAR_WEIGHT}. */
    private static final double SIDEBAR_WEIGHT = 0.31;

    /** Main column weight. */
    private static final double MAIN_WEIGHT = 1.0 - SIDEBAR_WEIGHT;

    /** Horizontal gap (points) between the sidebar and main columns. */
    private static final double COLUMN_GAP = 40.0;

    /**
     * Vertical gap (points) between consecutive blocks within a column.
     * Each block is wrapped in its own sub-section so this gap applies
     * <em>between</em> blocks only — never between the leaf paragraphs /
     * lines inside a block (whose rhythm is margin-driven). Flattening all
     * leaves into one section would multiply this gap across every child
     * and overflow the atomic page row.
     */
    private static final double BLOCK_GAP = 22.0;

    /** Visual gap (points) between the two grid columns in Awards / References. */
    private static final double GRID_COLUMN_GAP = 24.0;

    /** Experience entries rendered on page 1; the rest go to page 2. */
    private static final int EXPERIENCE_PAGE_ONE = 2;

    /** Expertise category labels shown beneath the badge. */
    private static final int EXPERTISE_LIMIT = 6;

    /** Skill bars rendered in the page-2 sidebar. */
    private static final int SKILL_LIMIT = 6;

    /** Inline contact / social icon edge length (points). */
    private static final double CONTACT_ICON_SIZE = 9.0;

    /** Social icon edge length (points) — the filled badges read larger. */
    private static final double SOCIAL_ICON_SIZE = 12.0;

    /** Expertise badge edge length (points). */
    private static final double BADGE_SIZE = 36.0;

    private static final String ICON_ROOT = "/templates/cv/mint-editorial/icons/";
    private static final Map<String, byte[]> ICON_CACHE = new ConcurrentHashMap<>();

    private static final List<String> INTERESTS_KEYS =
            List.of("interests", "interest");
    private static final List<String> EDUCATION_KEYS =
            List.of("education", "certifications");
    private static final List<String> SKILL_KEYS =
            List.of("skills", "technical skills", "expertise");
    private static final List<String> SUMMARY_KEYS =
            List.of("profile", "professional profile", "summary",
                    "professional summary");
    private static final List<String> EXPERIENCE_KEYS =
            List.of("experience", "employment", "professional experience",
                    "work");
    private static final List<String> AWARDS_KEYS =
            List.of("awards", "award", "honours", "honors");
    private static final List<String> REFERENCES_KEYS =
            List.of("references", "reference", "referees");

    private MintEditorial() {
    }

    /** Builds the preset with its Mint Editorial theme. */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.mintEditorial());
    }

    /**
     * Builds the preset with a caller-supplied theme (share the paired
     * cover letter's theme instance for a guaranteed visual match).
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;
        private final DocumentColor accent;

        Template(CvTheme theme) {
            this.theme = theme;
            // Mint carries its accent in the palette banner slot — single
            // source shared with the paired cover letter.
            this.accent = theme.palette().banner();
        }

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            double innerWidth = document.canvas().innerWidth();
            double pageWidth = document.canvas().width();
            // Bleed the masthead accent rule edge-to-edge: draw it at full
            // page width with negative side margins that cancel the page
            // margins, so the mint rule spans the whole page (matching the
            // reference) instead of only the content column.
            double ruleBleed = (pageWidth - innerWidth) / 2.0;
            // Gap is subtracted before the weighted split (see
            // NodeDefinitionSupport row measurement), so derive the column
            // inner widths the same way for the skill-bar track and grids.
            double usable = Math.max(0.0, innerWidth - COLUMN_GAP);
            double sidebarInner = usable * SIDEBAR_WEIGHT;
            double mainInner = usable * MAIN_WEIGHT;
            double gridColumnWidth = mainInner / 2.0;

            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            CvIdentity identity = doc.identity();

            CvSection interests = SectionLookup.firstMatching(sections, INTERESTS_KEYS);
            CvSection education = SectionLookup.firstMatching(sections, EDUCATION_KEYS);
            CvSection skills = SectionLookup.firstMatching(sections, SKILL_KEYS);
            CvSection profile = SectionLookup.firstMatching(sections, SUMMARY_KEYS);
            CvSection experience = SectionLookup.firstMatching(sections, EXPERIENCE_KEYS);
            CvSection awards = SectionLookup.firstMatching(sections, AWARDS_KEYS);
            CvSection references = SectionLookup.firstMatching(sections, REFERENCES_KEYS);

            List<CvEntry> experienceEntries = entriesOf(experience);
            List<CvEntry> experiencePage1 = experienceEntries.stream()
                    .limit(EXPERIENCE_PAGE_ONE).toList();
            List<CvEntry> experiencePage2 = experienceEntries.stream()
                    .skip(EXPERIENCE_PAGE_ONE).toList();

            document.dsl()
                    .pageFlow()
                    .name("CvV2MintEditorialRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("CvV2MintEditorialHeader",
                            section -> addHeader(section, identity))
                    .addLine(line -> line
                            .name("CvV2MintEditorialHeaderRule")
                            .horizontal(pageWidth)
                            .color(accent)
                            .thickness(theme.spacing().accentRuleWidth())
                            .margin(new DocumentInsets(8, -ruleBleed, 14, -ruleBleed)))
                    .addRow("CvV2MintEditorialPageOne", row -> {
                        row.spacing(COLUMN_GAP).weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                        row.addSection("CvV2MintEditorialPageOneSidebar", sidebar -> {
                            sidebar.spacing(BLOCK_GAP).padding(DocumentInsets.zero());
                            addContact(sidebar, identity);
                            addInterests(sidebar, interests);
                            addEducation(sidebar, education);
                        });
                        row.addSection("CvV2MintEditorialPageOneMain", main -> {
                            main.spacing(BLOCK_GAP).padding(DocumentInsets.zero());
                            addProfile(main, profile);
                            addExperience(main, "Experience", experiencePage1);
                        });
                    })
                    .addRow("CvV2MintEditorialPageTwo", row -> {
                        row.spacing(COLUMN_GAP).weights(SIDEBAR_WEIGHT, MAIN_WEIGHT);
                        row.addSection("CvV2MintEditorialPageTwoSidebar", sidebar -> {
                            sidebar.spacing(BLOCK_GAP).padding(DocumentInsets.zero());
                            addExpertise(sidebar, skills);
                            addSkills(sidebar, skills, sidebarInner);
                            addSocial(sidebar, identity);
                        });
                        row.addSection("CvV2MintEditorialPageTwoMain", main -> {
                            main.spacing(BLOCK_GAP).padding(DocumentInsets.zero());
                            addExperience(main, "Experience", experiencePage2);
                            addAwards(main, awards, gridColumnWidth);
                            addReferences(main, references, gridColumnWidth);
                        });
                    })
                    .build();
        }

        // -- Header --------------------------------------------------------

        private void addHeader(SectionBuilder section, CvIdentity identity) {
            // Headline.spacedCentered sets the section's spacing + padding;
            // it renders the centred spaced-caps name as the page's loudest
            // element. The tagline follows in the mint accent.
            Headline.spacedCentered(section, identity.name(), theme);
            String jobTitle = identity.jobTitle();
            if (jobTitle != null && !jobTitle.isBlank()) {
                Subheadline.centeredSpacedCaps(section, jobTitle, taglineStyle());
            }
        }

        // -- Sidebar: Contact ---------------------------------------------

        private void addContact(SectionBuilder section, CvIdentity identity) {
            section.addSection("CvV2MintEditorialContact", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, "Contact");
                DocumentTextStyle style = contactStyle();
                String phone = identity.contact().phone();
                if (!phone.isBlank()) {
                    IconTextRow.render(block, icon("phone.png"), CONTACT_ICON_SIZE,
                            phone, style, null, DocumentInsets.bottom(13));
                }
                String email = identity.contact().email();
                if (!email.isBlank()) {
                    IconTextRow.render(block, icon("email.png"), CONTACT_ICON_SIZE,
                            email, style, new DocumentLinkOptions("mailto:" + email),
                            DocumentInsets.bottom(13));
                }
                String address = identity.contact().address();
                if (!address.isBlank()) {
                    IconTextRow.render(block, icon("location.png"), CONTACT_ICON_SIZE,
                            address, style, null, DocumentInsets.bottom(13));
                }
                for (CvLink link : identity.links()) {
                    if (link.label().isBlank()) {
                        continue;
                    }
                    DocumentLinkOptions options = link.url().isBlank()
                            ? null
                            : new DocumentLinkOptions(link.url().trim());
                    IconTextRow.render(block, icon(contactIconFile(link.label())),
                            CONTACT_ICON_SIZE, link.label(), style, options,
                            DocumentInsets.bottom(13));
                }
            });
        }

        // -- Sidebar: Interests -------------------------------------------

        private void addInterests(SectionBuilder section, CvSection interests) {
            if (!(interests instanceof RowsSection rows) || rows.rows().isEmpty()) {
                return;
            }
            section.addSection("CvV2MintEditorialInterests", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, interests.title());
                for (CvRow row : rows.rows()) {
                    addLabel(block, MarkdownInline.plainText(row.label()));
                }
            });
        }

        // -- Sidebar: Education -------------------------------------------

        private void addEducation(SectionBuilder section, CvSection education) {
            List<CvEntry> entries = entriesOf(education);
            if (entries.isEmpty()) {
                return;
            }
            section.addSection("CvV2MintEditorialEducation", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, education.title());
                DocumentTextStyle degreeStyle = labelStyle();
                DocumentTextStyle metaStyle = smallStyle();
                for (CvEntry entry : entries) {
                    block.addParagraph(p -> p
                            .text(TextOrnaments.spacedUpper(
                                    MarkdownInline.plainText(entry.title())))
                            .textStyle(degreeStyle)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.bottom(5)));
                    if (!entry.subtitle().isBlank()) {
                        block.addParagraph(p -> p
                                .text(MarkdownInline.plainText(entry.subtitle()))
                                .textStyle(metaStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.bottom(5)));
                    }
                    if (!entry.date().isBlank()) {
                        block.addParagraph(p -> p
                                .text(MarkdownInline.plainText(entry.date()))
                                .textStyle(metaStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.bottom(20)));
                    }
                }
            });
        }

        // -- Sidebar: Expertise -------------------------------------------

        private void addExpertise(SectionBuilder section, CvSection skills) {
            if (!(skills instanceof SkillsSection skillsSection)) {
                return;
            }
            List<String> categories = new ArrayList<>();
            for (SkillGroup group : skillsSection.groups()) {
                if (!group.category().isBlank()) {
                    categories.add(group.category());
                }
            }
            if (categories.isEmpty()) {
                return;
            }
            section.addSection("CvV2MintEditorialExpertise", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, "Expertise");
                block.addImage(image -> image
                        .name("CvV2MintEditorialExpertiseBadge")
                        .source(icon("expertise-badge.png"))
                        .size(BADGE_SIZE, BADGE_SIZE)
                        .margin(DocumentInsets.bottom(18)));
                for (String category : categories.stream().limit(EXPERTISE_LIMIT).toList()) {
                    addLabel(block, category);
                }
            });
        }

        // -- Sidebar: Skills (bars) ---------------------------------------

        private void addSkills(SectionBuilder section, CvSection skills,
                               double trackWidth) {
            if (!(skills instanceof SkillsSection skillsSection)) {
                return;
            }
            List<CvSkill> flattened = new ArrayList<>();
            for (SkillGroup group : skillsSection.groups()) {
                flattened.addAll(group.entries());
            }
            if (flattened.isEmpty()) {
                return;
            }
            section.addSection("CvV2MintEditorialSkills", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, "Skills");
                for (CvSkill skill : flattened.stream().limit(SKILL_LIMIT).toList()) {
                    SkillBar.render(block, skill, trackWidth, theme);
                }
            });
        }

        // -- Sidebar: Social ----------------------------------------------

        private void addSocial(SectionBuilder section, CvIdentity identity) {
            if (identity.links().isEmpty()) {
                return;
            }
            section.addSection("CvV2MintEditorialSocial", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, "Social");
                DocumentTextStyle style = labelStyle();
                for (CvLink link : identity.links()) {
                    if (link.label().isBlank()) {
                        continue;
                    }
                    DocumentLinkOptions options = link.url().isBlank()
                            ? null
                            : new DocumentLinkOptions(link.url().trim());
                    IconTextRow.render(block, icon(socialIconFile(link.label())),
                            SOCIAL_ICON_SIZE, link.label(), style, options,
                            DocumentInsets.bottom(11));
                }
            });
        }

        // -- Main: Profile -------------------------------------------------

        private void addProfile(SectionBuilder section, CvSection profile) {
            if (!(profile instanceof ParagraphSection paragraph)
                    || paragraph.body().isBlank()) {
                return;
            }
            section.addSection("CvV2MintEditorialProfile", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, "Profile");
                DocumentTextStyle bodyStyle = bodyStyle();
                block.addParagraph(p -> p
                        .textStyle(bodyStyle)
                        .lineSpacing(theme.typography().bodyLineSpacing())
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.bottom(12))
                        .rich(rich -> MarkdownInline.appendTrimmed(rich,
                                paragraph.body(), bodyStyle)));
            });
        }

        // -- Main: Experience ---------------------------------------------

        private void addExperience(SectionBuilder section, String title,
                                   List<CvEntry> entries) {
            if (entries.isEmpty()) {
                return;
            }
            section.addSection("CvV2MintEditorialExperience", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, title);
                DocumentTextStyle titleStyle = labelStyle();
                DocumentTextStyle metaStyle = smallStyle();
                DocumentTextStyle bodyStyle = bodyStyle();
                for (CvEntry entry : entries) {
                    block.addParagraph(p -> p
                            .text(TextOrnaments.spacedUpper(
                                    MarkdownInline.plainText(entry.title())))
                            .textStyle(titleStyle)
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.bottom(5)));
                    String meta = composeMeta(entry);
                    if (!meta.isBlank()) {
                        block.addParagraph(p -> p
                                .text(meta)
                                .textStyle(metaStyle)
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.bottom(8)));
                    }
                    // The body may carry trailing markdown bullet lines
                    // ("- highlight"). Render the prose part as a paragraph
                    // and the bullet lines as a real bullet list. Bodies
                    // without bullet lines (the canonical sample) take the
                    // plain-paragraph path unchanged.
                    BodyParts parts = splitBody(entry.body());
                    boolean hasBullets = !parts.bullets().isEmpty();
                    if (!parts.prose().isBlank()) {
                        block.addParagraph(p -> p
                                .textStyle(bodyStyle)
                                .lineSpacing(theme.typography().bodyLineSpacing())
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.bottom(hasBullets ? 6 : 18))
                                .rich(rich -> MarkdownInline.appendTrimmed(rich,
                                        parts.prose(), bodyStyle)));
                    }
                    if (hasBullets) {
                        block.addList(list -> list
                                .bullet()
                                .items(parts.bullets())
                                .textStyle(smallStyle())
                                .lineSpacing(theme.typography().bodyLineSpacing())
                                .margin(new DocumentInsets(0, 0, 18, 12)));
                    }
                }
            });
        }

        // -- Main: Awards (2-col grid) ------------------------------------

        private void addAwards(SectionBuilder section, CvSection awards,
                               double gridColumnWidth) {
            if (!(awards instanceof RowsSection rows) || rows.rows().isEmpty()) {
                return;
            }
            List<CvRow> entries = rows.rows();
            DocumentTableStyle nameLeft = cellStyle(labelStyle(), 4, GRID_COLUMN_GAP);
            DocumentTableStyle nameRight = cellStyle(labelStyle(), 4, 0);
            DocumentTableStyle metaLeft = cellStyle(smallStyle(), 18, GRID_COLUMN_GAP);
            DocumentTableStyle metaRight = cellStyle(smallStyle(), 18, 0);
            DocumentTableStyle metaLeftLast = cellStyle(smallStyle(), 0, GRID_COLUMN_GAP);
            DocumentTableStyle metaRightLast = cellStyle(smallStyle(), 0, 0);

            section.addSection("CvV2MintEditorialAwards", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, awards.title());
                block.addTable(table -> {
                    table.name("CvV2MintEditorialAwardsGrid")
                            .columns(DocumentTableColumn.fixed(gridColumnWidth),
                                    DocumentTableColumn.fixed(gridColumnWidth))
                            .padding(DocumentInsets.zero())
                            .margin(DocumentInsets.zero());
                    int pairs = (entries.size() + 1) / 2;
                    for (int pairIndex = 0; pairIndex < pairs; pairIndex++) {
                        CvRow left = entries.get(pairIndex * 2);
                        CvRow right = pairIndex * 2 + 1 < entries.size()
                                ? entries.get(pairIndex * 2 + 1)
                                : null;
                        boolean lastPair = pairIndex == pairs - 1;
                        table.rowCells(
                                gridCell(TextOrnaments.spacedUpper(
                                        MarkdownInline.plainText(left.label())), nameLeft),
                                gridCell(right == null ? "" : TextOrnaments.spacedUpper(
                                        MarkdownInline.plainText(right.label())), nameRight));
                        table.rowCells(
                                gridCell(MarkdownInline.plainText(left.body()),
                                        lastPair ? metaLeftLast : metaLeft),
                                gridCell(right == null ? "" : MarkdownInline.plainText(right.body()),
                                        lastPair ? metaRightLast : metaRight));
                    }
                });
            });
        }

        // -- Main: References (2-col grid) --------------------------------

        private void addReferences(SectionBuilder section, CvSection references,
                                   double gridColumnWidth) {
            if (!(references instanceof RowsSection rows) || rows.rows().isEmpty()) {
                return;
            }
            List<CvRow> entries = rows.rows();
            DocumentTableStyle nameLeft = cellStyle(labelStyle(), 4, GRID_COLUMN_GAP);
            DocumentTableStyle nameRight = cellStyle(labelStyle(), 4, 0);
            DocumentTableStyle subLeft = cellStyle(smallStyle(), 3, GRID_COLUMN_GAP);
            DocumentTableStyle subRight = cellStyle(smallStyle(), 3, 0);
            DocumentTableStyle lastLeft = cellStyle(smallStyle(), 18, GRID_COLUMN_GAP);
            DocumentTableStyle lastRight = cellStyle(smallStyle(), 18, 0);
            DocumentTableStyle lastLeftEnd = cellStyle(smallStyle(), 0, GRID_COLUMN_GAP);
            DocumentTableStyle lastRightEnd = cellStyle(smallStyle(), 0, 0);

            section.addSection("CvV2MintEditorialReferences", block -> {
                block.spacing(0).padding(DocumentInsets.zero());
                addBlockHeading(block, references.title());
                block.addTable(table -> {
                    table.name("CvV2MintEditorialReferencesGrid")
                            .columns(DocumentTableColumn.fixed(gridColumnWidth),
                                    DocumentTableColumn.fixed(gridColumnWidth))
                            .padding(DocumentInsets.zero())
                            .margin(DocumentInsets.zero());
                    int pairs = (entries.size() + 1) / 2;
                    for (int pairIndex = 0; pairIndex < pairs; pairIndex++) {
                        CvRow left = entries.get(pairIndex * 2);
                        CvRow right = pairIndex * 2 + 1 < entries.size()
                                ? entries.get(pairIndex * 2 + 1)
                                : null;
                        boolean lastPair = pairIndex == pairs - 1;
                        // Name row (spaced-caps bold).
                        table.rowCells(
                                gridCell(TextOrnaments.spacedUpper(
                                        MarkdownInline.plainText(left.label())), nameLeft),
                                gridCell(right == null ? "" : TextOrnaments.spacedUpper(
                                        MarkdownInline.plainText(right.label())), nameRight));
                        // One table row per body line (Company / "P: phone" /
                        // email). The email line becomes a clickable mailto via
                        // a composed ParagraphNode cell — the working
                        // composed-cell shape on the PDF backend (paragraph
                        // cells render; section cells do not).
                        List<String> leftLines = bodyLines(left);
                        List<String> rightLines = right == null
                                ? List.of() : bodyLines(right);
                        int lineRows = Math.max(leftLines.size(), rightLines.size());
                        for (int i = 0; i < lineRows; i++) {
                            boolean lastLine = i == lineRows - 1;
                            DocumentTableStyle leftStyle = lastLine
                                    ? (lastPair ? lastLeftEnd : lastLeft) : subLeft;
                            DocumentTableStyle rightStyle = lastLine
                                    ? (lastPair ? lastRightEnd : lastRight) : subRight;
                            table.rowCells(
                                    referenceLineCell(
                                            i < leftLines.size() ? leftLines.get(i) : "",
                                            leftStyle),
                                    referenceLineCell(
                                            i < rightLines.size() ? rightLines.get(i) : "",
                                            rightStyle));
                        }
                    }
                });
            });
        }

        private List<String> bodyLines(CvRow row) {
            List<String> out = new ArrayList<>();
            for (String raw : row.body().split("\\R")) {
                String line = MarkdownInline.plainText(raw).strip();
                if (!line.isBlank()) {
                    out.add(line);
                }
            }
            return out;
        }

        /**
         * Builds a single reference detail cell. A line that contains an
         * email becomes a clickable {@code mailto:} link via a composed
         * {@link com.demcha.compose.document.node.ParagraphNode} cell (the
         * working composed-cell shape on the PDF backend); every other line
         * is a plain text cell.
         */
        private DocumentTableCell referenceLineCell(String line,
                                                    DocumentTableStyle cellStyle) {
            if (line == null || line.isBlank()) {
                return gridCell("", cellStyle);
            }
            String email = extractEmail(line);
            if (email == null) {
                return gridCell(line, cellStyle);
            }
            DocumentLinkOptions link = new DocumentLinkOptions("mailto:" + email);
            DocumentTextStyle linkStyle = CvTextStyles.of(
                    theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.UNDERLINE, theme.palette().muted());
            ParagraphNode linked = new ParagraphBuilder()
                    .name("CvV2MintEditorialReferenceEmail")
                    .align(TextAlign.LEFT)
                    .inlineText(line, linkStyle, link)
                    .build();
            return DocumentTableCell.node(linked).withStyle(cellStyle);
        }

        // -- Shared block heading (spaced-caps accent title) --------------

        private void addBlockHeading(SectionBuilder block, String title) {
            if (title == null || title.isBlank()) {
                return;
            }
            block.addParagraph(p -> p
                    .text(TextOrnaments.spacedUpper(title))
                    .textStyle(headingStyle())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.bottom(18)));
        }

        private void addLabel(SectionBuilder section, String text) {
            if (text == null || text.isBlank()) {
                return;
            }
            section.addParagraph(p -> p
                    .text(TextOrnaments.spacedUpper(text))
                    .textStyle(labelStyle())
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.bottom(14)));
        }

        private DocumentTableCell gridCell(String text, DocumentTableStyle style) {
            return DocumentTableCell.text(text == null ? "" : text).withStyle(style);
        }

        // -- Style factories ----------------------------------------------

        private DocumentTextStyle taglineStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.BOLD, accent);
        }

        private DocumentTextStyle headingStyle() {
            return CvTextStyles.of(theme.typography().headlineFont(),
                    theme.typography().sizeBanner(),
                    DocumentTextDecoration.BOLD, accent);
        }

        private DocumentTextStyle labelStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryTitle(),
                    DocumentTextDecoration.BOLD, theme.palette().ink());
        }

        private DocumentTextStyle bodyStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeBody(),
                    DocumentTextDecoration.DEFAULT, theme.palette().ink());
        }

        private DocumentTextStyle contactStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeContact(),
                    DocumentTextDecoration.DEFAULT, theme.palette().muted());
        }

        private DocumentTextStyle smallStyle() {
            return CvTextStyles.of(theme.typography().bodyFont(),
                    theme.typography().sizeEntryDate(),
                    DocumentTextDecoration.DEFAULT, theme.palette().muted());
        }

        /**
         * No-border, no-fill table cell style — a zero-width white stroke
         * and a white fill suppress the engine's default 1pt black cell
         * border. {@code rightPadding} drives the inter-column gap on the
         * left column only (keep the right column at zero).
         */
        private DocumentTableStyle cellStyle(DocumentTextStyle textStyle,
                                             double bottomPadding,
                                             double rightPadding) {
            return DocumentTableStyle.builder()
                    .textStyle(textStyle)
                    .padding(new DocumentInsets(0, rightPadding, bottomPadding, 0))
                    .stroke(DocumentStroke.of(DocumentColor.WHITE, 0))
                    .fillColor(DocumentColor.WHITE)
                    .build();
        }

        private DocumentImageData icon(String fileName) {
            return DocumentImageData.fromBytes(
                    ICON_CACHE.computeIfAbsent(ICON_ROOT + fileName,
                            MintEditorial::readIconBytes));
        }
    }

    // -- Static helpers ----------------------------------------------------

    private static String composeMeta(CvEntry entry) {
        String subtitle = MarkdownInline.plainText(entry.subtitle());
        String date = MarkdownInline.plainText(entry.date());
        if (subtitle.isBlank()) {
            return date;
        }
        if (date.isBlank()) {
            return subtitle;
        }
        return subtitle + " | " + date;
    }

    private static List<CvEntry> entriesOf(CvSection section) {
        if (section instanceof EntriesSection entries) {
            return entries.entries();
        }
        return List.of();
    }

    /**
     * Splits an experience body into a leading prose block and trailing
     * markdown bullet lines (lines beginning with {@code "- "} or
     * {@code "* "}). Bodies with no bullet lines yield the whole text as
     * prose and an empty bullet list, so the canonical sample renders
     * exactly as before.
     */
    private static BodyParts splitBody(String body) {
        if (body == null || body.isBlank()) {
            return new BodyParts("", List.of());
        }
        List<String> prose = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        for (String rawLine : body.split("\\R")) {
            String line = rawLine.strip();
            if (line.startsWith("- ") || line.startsWith("* ")) {
                String item = line.substring(2).strip();
                if (!item.isBlank()) {
                    bullets.add(item);
                }
            } else if (!line.isBlank()) {
                prose.add(line);
            }
        }
        return new BodyParts(String.join(" ", prose), List.copyOf(bullets));
    }

    /** Prose + bullet split of an experience entry body. */
    private record BodyParts(String prose, List<String> bullets) {
    }

    /**
     * Returns the bare email address contained in {@code line} (e.g. the
     * {@code "hello@email.com"} inside {@code "E: hello@email.com"}), or
     * {@code null} when the line carries no email-shaped token.
     */
    private static String extractEmail(String line) {
        if (line == null) {
            return null;
        }
        for (String token : line.split("\\s+")) {
            int at = token.indexOf('@');
            if (at > 0 && token.indexOf('.', at) > at + 1) {
                return token;
            }
        }
        return null;
    }

    /** Maps a contact link label to its small inline glyph file. */
    private static String contactIconFile(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("linkedin")) {
            return "linkedin.png";
        }
        if (normalized.contains("twitter")) {
            return "twitter.png";
        }
        if (normalized.contains("facebook")) {
            return "facebook.png";
        }
        if (normalized.contains("pinterest")) {
            return "pinterest.png";
        }
        // GitHub, portfolio, personal site, etc. → the globe glyph.
        return "website.png";
    }

    /** Maps a social link label to its filled-badge glyph file. */
    private static String socialIconFile(String label) {
        String normalized = SectionLookup.normalize(label);
        if (normalized.contains("twitter")) {
            return "twitter.png";
        }
        if (normalized.contains("facebook")) {
            return "facebook.png";
        }
        if (normalized.contains("pinterest")) {
            return "pinterest.png";
        }
        if (normalized.contains("linkedin")) {
            return "linkedin.png";
        }
        // GitHub, portfolio, personal site, etc. → the globe glyph.
        return "website.png";
    }

    private static byte[] readIconBytes(String resourcePath) {
        try (InputStream input = MintEditorial.class
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing mint editorial icon: " + resourcePath);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read mint editorial icon: " + resourcePath, e);
        }
    }
}
