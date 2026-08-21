package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.cv.api.ModularCvTemplate;
import com.demcha.compose.document.templates.cv.data.BodyStyle;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.CvSection;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.presets.CvTemplates;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a CV whose every section is decided at <strong>runtime</strong> — the
 * shape of each one arrives as data rather than being chosen when the code is
 * written — and renders it through a template picked by id from
 * {@link CvTemplates#modular()}.
 *
 * <p>Output:
 * {@code examples/target/generated-pdfs/templates/cv/cv-runtime-modules-v2.pdf}.</p>
 *
 * <h2>What this shows that the other CV examples do not</h2>
 *
 * <p>Every other example here writes its sections in Java: you pick
 * {@code EntriesSection} for Experience, {@code SkillsSection} for skills, and
 * the compiler checks you. That is the right model when a person writes the CV.
 * It is the wrong one when the CV <em>arrives</em> — from a form, a JSON
 * payload, an LLM — because the shape is not known until it does, and a user who
 * has just chosen "dated entries" from a menu cannot instantiate a different
 * record per choice.</p>
 *
 * <p>{@link ModuleSection} moves that choice into a value. {@link #PAYLOAD}
 * stands in for what a form or a JSON document would give you: a title, a role,
 * a kind, and items. {@link #toSection} turns one of those into a section, and
 * it is the only mapping code needed whatever the user picked.</p>
 *
 * <h2>Three claims, each checkable in the PDF</h2>
 *
 * <ol>
 *   <li><b>The kind decides which fields are read.</b> "Experience" and
 *       "Certifications" carry <em>identical</em> items — same subtitle, same
 *       location, same period, same body. The only difference between the two
 *       modules is {@link CvKind#ENTRIES_DATED} against {@link CvKind#ENTRIES},
 *       and the only difference in the PDF is the date column.</li>
 *   <li><b>A category the catalogue has no name for needs no new type.</b>
 *       "Volunteering" carries {@link SectionRole#OTHER} and is built exactly
 *       like "Experience" — same kind, same fields — so it renders exactly like
 *       it, with a heading nobody in the library chose.</li>
 *   <li><b>The heading stays the author's.</b> This preset renames a classic
 *       section by keyword — a {@code CvSection} titled "Certifications" prints
 *       as "EDUCATION" — and deliberately does not do that to a module. The
 *       PDF says CERTIFICATIONS.</li>
 * </ol>
 *
 * <p>Swap {@link #TEMPLATE_ID} for any id in {@code CvTemplates.modular()} to
 * render the same data through a different design. That list, rather than
 * {@code CvTemplates.ids()}, is the one to offer: a template outside it composes
 * a fixed set of slots and silently drops a module it was not written for.</p>
 */
public final class CvRuntimeModulesExample {

    /**
     * The template this example renders with — an id from
     * {@code CvTemplates.modular()}.
     */
    private static final String TEMPLATE_ID = "editorial-blue";

    /**
     * Stands in for the payload a form or a JSON document would hand you: each
     * row is one section the user built, with the shape they chose for it.
     *
     * <p>Written as plain values on purpose — nothing here is a compile-time
     * decision. Parsing JSON into this shape is the caller's job; the CV layer
     * starts where this ends. Absent fields are {@code null}, as a parser would
     * leave them.</p>
     */
    private static final List<ModuleInput> PAYLOAD = List.of(
            new ModuleInput("Professional Profile", SectionRole.SUMMARY, CvKind.PARAGRAPH,
                    List.of(new ItemInput(null, null, null, null,
                            List.of("Platform engineer with ten years spent on document "
                                    + "pipelines, layout engines, and the template systems "
                                    + "other teams build on."), BodyStyle.PARAGRAPH))),

            new ModuleInput("Experience", SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED,
                    List.of(
                            new ItemInput("Principal Platform Engineer", "Acme Rendering",
                                    "London, UK", "2022 - 2025",
                                    List.of("Owns the rendering pipeline and its release train."),
                                    BodyStyle.PARAGRAPH),
                            new ItemInput("Senior Backend Engineer", "Northwind Data",
                                    "Manchester, UK", "2019 - 2022",
                                    List.of("Built the typed reporting layer behind the exports."),
                                    BodyStyle.PARAGRAPH))),

            // Claim 1, isolated: the items below use the same fields as Experience
            // above — subtitle, location, period, one prose line. ENTRIES rather
            // than ENTRIES_DATED is the only difference, and the dates vanish.
            new ModuleInput("Certifications", SectionRole.EDUCATION, CvKind.ENTRIES,
                    List.of(
                            new ItemInput("Oracle Certified Professional, Java SE", "Oracle",
                                    "Remote", "2023 - 2024",
                                    List.of("Records, sealed types, pattern matching, threads."),
                                    BodyStyle.PARAGRAPH),
                            new ItemInput("MSc Computer Science", "University of Manchester",
                                    "Manchester, UK", "2018 - 2020",
                                    List.of("Distinction, thesis on deterministic rendering."),
                                    BodyStyle.PARAGRAPH))),

            new ModuleInput("Technical Skills", SectionRole.SKILLS, CvKind.INLINE_LIST,
                    List.of(
                            new ItemInput("Languages", null, null, null,
                                    List.of("Java 21", "Kotlin", "SQL"), BodyStyle.BULLETS),
                            new ItemInput("Document & print", null, null, null,
                                    List.of("PDFBox", "font metrics", "ICC colour profiles"),
                                    BodyStyle.BULLETS))),

            new ModuleInput("Selected Projects", SectionRole.PROJECTS, CvKind.BULLETS_STACKED,
                    List.of(
                            new ItemInput("GraphCompose", null, null, null,
                                    List.of("Declarative Java PDF layout engine with a "
                                            + "semantic authoring DSL"), BodyStyle.PARAGRAPH),
                            new ItemInput("LayoutLint", null, null, null,
                                    List.of("Static analyser that flags fragile authoring "
                                            + "patterns before they ship"), BodyStyle.PARAGRAPH))),

            // Claim 2, isolated: same kind and same field set as Experience, and
            // a role the catalogue has no name for. It renders like Experience
            // because nothing about it is special-cased.
            new ModuleInput("Volunteering", SectionRole.OTHER, CvKind.ENTRIES_DATED,
                    List.of(new ItemInput("Workshop mentor", "Rails Girls Berlin",
                            "Berlin, DE", "2019 - 2021",
                            List.of("Ran three weekend workshops a year for newcomers."),
                            BodyStyle.PARAGRAPH))));

    private CvRuntimeModulesExample() {
    }

    /**
     * Renders the payload above through {@link #TEMPLATE_ID}.
     *
     * @return absolute path of the rendered PDF
     * @throws Exception if rendering fails
     */
    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare(
                "templates/cv", "cv-runtime-modules-v2.pdf");

        List<CvSection> sections = new ArrayList<>();
        for (ModuleInput input : PAYLOAD) {
            sections.add(toSection(input));
        }

        CvDocument doc = CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Platform Engineer")
                        .contact("+44 20 5555 1000", "jordan.rivera@example.com", "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .sections(sections)
                .build();

        // Looked up through modular(), not ids(): these are the templates that
        // promise to draw a module they were not written for, and the promise is
        // the type. A template outside this list would drop every section above.
        ModularCvTemplate template = CvTemplates.modular().stream()
                .filter(candidate -> TEMPLATE_ID.equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        TEMPLATE_ID + " is not in CvTemplates.modular()"));
        float margin = CvTemplates.recommendedMargin(TEMPLATE_ID)
                .orElseThrow(() -> new IllegalStateException("no margin for " + TEMPLATE_ID))
                .floatValue();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(margin, margin, margin, margin)
                .create()) {
            template.compose(document, doc);
            document.buildPdf();
        }
        return outputFile;
    }

    /**
     * The whole mapping layer: one payload row becomes one section, whatever
     * shape the user chose for it.
     *
     * <p>Nothing here checks for blanks — {@link CvItem} normalises {@code null}
     * and empty strings itself, which is what makes a parsed payload safe to
     * hand over field by field.</p>
     *
     * @param input one section as it arrived
     * @return the section to hand to a template
     */
    private static CvSection toSection(ModuleInput input) {
        ModuleSection.Builder module =
                ModuleSection.builder(input.title(), input.role(), input.kind());
        for (ItemInput item : input.items()) {
            // A paragraph module carries its prose on an item that has no title
            // of its own; every other kind reads the title, so nothing is
            // substituted for it there.
            String title = item.title() == null && input.kind() == CvKind.PARAGRAPH
                    ? input.title()
                    : item.title();
            module.item(CvItem.of(title)
                    .at(item.subtitle())
                    .in(item.location())
                    .period(item.period())
                    .body(item.body(), item.bodyStyle()));
        }
        return module.build();
    }

    /** One section as a form or JSON payload would carry it. */
    private record ModuleInput(String title, SectionRole role, CvKind kind,
                               List<ItemInput> items) {
    }

    /**
     * One item. Every field is optional but the title, absent ones arrive as
     * {@code null}, and whatever the section's {@link CvKind} does not read is
     * ignored rather than dropped from the data.
     *
     * @param bodyStyle whether the body reads as sentences or as discrete
     *                  points — a separate axis from the kind, and one only the
     *                  author of the data knows
     */
    private record ItemInput(String title, String subtitle, String location,
                             String period, List<String> body, BodyStyle bodyStyle) {
    }

    /**
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
