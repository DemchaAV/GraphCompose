package com.demcha.examples.templates.cv.v2;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
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
 * written — and renders it through a preset picked by id from
 * {@link CvTemplates}.
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
 * below stands in for what a form or a JSON document would give you: a title, a
 * role, a kind, and items. {@link #toSection} turns one of those into a section
 * — and it is the only mapping code needed, whatever the user picked.</p>
 *
 * <h2>Three things worth looking at in the PDF</h2>
 *
 * <ol>
 *   <li><b>The kind decides which fields are read.</b> "Certifications" and
 *       "Experience" carry the same item fields; the first is
 *       {@link CvKind#ENTRIES} and prints no dates, the second is
 *       {@link CvKind#ENTRIES_DATED} and prints them. Same data, one value
 *       different.</li>
 *   <li><b>A category the library never heard of.</b> "Volunteering" is not a
 *       role in the catalogue — its {@link SectionRole} is {@code OTHER} — and it
 *       is shaped exactly like Education without a new type existing for it.</li>
 *   <li><b>The heading is the author's.</b> Nothing here renames a section to a
 *       preset's own vocabulary.</li>
 * </ol>
 *
 * <p>Swap {@link #TEMPLATE_ID} for any id in {@code CvTemplates.ids()} to render
 * the same data through a different design. {@code CvTemplates.modular()} is the
 * list to offer a user when the CV is assembled this way: those templates promise
 * to draw a module they were not written for.</p>
 */
public final class CvRuntimeModulesExample {

    /**
     * The preset this example renders with. Any id from
     * {@code CvTemplates.ids()} works; the modular ones are listed by
     * {@code CvTemplates.modular()}.
     */
    private static final String TEMPLATE_ID = "boxed-sections";

    /**
     * Stands in for the payload a form or a JSON document would hand you: each
     * row is one section the user built, with the shape they chose for it.
     *
     * <p>Written as plain values on purpose — the point is that nothing here is
     * a compile-time decision. Parsing JSON into this shape is the caller's job;
     * the CV layer starts where this ends.</p>
     */
    private static final List<ModuleInput> PAYLOAD = List.of(
            new ModuleInput("Professional Profile", SectionRole.SUMMARY, CvKind.PARAGRAPH,
                    List.of(new ItemInput("", "", "", "",
                            List.of("Platform engineer with ten years spent on document "
                                    + "pipelines, layout engines, and the template systems "
                                    + "other teams build on."), true))),

            new ModuleInput("Experience", SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED,
                    List.of(
                            new ItemInput("Principal Platform Engineer", "Acme Rendering",
                                    "London, UK", "2022 - present",
                                    List.of("Owns the rendering pipeline and its release train.",
                                            "Cut p99 render latency from 1.4s to 380ms."), false),
                            new ItemInput("Senior Backend Engineer", "Northwind Data",
                                    "Manchester, UK", "2019 - 2022",
                                    List.of("Built the typed reporting layer behind the "
                                            + "billing exports."), false))),

            // Same item fields as Experience above; ENTRIES rather than
            // ENTRIES_DATED, so the period is simply not read.
            new ModuleInput("Certifications", SectionRole.EDUCATION, CvKind.ENTRIES,
                    List.of(
                            new ItemInput("Oracle Certified Professional, Java SE",
                                    "Oracle", "", "2023",
                                    List.of("Records, sealed types, pattern matching, "
                                            + "virtual threads."), true),
                            new ItemInput("MSc Computer Science",
                                    "University of Manchester", "", "2021",
                                    List.of("Distinction."), true))),

            new ModuleInput("Technical Skills", SectionRole.SKILLS, CvKind.INLINE_LIST,
                    List.of(
                            new ItemInput("Languages", "", "", "",
                                    List.of("Java 21", "Kotlin", "SQL"), false),
                            new ItemInput("Document & print", "", "", "",
                                    List.of("PDFBox", "font metrics", "ICC colour profiles"), false))),

            new ModuleInput("Selected Projects", SectionRole.PROJECTS,
                    CvKind.BULLETS_STACKED,
                    List.of(
                            new ItemInput("GraphCompose", "", "", "",
                                    List.of("Declarative Java PDF layout engine with a "
                                            + "semantic authoring DSL"), true),
                            new ItemInput("LayoutLint", "", "", "",
                                    List.of("Static analyser that flags fragile authoring "
                                            + "patterns before they ship"), true))),

            // No role in the catalogue names this, so it carries OTHER — and is
            // shaped exactly like the Certifications module above.
            new ModuleInput("Volunteering", SectionRole.OTHER, CvKind.ENTRIES_DATED,
                    List.of(new ItemInput("Workshop mentor", "Rails Girls Berlin",
                            "Berlin, DE", "2019 - 2021",
                            List.of("Ran three weekend workshops a year for newcomers."),
                            true))));

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
                        .contact("+44 20 5555 1000", "jordan.rivera@example.com",
                                "London, UK")
                        .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                        .link("GitHub", "https://github.com/jrivera-demo")
                        .build())
                .sections(sections.toArray(new CvSection[0]))
                .build();

        // Picking the design at runtime too: an id in, a template out.
        DocumentTemplate<CvDocument> template = CvTemplates.byId(TEMPLATE_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "unknown template id: " + TEMPLATE_ID));
        float margin = CvTemplates.recommendedMargin(TEMPLATE_ID).orElse(48.0).floatValue();

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
     * @param input one section as it arrived
     * @return the section to hand to a template
     */
    private static CvSection toSection(ModuleInput input) {
        ModuleSection.Builder module =
                ModuleSection.builder(input.title(), input.role(), input.kind());
        for (ItemInput item : input.items()) {
            CvItem built = CvItem.of(item.title().isBlank() ? input.title() : item.title());
            if (!item.subtitle().isBlank()) {
                built = built.at(item.subtitle());
            }
            if (!item.location().isBlank()) {
                built = built.in(item.location());
            }
            if (!item.period().isBlank()) {
                built = built.period(item.period());
            }
            if (!item.body().isEmpty()) {
                // Prose reads as prose and points read as points; the kind
                // decides how the section is laid out around them.
                built = item.prose()
                        ? built.paragraphs(item.body().toArray(new String[0]))
                        : built.bullets(item.body().toArray(new String[0]));
            }
            module.item(built);
        }
        return module.build();
    }

    /** One section as a form or JSON payload would carry it. */
    private record ModuleInput(String title, SectionRole role, CvKind kind,
                               List<ItemInput> items) {
    }

    /**
     * One item; every field optional but the title, and unread fields ignored.
     *
     * @param prose whether the body reads as sentences or as discrete points —
     *              a separate axis from the section's {@link CvKind}, and one the
     *              payload carries because only its author knows
     */
    private record ItemInput(String title, String subtitle, String location,
                             String period, List<String> body, boolean prose) {
    }

    /**
     * @param args ignored
     * @throws Exception if rendering fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
