package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvKind;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.SectionRole;
import com.demcha.compose.document.templates.cv.data.Slot;
import com.demcha.compose.document.templates.cv.presets.CvTemplates;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A runtime module handed to a preset under a heading that preset knows must reach
 * the page — on <em>every</em> preset in the catalogue, not a hand-picked list.
 *
 * <p>The catalogue is the source of presets on purpose. Sibling coverage tests name
 * their presets in three hand-written {@code Stream.of(...)} lists, so a preset added
 * to {@link CvTemplates} joins the shipped set without joining any gate. Reading
 * {@link CvTemplates#all()} here means a new preset is held to this from the day it
 * is registered, and a preset that stops rendering modules cannot escape by being
 * left out of a list.</p>
 *
 * <p>The case is narrow and deliberate. Routing a module by its <em>role</em> is a
 * meaning the template would be inferring, and the constructor layer stopped doing
 * it. Routing it by its <em>heading</em> is not: the heading is the document author's
 * own word for the block, matched exactly as a hand-written section's is. Losing that
 * distinction cost every module handed to a preset that composes fixed slots and
 * keeps no {@code SectionAllocation.remaining()} tail — ClassicSerif, CompactMono,
 * EngineeringResume, NordicClean and Panel dropped heading and body together, with
 * nothing in the API or the PDF reporting it.</p>
 *
 * <p><b>What this does not claim.</b> A module under a heading no preset recognises
 * still reaches the page only on presets that keep a {@code remaining()} tail. That
 * gap is older than the constructor layer and is the unfinished half of the
 * no-silent-loss work; it is not asserted here because asserting it would fail, and
 * a test that documents a gap is worth more than one that pretends it is closed.</p>
 *
 * @author Artem Demchyshyn
 */
class ModuleReachesEveryPresetTest {

    /** A phrase no preset chrome contains, so finding it means the module drew. */
    private static final String MARKER = "Sentinel Rendering GmbH";

    static Stream<DocumentTemplate<CvDocument>> everyPresetInTheCatalogue() {
        return CvTemplates.all().stream();
    }

    @ParameterizedTest
    @MethodSource("everyPresetInTheCatalogue")
    void aModuleUnderAKnownHeadingReachesThePage(DocumentTemplate<CvDocument> preset)
            throws Exception {
        ModuleSection module = ModuleSection.builder("Professional Experience",
                        SectionRole.EXPERIENCE, CvKind.ENTRIES_DATED)
                .item(CvItem.of("Senior Backend Engineer")
                        .at(MARKER)
                        .period("2021 - Present")
                        .paragraphs("Owned the rendering pipeline."))
                .build();

        String text = render(preset, module);

        assertThat(text)
                .describedAs("%s dropped a runtime module whose heading its own keyword "
                                + "list contains — heading and body together",
                        preset.getClass().getName())
                .contains(MARKER)
                .contains("Owned the rendering pipeline.");

        // The item title is asserted separately because several presets style a position
        // in caps and one letter-spaces it, so neither case nor the spaces between glyphs
        // is the template's promise. Stripping both asks the only question that matters
        // here — did the title reach the page — without pinning anyone's typography.
        assertThat(text.replace(" ", "").toLowerCase(Locale.ROOT))
                .describedAs("%s drew the module but not its item title",
                        preset.getClass().getName())
                .contains("seniorbackendengineer");
    }

    private static String render(DocumentTemplate<CvDocument> preset, ModuleSection module)
            throws Exception {
        CvDocument document = CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Jordan", "Rivera")
                        .jobTitle("Backend Engineer")
                        .contact("+1 555 0100", "jordan@example.com", "Berlin, DE")
                        .build())
                .section(Slot.MAIN, module)
                .build();

        byte[] bytes;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(24))
                .create()) {
            preset.compose(session, document);
            bytes = session.toPdfBytes();
        }
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            // Collapse the layout's own wrapping: this asks whether the text reached
            // the page, not where the engine chose to break it.
            return new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
        }
    }
}
